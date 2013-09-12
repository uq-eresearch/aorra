package controllers;

import models.CacheableUser;
import play.Play;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;
import providers.CacheableUserProvider;
import service.GuiceInjectionPlugin;

import com.feth.play.module.pa.PlayAuthenticate;

public class SubjectPresentAction extends Action.Simple {

  @Override
  public Result call(final Context ctx) throws Throwable {
    if (getUser(ctx) == null) {
      return redirect(PlayAuthenticate.getResolver().login());
    }
    return delegate.call(ctx);
  }

  protected CacheableUserProvider getSubjectHandler() {
    return GuiceInjectionPlugin.getInjector(Play.application())
        .getInstance(CacheableUserProvider.class);
  }

  protected CacheableUser getUser(Context ctx) {
    return getSubjectHandler().getUser(ctx.session());
  }

}
