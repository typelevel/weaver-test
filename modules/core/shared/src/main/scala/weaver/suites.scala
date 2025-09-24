package weaver

import scala.concurrent.duration.FiniteDuration

import cats.data.Chain
import cats.data.NonEmptyList
import cats.effect.{ Async, Resource }
import cats.syntax.all._

import fs2.Stream
import org.portablescala.reflect.annotation.EnableReflectiveInstantiation
import org.junit.runner.RunWith

// Just a non-parameterized marker trait to help SBT's test detection logic.
@EnableReflectiveInstantiation
trait BaseSuiteClass {}

trait Suite[F[_]] extends BaseSuiteClass {
  def name: String
  def spec(args: List[String]): Stream[F, TestOutcome]
}

// A version of EffectSuite that has a type member instead of a type parameter.
protected[weaver] trait EffectSuiteAux {
  type EffectType[A]
  implicit protected def effect: Async[EffectType]
}

// format: off
trait EffectSuite[F[_]] extends Suite[F] with EffectSuiteAux with SourceLocation.Here { self =>

  final type EffectType[A] = F[A]
  implicit protected def effectCompat: EffectCompat[F]
  implicit final protected def effect: Async[F] = effectCompat.effect

  override def name : String = self.getClass.getName.replace("$", "")

  protected def adaptRunError: PartialFunction[Throwable, Throwable] = PartialFunction.empty

  final def run(args : List[String])(report : TestOutcome => F[Unit]) : F[Unit] =
    spec(args).evalMap(report).compile.drain.adaptErr(adaptRunError)
}

object EffectSuite {

  trait Provider[F[_]]{
    def getSuite : EffectSuite[F]
  }

}

@RunWith(classOf[weaver.junit.WeaverRunner])
abstract class RunnableSuite[F[_]] extends EffectSuite[F] {
  implicit protected def effectCompat: UnsafeRun[EffectType]
  private[weaver] def getEffectCompat: UnsafeRun[EffectType] = effectCompat
  /** A list of all test names defined in this suite. */
  def plan: List[TestName]

  def isCI: Boolean = System.getenv("CI") == "true"

  /** Determines the tests to be run by the [[weaver.junit.WeaverRunner]] */
  private[weaver] final def analyze: WeaverRunnerAnalysisResult = {
    RunnableSuite.analyze(name, plan, isCI, Nil) match {
      case TagAnalysisResult.Outcomes(ignored, outcomes) =>
        // The JUnit runner is being used in a CI environment.
        WeaverRunnerAnalysisResult(ignored.map(_.name),
                                   outcomes.map(_.name).toList)
      case TagAnalysisResult.FilteredTests(ignored, toBeRun) =>
        WeaverRunnerAnalysisResult(ignored.map(_.name), toBeRun.map(_.name))
    }
  }

  /** Called by the [[weaver.junit.WeaverRunner]] runner to run tests. */
  private[weaver] def runUnsafe(report: TestOutcome => Unit): Unit =
    effectCompat.unsafeRunSync(run(List.empty)(outcome =>
      effectCompat.effect.delay(report(outcome))))

  /**
   * Evaluates a subset of tests in the `plan`.
   *
   * All tests present in testNames should have outcomes present in the output
   * stream. The output may contain additional tests that are not present in
   * testNames. For example, the `weaver.discipline.DisciplineFSuite` "expands"
   * each test into multiple additional tests. However these additinal tests
   * must be present in the `plan` before their outcomes are outputted by the
   * stream.
   *
   * @param testNames
   *   A subset of the tests defined in `plan`. The `plan` is first filtered
   *   using tags (e.g `.only`) and arguments e.g. `--only`.
   */
  def eval(testNames: NonEmptyList[TestName]): Stream[F, TestOutcome]

  final override def spec(args: List[String]): Stream[F, TestOutcome] = {
    RunnableSuite.analyze(name, plan, isCI, args) match {
      case TagAnalysisResult.Outcomes(_, outcomes) =>
        Stream.emits(outcomes.toList)
      case TagAnalysisResult.FilteredTests(_, testNames) =>
        testNames.toNel match {
          case Some(testNamesNel) => eval(testNamesNel)
          case None               => Stream.empty
        }
    }
  }
}

private[weaver] object RunnableSuite {

  private[weaver] def analyze(
      suiteName: String,
      testNames: List[TestName],
      isCI: Boolean,
      args: List[String]): TagAnalysisResult = {

    val (taggedOnly, notTaggedOnly) =
      testNames.partition(_.tags(TestName.Tags.only))

    taggedOnly.toNel match {
      case Some(taggedOnlyNel) if isCI =>
        // The only keyword was used in CI. Fail these tests early and ignore the rest.
        TagAnalysisResult.Outcomes(notTaggedOnly,
                                   taggedOnlyNel.map(onlyNotOnCiFailure))
      case _ =>
        val (taggedOnlyAndIgnored, taggedOnlyAndNotIgnored) =
          taggedOnly.partition(_.tags(TestName.Tags.ignore))
        taggedOnlyAndNotIgnored.toNel match {
          case Some(taggedOnlyAndNotIgnoredNel) =>
            // Some tests are tagged with `only`. Run them.
            TagAnalysisResult.FilteredTests(
              taggedOnlyAndIgnored ++ notTaggedOnly,
              taggedOnlyAndNotIgnoredNel.toList)
          case None =>
            // There are no tests tagged with `only`.
            // Use argument filters to determine the tests to be run.
            val (ignored, notIgnored) =
              notTaggedOnly.partition(_.tags(TestName.Tags.ignore))
            val argsFilter = Filters.filterTests(suiteName)(args)
            val (testsToBeRun, testsFilteredOutByArgs) =
              notIgnored.partition(argsFilter)
            TagAnalysisResult.FilteredTests(
              taggedOnlyAndIgnored ++ ignored ++ testsFilteredOutByArgs,
              testsToBeRun
            )
        }
    }
  }

  private[this] def onlyNotOnCiFailure(test: TestName): TestOutcome = {
    val result = Result.OnlyTagNotAllowedInCI(location = test.location)
    TestOutcome(
      name = test.name,
      duration = FiniteDuration(0, "ns"),
      result = result,
      log = Chain.empty
    )
  }

}

private[weaver] sealed trait TagAnalysisResult

private[weaver] object TagAnalysisResult {
  case class Outcomes(
      ignored: List[TestName],
      outcomes: NonEmptyList[TestOutcome]) extends TagAnalysisResult
  case class FilteredTests(ignored: List[TestName], toBeRun: List[TestName])
      extends TagAnalysisResult
}

private[weaver] case class WeaverRunnerAnalysisResult(
    ignored: List[String],
    toBeRun: List[String])

abstract class MutableFSuite[F[_]] extends RunnableSuite[F]  {

  type Res
  def sharedResource : Resource[F, Res]
  def maxParallelism : Int = 10000

  protected def registerTest(name: TestName)(f: Res => F[TestOutcome]): Unit =
    synchronized {
      if (isInitialized) throw initError
      testSeq = testSeq :+ (name -> f)
    }

  def pureTest(name: TestName)(run : => Expectations) :  Unit = registerTest(name)(_ => Test(name.name, effectCompat.effect.delay(run)))
  def loggedTest(name: TestName)(run: Log[F] => F[Expectations]) : Unit = registerTest(name)(_ => Test[F](name.name, log => run(log)))
  def test(name: TestName) : PartiallyAppliedTest = new PartiallyAppliedTest(name)

  class PartiallyAppliedTest(name : TestName) {
    def apply(run: => F[Expectations]) : Unit = registerTest(name)(_ => Test(name.name, run))
    def apply(run : Res => F[Expectations]) : Unit = registerTest(name)(res => Test(name.name, run(res)))
    def apply(run : (Res, Log[F]) => F[Expectations]) : Unit = registerTest(name)(res => Test[F](name.name, log => run(res, log)))

    // this alias helps using pattern matching on `Res`
    def usingRes(run : Res => F[Expectations]) : Unit = apply(run)
  }

  def eval(testNames: NonEmptyList[TestName]) = {
    synchronized {
      isInitialized = true
      val tests         = testSeq.toMap
      val parallelism   = math.max(1, maxParallelism)
      val filteredTests = testNames.map { name => tests(name) }

      for {
        resource <- Stream.resource(sharedResource)
        tests      = filteredTests.map(_.apply(resource))
        testStream = Stream.emits(tests.toList).covary[F]
        result <-
          testStream.parEvalMap(parallelism)(identity)(effectCompat.effect)
      } yield result
    }
  }

  private[this] var testSeq: Seq[(TestName, Res => F[TestOutcome])] = Seq.empty

  def plan: List[TestName] = testSeq.map(_._1).toList

  private[this] var isInitialized = false

}

trait FunSuiteAux {
  def test(name: TestName)(run: => Expectations): Unit
}

abstract class FunSuiteF[F[_]] extends RunnableSuite[F] with FunSuiteAux { self =>
  override def test(name: TestName)(run: => Expectations): Unit = synchronized {
    if(isInitialized) throw initError
    testSeq = testSeq :+ (name -> ((_: Unit) => Test.pure(name.name)(() => run)))
  }

  override def name : String = self.getClass.getName.replace("$", "")

  def eval(testNames: NonEmptyList[TestName]): Stream[F, TestOutcome] = {
    synchronized {
      isInitialized = true
      val tests         = testSeq.toMap
      val filteredTests = testNames.toList.map(name => tests(name))
      Stream.emits(filteredTests.map(execute => execute(())))
    }
  }

  private[this] var testSeq = Seq.empty[(TestName, Unit => TestOutcome)]
  def plan: List[TestName] = testSeq.map(_._1).toList

  private[this] var isInitialized = false
}

private[weaver] object initError extends AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )
