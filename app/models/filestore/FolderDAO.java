package models.filestore;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;

public class FolderDAO extends AbstractJcrDAO<Folder> {

  public FolderDAO(Session session, Jcrom jcrom) {
    super(session, jcrom);
  }

}
