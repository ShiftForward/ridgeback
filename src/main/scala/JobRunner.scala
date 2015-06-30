object JobRunner {
  def main(args: Array[String]): Unit = {
    val config = YAMLParser.loadYaml[TestsConfiguration](YAMLParser.text)
    config.process
  }
}
