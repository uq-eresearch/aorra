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
    Seq<Identity> allUsers = userService().list();
    return ok(index.render(Option.apply(getUser()), allUsers));
  }

  @SecureSocial.SecuredAction
  public final static Result userInfo() {
    return ok(info.render(getUser()));
  }

  private final static JackrabbitUserService userService() {
    return Play.application().plugin(JackrabbitUserService.class);
  }

  private final static Identity getUser() {
    return (Identity) ctx().args.get(SecureSocial.USER_KEY);
  }

}