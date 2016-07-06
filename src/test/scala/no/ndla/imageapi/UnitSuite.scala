package no.ndla.imageapi

import org.scalatest._
import org.scalatest.mock.MockitoSugar

object IntegrationTest extends Tag("no.ndla.IntegrationTest")

abstract class UnitSuite extends FunSuite with Matchers with OptionValues with Inside with Inspectors with MockitoSugar with BeforeAndAfterEach with BeforeAndAfterAll with PrivateMethodTester {
  val DEFAULT_PAGE_SIZE = 12
  val MAX_PAGE_SIZE = 548

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
    "META_MAX_CONNECTIONS" -> Some("20"),
    "SEARCH_DEFAULT_PAGE_SIZE" -> Some(DEFAULT_PAGE_SIZE.toString()),
    "SEARCH_MAX_PAGE_SIZE" -> Some(MAX_PAGE_SIZE.toString()),
    "SEARCH_INDEX" -> Some("search-index"),
    "SEARCH_DOCUMENT" -> Some("search-document")

  ))
}