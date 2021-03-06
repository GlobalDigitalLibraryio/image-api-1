package no.ndla.imageapi.service

import no.ndla.imageapi.ImageApiProperties
import no.ndla.imageapi.model.api.{StoredParameters, License}
import no.ndla.imageapi.model.domain._
import no.ndla.imageapi.model.{ValidationException, ValidationMessage}
import no.ndla.mapping.License.getLicense
import org.jsoup.Jsoup
import org.jsoup.safety.Whitelist
import org.scalatra.servlet.FileItem

import scala.util.{Failure, Success, Try}

trait ValidationService {
  val validationService: ValidationService

  class ValidationService {
    def validateStoredParameters(parameters: StoredParameters): Option[ValidationMessage] = {

      def validPercentage(n: Int): Boolean = n >= 0 && n <= 100
      val r = parameters.rawImageQueryParameters
      if (!Seq(r.cropStartX, r.cropStartY, r.cropEndX, r.cropEndY, r.focalX, r.focalY).flatten.forall(validPercentage)) {
        Some(ValidationMessage("rawImageQueryParameters", "Percentage values must be in the range [0, 100]"))
      } else if (!parameters.imageUrl.startsWith("/")) {
        Some(ValidationMessage("imageUrl", "Image URL has to start with a '/'"))
      } else {
        None
      }
    }

    def validateImageFile(imageFile: FileItem): Option[ValidationMessage] = {
      val validFileExtensions = Seq(".jpg", ".png", ".jpg", ".jpeg", ".bmp", ".gif", ".svg")
      if (!hasValidFileExtension(imageFile.name.toLowerCase, validFileExtensions))
        return Some(ValidationMessage("file", s"The file ${imageFile.name} does not have a known file extension. Must be one of ${validFileExtensions.mkString(",")}"))

      val validMimeTypes = Seq("image/bmp", "image/gif", "image/jpeg", "image/x-citrix-jpeg", "image/pjpeg", "image/png", "image/x-citrix-png", "image/x-png", "image/svg+xml")
      val actualMimeType = imageFile.contentType.getOrElse("")

      if (!validMimeTypes.contains(actualMimeType))
        return Some(ValidationMessage("file", s"The file ${imageFile.name} is not a valid image file. Only valid type is '${validMimeTypes.mkString(",")}', but was '$actualMimeType'"))

      None
    }

    private def hasValidFileExtension(filename: String, extensions: Seq[String]): Boolean = {
      extensions.exists(extension => filename.toLowerCase.endsWith(extension))
    }

    def validate(image: ImageMetaInformation): Try[ImageMetaInformation] = {
      val validationMessages = image.titles.flatMap(title => validateTitle("title", title))  ++
        validateCopyright(image.copyright) ++
        validateTags(image.tags) ++
        image.alttexts.flatMap(alt => validateAltText("altTexts", alt)) ++
        image.captions.flatMap(caption => validateCaption("captions", caption))

      if (validationMessages.isEmpty)
        return Success(image)

      Failure(new ValidationException(errors = validationMessages))
    }

    private def validateTitle(fieldPath: String, title: ImageTitle): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, title.title).toList
    }

    private def validateAltText(fieldPath: String, altText: ImageAltText): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, altText.alttext).toList
    }

    private def validateCaption(fieldPath: String, caption: ImageCaption): Seq[ValidationMessage] = {
      containsNoHtml(fieldPath, caption.caption).toList
    }

    def validateCopyright(copyright: Copyright): Seq[ValidationMessage] = {
      copyright.creators.flatMap(a => validateAuthor("copyright.creators", a, ImageApiProperties.creatorTypes)) ++
      copyright.processors.flatMap(a => validateAuthor("copyright.processors", a, ImageApiProperties.processorTypes)) ++
      copyright.rightsholders.flatMap(a => validateAuthor("copyright.rightsholders", a, ImageApiProperties.rightsholderTypes)) ++
      containsNoHtml("copyright.origin", copyright.origin) ++
      validateAgreement(copyright)
    }

    def validateAgreement(copyright: Copyright): Seq[ValidationMessage] = {
      copyright.agreementId match {
        case _ => Seq()
      }
    }

    def validateLicense(license: License): Seq[ValidationMessage] = {
      Try(io.digitallibrary.license.model.License(license.license)) match {
        case Success(_) => Seq()
        case Failure(ex) => Seq(ValidationMessage("license.license", ex.getMessage))
      }
    }

    def validateAuthor(fieldPath: String, author: Author, allowedTypes: Seq[String]): Seq[ValidationMessage] = {
      containsNoHtml(s"$fieldPath.type", author.`type`).toList ++
        containsNoHtml(s"$fieldPath.name", author.name).toList ++
        validateAuthorType(s"$fieldPath.type", author.`type`, allowedTypes).toList
    }

    def validateAuthorType(fieldPath: String, `type`: String, allowedTypes: Seq[String]): Option[ValidationMessage] = {
      if(allowedTypes.contains(`type`.toLowerCase)) {
        None
      } else {
        Some(ValidationMessage(fieldPath, s"Author is of illegal type. Must be one of ${allowedTypes.mkString(", ")}"))
      }
    }

    def validateTags(tags: Seq[ImageTag]): Seq[ValidationMessage] = {
      tags.flatMap(tagList => {
        tagList.tags.flatMap(containsNoHtml("tags.tags", _)).toList
      })
    }

    private def containsNoHtml(fieldPath: String, text: String): Option[ValidationMessage] = {
      if (Jsoup.isValid(text, Whitelist.none())) {
        None
      } else {
        Some(ValidationMessage(fieldPath, "The content contains illegal html-characters. No HTML is allowed"))
      }
    }

  }
}
