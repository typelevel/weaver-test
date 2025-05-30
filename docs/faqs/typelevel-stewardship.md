# Why has the organization changed?

The stewardship of weaver has moved from `disneystreaming` to `typelevel`.

Weaver makes heavy use of the `cats-effect` and `fs2` Typelevel projects. These enable weaver to run tests concurrently, provide safe resource handling, composable assertions and much more. By becoming part of the Typelevel umbrella, weaver can be maintained more easily alongside its core dependencies.

The new weaver project is licensed under Apache 2.0 and requires no CLA for contributions.

## Migrating from `org.disneystreaming` 

If you use [Scala Steward](https://github.com/scala-steward-org/scala-steward), you will migrate automatically. If not, read the [`0.9.0` migration guide](https://github.com/typelevel/weaver-test/releases/tag/v0.9.0).
