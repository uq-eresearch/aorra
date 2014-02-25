package controllers;

import java.util.concurrent.TimeUnit;

import play.core.j.FPromiseHelper;
import play.libs.F;
import play.mvc.HandlerRef;
import play.mvc.Result;
import play.mvc.SimpleResult;
import play.test.FakeRequest;
import scala.concurrent.Future;

@SuppressWarnings(value = { "unchecked", "rawtypes", "deprecation" })
public class Helpers {

  private static class P<A> extends F.Promise<A> {

    public P(Future<A> future) {
      super(future);
    }

    @Override
    public A get() {
      return FPromiseHelper.get(this, 5, TimeUnit.MINUTES);
    }

  }

  private static Result invokeHandler(play.api.mvc.Handler handler, FakeRequest fakeRequest) {
      if(handler instanceof play.core.j.JavaAction) {
          play.api.mvc.Action action = (play.api.mvc.Action)handler;
          return wrapScalaResult(action.apply(fakeRequest.getWrappedRequest()));
      } else {
          throw new RuntimeException("This is not a JavaAction and can't be invoked this way.");
      }
  }

  private static SimpleResult wrapScalaResult(scala.concurrent.Future<play.api.mvc.SimpleResult> result) {
    if (result == null) {
        return null;
    } else {
        final play.api.mvc.SimpleResult simpleResult = new P<play.api.mvc.SimpleResult>(result).get();
        return new SimpleResult() {
            public play.api.mvc.SimpleResult getWrappedSimpleResult() {
                return simpleResult;
            }
        };
    }
}
  /**
   * Call an action method while decorating it with the right @With interceptors.
   */
  public static Result callAction(HandlerRef actionReference) {
      return callAction(actionReference, fakeRequest());
  }

  /**
   * Call an action method while decorating it with the right @With interceptors.
   */
  public static Result callAction(HandlerRef actionReference, FakeRequest fakeRequest) {
      play.api.mvc.HandlerRef handlerRef = (play.api.mvc.HandlerRef)actionReference;
      return invokeHandler(handlerRef.handler(), fakeRequest);
  }

  /**
   * Build a new GET / fake request.
   */
  public static FakeRequest fakeRequest() {
      return new FakeRequest();
  }

}
