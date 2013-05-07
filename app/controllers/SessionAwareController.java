package controllers;

import javax.jcr.Session;

import models.CacheableUser;

import org.jcrom.Jcrom;

import play.libs.F;
import play.mvc.Controller;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;

public abstract class SessionAwareController extends Controller {

  protected final JcrSessionFactory sessionFactory;
  protected final Jcrom jcrom;
  protected final CacheableUserProvider subjectHandler;

  protected SessionAwareController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom, final CacheableUserProvider subjectHandler) {
    this.sessionFactory = sessionFactory;
    this.jcrom = jcrom;
    this.subjectHandler = subjectHandler;
  }

  protected CacheableUser getUser() {
    return subjectHandler.getUser(ctx().session());
  }

  protected <A extends Object> A inUserSession(final F.Function<Session, A> f) {
    return sessionFactory.inSession(getUser().getJackrabbitUserId(), f);
  }

}
