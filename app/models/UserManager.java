package models;

import javax.jcr.Session;
import org.apache.jackrabbit.api.JackrabbitSession;

public abstract class UserManager {

  public static final String PATH_ATTRIBUTE = "actualUserPath";

  public final JackrabbitSession session;

  public UserManager(Session session) {
    this.session = (JackrabbitSession) session;
  }

}
