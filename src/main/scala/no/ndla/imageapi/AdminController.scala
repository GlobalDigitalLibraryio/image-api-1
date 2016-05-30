package no.ndla.imageapi

import com.typesafe.scalalogging.LazyLogging
import no.ndla.imageapi.model.Error
import no.ndla.imageapi.model.Error._
import no.ndla.imageapi.network.ApplicationUrl
import no.ndla.logging.LoggerContext
import org.json4s.{DefaultFormats, Formats}
import org.scalatra.json.NativeJsonSupport
import org.scalatra.{Ok, ScalatraServlet}

class AdminController extends ScalatraServlet with NativeJsonSupport with LazyLogging  {

  protected implicit override val jsonFormats: Formats = DefaultFormats

  before() {
    contentType = formats("json")
    LoggerContext.setCorrelationID(Option(request.getHeader("X-Correlation-ID")))
    ApplicationUrl.set(request)
  }

  after() {
    LoggerContext.clearCorrelationID
    ApplicationUrl.clear
  }

  error {
    case t:Throwable => {
      val error = Error(Error.GENERIC, t.getMessage)
      logger.error(error.toString, t)
      halt(status = 500, body = error)
    }
  }

  post("/index") {
    Ok(ComponentRegistry.searchIndexer.indexDocuments())
  }

  get("/extern/:image_id") {
    val externalId = params("image_id")

    logger.info("GET /extern/{}", externalId)

    if(externalId.forall(_.isDigit)) {
      ComponentRegistry.imageRepository.withExternalId(externalId) match {
        case Some(image) => image
        case None => halt(status = 404, body = Error(NOT_FOUND, s"Image with external id $externalId not found"))
      }
    } else {
      halt(status = 404, body = Error(NOT_FOUND, s"Image with external id $externalId not found"))
    }
  }

  post("/import/:range_from/:range_to") {
    val rangeFrom = params("range_from").toInt
    val rangeTo = params("range_to").toInt
    val (numImported, numFailed) = ComponentRegistry.importService.doImport(rangeFrom, rangeTo)
    Ok("Imported " + numImported + " images. Failed to import " + numFailed + " images")
  }

  post("/import/:image_id") {

  }
}
