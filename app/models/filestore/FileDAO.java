package models.filestore;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;

public class FileDAO extends AbstractJcrDAO<File> {

  public FileDAO(final Session session, final Jcrom jcrom) {
    super(session, jcrom);
  }

}
