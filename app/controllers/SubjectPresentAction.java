package controllers;

import static play.libs.F.Promise.pure;

import models.CacheableUser;
import play.Play;
import play.mvc.SimpleResult;
import play.libs.F.Promise;
import play.mvc.Action;
import play.mvc.Http.Context;
import play.mvc.Result;
import providers.CacheableUserProvider;
import service.GuiceInjectionPlugin;

import com.feth.play.module.pa.PlayAuthenticate;

public class SubjectPresentAction extends Action.Simple {

  @Override
  public Promise<SimpleResult> call(final Context ctx) throws Throwable {
    if (getUser(ctx) == null) {
      return pure(redirect(PlayAuthenticate.getResolver().login()));
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
