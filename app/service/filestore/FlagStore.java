package service.filestore;

import java.util.Set;

import javax.jcr.ItemExistsException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.VersionException;

import org.apache.jackrabbit.commons.JcrUtils;
import org.jcrom.Jcrom;

import models.Flag;
import models.FlagDAO;
import models.User;

import com.google.common.collect.ImmutableSet;

public class FlagStore {

  public enum FlagType {
    WATCH ("/flags/watch"),
    EDIT  ("/flags/edit");

    private String rootPath;

    private FlagType(final String rootPath) {
      this.rootPath = rootPath;
    }

    public String getRootPath() {
      return rootPath;
    }
  }


  public class Manager {

    private final FlagDAO flagDao;

    Manager(final Session session) {
      this.flagDao = new FlagDAO(session, jcrom);
    }

    public Set<Flag> getFlags(FlagType t) {
      return ImmutableSet.copyOf(flagDao.findAll(t.getRootPath()));
    }

    public Flag setFlag(FlagType t, String targetId, User user) {
      // Create new flag
      final models.Flag entity = flagDao.create(
          t.getRootPath(),
          new models.Flag(null, targetId, user));
      return entity;
    }

    public void unsetFlag(FlagType t, String flagId) {
      flagDao.removeById(flagId);
    }

  }

  protected final Jcrom jcrom;

  public FlagStore(final Jcrom jcrom, final Session initialSession)
      throws RepositoryException {
    this.jcrom = jcrom;
    for (FlagType ft : FlagType.values()) {
      JcrUtils.getOrCreateByPath(ft.getRootPath(),
          NodeType.NT_UNSTRUCTURED, initialSession);
    }
  }

  public Manager getManager(Session session) {
    return new Manager(session);
  }

}
