package models;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;

public class FlagDAO extends AbstractJcrDAO<Flag> {

  public FlagDAO(final Session session, final Jcrom jcrom) {
    super(session, jcrom);
  }

}
