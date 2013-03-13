package controllers

import play.api._
import play.api.mvc._
import securesocial.core.{Identity, Authorization}

object Application extends Controller with securesocial.core.SecureSocial {

  def index = SecuredAction { implicit request =>
    Ok(views.html.index(request.user))
  }

}