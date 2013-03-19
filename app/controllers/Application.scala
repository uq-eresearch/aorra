package controllers

import play.api._
import play.api.mvc._
import securesocial.core.{Identity, Authorization}
import service.JackrabbitUserService

object Application extends Controller with securesocial.core.SecureSocial {

  def index = UserAwareAction { implicit request =>
    val allUsers = Play.current.plugin(classOf[JackrabbitUserService])
        .get.list
    Ok(views.html.index(request.user, allUsers))
  }

  def userInfo = SecuredAction { implicit request =>
    Ok(views.html.user.info(request.user))
  }

}