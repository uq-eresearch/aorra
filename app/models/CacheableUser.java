package models;

import java.io.Serializable;
import java.util.List;

import com.feth.play.module.pa.user.EmailIdentity;
import com.google.common.collect.ImmutableList;

public class CacheableUser implements IdentifiableUser, EmailIdentity,
    Serializable {

  private static final long serialVersionUID = 2956403030598144283L;

  private final String id;
  private final String provider;
  private final String email;
  private final String name;
  private final String jackrabbitUserId;
  private final List<String> roles;

  public CacheableUser(String provider, User user, Iterable<String> roles) {
    this.id = user.getId();
    this.provider = provider;
    this.email = user.getEmail();
    this.name = user.getName();
    this.jackrabbitUserId = user.getJackrabbitUserId();
    this.roles = ImmutableList.copyOf(roles);
  }

  @Override
  public String getId() { return id; }

  public String getIdentifier() { return id; }

  @Override
  public String getProvider() { return provider; }

  @Override
  public String getEmail() { return email; }

  @Override
  public String getName() { return name; }

  public String getJackrabbitUserId() { return jackrabbitUserId; }

  @Override
  public String toString() {
    return String.format("%s <%s>", name, email);
  }

  public boolean hasRole(String name) {
    for (String role : roles) {
      if (role.equals(name))
        return true;
    }
    return false;
  }

  public List<String> getRoles() {
    return roles;
  }

}