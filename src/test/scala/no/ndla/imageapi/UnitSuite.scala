package no.ndla.imageapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar

object IntegrationTest extends Tag("no.ndla.IntegrationTest")

abstract class UnitSuite extends FlatSpec with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach {
  ImageApiProperties.setProperties(Map(
    "STORAGE_NAME" -> Some("TestBucket"),
    "CONTACT_EMAIL" -> Some("ndla@knowit.no"),
    "META_USER_NAME" -> Some("postgres"),
    "META_PASSWORD" -> Some("hemmelig"),
    "META_RESOURCE" -> Some("postgres"),
    "META_SERVER" -> scala.util.Properties.envOrNone("DOCKER_ADDR"),
    "META_PORT" -> Some("5432"),
    "META_SCHEMA" -> Some("imageapi"),
    "META_INITIAL_CONNECTIONS" -> Some("3"),
    "META_MAX_CONNECTIONS" -> Some("20")
  ))
}
