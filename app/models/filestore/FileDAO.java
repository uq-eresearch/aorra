package models.filestore;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;

public class FileDAO extends AbstractJcrDAO<File> {

  public FileDAO(final Session session, final Jcrom jcrom) {
    super(session, jcrom);
  }

  @Override
  public File create(File entity) {
    entity.setLastModified(session.getUserID());
    final File savedEntity = super.create(entity);
    return get(savedEntity.getPath());
  }

  @Override
  public File update(File entity) {
    entity.setLastModified(session.getUserID());
    final File savedEntity = super.update(entity);
    return get(savedEntity.getPath());
  }

}
