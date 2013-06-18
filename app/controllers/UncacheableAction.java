package controllers;

import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;

public class UncacheableAction extends Action.Simple {

  @Override
  public Result call(final Context ctx) throws Throwable {
    ctx.response().setHeader("Cache-Control", "no-cache");
    return delegate.call(ctx);
  }

}
