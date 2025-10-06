import sbt.internal.ProjectMatrix

lazy val V = _root_.scalafix.sbt.BuildInfo

lazy val rulesCrossVersions = Seq(V.scala213, V.scala212)
lazy val scala3Version      = "3.5.0"

inThisBuild(
  List(
    organization := "typelevel",
    homepage     := Some(url("https://github.com/typelevel/weaver-test")),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    semanticdbEnabled := true,
    semanticdbVersion := scalafixSemanticdb.revision,
    resolvers += "Sonatype central snapshots" at "https://central.sonatype.com/repository/maven-snapshots"
  )
)

lazy val `weaver-test` = (project in file("."))
  .aggregate(
    rules.projectRefs ++
      v0_8_3_input.projectRefs ++
      v0_8_3_output.projectRefs ++
      v0_8_3_tests.projectRefs ++
      v0_9_0_input.projectRefs ++
      v0_9_0_output.projectRefs ++
      v0_9_0_tests.projectRefs: _*
  )
  .settings(
    publish / skip := true
  )

lazy val rules = projectMatrix
  .settings(
    moduleName := "scalafix",
    libraryDependencies += "ch.epfl.scala" %% "scalafix-core" % V.scalafixVersion
  )
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(rulesCrossVersions)

lazy val v0_8_3_input =
  makeInput("v0_8_3", "com.disneystreaming" %% "weaver-cats" % "0.8.3")
lazy val v0_8_3_output =
  makeOutput("v0_8_3", "com.disneystreaming" %% "weaver-cats" % "0.8.3")
lazy val v0_8_3_tests = makeTests("v0_8_3", v0_8_3_input, v0_8_3_output)

lazy val v0_9_0_input =
  makeInput("v0_9_0", "org.typelevel" %% "weaver-cats" % "0.9.0")
lazy val v0_9_0_output =
  makeOutput("v0_9_0", "org.typelevel" %% "weaver-cats" % "0.9.0")
lazy val v0_9_0_tests = makeTests("v0_9_0", v0_9_0_input, v0_9_0_output)

lazy val v0_10_1_input =
  makeInput("v0_10_1", "org.typelevel" %% "weaver-cats" % "0.10.1")
lazy val v0_10_1_output =
  makeOutput("v0_10_1", "org.typelevel" %% "weaver-cats" % "0.11-799b8e6-SNAPSHOT")
lazy val v0_10_1_tests = makeTests("v0_10_1", v0_10_1_input, v0_10_1_output)

lazy val testsAggregate = Project("tests", file("target/testsAggregate"))
  .aggregate(v0_8_3_tests.projectRefs ++ v0_9_0_tests.projectRefs ++ v0_10_1_tests.projectRefs: _*)
  .settings(
    publish / skip := true
  )

def makeOutput(name: String, weaver: ModuleID): ProjectMatrix = makeInputOrOutput(name, weaver, "output")
def makeInput(name: String, weaver: ModuleID): ProjectMatrix = makeInputOrOutput(name, weaver, "input")

def makeInputOrOutput(name: String, weaver: ModuleID, inputOrOutput: String): ProjectMatrix = ProjectMatrix(
  s"${name}_$inputOrOutput",
  file(s"$name/$inputOrOutput")
).settings(
  publish / skip := true,
  libraryDependencies += weaver
).defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(scalaVersions = rulesCrossVersions :+ scala3Version)

def makeTests(
    name: String,
    input: sbt.internal.ProjectMatrix,
    output: sbt.internal.ProjectMatrix) = sbt.internal.ProjectMatrix(
  s"${name}_tests",
  file(s"$name/tests")
)
  .settings(
    publish / skip := true,
    scalafixTestkitOutputSourceDirectories :=
      TargetAxis
        .resolve(output, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputSourceDirectories :=
      TargetAxis
        .resolve(input, Compile / unmanagedSourceDirectories)
        .value,
    scalafixTestkitInputClasspath :=
      TargetAxis.resolve(input, Compile / fullClasspath).value,
    scalafixTestkitInputScalacOptions :=
      TargetAxis.resolve(input, Compile / scalacOptions).value,
    scalafixTestkitInputScalaVersion :=
      TargetAxis.resolve(input, Compile / scalaVersion).value
  )
  .defaultAxes(
    rulesCrossVersions.map(VirtualAxis.scalaABIVersion) :+ VirtualAxis.jvm: _*
  )
  .jvmPlatform(
    scalaVersions = Seq(V.scala212),
    axisValues = Seq(TargetAxis(scala3Version)),
    settings = Seq()
  )
  .jvmPlatform(
    scalaVersions = Seq(V.scala213),
    axisValues = Seq(TargetAxis(V.scala213)),
    settings = Seq()
  )
  .jvmPlatform(
    scalaVersions = Seq(V.scala212),
    axisValues = Seq(TargetAxis(V.scala212)),
    settings = Seq()
  )
  .dependsOn(rules)
  .enablePlugins(ScalafixTestkitPlugin)
