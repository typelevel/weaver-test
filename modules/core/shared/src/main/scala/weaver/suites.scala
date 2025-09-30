package weaver

import scala.concurrent.duration.FiniteDuration

import cats.data.Chain
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

trait EffectSuite[F[_]] extends Suite[F] with EffectSuiteAux
    with SourceLocation.Here { self =>

  final type EffectType[A] = F[A]
  implicit protected def effectCompat: EffectCompat[F]
  implicit final protected def effect: Async[F] = effectCompat.effect

  override def name: String = self.getClass.getName.replace("$", "")

  protected def adaptRunError: PartialFunction[Throwable, Throwable] =
    PartialFunction.empty

  final def run(args: List[String])(report: TestOutcome => F[Unit]): F[Unit] =
    spec(args).evalMap(report).compile.drain.adaptErr(adaptRunError)
}

object EffectSuite {

  trait Provider[F[_]] {
    def getSuite: EffectSuite[F]
  }

}

@RunWith(classOf[weaver.junit.WeaverRunner])
abstract class RunnableSuite[F[_]] extends EffectSuite[F] {
  implicit protected def effectCompat: UnsafeRun[EffectType]
  private[weaver] def getEffectCompat: UnsafeRun[EffectType] = effectCompat
  private[weaver] def plan: WeaverRunnerPlan
  private[weaver] def runUnsafe(
      report: TestOutcome => Unit): Unit =
    effectCompat.unsafeRunSync(run(List.empty)(outcome =>
      effectCompat.effect.delay(report(outcome))))

  def isCI: Boolean = System.getenv("CI") == "true"

  private[weaver] def analyze[A](
      testSeq: Seq[(TestName, A)],
      args: List[String]): TagAnalysisResult[A] = {

    val (testsTaggedOnly,   // "foo".only and "foo".only.ignore
         testsNotTaggedOnly // "foo" and "foo".ignore
    ) = testSeq.partition(_._1.tags(TestName.Tags.only))
    val (testsTaggedOnlyAndIgnored, // "foo".only.ignore
         onlyTestsNotIgnored        // "foo".only
    ) = testsTaggedOnly.partition(_._1.tags(TestName.Tags.ignore))

    val (testsNotTaggedOnlyAndIgnored,   // "foo".ignore
         testsNotTaggedOnlyAndNotIgnored // "foo"
    ) = testsNotTaggedOnly.partition(_._1.tags(TestName.Tags.ignore))

    if (testsTaggedOnly.nonEmpty && isCI) {
      // We're running in a CI environment and some tests are tagged
      // "foo".only. These tests should fail, and the rest should not
      // be run.
      val failureOutcomes = testsTaggedOnly.map(_._1).map(onlyNotOnCiFailure)
      TagAnalysisResult.Outcomes(
        testsNotTaggedOnly.map(_._1.name),
        failureOutcomes)
    } else if (onlyTestsNotIgnored.isEmpty) {
      // No tests are tagged "foo".only, but tests may be tagged
      // "foo".only.ignore. Use the argument filters to determine the
      // tests to be run.

      val argsFilter = Filters.filterTests(this.name)(args)

      val testsIgnored =
        testsTaggedOnlyAndIgnored ++ testsNotTaggedOnlyAndIgnored // "foo".only.ignore and "foo".ignore

      val (filteredTests, filteredOutTests) =
        testsNotTaggedOnlyAndNotIgnored.partition {
          case (name, _) => argsFilter(name)
        }
      TagAnalysisResult.FilteredTests(
        (testsIgnored ++ filteredOutTests).map(_._1.name),
        filteredTests.map { case (name, test) => (name.name, test) })
    } else {
      // Some tests are tagged "foo".only. Run these tests.
      TagAnalysisResult.FilteredTests(
        (testsNotTaggedOnly ++ testsTaggedOnlyAndIgnored).map(_._1.name),
        onlyTestsNotIgnored.map { case (name, test) => (name.name, test) }
      )
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

private[weaver] sealed trait TagAnalysisResult[A]
private[weaver] object TagAnalysisResult {
  case class Outcomes[A](ignoredTests: Seq[String], outcomes: Seq[TestOutcome])
      extends TagAnalysisResult[A]
  case class FilteredTests[A](
      ignoredTests: Seq[String],
      tests: Seq[(String, A)])
      extends TagAnalysisResult[A]
}

private[weaver] case class WeaverRunnerPlan(
    ignoredTests: List[String],
    filteredTests: List[String])
private[weaver] object WeaverRunnerPlan {
  def apply(result: TagAnalysisResult[_]): WeaverRunnerPlan = result match {
    case TagAnalysisResult.Outcomes(ignored, outcomes) =>
      WeaverRunnerPlan(ignored.toList, outcomes.map(_.name).toList)
    case TagAnalysisResult.FilteredTests(ignored, tests) =>
      WeaverRunnerPlan(ignored.toList, tests.map(_._1).toList)
  }
}

abstract class MutableFSuite[F[_]] extends RunnableSuite[F] {

  type Res
  def sharedResource: Resource[F, Res]

  def maxParallelism: Int = 10000

  protected def registerTest(name: TestName)(f: Res => F[TestOutcome]): Unit =
    synchronized {
      if (isInitialized) throw initError()
      testSeq = testSeq :+ (name -> f)
    }

  def pureTest(name: TestName)(run: => Expectations): Unit =
    registerTest(name)(_ => Test(name.name, effectCompat.effect.delay(run)))
  def loggedTest(name: TestName)(run: Log[F] => F[Expectations]): Unit =
    registerTest(name)(_ => Test[F](name.name, log => run(log)))
  def test(name: TestName): PartiallyAppliedTest =
    new PartiallyAppliedTest(name)

  class PartiallyAppliedTest(name: TestName) {
    def apply(run: => F[Expectations]): Unit =
      registerTest(name)(_ => Test(name.name, run))
    def apply(run: Res => F[Expectations]): Unit =
      registerTest(name)(res => Test(name.name, run(res)))
    def apply(run: (Res, Log[F]) => F[Expectations]): Unit =
      registerTest(name)(res => Test[F](name.name, log => run(res, log)))

    // this alias helps using pattern matching on `Res`
    def usingRes(run: Res => F[Expectations]): Unit = apply(run)
  }

  override def spec(args: List[String]): Stream[F, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      val parallelism = math.max(1, maxParallelism)

      analyze(testSeq, args) match {
        case TagAnalysisResult.Outcomes(_, outcomes) =>
          fs2.Stream.emits(outcomes)
        case TagAnalysisResult.FilteredTests(_, filteredTests)
            if filteredTests.isEmpty =>
          Stream.empty // no need to allocate resources
        case TagAnalysisResult.FilteredTests(_, filteredTests) => for {
            resource <- Stream.resource(sharedResource)
            tests      = filteredTests.map(_._2.apply(resource))
            testStream = Stream.emits(tests).covary[F]
            result <- if (parallelism > 1)
              testStream.parEvalMap(parallelism)(identity)(effectCompat.effect)
            else testStream.evalMap(identity)
          } yield result
      }
    }

  private[this] var testSeq: Seq[(TestName, Res => F[TestOutcome])] = Seq.empty

  private[weaver] def plan: WeaverRunnerPlan =
    WeaverRunnerPlan(analyze(testSeq.toList, List.empty))

  private[this] var isInitialized = false

  private[this] def initError() =
    new AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )

}

trait FunSuiteAux {
  def test(name: TestName)(run: => Expectations): Unit
}

abstract class FunSuiteF[F[_]] extends RunnableSuite[F] with FunSuiteAux {
  self =>
  override def test(name: TestName)(run: => Expectations): Unit = synchronized {
    if (isInitialized) throw initError
    testSeq =
      testSeq :+ (name -> ((_: Unit) => Test.pure(name.name)(() => run)))
  }

  override def name: String = self.getClass.getName.replace("$", "")

  private def pureSpec(args: List[String]): fs2.Stream[fs2.Pure, TestOutcome] =
    synchronized {
      if (!isInitialized) isInitialized = true
      analyze[Unit => TestOutcome](testSeq, args) match {
        case TagAnalysisResult.Outcomes(_, outcomes) =>
          fs2.Stream.emits(outcomes)
        case TagAnalysisResult.FilteredTests(_, filteredTests) =>
          fs2.Stream.emits(filteredTests.map { case (_, execute) =>
            execute(())
          })
      }
    }

  override def spec(args: List[String]) = pureSpec(args).covary[F]

  override def runUnsafe(report: TestOutcome => Unit) =
    pureSpec(List.empty).compile.toVector.foreach(report)

  private[this] var testSeq = Seq.empty[(TestName, Unit => TestOutcome)]

  private[weaver] def plan: WeaverRunnerPlan =
    WeaverRunnerPlan(analyze(testSeq.toList, List.empty))

  private[this] var isInitialized = false
}

private[weaver] object initError extends AssertionError(
      "Cannot define new tests after TestSuite was initialized"
    )
