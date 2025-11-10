# Scalafix rules for weaver-test

To develop rule:
```
sbt ~tests/test
# edit rules/src/main/scala/fix/RenameAssertToExpect.scala
```

## Exploring symbols for the `SymbolMatcher`

Create an input file containing the symbol.

```
# create v0_9_0/input/src/main/scala/fix/AddClueToExpectTest.scala
```

Compile the code

```
sbt compile
```

Find the corresponding `.semanticdb` file.

```
ls v0_9_0/input/target/jvm-3/meta/META-INF/semanticdb/v0_9_0/input/src/main/scala/fix/AddClueToExpectTest.scala.semanticdb
```

Explore the symbols using the [metap](https://scalameta.org/docs/semanticdb/guide.html#installation) tool.

```
metap v0_9_0/input/target/jvm-3/meta/META-INF/semanticdb/v0_9_0/input/src/main/scala/fix/AddClueToExpectTest.scala.semanticdb
```
