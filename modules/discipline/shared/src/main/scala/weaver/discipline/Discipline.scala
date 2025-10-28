package weaver
package discipline

import scala.collection.mutable
import scala.util.control.NoStackTrace

import cats.data.Kleisli
import cats.effect.Resource
import cats.implicits._

import fs2.Stream
import org.scalacheck.Prop.Arg
import org.scalacheck.Test._
import org.scalacheck.util.Pretty
import org.scalacheck.{ Prop, Test => ScalaCheckTest }
import org.typelevel.discipline.Laws

import Discipline._

trait Discipline { self: SharedResourceSuiteAux =>

  def checkAll(
      name: TestName,
      ruleSet: Laws#RuleSet,
      parameters: Parameters => Parameters = identity): Unit =
    ruleSet.all.properties.toList.foreach {
      case (id, prop) =>
        val testName = name.copy(s"${name.name}: $id")
        registerTest(testName) { _ =>
          effect.pure(Test.pure(testName.name)(() =>
            executeProp(prop, name.location, parameters)))
        }
    }

}

trait DisciplineFSuite[F[_]] extends RunnableSuite[F] {

  type Res
  def sharedResource: Resource[F, Res]

  /**
   * Defines max parallelism within whole suite (maxSuiteParallelism = 1 means
   * each checkAll will be run sequentially)
   */
  def maxSuiteParallelism: Int = 10000

  /**
   * Defines max parallelism within single rule set (maxRuleSetParallelism = 1
   * means each property of a law will be run sequentially)
   */
  def maxRuleSetParallelism: Int = 10000

  protected def registerTest(tests: Res => F[List[F[TestOutcome]]]): Unit =
    registeredTests.synchronized {
      if (isInitialized) throw initError()
      registeredTests += tests
      ()
    }

  def checkAll(
      name: TestName,
      parameters: Parameters => Parameters = identity
  ): PartiallyAppliedCheckAll = new PartiallyAppliedCheckAll(name, parameters)

  class PartiallyAppliedCheckAll(
      name: TestName,
      parameters: Parameters => Parameters) {
    def apply(run: => F[Laws#RuleSet]): Unit     = apply(_ => run)
    def apply(run: Res => F[Laws#RuleSet]): Unit = {
      registerTest(
        Kleisli(run).map(_.all.properties.toList.map {
          case (id, prop) =>
            val propTestName = s"${name.name}: $id"
            val runProp      = effectCompat.effect.delay(
              executeProp(prop, name.location, parameters)
            )
            foundProps.synchronized {
              foundProps += name.copy(propTestName)
            }
            Test(propTestName, runProp)
        }).run
      )
    }

    // this alias helps using pattern matching on `Res`
    def usingRes(run: Res => F[Laws#RuleSet]): Unit = apply(run)

    def pure(run: Res => Laws#RuleSet): Unit = apply(run.andThen(_.pure[F]))
  }

  override def spec(args: List[String]): Stream[F, TestOutcome] =
    registeredTests.synchronized {
      if (!isInitialized) isInitialized = true
      val suiteParallelism   = math.max(1, maxSuiteParallelism)
      val ruleSetParallelism = math.max(1, maxRuleSetParallelism)
      Stream.resource(sharedResource).flatMap { resource =>
        Stream.emits(registeredTests).covary[F]
          .parEvalMap(suiteParallelism)(_.apply(resource))
          .map { ruleSet =>
            Stream.emits(ruleSet).covary[F]
              .parEvalMap(ruleSetParallelism)(identity)
          }
          .parJoin(suiteParallelism)
      }
    }

  private[weaver] override def plan: WeaverRunnerPlan =
    foundProps.synchronized {
      WeaverRunnerPlan(Nil, foundProps.toList.map(_.name))
    }

  private[this] val foundProps = mutable.Buffer.empty[TestName]

  private[this] val registeredTests =
    mutable.Buffer.empty[Res => F[List[F[TestOutcome]]]]

  private[this] var isInitialized = false

  private[this] def initError() = new AssertionError(
    "Cannot define new tests after TestSuite was initialized")
}

object Discipline {

  private[discipline] case class PropertyException(
      input: List[Arg[Any]],
      cause: Throwable)
      extends Exception(cause)
      with NoStackTrace {
    override def getMessage() =
      "Property failed with an exception\n" + printArgs(input)
  }

  private[discipline] def executeProp(
      prop: Prop,
      location: SourceLocation,
      parameters: Parameters => Parameters
  ): Expectations = {
    import Expectations.Helpers._

    ScalaCheckTest.check(prop)(parameters).status match {
      case Passed | Proved(_) => success
      case Exhausted          => failure("Property exhausted")(location)
      case Failed(input, _)   =>
        failure(s"Property violated \n" + printArgs(input))(location)
      case PropException(input, cause, _) =>
        throw PropertyException(input, cause)
    }
  }

  private def printArgs(args: Seq[Arg[Any]]) =
    args.zipWithIndex.map { case (arg, idx) =>
      s"ARG $idx: " + arg.prettyArg(Pretty.defaultParams)
    }.mkString("\n")
}
