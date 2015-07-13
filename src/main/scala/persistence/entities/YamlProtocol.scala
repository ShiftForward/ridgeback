package persistence.entities

import net.jcazevedo.moultingyaml.DefaultYamlProtocol

object YamlProtocol extends DefaultYamlProtocol {
  implicit val jobDefinitionFormat = yamlFormat6(JobDefinition)
  implicit val testsConfigurationFormat = yamlFormat3(TestsConfiguration)
}
