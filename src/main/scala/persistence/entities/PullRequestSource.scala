package persistence.entities

case class PullRequestSource(
  provider: String, // bitbucket, github
  repoFullName: String, // account/repo
  pullRequestId: Int)
