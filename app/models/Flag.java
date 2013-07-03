package models;

import javax.jcr.nodetype.NodeType;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.jcrom.AbstractJcrEntity;
import org.jcrom.annotations.JcrIdentifier;
import org.jcrom.annotations.JcrNode;
import org.jcrom.annotations.JcrProperty;
import org.jcrom.annotations.JcrReference;

@JcrNode(
    nodeType = NodeType.NT_UNSTRUCTURED,
    mixinTypes = {
      NodeType.MIX_CREATED,
      NodeType.MIX_LAST_MODIFIED,
      NodeType.MIX_REFERENCEABLE
    },
    classNameProperty = "className")
public class Flag extends AbstractJcrEntity {

  private static final long serialVersionUID = 1L;

  @JcrIdentifier
  protected String id;

  @JcrProperty
  protected String targetId;

  @JcrReference
  protected User user;

  public Flag() {}

  public Flag(final String id, final String targetId, final User user) {
    this.id = id;
    this.name = generateName(targetId, user);
    this.targetId = targetId;
    this.user = user;
  }

  public String getId() {
    return id;
  }

  public String getTargetId() {
    return targetId;
  }

  public User getUser() {
    return user;
  }

  @Override
  public boolean equals(Object other) {
    return EqualsBuilder.reflectionEquals(this, other);
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

  public static String generateName(final String targetId, final User user) {
    return String.format("%s_%s", user.getId(), targetId);
  }

}
