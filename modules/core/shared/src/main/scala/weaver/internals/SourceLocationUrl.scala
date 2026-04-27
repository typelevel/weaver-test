package weaver.internals

import cats.syntax.all._

/**
 * Constructs base URL to use for displaying source locations in failure
 * messages.
 */
private[weaver] object SourceLocationUrl {

  def apply(): Option[String] = {
    // Users can support non-GitHub CIs by setting WEAVER_SOURCE_URL
    def readWeaverUrl(): Option[String] = Env.get("WEAVER_SOURCE_URL")
    // If tests are run on GitHub Actions, construct a URL to the test source code.
    // See https://docs.github.com/en/actions/reference/workflows-and-actions/variables#default-environment-variables
    def readGitHubUrl(): Option[String] = (
      Env.get("GITHUB_SERVER_URL"),
      Env.get("GITHUB_REPOSITORY"),
      Env.get("GITHUB_SHA")
    ).mapN { case (serverUrl, repo, sha) =>
      s"$serverUrl/$repo/tree/$sha/"
    }

    readWeaverUrl().orElse(readGitHubUrl())
  }
}
