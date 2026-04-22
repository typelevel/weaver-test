package weaver.internals

import cats.effect.std.Env
import cats.Monad
import cats.syntax.all._

/**
 * Constructs base URL to use for displaying source locations in failure
 * messages.
 */
private[weaver] object SourceLocationUrl {
  def apply[F[_]](implicit E: Env[F], F: Monad[F]): F[Option[String]] = {
    // Users can support non-GitHub CIs by setting WEAVER_SOURCE_URL
    val readWeaverUrl = E.get("WEAVER_SOURCE_URL")
    // If tests are run on GitHub Actions, construct a URL to the test source code.
    // See https://docs.github.com/en/actions/reference/workflows-and-actions/variables#default-environment-variables
    val readGitHubUrl = (
      E.get("GITHUB_SERVER_URL"),
      E.get("GITHUB_REPOSITORY"),
      E.get("GITHUB_SHA")
    ).tupled.map(_.mapN { case (serverUrl, repo, sha) =>
      s"$serverUrl/$repo/tree/$sha/"
    })
    (readWeaverUrl, readGitHubUrl).mapN {
      case (weaverUrl, gitHubUrl) => weaverUrl.orElse(gitHubUrl)
    }
  }

}
