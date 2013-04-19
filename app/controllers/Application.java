package controllers;

import models.User.Login;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider;
import com.google.inject.Inject;

import static play.data.Form.form;
import play.Play;
import play.data.Form;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import providers.JackrabbitEmailPasswordAuthProvider;

public final class Application extends Controller {

  @Inject
  Application() {}

  public final Result index() {
    if (!isAuthenticated()) return login();
    // TODO: Implement
    return ok();
  }

  public final Result login() {
    return ok(views.html.Application.login.render(form(Login.class)));
  }

  public final Result postLogin() {
    com.feth.play.module.pa.controllers.Authenticate.noCache(response());
    final Form<Login> filledForm = form(Login.class).bindFromRequest();
    if (filledForm.hasErrors()) {
      // User did not fill everything properly
      return badRequest(views.html.Application.login.render(filledForm));
    } else {
      // Everything was filled
      return UsernamePasswordAuthProvider.handleLogin(ctx());
    }
  }

  public final Result oAuthDenied(String providerKey) {
    // TODO: Implement
    return ok();
  }

  @Security.Authenticated(Secured.class)
  public final Result userInfo() {
    // TODO: Implement
    return ok();
  }

  public final Result userExists() {
    // TODO: Implement
    return ok();
  }

  public final Result userUnverified() {
    // TODO: Implement
    return ok();
  }


  private boolean isAuthenticated() {
    return PlayAuthenticate.getUser(ctx().session()) != null;
  }

  private JackrabbitEmailPasswordAuthProvider provider() {
    return Play.application().plugin(JackrabbitEmailPasswordAuthProvider.class);
  }


}