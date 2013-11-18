package service.filestore;

import java.util.Calendar;
import java.util.SortedSet;

import javax.jcr.Session;

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
}
