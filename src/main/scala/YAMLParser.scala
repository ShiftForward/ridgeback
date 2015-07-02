import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.Constructor
import scala.reflect._

object YAMLParser {
  val text =
  """
  before_jobs:
    - mkdir test
    - touch test/hi.txt

  jobs:
    - name: job1_google
      metric: time_seconds
      script:
        - curl -w %{time_total} -o /dev/null -s http://google.com/
    - name: job2_bing
      metric: time_seconds
      before_script:
        - touch executingjob2.txt
      script:
        - curl -w %{time_total} -o /dev/null -s http://bing.com/
      after_script:
        - rm executingjob2.txt
    - name: job3_pi
      metric: time_seconds
      script:
        - curl -w %{time_total} -o /dev/null -s http://jpdias.noip.me:8080/

  after_jobs:
    - rm test/hi.txt
    - rmdir test
  """.stripMargin

  def loadYaml[T](text : String)(implicit tag: ClassTag[T]): T = {
    val yaml =  new Yaml(new Constructor(tag.runtimeClass))
    yaml.load(text).asInstanceOf[T]
  }
}
