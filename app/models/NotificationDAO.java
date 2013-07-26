package models;

import javax.jcr.Session;

import org.jcrom.Jcrom;
import org.jcrom.dao.AbstractJcrDAO;

public class NotificationDAO extends AbstractJcrDAO<Notification> {

    public NotificationDAO(Session session, Jcrom jcrom) {
        super(session, jcrom);
    }

}
