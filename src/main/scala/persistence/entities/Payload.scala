package persistence.entities

trait Payload {
  val comment: String
  val provider: String // bitbucket, github
  val repoFullName: String // account/repo
  val commit: String
}

case class PullRequestPayload(
  comment: String,
  provider: String,
  repoFullName: String,
  commit: String,
  branch: String,
  pullRequestId: Int) extends Payload

case class CommitPayload(
  comment: String,
  provider: String,
  repoFullName: String,
  commit: String,
  commentId: Int) extends Payload
