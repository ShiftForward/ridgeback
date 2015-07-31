package core

import java.util.regex.Matcher

import persistence.entities.Job
import utils.RichDuration.RichDuration
import utils._

import scala.concurrent.{ ExecutionContext, Future }
import scala.concurrent.duration.Duration

object CommentBuilder {
  def buildComment(job: Job, commentWriter: CommentWriter,
                   modules: Configuration with PersistenceModule,
                   commentTemplate: Option[String])(implicit ec: ExecutionContext): Future[String] = {
    val description = s"${job.jobName} (${job.id.get})"
    job.durations match {
      case ds if ds.isEmpty => Future(s"- ${commentWriter.actionUnknown} Job $description had no output")
      case ds =>
        modules.jobsDal.getPastJobs(job).map { pastJobs =>
          val template = commentTemplate.getOrElse(modules.config.getString("worker.commentTemplate"))
          """\$[A-Za-z0-9%]+""".r
            .replaceAllIn(template, m => keywordToString(
              Matcher.quoteReplacement(m.group(0)).drop(2 /* /$ */ ), job, pastJobs, commentWriter, modules))
        }
    }
  }

  private def currentMean(job: Job): Duration = MeanDurationStat(job.durations).getOrElse(Duration.Undefined)
  private def currentMin(job: Job): Duration = MinDurationStat(job.durations).getOrElse(Duration.Undefined)
  private def currentMax(job: Job): Duration = MaxDurationStat(job.durations).getOrElse(Duration.Undefined)

  private def previousMean(prevJobs: Seq[Job], n: Int): Duration = {
    MeanDurationStat(prevJobs.take(n).flatMap(j => MeanDurationStat(j.durations)))
      .getOrElse(Duration.Undefined)
  }

  private def actionMean(job: Job, prevJobs: Seq[Job],
                         n: Int, commentWriter: CommentWriter,
                         modules: Configuration): String = {
    val mean = currentMean(job)
    previousMean(prevJobs, n) match {
      case d if !d.isFinite() => commentWriter.actionNew
      case pastMean =>
        val defaultThreshold = modules.config.getInt("worker.defaultThreshold")
        pastMean.compareWithThreshold(mean, job.threshold.getOrElse(defaultThreshold)) match {
          case c if c < 0 => commentWriter.actionWorse
          case c if c > 0 => commentWriter.actionBetter
          case _ => commentWriter.actionEqual
        }
    }
  }

  private val keywords = Seq("id", "name", "mean", "min", "max",
    "prevMean", "prevMean5", "actionMean", "actionMean5",
    "diffMean", "diffMean5", "diff%Mean", "diff%Mean5")

  private def keywordToString(keyword: String, job: Job,
                              prevJobs: Seq[Job],
                              commentWriter: CommentWriter, modules: Configuration): String = {
    println("keyword: " + keyword)
    keyword match {
      case "id" => job.id.getOrElse(0).toString
      case "name" => job.jobName
      case "mean" => currentMean(job).toShortString
      case "min" => currentMin(job).toShortString
      case "max" => currentMax(job).toShortString
      case "prevMean" => previousMean(prevJobs, 1).toShortString
      case "prevMean5" => previousMean(prevJobs, 5).toShortString
      case "actionMean" => actionMean(job, prevJobs, 1, commentWriter, modules)
      case "actionMean5" => actionMean(job, prevJobs, 5, commentWriter, modules)
      case "diffMean" => DiffDurationStat(currentMean(job), previousMean(prevJobs, 1)).toShortString
      case "diffMean5" => DiffDurationStat(currentMean(job), previousMean(prevJobs, 5)).toShortString
      case "diff%Mean" => "%.2f".format(PercentageDiffDurationStat(currentMean(job), previousMean(prevJobs, 1)))
      case "diff%Mean5" => "%.2f".format(PercentageDiffDurationStat(currentMean(job), previousMean(prevJobs, 5)))
      case unk => '?' + unk
    }
  }
}
