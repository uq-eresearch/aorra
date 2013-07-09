package models;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;

import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.EmailIdentity;
import com.feth.play.module.pa.user.NameIdentity;
import com.google.common.collect.ImmutableList;

public class CacheableUser implements Subject, NameIdentity, EmailIdentity,
    Serializable {

  private static final long serialVersionUID = 2956403030598144283L;

  private final String id;
  private final String provider;
  private final String email;
  private final String name;
  private final String jackrabbitUserId;
  private final List<Role> roles;

  public CacheableUser(AuthUser authUser, User user, Iterable<Role> roles) {
    this.id = user.getId();
    this.provider = authUser.getProvider();
    this.email = user.getEmail();
    this.name = user.getName();
    this.jackrabbitUserId = user.getJackrabbitUserId();
    this.roles = ImmutableList.copyOf(roles);
  }

  @Override
  public String getId() { return id; }

  @Override
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
    for (Role role : roles) {
      if (role.getName().equals(name))
        return true;
    }
    return false;
  }

  @Override
  public List<? extends Role> getRoles() {
    return roles;
  }

  @Override
  public List<? extends Permission> getPermissions() {
    return Collections.emptyList();
  }

}