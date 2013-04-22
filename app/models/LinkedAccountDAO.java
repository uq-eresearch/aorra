package models;

import javax.jcr.Session;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;

public class LinkedAccountDAO extends AbstractJcrDAO<LinkedAccount> {

  public static final String PATH_ATTRIBUTE = "actualUserPath";

  public final JackrabbitSession session;

  public LinkedAccountDAO(Session session, Jcrom jcrom) {
    super(LinkedAccount.class, session, jcrom);
    this.session = (JackrabbitSession) session;
  }

}
