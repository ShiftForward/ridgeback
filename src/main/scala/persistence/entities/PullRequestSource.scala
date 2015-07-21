package persistence.entities

case class PullRequestSource(
  provider: String, // bitbucket, github
  repoFullName: String, // account/repo
  commit: String,
  pullRequestId: Int)

case class CommitSource(
  provider: String,
  repoFullName: String,
  commit: String,
  commentId: Int)
