package controllers;

import com.google.inject.Inject;
import play.mvc.Controller;
import play.mvc.Result;

public final class Application extends Controller {

  @Inject
  Application() {}

  public final Result index() {
    // TODO: Implement
    return ok();
  }

  public final Result login() {
    // TODO: Implement
    return ok();
  }

  public final Result oAuthDenied(String providerKey) {
    // TODO: Implement
    return ok();
  }

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

}