package models;

import java.util.Date;

import javax.jcr.nodetype.NodeType;

import org.jcrom.annotations.JcrCreated;
import org.jcrom.annotations.JcrIdentifier;
import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrParentNode;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProperty;

@JcrNode(
        nodeType = NodeType.NT_UNSTRUCTURED,
        mixinTypes = {
          NodeType.MIX_CREATED,
          NodeType.MIX_LAST_MODIFIED,
          NodeType.MIX_REFERENCEABLE
        },
        classNameProperty = "className")
public class Notification {

    @JcrIdentifier private String id;
    @JcrName private String nodeName;
    @JcrPath private String nodePath;
    @JcrProperty private String message;
    @JcrProperty private boolean read;
    @JcrParentNode private User parent;
    @JcrCreated private Date created;

    public Notification() {}

    public Notification(User parent, String message) {
        this.parent = parent;
        this.message = message;
        this.read = false;
        this.nodeName = "notification";
    }

    public boolean isRead() {
        return read;
    }

    public void setRead(boolean read) {
        this.read = read;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public Date getCreated() {
        return created;
    }

    @Override
    public String toString() {
      return String.format("[%s] %s", id, message);
    }

}
