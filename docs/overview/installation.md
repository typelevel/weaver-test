Installation
============

@:callout(info)
The stewardship of Weaver has moved from `disneystreaming` to `typelevel`.

You can migrate from previous versions of weaver by following the [migration guide](https://github.com/typelevel/weaver-test/releases/tag/v0.9.0), or [learn more about the stewardship](../faqs/typelevel-stewardship.md).
@:@

Weaver-test is currently published for **Scala 2.12, 2.13, and 3**

### SBT (1.9.0+)

Newer versions of SBT have [`weaver` automatically integrated](https://github.com/sbt/sbt/pull/7263).

```scala
libraryDependencies +=  "org.typelevel" %% "weaver-cats" % "@VERSION@" % Test
```

### SBT (older versions)

Internally, SBT has a hardcoded list of test frameworks it integrates with. `weaver` must be manually added to this list.

```scala
libraryDependencies +=  "org.typelevel" %% "weaver-cats" % "@VERSION@" % Test
testFrameworks += new TestFramework("weaver.framework.CatsEffect")
```

### Mill
```scala
object test extends Tests {
  def ivyDeps = Agg(
    ivy"org.typelevel::weaver-cats:@VERSION@"
  )
  def testFramework = "weaver.framework.CatsEffect"
}
```

### scala-cli
```scala
//> using lib "org.typelevel::weaver-cats:@VERSION@"
//> using testFramework "weaver.framework.CatsEffect" // this may be neccessary if you have other testFramework on your dependencies
```

### Gradle
With [Gradle plugin for multi-backend Scala](https://github.com/dubinsky/scalajs-gradle):
```groovy
plugins {
  id 'org.podval.tools.scalajs' version '<latest version>'
}
dependencies {
  testImplementation scalaBackend.testFramework(org.podval.tools.test.framework.WeaverTest)
}
```

## Getting started


Start with importing the following :

```scala mdoc
import weaver._
```

The most basic usage is to extend `SimpleIOSuite`.

```scala mdoc
import cats.effect._

// Suites must be "objects" for them to be picked by the framework
object MySuite extends SimpleIOSuite {

  val randomUUID = IO(java.util.UUID.randomUUID())

  test("failing test with side-effects") {
    for {
      x <- randomUUID
      y <- randomUUID
    } yield expect.eql(x, y)
  }

  pureTest("pure failing test"){
    expect.eql("hello".size, 6)
  }

  loggedTest("failing test with logs"){ log =>
    for {
      x <- randomUUID
      _ <- log.info(s"x : $x")
      y <- randomUUID
      _ <- log.info(s"y : $y")
    } yield expect.eql(x, y)
  }
}
```

Run your tests with your build tool, e.g. `sbt test`.

```scala mdoc:passthrough
println(weaver.docs.Output.runSuites(MySuite))
```

#### Other suites

Weaver also includes support for

| Suite                                                      | Use case                                      |
|------------------------------------------------------------|-----------------------------------------------|
| [SimpleIOSuite](../features/expectations.md#example-suite) | Each test is a standalone `IO` action         |
| [IOSuite](../features/resources.md)                        | Each test needs access to a shared `Resource` |
| [FunSuite](../features/funsuite.md)                        | Each test is a pure function                  |
