package controllers;

import java.text.SimpleDateFormat;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.jcrom.Jcrom;

import play.libs.F;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.CommentStore;
import service.filestore.CommentStore.Comment;
import service.filestore.EventManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

@With(UncacheableAction.class)
public final class CommentController extends SessionAwareController {

  private final CommentStore commentStore;
  private final EventManager eventManager;

  @Inject
  CommentController(JcrSessionFactory sessionFactory, Jcrom jcrom,
      CacheableUserProvider subjectHandler,
      EventManager eventManager,
      CommentStore commentStore) {
    super(sessionFactory, jcrom, subjectHandler);
    this.commentStore = commentStore;
    this.eventManager = eventManager;
  }

  @SubjectPresent
  public Result create(final String targetId) {
    final JsonNode params = ctx().request().body().asJson();
    final String message = params.get("message").asText();
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final CommentStore.Manager csm = commentStore.getManager(session);
        final CommentStore.Comment comment =
            csm.create(getUser().getId(), targetId, message);
        return created(toJson(comment)).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result list(final String targetId) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final CommentStore.Manager csm = commentStore.getManager(session);
        final ArrayNode json = JsonNodeFactory.instance.arrayNode();
        for (final Comment comment : csm.findByTarget(targetId)) {
          json.add(toJson(comment));
        }
        return ok(json).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result get(final String commentId, final String targetIds) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final CommentStore.Manager csm = commentStore.getManager(session);
        final CommentStore.Comment comment = csm.findById(commentId);
        if (comment == null) {
          return notFound();
        }
        return ok(toJson(comment)).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result update(final String commentId, final String targetId) {
    final JsonNode params = ctx().request().body().asJson();
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final CommentStore.Manager csm = commentStore.getManager(session);
        final CommentStore.Comment comment = csm.findById(commentId);
        if (comment == null) {
          return notFound();
        }
        comment.setMessage(params.get("message").asText());
        return ok(toJson(csm.update(comment)))
            .as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result delete(final String commentId, final String targetId) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final CommentStore.Manager csm = commentStore.getManager(session);
        final CommentStore.Comment comment = csm.findById(commentId);
        if (comment == null) {
          return notFound();
        }
        csm.delete(comment);
        return noContent();
      }
    });
  }

  private ObjectNode toJson(CommentStore.Comment comment) {
    final SimpleDateFormat iso8601 = new SimpleDateFormat(
        "yyyy-MM-dd'T'HH:mm:ss.SZ");
    final ObjectNode json = JsonNodeFactory.instance.objectNode();
    json.put("id", comment.getId());
    json.put("userId", comment.getUserId());
    json.put("targetId", comment.getTargetId());
    json.put("message", comment.getMessage());
    json.put("created",
        iso8601.format(comment.getCreationTime().getTime()));
    json.put("modified",
        iso8601.format(comment.getModificationTime().getTime()));
    return json;
  }
}
