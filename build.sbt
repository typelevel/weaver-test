import laika.helium.config.TextLink
import laika.helium.config.ReleaseInfo
import laika.helium.config.HeliumIcon
import laika.helium.config.IconLink
import laika.helium.config.LinkGroup
import laika.helium.config.VersionMenu
import laika.ast.Path.Root
import laika.ast.Image
import laika.config.LinkValidation
import org.typelevel.sbt.site.TypelevelSiteSettings
import sbt.librarymanagement.Configurations.ScalaDocTool

// https://typelevel.org/sbt-typelevel/faq.html#what-is-a-base-version-anyway
ThisBuild / tlBaseVersion := "0.0" // your current series x.y

ThisBuild / startYear := Some(2019)
ThisBuild / licenses  := Seq(License.Apache2)
ThisBuild / developers := List(
  tlGitHubDev("baccata", "Olivier Mélois"),
  tlGitHubDev("keynmol", "Anton Sviridov"),
  tlGitHubDev("valencik", "Andrew Valencik")
)

ThisBuild / tlCiHeaderCheck := false

// publish to s01.oss.sonatype.org (set to true to publish to oss.sonatype.org instead)
ThisBuild / tlSonatypeUseLegacyHost := false

// enable the sbt-typelevel-site laika documentation
ThisBuild / tlSitePublishBranch := Some("main")

// use JDK 11
ThisBuild / githubWorkflowJavaVersions := Seq(JavaSpec.temurin("11"))

val scala212 = "2.12.19"
val scala213 = "2.13.12"
ThisBuild / crossScalaVersions := Seq(scala212, scala213, "3.3.3")
ThisBuild / scalaVersion       := scala213 // the default Scala

val Version = new {
  val catsEffect             = "3.5.4"
  val catsLaws               = "2.9.0"
  val discipline             = "1.5.1"
  val fs2                    = "3.10.2"
  val junit                  = "4.13.2"
  val portableReflect        = "1.1.2"
  val scalaJavaTime          = "2.4.0"
  val scalacheck             = "1.17.0"
  val scalajsMacroTask       = "1.1.1"
  val scalajsStubs           = "1.1.0"
  val testInterface          = "1.0"
  val scalacCompatAnnotation = "0.1.4"
  val http4s                 = "0.23.26"
  val munitDiff              = "1.0.0-RC1"
}

lazy val root = tlCrossRootProject.aggregate(core,
                                             framework,
                                             coreCats,
                                             cats,
                                             scalacheck,
                                             discipline)

lazy val core = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/core"))
  .settings(
    name := "weaver-core",
    libraryDependencies ++= Seq(
      "co.fs2"        %%% "fs2-core"    % Version.fs2,
      "org.typelevel" %%% "cats-effect" % Version.catsEffect,
      // https://github.com/portable-scala/portable-scala-reflect/issues/23
      "org.portable-scala" %%% "portable-scala-reflect" % Version.portableReflect cross CrossVersion.for3Use2_13,
      "org.typelevel" %% "scalac-compat-annotation" % Version.scalacCompatAnnotation,
      "org.scalameta" %%% "munit-diff" % Version.munitDiff,
      if (scalaVersion.value.startsWith("3."))
        "org.scala-lang" % "scala-reflect" % scala213
      else
        "org.scala-lang" % "scala-reflect" % scalaVersion.value
    ),
    // Shades the scala-diff dependency.
    shadedDependencies += "org.scalameta" %%% "munit-diff" % "<ignored>",
    shadingRules += ShadingRule.moveUnder("munit.diff",
                                          "weaver.internal.shaded"),
    validNamespaces ++= Set("weaver", "org")
  ).enablePlugins(ShadingPlugin)

lazy val coreJVM = core.jvm
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scalajs-stubs" % Version.scalajsStubs % "provided" cross CrossVersion.for3Use2_13,
      "junit" % "junit" % Version.junit % Optional
    )
  )

lazy val framework = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/framework"))
  .dependsOn(core)
  .settings(
    name := "weaver-framework",
    libraryDependencies ++= Seq(
      "junit" % "junit" % Version.junit
    )
  )

lazy val frameworkJVM = framework.jvm
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-sbt" % "test-interface" % Version.testInterface,
      "org.scala-js" %%% "scalajs-stubs" % Version.scalajsStubs % "provided" cross CrossVersion.for3Use2_13
    )
  )

lazy val frameworkJS = framework.js
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %% "scalajs-test-interface" % scalaJSVersion cross CrossVersion.for3Use2_13
    )
  )

lazy val frameworkNative = framework.native
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-native" %%% "test-interface" % nativeVersion
    )
  )

lazy val coreCats = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/core-cats"))
  .dependsOn(core)
  .settings(
    libraryDependencies ++= Seq(
      "junit" % "junit" % Version.junit % ScalaDocTool
    )
  )
  .settings(name := "weaver-cats-core")

lazy val coreCatsJS = coreCats.js
  .settings(
    libraryDependencies ++= Seq(
      "org.scala-js" %%% "scala-js-macrotask-executor" % Version.scalajsMacroTask)
  )

lazy val cats = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/framework-cats"))
  .dependsOn(framework, coreCats)
  .settings(
    name           := "weaver-cats",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect"))
  )

lazy val scalacheck = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/scalacheck"))
  .dependsOn(core, cats % "test->compile")
  .settings(
    name           := "weaver-scalacheck",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.scalacheck" %%% "scalacheck"          % Version.scalacheck,
      "org.typelevel"  %%% "cats-effect-testkit" % Version.catsEffect % Test)
  )

lazy val discipline = crossProject(JVMPlatform, JSPlatform, NativePlatform)
  .in(file("modules/discipline"))
  .dependsOn(core, cats)
  .settings(
    name           := "weaver-discipline",
    testFrameworks := Seq(new TestFramework("weaver.framework.CatsEffect")),
    libraryDependencies ++= Seq(
      "org.typelevel" %%% "discipline-core" % Version.discipline,
      "org.typelevel" %%% "cats-laws"       % Version.catsLaws % Test
    )
  )

lazy val docsOutput = crossProject(JVMPlatform)
  .in(file("modules/docs"))
  .enablePlugins(NoPublishPlugin)
  .dependsOn(core, framework, coreCats, cats, scalacheck, discipline)
  .settings(
    moduleName := "docs-output",
    name       := "output for documentation",
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "fansi" % "0.4.0"
    )
  )

lazy val docs = project
  .in(file("site"))
  .enablePlugins(TypelevelSitePlugin)
  .dependsOn(
    core.jvm,
    framework.jvm,
    coreCats.jvm,
    cats.jvm,
    scalacheck.jvm,
    discipline.jvm,
    docsOutput.jvm)
  .settings(
    moduleName := "weaver-docs",
    name       := "Weaver documentation",
    watchSources += (ThisBuild / baseDirectory).value / "docs",
    libraryDependencies ++= Seq(
      "org.http4s"    %% "http4s-ember-client" % Version.http4s,
      "org.typelevel" %% "cats-laws"           % Version.catsLaws
    ),
    tlSiteHelium ~= (_.site.internalCSS(Root / "assets")
      .site.landingPage(
        logo = Some(Image.internal(Root / "assets/logo.png")),
        title = Some("Weaver"),
        subtitle = Some("A test framework that runs everything in parallel."),
        latestReleases = Seq(
          ReleaseInfo("Upcoming Stable Release", "1.0.0")
        ),
        license = Some("Apache2"),
        titleLinks = Seq(
          VersionMenu.create(unversionedLabel = "Getting Started"),
          LinkGroup.create(
            IconLink.external("https://github.com/typelevel/weaver-test",
                              HeliumIcon.github),
            IconLink.external("https://discord.gg/XF3CXcMzqD", HeliumIcon.chat)
          )
        ),
        documentationLinks = Seq(
          TextLink.internal(Root / "overview/installation.md", "Installation"),
          TextLink.internal(Root / "features/expectations.md",
                            "Expectations (Assertions)")
        )
      )),
    laikaConfig ~= (_.withRawContent)
  )
