import java.util

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import scala.beans.BeanProperty

object YAMLParser {
  val text =
  """
  before_jobs:
    - mkdir test
    - touch test/hi.txt
    - \"testing test\" > test/hi.txt

  jobs:
    - name: job1
      metric: time
      script:
        - time curl http://google.com
    - name: job2
      metric: time
      before_script:
        - touch executingjob2.txt
      script:
        - time curl http://bing.com
      after_script:
        - rm executingjob2.txt

  after_jobs:
    - rm test/hi.txt
    - rmdir test
  """.stripMargin

  def main(args: Array[String]): Unit = {
    val yaml =  new Yaml(new Constructor(classOf[TestsConfiguration]))
    val e = yaml.load(text).asInstanceOf[TestsConfiguration]
    println(e)
  }
}

class TestsConfiguration {
  @BeanProperty var before_jobs = new util.ArrayList[String]()
  @BeanProperty var jobs = new util.ArrayList[JobDefinition]()
  @BeanProperty var after_jobs = new util.ArrayList[String]()

  override def toString: String = {
    "before: %s, jobs: %s, after: %s".format(before_jobs.toString, jobs.toString, after_jobs.toString)
  }
}

class JobDefinition {
  @BeanProperty var name: String = null
  @BeanProperty var metric: String = null
  @BeanProperty var before_script = new util.ArrayList[String]()
  @BeanProperty var script = new util.ArrayList[String]()
  @BeanProperty var after_script = new util.ArrayList[String]()

  override def toString: String = {
    "name: %s, metric: %s, before: %s, script: %s after: %s".format(name, metric, before_script.toString,
      script.toString, after_script.toString)
  }
}
