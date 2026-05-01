<img src="https://github.com/typelevel/weaver-test/raw/main/docs/assets/logo.png" width="200px" height="231px" align="right">

[![CI](https://github.com/typelevel/weaver-test/actions/workflows/ci.yml/badge.svg?branch=main)](https://github.com/typelevel/weaver-test/actions/workflows/ci.yml)
[![Latest version](https://index.scala-lang.org/typelevel/weaver-test/weaver-core/latest.svg?color=orange)](https://index.scala-lang.org/typelevel/weaver-test/weaver-core)
[![Discord](https://img.shields.io/discord/632277896739946517.svg?label=&logo=discord&logoColor=ffffff&color=404244&labelColor=6A7EC2)](https://discord.gg/xQETVDrGxy)
[![Scala Steward badge](https://img.shields.io/badge/Scala_Steward-helping-blue.svg?style=flat&logo=data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAA4AAAAQCAMAAAARSr4IAAAAVFBMVEUAAACHjojlOy5NWlrKzcYRKjGFjIbp293YycuLa3pYY2LSqql4f3pCUFTgSjNodYRmcXUsPD/NTTbjRS+2jomhgnzNc223cGvZS0HaSD0XLjbaSjElhIr+AAAAAXRSTlMAQObYZgAAAHlJREFUCNdNyosOwyAIhWHAQS1Vt7a77/3fcxxdmv0xwmckutAR1nkm4ggbyEcg/wWmlGLDAA3oL50xi6fk5ffZ3E2E3QfZDCcCN2YtbEWZt+Drc6u6rlqv7Uk0LdKqqr5rk2UCRXOk0vmQKGfc94nOJyQjouF9H/wCc9gECEYfONoAAAAASUVORK5CYII=)](https://github.com/scala-steward-org/scala-steward)
# Weaver-test

A test-framework built on [cats-effect](https://github.com/typelevel/cats-effect) and
[fs2](https://github.com/functional-streams-for-scala/fs2)

## Installation

Weaver-test is currently published for **Scala 2.12, 2.13, and 3**

### SBT (1.9.0+)

Refer yourself to the [releases](https://github.com/typelevel/weaver-test/releases) page to know the latest released version, and add the following to your `build.sbt` file.

```scala
libraryDependencies +=  "org.typelevel" %% "weaver-cats" % "@VERSION@" % Test
```

For other build tools and older SBT versions, read the [installation guide](https://typelevel.org/weaver-test/overview/installation.html).

## Documentation

Full documentation is available on the [Weaver microsite](https://typelevel.org/weaver-test/).

### Quick links

- [Installation](https://typelevel.org/weaver-test/overview/installation.html)
- [Getting started](https://typelevel.org/weaver-test/overview/installation.html#getting-started)
- [Writing expectations (assertions)](https://typelevel.org/weaver-test/features/expectations.html)
- [Filtering tests](https://typelevel.org/weaver-test/features/filtering.html)
- [ScalaCheck integration](https://typelevel.org/weaver-test/features/scalacheck.html)

## Motivation

Weaver was built for integration/end-to-end tests. It makes tests faster and easier to debug by using cats-effect `IO`.

![time](docs/assets/time.png)

- Tests are run concurrently by default for quickest results possible.
- Expectations (ie assertions) are composable values.
- Failures are aggregated and reported at the end of the run
- Logs are only displayed when a test fails. This works perfectly well with concurrent runs.
- “beforeAll” and “afterAll” logic is represented using a `cats.effect.Resource`. This ensures that resources such as HTTP clients, connection pools and file handles are cleaned up correctly and predictably.

For more details, see [why Weaver?](https://typelevel.org/weaver-test/overview/motivation.html)

## Contributing

Contributions are most welcome !

### Building the website

If you're changing documentation, here's how you can check your changes locally:

```bash
sbt docs/tlSitePreview
```

Then navigate to `http://localhost:4242` to see the documentation site.

Weaver uses `sbt-typelevel-site`, for more details on writing documentation
[see here](https://typelevel.org/sbt-typelevel/site.html#quick-start).

### PR Guidelines

Please:

- Write positive and negative tests
- Include documentation

## Inspiration

A **HUGE** thank you to Alexandru Nedelcu, author of [Monix](https://github.com/monix/monix)
and contributor to cats-effect, as he wrote the [minitest](https://github.com/monix/minitest)
framework which got this framework started.
