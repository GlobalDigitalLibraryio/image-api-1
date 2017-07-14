package no.ndla.imageapi.controller

import javax.servlet.http.HttpServletRequest

import no.ndla.imageapi.model.api.Error
import no.ndla.imageapi.model.domain.ImageStream
import no.ndla.imageapi.repository.ImageRepository
import no.ndla.imageapi.service.{ImageConverter, ImageStorageService}
import org.scalatra.swagger.DataType.ValueDataType
import org.scalatra.swagger.SwaggerSupportSyntax.OperationBuilder
import org.scalatra.swagger.{ResponseMessage, Swagger, SwaggerSupport}

import scala.util.{Failure, Success, Try}

trait RawController {
  this: ImageStorageService with ImageConverter with ImageRepository =>
  val rawController: RawController

  class RawController(implicit val swagger: Swagger) extends NdlaController with SwaggerSupport {
    protected val applicationDescription = "API for accessing image files from ndla.no."

    registerModel[Error]()

    val response404 = ResponseMessage(404, "Not found", Some("Error"))
    val response500 = ResponseMessage(500, "Unknown error", Some("Error"))

    val getImageFile = new OperationBuilder(ValueDataType("file", Some("binary")))
      .nickname("getImageFile")
      .summary("Fetches a raw image")
      .notes("Fetches a image with options to resize and crop")
      .produces("application/octet-stream")
      .authorizations("oauth2")
      .parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("name").description("The name of the image"),
        queryParam[Option[Int]]("width").description("The target width to resize the image. Image proportions are kept intact"),
        queryParam[Option[Int]]("height").description("The target height to resize the image. Image proportions are kept intact"),
        queryParam[Option[String]]("cropStart").description(
          """The first image coordinate (X,Y) specifying the crop start position.
            |The coordinate is a comma separated value of the form column,row (e.g 100,10).
            |If cropStart is specified cropEnd must also be specified""".stripMargin),
        queryParam[Option[String]]("cropEnd").description(
          """The second image coordinate (X,Y) specifying the crop end position, forming a square cutout.
            |The coordinate is a comma separated value of the form column,row (e.g 200,100).
            |If cropEnd is specified cropStart must also be specified""".stripMargin)
      ).responseMessages(response404, response500)

    val getImageFileById = new OperationBuilder(ValueDataType("file", Some("binary")))
      .nickname("getImageFileById")
      .summary("Fetches a raw image using the image id")
      .notes("Fetches a image with options to resize and crop")
      .produces("application/octet-stream")
      .authorizations("oauth2")
      .parameters(
        headerParam[Option[String]]("X-Correlation-ID").description("User supplied correlation-id. May be omitted."),
        headerParam[Option[String]]("app-key").description("Your app-key. May be omitted to access api anonymously, but rate limiting may apply on anonymous access."),
        pathParam[String]("id").description("The ID of the image"),
        queryParam[Option[Int]]("width").description("The target width to resize the image. Image proportions are kept intact"),
        queryParam[Option[Int]]("height").description("The target height to resize the image. Image proportions are kept intact"),
        queryParam[Option[String]]("cropStart").description(
          """The first image coordinate (X,Y) specifying the crop start position.
            |The coordinate is a comma separated value of the form column,row (e.g 100,10).
            |If cropStart is specified cropEnd must also be specified""".stripMargin),
        queryParam[Option[String]]("cropEnd").description(
          """The second image coordinate (X,Y) specifying the crop end position, forming a square cutout.
            |The coordinate is a comma separated value of the form column,row (e.g 200,100).
            |If cropEnd is specified cropStart must also be specified""".stripMargin)
      ).responseMessages(response404, response500)

    get("/:name", operation(getImageFile)) {
      getRawImage(params("name"))
    }

    get("/id/:id", operation(getImageFileById)) {
     imageRepository.withId(long("id")) match {
        case Some(imageMeta) => getRawImage(imageMeta.imageUrl)
        case None => None
      }
    }

    private def getRawImage(imageName: String): ImageStream = {
      imageStorage.get(imageName).flatMap(crop).flatMap(resize) match {
        case Success(img) => img
        case Failure(e) => throw e
      }
    }

    def crop(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      (paramAsListOfInt("cropStart"), paramAsListOfInt("cropEnd")) match {
        case (List(startX, startY), List(endX, endY)) => imageConverter.crop(image, Point(startX, startY), Point(endX, endY))
        case _ => Success(image)
      }
    }

    def resize(image: ImageStream)(implicit request: HttpServletRequest): Try[ImageStream] = {
      extractDoubleOpts("width", "height") match {
        case Seq(Some(width), _) => imageConverter.resizeWidth(image, width.toInt)
        case Seq(_, Some(height)) => imageConverter.resizeHeight(image, height.toInt)
        case Seq(Some(width), Some(height)) => imageConverter.resize(image, width.toInt, height.toInt)
        case _ => Success(image)
      }
    }

  }
}