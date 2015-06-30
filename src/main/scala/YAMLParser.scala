import java.util

import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor

import scala.beans.BeanProperty

object YAMLParser {
  val text =
    """
  setup:
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
      script:
        - time curl http://bing.com
    """.stripMargin

  def main(args: Array[String]): Unit = {
    val yaml =  new Yaml(new Constructor(classOf[TestsConfiguration]))
    val e = yaml.load(text).asInstanceOf[TestsConfiguration]
    println(e)
  }
}

class TestsConfiguration {
  @BeanProperty var setup = new util.ArrayList[String]()
  @BeanProperty var jobs = new util.ArrayList[JobDefinition]()

  override def toString: String = {
    "setup: %s, jobs: %s".format(setup.toString, jobs.toString)
  }
}

class JobDefinition {
  @BeanProperty var name: String = null
  @BeanProperty var metric: String = null
  @BeanProperty var script = new util.ArrayList[String]()

  override def toString: String = {
    "name: %s, metric: %s, script: %s".format(name, metric, script.toString)
  }
}
