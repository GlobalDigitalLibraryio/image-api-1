package no.ndla.imageapi.service

import com.sksamuel.elastic4s.testkit.ElasticSugar
import no.ndla.imageapi.model._
import no.ndla.imageapi.{TestEnvironment, UnitSuite}


class ElasticContentSearchTest extends UnitSuite with TestEnvironment with ElasticSugar {

  override val elasticClient = client
  override val elasticContentIndex = new ElasticContentIndex
  override val searchService = new ElasticContentSearch

  val getStartAtAndNumResults = PrivateMethod[(Int, Int)]('getStartAtAndNumResults)

  val largeImageVariant = ImageVariants(Some(Image("large-thumb-url", 1000, "jpg")), Some(Image("large-full-url", 10000, "jpg")))
  val smallImageVariant = ImageVariants(Some(Image("small-thumb-url", 10, "jpg")), Some(Image("small-full-url", 100, "jpg")))

  val byNcSa = Copyright(License("by-nc-sa", "Attribution-NonCommercial-ShareAlike", None), "Gotham City", List(Author("Forfatter", "DC Comics")))
  val publicDomain = Copyright(License("publicdomain", "Public Domain", None), "Metropolis", List(Author("Forfatter", "Bruce Wayne")))

  val image1 = ImageMetaInformation("1", List(ImageTitle("Batmen er på vift med en bil", Some("nb"))), List(ImageAltText("Bilde av en bil flaggermusmann som vifter med vingene bil.", Some("nb"))), largeImageVariant, byNcSa, List(ImageTag(List("fugl"), Some("nb"))))
  val image2 = ImageMetaInformation("2", List(ImageTitle("Pingvinen er ute og går", Some("nb"))), List(ImageAltText("Bilde av en en pingvin som vagger borover en gate.", Some("nb"))), largeImageVariant, publicDomain, List(ImageTag(List("fugl"), Some("nb"))))
  val image3 = ImageMetaInformation("3", List(ImageTitle("Donald Duck kjører bil", Some("nb"))), List(ImageAltText("Bilde av en en and som kjører en rød bil.", Some("nb"))), smallImageVariant, byNcSa, List(ImageTag(List("and"), Some("nb"))))

  override def beforeAll = {
    val indexName = "testindex"
    elasticContentIndex.createIndex(indexName)
    elasticContentIndex.updateAliasTarget(indexName, None)

    elasticContentIndex.indexDocuments(List(
      image1, image2, image3
    ), indexName)

    blockUntilCount(3, indexName)
  }

  test("That getStartAtAndNumResults returns default values for None-input") {
    searchService invokePrivate getStartAtAndNumResults(None, None) should equal((0, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns SEARCH_MAX_PAGE_SIZE for value greater than SEARCH_MAX_PAGE_SIZE") {
    searchService invokePrivate getStartAtAndNumResults(None, Some(1000)) should equal((0, MAX_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size with default page-size") {
    val page = 74
    val expectedStartAt = (page - 1) * DEFAULT_PAGE_SIZE
    searchService invokePrivate getStartAtAndNumResults(Some(page), None) should equal((expectedStartAt, DEFAULT_PAGE_SIZE))
  }

  test("That getStartAtAndNumResults returns the correct calculated start at for page and page-size") {
    val page = 123
    val pageSize = 321
    val expectedStartAt = (page - 1) * pageSize
    searchService invokePrivate getStartAtAndNumResults(Some(page), Some(pageSize)) should equal((expectedStartAt, pageSize))
  }

  test("That all returns all documents ordered by id ascending") {
    val searchResult = searchService.all(None, None, None, None)
    searchResult.totalCount should be (3)
    searchResult.results.size should be (3)
    searchResult.page should be (1)
    searchResult.results.head.id should be ("1")
    searchResult.results.last.id should be ("3")
  }

  test("That all filtering on minimumsize only returns images larger than minimumsize") {
    val searchResult = searchService.all(Some(500), None, None, None)
    searchResult.totalCount should be (2)
    searchResult.results.size should be (2)
    searchResult.results.head.id should be ("1")
    searchResult.results.last.id should be ("2")
  }

  test("That all filtering on license only returns images with given license") {
    val searchResult = searchService.all(None, Some("publicdomain"), None, None)
    searchResult.totalCount should be (1)
    searchResult.results.size should be (1)
    searchResult.results.head.id should be ("2")
  }

  test("That paging returns only hits on current page and not more than page-size") {
    val searchResultPage1 = searchService.all(None, None, Some(1), Some(2))
    val searchResultPage2 = searchService.all(None, None, Some(2), Some(2))
    searchResultPage1.totalCount should be (3)
    searchResultPage1.page should be (1)
    searchResultPage1.pageSize should be (2)
    searchResultPage1.results.size should be (2)
    searchResultPage1.results.head.id should be ("1")
    searchResultPage1.results.last.id should be ("2")

    searchResultPage2.totalCount should be (3)
    searchResultPage2.page should be (2)
    searchResultPage2.pageSize should be (2)
    searchResultPage2.results.size should be (1)
    searchResultPage2.results.head.id should be ("3")
  }

  test("That both minimum-size and license filters are applied.") {
    val searchResult = searchService.all(Some(500), Some("publicdomain"), None, None)
    searchResult.totalCount should be (1)
    searchResult.results.size should be (1)
    searchResult.results.head.id should be ("2")
  }

  test("That search matches title and alttext ordered by relevance") {
    val searchResult = searchService.matchingQuery(Seq("bil"), None, Some("nb"), None, None, None)
    searchResult.totalCount should be (2)
    searchResult.results.size should be (2)
    searchResult.results.head.id should be ("1")
    searchResult.results.last.id should be ("3")
  }

  test("That search matches title") {
    val searchResult = searchService.matchingQuery(Seq("Pingvinen"), None, Some("nb"), None, None, None)
    searchResult.totalCount should be (1)
    searchResult.results.size should be (1)
    searchResult.results.head.id should be ("2")
  }

  test("That search matches tags") {
    val searchResult = searchService.matchingQuery(Seq("and"), None, Some("nb"), None, None, None)
    searchResult.totalCount should be (1)
    searchResult.results.size should be (1)
    searchResult.results.head.id should be ("3")
  }
}