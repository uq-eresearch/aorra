package controllers;

import play.mvc.Http.Context;
import play.mvc.Result;
import play.mvc.Security;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;

import controllers.routes;

public class Secured extends Security.Authenticator {

  public static final String FLASH_MESSAGE_KEY = "message";

  @Override
  public String getUsername(final Context ctx) {
    final AuthUser u = PlayAuthenticate.getUser(ctx.session());
    return (u != null ? u.getId() : null);
  }

  @Override
  public Result onUnauthorized(final Context ctx) {
    return redirect(routes.Application.index());
  }
}