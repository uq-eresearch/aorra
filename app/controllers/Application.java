package controllers;

import securesocial.core.java.SecureSocial;
import securesocial.core.Identity;
import service.JackrabbitUserService;
import play.mvc.Controller;
import play.Play;
import play.mvc.Result;
import scala.Option;
import scala.collection.Seq;
import views.html.index;
import views.html.user.info;

public final class Application extends Controller {

  @SecureSocial.UserAwareAction
  public final static Result index() {
    Identity user = (Identity) ctx().args.get(SecureSocial.USER_KEY);
    Seq<Identity> allUsers = userService().list();
    return ok(index.render(Option.apply(user), allUsers));
  }

  @SecureSocial.SecuredAction
  public final static Result userInfo() {
    Identity user = (Identity) ctx().args.get(SecureSocial.USER_KEY);
    return ok(info.render(user));
  }

  private final static JackrabbitUserService userService() {
    return Play.application().plugin(JackrabbitUserService.class);
  }

}