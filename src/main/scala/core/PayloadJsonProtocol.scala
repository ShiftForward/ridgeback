package core

import persistence.entities.{ CommitPayload, Payload, PullRequestPayload }
import spray.json.lenses.JsonLenses._
import spray.json.{ DefaultJsonProtocol, _ }

object PayloadJsonProtocol extends DefaultJsonProtocol {
  implicit object BitbucketPayloadJsonReader extends RootJsonReader[Payload] {
    def read(json: JsValue): Payload = {
      (json.extract[Option[JsObject]]('pullrequest.?), json.extract[Option[JsObject]]('commit.?)) match {
        case (Some(_), None) =>
          val commitOpt = json.extract[Option[String]]('pullrequest / 'source / 'commit / 'hash)
          val branchOpt = json.extract[Option[String]]('pullrequest / 'source / 'branch / 'name)
          val prIdOpt = json.extract[Option[Int]]('comment / 'pullrequest / 'id)
          val repoNameOpt = json.extract[Option[String]]('repository / 'full_name)
          val commentOpt = json.extract[Option[String]]('comment / 'content / 'raw)

          (commitOpt, branchOpt, prIdOpt, repoNameOpt, commentOpt) match {
            case (Some(commit), Some(branch), Some(prId), Some(repoName), Some(comment)) =>
              PullRequestPayload(comment, "bitbucket", repoName, commit, branch, prId)
            case _ => deserializationError(s"Could not extract PR fields from the PR Bitbucket payload: ${json.toString()}")
          }
        case (None, Some(_)) =>
          val commitOpt = json.extract[Option[String]]('commit / 'hash)
          val commentIdOpt = json.extract[Option[Int]]('comment / 'id)
          val repoNameOpt = json.extract[Option[String]]('repository / 'full_name)
          val commentOpt = json.extract[Option[String]]('comment / 'content / 'raw)

          (commitOpt, commentIdOpt, repoNameOpt, commentOpt) match {
            case (Some(commit), Some(commentId), Some(repoName), Some(comment)) =>
              CommitPayload(comment, "bitbucket", repoName, commit, commentId)
            case _ => deserializationError(s"Could not extract commit fields from the commit Bitbucket payload: ${json.toString()}")
          }
        case _ => deserializationError(s"Bitbucket payload doesn't match anything known: ${json.toString()}")
      }
    }
  }
}
