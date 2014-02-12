package controllers

import scala.collection.JavaConversions.mapAsJavaMap
import scala.concurrent.Future

import com.feth.play.module.pa.PlayAuthenticate

import models.CacheableUser
import play.api.Play
import play.api.mvc.Action
import play.api.mvc.AnyContent
import play.api.mvc.Call
import play.api.mvc.EssentialAction
import play.api.mvc.Request
import play.api.mvc.RequestHeader
import play.api.mvc.Result
import play.api.mvc.Results.Redirect
import play.api.mvc.Security.Authenticated
import play.api.mvc.Security.AuthenticatedBuilder
import play.api.mvc.Security.AuthenticatedRequest
import play.api.mvc.SimpleResult
import providers.CacheableUserProvider
import service.GuiceInjectionPlugin

object ScalaSecured {

  type AuthenticatedUserFunction[R] = CacheableUser => Request[AnyContent] => R

  def isAuthenticated(f: => AuthenticatedUserFunction[Result]): EssentialAction =
    Authenticated(user, onUnauthorized) { user =>
      Action(request => f(user)(request))
    }

  def isAuthenticatedAsync(f: => AuthenticatedUserFunction[Future[SimpleResult]]): Action[AnyContent] =
    AuthenticatedBuilder(user, onUnauthorized).async({ authReq: AuthenticatedRequest[AnyContent, CacheableUser] =>
      f(authReq.user)(authReq)
    })

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