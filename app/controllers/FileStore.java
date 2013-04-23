package controllers;

import javax.jcr.Session;

import org.codehaus.jackson.node.ArrayNode;

import play.libs.F;
import play.mvc.Controller;
import play.mvc.Result;
import service.JcrSessionFactory;

import com.google.inject.Inject;

public final class FileStore extends Controller {

  private final service.filestore.FileStore fileStore;
  private final JcrSessionFactory sessionFactory;

  @Inject
  public FileStore(final JcrSessionFactory sessionFactory,
      final service.filestore.FileStore fileStore) {
    this.fileStore = fileStore;
    this.sessionFactory = sessionFactory;
  }

  public Result tree() {
    return sessionFactory.inSession(new F.Function<Session, Result>() {
      @Override
      public Result apply(Session session) throws Throwable {
        ArrayNode tree =
            (new service.filestore.JsonBuilder(fileStore, session)).tree();
        return ok(tree).as("application/json");
      }
    });
  }



}