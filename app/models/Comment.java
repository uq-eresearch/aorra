package models;

import java.util.Calendar;
import java.util.Date;

import javax.jcr.nodetype.NodeType;

import org.jcrom.annotations.JcrCreated;
import org.jcrom.annotations.JcrIdentifier;
import org.jcrom.annotations.JcrName;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrPath;
import org.jcrom.annotations.JcrProperty;
import org.jcrom.annotations.JcrProtectedProperty;
import org.jcrom.annotations.JcrReference;

@JcrNode(
  nodeType = NodeType.NT_UNSTRUCTURED,
  mixinTypes = {
    NodeType.MIX_CREATED,
    NodeType.MIX_LAST_MODIFIED,
    NodeType.MIX_REFERENCEABLE
  },
  classNameProperty = "className")
public class Comment {

  @JcrIdentifier
  private String id;
  @JcrPath
  public String nodePath;
  @JcrProperty
  private String message;
  @JcrProperty
  private boolean read;
  @JcrName
  protected String targetId;
  @JcrProperty
  private String userId;
  @JcrProtectedProperty(name = javax.jcr.Property.JCR_CREATED)
  private Calendar created;
  @JcrProperty(name = javax.jcr.Property.JCR_LAST_MODIFIED)
  protected Calendar lastModified;

  public Comment() {
  }

  public Comment(String userId, String targetId, String message) {
    this.userId = userId;
    this.targetId = targetId;
    this.message = message;
  }

  public Calendar getCreated() {
    return created;
  }

  public String getId() {
    return id;
  }

  public Calendar getLastModified() {
    if (lastModified == null)
      return created;
    else
      return lastModified;
  }

  public String getMessage() {
    return message;
  }

  public String getTargetId() {
    return targetId;
  }

  public String getUserId() {
    return userId;
  }

  public void setMessage(String message) {
    this.message = message;
    this.lastModified = Calendar.getInstance();
  }

  @Override
  public String toString() {
    return String.format("[%s] %s", id, message);
  }

}
