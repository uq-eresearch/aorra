package models;

import java.util.Collections;
import java.util.List;

import be.objectify.deadbolt.core.models.Permission;
import be.objectify.deadbolt.core.models.Role;
import be.objectify.deadbolt.core.models.Subject;

import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.EmailIdentity;
import com.feth.play.module.pa.user.NameIdentity;

public class CacheableUser implements Subject, NameIdentity, EmailIdentity {

  private final String id;
  private final String provider;
  private final String email;
  private final String name;
  private final String jackrabbitUserId;

  public CacheableUser(AuthUser authUser, User user) {
    this.id = user.getId();
    this.provider = authUser.getProvider();
    this.email = user.getEmail();
    this.name = user.getName();
    this.jackrabbitUserId = user.getJackrabbitUserId();
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

  @Override
  public List<? extends Role> getRoles() {
    return Collections.emptyList();
  }

  @Override
  public List<? extends Permission> getPermissions() {
    return Collections.emptyList();
  }

}