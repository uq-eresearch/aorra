package models;

import com.feth.play.module.pa.user.NameIdentity;

public interface IdentifiableUser extends NameIdentity {

  public String getId();
  String getEmail();

}
