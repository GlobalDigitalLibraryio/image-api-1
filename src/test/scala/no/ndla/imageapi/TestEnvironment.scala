/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

package no.ndla.imageapi


import com.amazonaws.services.s3.AmazonS3
import io.digitallibrary.network.GdlClient
import no.ndla.imageapi.auth.{Role, User}
import no.ndla.imageapi.controller._
import no.ndla.imageapi.integration._
import no.ndla.imageapi.repository._
import no.ndla.imageapi.service._
import no.ndla.imageapi.service.search.{IndexBuilderService, IndexService, SearchConverterService, SearchService}
import org.scalatest.mockito.MockitoSugar

trait TestEnvironment
    extends ElasticClient
    with IndexService
    with SearchService
    with SearchConverterService
    with DataSource
    with ConverterService
    with ValidationService
    with ImageRepository
    with WriteService
    with AmazonClient
    with ImageStorageService
    with IndexBuilderService
    with GdlClient
    with InternController
    with ImageControllerV2
    with HealthController
    with RawController
    with ImageConverter
    with MockitoSugar
    with User
    with Role
    with Clock
    with CloudinaryClient
{
  val amazonClient = mock[AmazonS3]
  val esClient = mock[E4sClient]

  val dataSource = mock[javax.sql.DataSource]
  val indexService = mock[IndexService]
  val searchService = mock[SearchService]
  val indexBuilderService = mock[IndexBuilderService]
  val imageRepository = mock[ImageRepository]
  val writeService = mock[WriteService]
  val imageStorage = mock[AmazonImageStorageService]

  val gdlClient = mock[GdlClient]
  val rawController = mock[RawController]
  val internController = mock[InternController]
  val imageControllerV2= mock[ImageControllerV2]
  val converterService = mock[ConverterService]
  val validationService = mock[ValidationService]
  val searchConverterService = mock[SearchConverterService]
  val imageConverter = mock[ImageConverter]
  val healthController = mock[HealthController]

  val clock = mock[SystemClock]
  val authUser = mock[AuthUser]
  val authRole = new AuthRole
  val cloudinaryClient = mock[CloudinaryClient]
}
