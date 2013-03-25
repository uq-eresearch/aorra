package controllers;

import com.google.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;
import scala.Option;
import scala.collection.Seq;
import securesocial.core.Identity;
import securesocial.core.java.SecureSocial;
import service.JackrabbitUserService;
import views.html.index;
import views.html.user.info;

public final class Application extends Controller {

  private JackrabbitUserService userService;

  @Inject
  Application(JackrabbitUserService userService) {
    this.userService = userService;
  }

  @SecureSocial.UserAwareAction
  public final Result index() {
    Seq<Identity> allUsers = userService.list();
    return ok(index.render(Option.apply(getUser()), allUsers));
  }

  @SecureSocial.SecuredAction
  public final Result userInfo() {
    return ok(info.render(getUser()));
  }

  /**
   * Fetches the user Identity from the session context.
   * @returns Identity
   */
  private final Identity getUser() {
    return (Identity) ctx().args.get(SecureSocial.USER_KEY);
  }

}