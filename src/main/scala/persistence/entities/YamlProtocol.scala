package persistence.entities

import net.jcazevedo.moultingyaml.DefaultYamlProtocol

object YamlProtocol extends DefaultYamlProtocol {
  implicit val testsConfigurationFormat = yamlFormat3(TestsConfiguration)
  implicit val jobDefinitionFormat = yamlFormat9(JobDefinition)
}
