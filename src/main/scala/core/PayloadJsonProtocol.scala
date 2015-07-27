package core

import persistence.entities.{ CommitPayload, PullRequestPayload, Payload }
import spray.json.{ JsNumber, JsString }
import utils.json.Implicits._

import spray.json.{ DefaultJsonProtocol, _ }

object PayloadJsonProtocol extends DefaultJsonProtocol {
  implicit object BitbucketPayloadJsonReader extends RootJsonReader[Payload] {
    def read(json: JsValue): Payload = {
      if (json.getPath[JsString]("pullrequest.type").isDefined) {
        val commitOpt = json.getPath[JsString]("pullrequest.source.commit.hash").map(_.value)
        val branchOpt = json.getPath[JsString]("pullrequest.source.branch.name").map(_.value)
        val prIdOpt = json.getPath[JsNumber]("comment.pullrequest.id").map(_.value)
        val repoNameOpt = json.getPath[JsString]("repository.full_name").map(_.value)
        val commentOpt = json.getPath[JsString]("comment.content.raw").map(_.value)

        (commitOpt, branchOpt, prIdOpt, repoNameOpt, commentOpt) match {
          case (Some(commit), Some(branch), Some(prId), Some(repoName), Some(comment)) =>
            PullRequestPayload(comment, "bitbucket", repoName, commit, prId.toIntExact)
          case _ => deserializationError(s"Could not extract PR fields from the PR Bitbucket payload: ${json.toString()}")
        }
      } else if (json.getPath[JsString]("commit.type").isDefined) {
        val commitOpt = json.getPath[JsString]("commit.hash").map(_.value)
        val commentIdOpt = json.getPath[JsNumber]("comment.id").map(_.value)
        val repoNameOpt = json.getPath[JsString]("repository.full_name").map(_.value)
        val commentOpt = json.getPath[JsString]("comment.content.raw").map(_.value)

        (commitOpt, commentIdOpt, repoNameOpt, commentOpt) match {
          case (Some(commit), Some(commentId), Some(repoName), Some(comment)) =>
            CommitPayload(comment, "bitbucket", repoName, commit, commentId.toIntExact)
          case _ => deserializationError(s"Could not extract commit fields from the commit Bitbucket payload: ${json.toString()}")
        }
      } else {
        deserializationError(s"Bitbucket payload doesn't match anything known: ${json.toString()}")
      }
    }
  }
}
