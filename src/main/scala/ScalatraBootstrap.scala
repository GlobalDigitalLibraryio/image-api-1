/*
 * Part of NDLA image_api.
 * Copyright (C) 2016 NDLA
 *
 * See LICENSE
 *
 */

import javax.servlet.ServletContext

import no.ndla.imageapi._
import org.scalatra.LifeCycle

class ScalatraBootstrap extends LifeCycle {

  override def init(context: ServletContext) {
    context.mount(ComponentRegistry.imageController, ImageApiProperties.ImageControllerPath, "images")
    context.mount(ComponentRegistry.resourcesApp, "/api-docs")
    context.mount(ComponentRegistry.internController, "/intern")
    context.mount(ComponentRegistry.healthController, ImageApiProperties.HealthControllerPath)
  }
}
