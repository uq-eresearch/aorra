package service.filestore;

import java.util.Calendar;
import java.util.Map;
import java.util.SortedSet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.IdentifiableUser;
import service.EventManager.Event;

import com.google.common.collect.ImmutableMap;

public interface CommentStore {

  public Manager getManager(Session session);

  public static interface Manager {
    public Comment create(String userId, String targetId, String message);
    public Comment findById(String commentId);
    public SortedSet<Comment> findByTarget(String targetId);
    public Comment update(Comment modifiedComment);
    public void delete(Comment comment);
  }

  public static interface Comment extends Comparable<Comment> {
    public String getId();
    public String getUserId();
    public String getTargetId();
    public String getMessage();
    public Calendar getCreationTime();
    public Calendar getModificationTime();
    public void setMessage(String msg);
  }

  public static class Events {

    public static Event create(
        final CommentStore.Comment comment,
        final IdentifiableUser author)
        throws RepositoryException {
      return new Event("comment:create", nodeInfo(comment, author));
    }

    public static Event update(
        final CommentStore.Comment comment,
        final IdentifiableUser author)
        throws RepositoryException {
      return new Event("comment:update", nodeInfo(comment, author));
    }

    private static Map<String, String> nodeInfo(
        final CommentStore.Comment comment,
        final IdentifiableUser author) {
      final ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
      b.put("id", comment.getId());
      b.put("author:id", author.getId());
      b.put("author:email", author.getEmail());
      b.put("author:name", author.getName());
      b.put("target:id", comment.getTargetId());
      return b.build();
    }
  }
}
