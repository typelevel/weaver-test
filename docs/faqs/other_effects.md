What happened to other effect types ?
=====================================

Starting with version 0.8.0, Weaver only supports cats-effect out-of-the-box.

We (maintainers) are happy to keep the core of weaver effect-agnostic, in an
effort to allow for third party to resurrect support for the effect types they
use in repository they control.

If you are looking for documentation of the legacy branches of weaver that did
support other effect types, you can find it [in the archived repository](https://github.com/disneystreaming/weaver-test/blob/v0.6.15/docs/installation.md).

Weaver versions before `0.9.0` (`0.8.x`) are published under the `com.disneystreaming` organization.

To summarize :

| Version | Cats Effect | ZIO    | Monix  | maintained? |
| ------- | ----------- | ------ | ------ | ----------- |
| 0.9.x   | ✅ 3.x      | ❌     | ❌     | ✅          |
| 0.8.x   | ✅ 3.x      | ❌     | ❌     | ❌          |
| 0.7.x   | ✅ 3.x      | ✅ 1.x | ❌     | ❌          |
| 0.6.x   | ✅ 2.x      | ✅ 1.x | ✅ 3.x | ❌          |
