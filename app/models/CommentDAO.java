package models;

import java.util.List;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;

public class CommentDAO extends AbstractJcrDAO<Comment> {

  public static final String BASE_PATH = "/comments";

  public CommentDAO(final Session session, final Jcrom jcrom) {
    super(session, jcrom);
  }

  @Override
  public Comment create(Comment comment) {
    return create(BASE_PATH, comment);
  }

}
