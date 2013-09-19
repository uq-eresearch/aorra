package controllers

import scala.annotation.implicitNotFound
import scala.collection.JavaConversions.mapAsJavaMap
import com.feth.play.module.pa.PlayAuthenticate
import models.CacheableUser
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.mvc.Security.Authenticated
import providers.CacheableUserProvider
import play.api.Play
import service.GuiceInjectionPlugin

object ScalaSecured {

  def isAuthenticated(f: => CacheableUser => Request[AnyContent] => Result) = {
    Authenticated(user, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }
  }

  private def user(request: RequestHeader): Option[CacheableUser] =
    Option(getSubjectHandler().getUser(javaSession(request)))

  private def getSubjectHandler(): CacheableUserProvider =
    injector.getInstance(classOf[CacheableUserProvider])

  private def onUnauthorized(request: RequestHeader) = {
    Redirect(toScalaCall(PlayAuthenticate.getResolver.login))
  }

  private def javaSession(request: RequestHeader): play.mvc.Http.Session = {
    new play.mvc.Http.Session(request.session.data)
  }

  private def toScalaCall(call: play.mvc.Call): Call = {
    Call(call.method, call.url)
  }

  private def injector =
    Play.current.plugin(classOf[GuiceInjectionPlugin]).get.getInjector()

}