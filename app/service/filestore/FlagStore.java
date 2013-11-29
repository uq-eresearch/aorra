package service.filestore;

import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;

import models.Flag;
import models.FlagDAO;
import models.IdentifiableUser;
import models.User;

import org.apache.jackrabbit.commons.JcrUtils;
import org.jcrom.JcrMappingException;
import org.jcrom.Jcrom;

import service.EventManager.Event;

import com.google.common.collect.ImmutableMap;
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

    public boolean hasFlag(FlagType t, String targetId, User user) {
      return getFlag(t, targetId, user) != null;
    }

    public Flag getFlag(String flagId) {
      return flagDao.loadById(flagId);
    }

    public Flag getFlag(FlagType t, String flagId) {
      try {
        Flag flag = flagDao.loadById(flagId);
        if (!flag.getPath().startsWith(t.getRootPath()))
          return null;
        return flag;
      } catch (JcrMappingException e) {
        return null;
      }
    }

    public Flag getFlag(FlagType t, String targetId, User user) {
      return flagDao.get(t.getRootPath()+"/"+Flag.generateName(targetId, user));
    }

    public Flag setFlag(FlagType t, String targetId, User user) {
      if (hasFlag(t, targetId, user)) {
        return getFlag(t, targetId, user);
      } else {
        return flagDao.create(
            t.getRootPath(),
            new models.Flag(null, targetId, user));
      }
    }

    public void unsetFlag(FlagType t, String flagId) {
      deleteFlag(this.getFlag(t, flagId));
    }

    private void deleteFlag(Flag flag) {
      if (flag != null)
        flagDao.removeById(flag.getId());
    }

  }

  public static class Events {

    public static Event create(Flag flag, FlagType t, IdentifiableUser u)
        throws RepositoryException {
      return new Event("flag:create", nodeInfo(flag, t, u));
    }

    public static Event delete(Flag flag, FlagType t, IdentifiableUser u)
        throws RepositoryException {
      return new Event("flag:delete", nodeInfo(flag, t, u));
    }

    private static Map<String, String> nodeInfo(Flag flag, FlagType t,
        IdentifiableUser u) {
      final ImmutableMap.Builder<String, String> b = ImmutableMap.builder();
      b.put("id", flag.getId());
      b.put("type", t.toString());
      b.put("owner:id", flag.getUser().getId());
      b.put("owner:email", flag.getUser().getEmail());
      b.put("owner:name", flag.getUser().getName());
      b.put("target:id", flag.getTargetId());
      if (!u.getId().equals(flag.getUser().getId())) {
        b.put("instigator:id", u.getId());
        b.put("instigator:email", u.getEmail());
        b.put("instigator:name", u.getName());
      }
      return b.build();
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
