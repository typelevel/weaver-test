Why are my error messages worse ?
=====================================

Weaver `0.9.0` reduced the error reporting power of `expect`.

Prior to `0.9.0`, `expect` would capture the values of its contents on failure. This used the [expecty](https://github.com/eed3si9n/expecty/) macro library to introspect code. While the macro behaved well in most cases, it came with a high maintenance burden. Users reported many bugs in the Scala 3 implementation that were non-trivial to fix.

As the stewardship of weaver moved to Typelevel, it was decided to cut the cord with expecty in order to eliminate this maintenance burden, and to bring in `clue` (inspired from munit) as a much simpler alternative.

Expecty's error messages were useful, and this decision may be disappointing for users. However, it will allow maintainers to focus on improving other aspects of weaver.

## How can I improve my error messages?

You can rewrite your `expect` assertions into `expect.same` calls, and add `clue` to any remaining assertions.

We recommend you do this automatically by [applying a scalafix rule](https://scalacenter.github.io/scalafix/docs/rules/external-rules.html).

Run the `RewriteExpect` rule to rewrite `expect` and `expect.all` into assertions with better failure messages.

```sh
sbt scalafixAll github:typelevel/weaver-test/RewriteExpect?sha=@VERSION@
```

Run the `AddClueToExpect` rule to add `clue` calls to the remaining `expect` assertions.

```sh
sbt scalafixAll github:typelevel/weaver-test/AddClueToExpect?sha=@VERSION@
```

Your failure messages will now include the values captured in `clue`.
