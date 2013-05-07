package providers;

import play.Play;
import play.mvc.Http.Context;
import play.mvc.Result;
import service.GuiceInjectionPlugin;
import be.objectify.deadbolt.core.models.Subject;
import be.objectify.deadbolt.java.DeadboltHandler;
import be.objectify.deadbolt.java.DynamicResourceHandler;

public class DeadboltHandlerProxy implements DeadboltHandler {

  private DeadboltHandler impl = null;

  @Override
  public Result beforeAuthCheck(Context context) {
    return getImpl().beforeAuthCheck(context);
  }

  @Override
  public Subject getSubject(Context context) {
    return getImpl().getSubject(context);
  }

  @Override
  public Result onAuthFailure(Context context, String content) {
    return getImpl().onAuthFailure(context, content);
  }

  @Override
  public DynamicResourceHandler getDynamicResourceHandler(Context context) {
    return getDynamicResourceHandler(context);
  }

  protected DeadboltHandler getImpl() {
    if (impl == null) {
      impl = Play.application().plugin(GuiceInjectionPlugin.class)
        .getInjector()
        .getInstance(DeadboltHandlerImpl.class);
    }
    return impl;
  }

}
