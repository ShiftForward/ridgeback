package core

import com.typesafe.scalalogging.LazyLogging
import persistence.entities.{ CommitRequestSource, PullRequestSource }
import spray.json.{ JsNumber, JsString, JsObject }
import utils.json.Implicits._

trait PayloadExtractor {
  def extractPullRequest(json: JsObject): Option[PullRequestSource]
  def extractCommit(json: JsObject): Option[CommitRequestSource]
  def extractComment(json: JsObject): Option[String]
}

object PayloadExtractor extends LazyLogging {
  def extractComment(provider: String, json: JsObject): Option[String] = provider match {
    case "bitbucket" => BitbucketPayloadExtractor.extractComment(json)
    case "github" => GithubPayloadExtractor.extractComment(json)
    case _ => ???
  }

  def extract(provider: String, json: JsObject): Option[Either[PullRequestSource, CommitRequestSource]] = provider match {
    case "bitbucket" =>
      if (json.getPath[JsString]("pullrequest.type").isDefined)
        BitbucketPayloadExtractor.extractPullRequest(json).map(s => Left(s))
      else if (json.getPath[JsString]("commit.type").isDefined)
        BitbucketPayloadExtractor.extractCommit(json).map(s => Right(s))
      else {
        logger.error(s"Bitbucket payload doesn't match anything known: ${json.toString()}")
        None
      }
    case "github" =>
      if (json.getPath[JsString]("pull_request.url").isDefined)
        GithubPayloadExtractor.extractPullRequest(json).map(s => Left(s))
      else if (json.getPath[JsString]("issue.url").isDefined)
        GithubPayloadExtractor.extractCommit(json).map(s => Right(s))
      else {
        logger.error(s"Github payload doesn't match anything known: ${json.toString()}")
        None
      }
    case _ => ???
  }
}

object BitbucketPayloadExtractor extends PayloadExtractor {
  def extractPullRequest(json: JsObject): Option[PullRequestSource] = {
    val commitOpt = json.getPath[JsString]("pullrequest.source.commit.hash")
    val branchOpt = json.getPath[JsString]("pullrequest.source.branch.name")
    val prIdOpt = json.getPath[JsNumber]("comment.pullrequest.id")
    val repoNameOpt = json.getPath[JsString]("repository.full_name")

    (commitOpt, branchOpt, prIdOpt, repoNameOpt) match {
      case (Some(commit), Some(branch), Some(prId), Some(repoName)) =>
        Some(PullRequestSource("bitbucket", repoName.value, commit.value, prId.value.toIntExact))
      case _ => None
    }
  }

  def extractCommit(json: JsObject): Option[CommitRequestSource] = ???

  def extractComment(json: JsObject): Option[String] = json.getPath[JsString]("comment.content.raw").map(jss => jss.value)
}

object GithubPayloadExtractor extends PayloadExtractor {
  def extractPullRequest(json: JsObject): Option[PullRequestSource] = ???
  def extractCommit(json: JsObject): Option[CommitRequestSource] = ???
  def extractComment(json: JsObject): Option[String] = json.getPath[JsString]("comment.body").map(jss => jss.value)
}
