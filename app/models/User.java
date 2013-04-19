package models;

import javax.jcr.Credentials;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;

import com.feth.play.module.pa.providers.password.UsernamePasswordAuthProvider.UsernamePassword;

import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.Required;

public class User {

  public static class Login implements UsernamePassword {
    @Required
    @Email
    public String email;

    @Required
    public String password;

    @Override
    public String getEmail() {
      return email;
    }

    @Override
    public String getPassword() {
      return password;
    }
  }

  public static class Invite implements UsernamePassword {
    @Required
    @Email
    public String email;

    public String name;

    public Invite() {}
    public Invite(String e, String n) { this.email = e; this.name = n; }

    @Override
    public String getEmail() {
      return email;
    }

    @Override
    public String getPassword() {
      return null;
    }
  }

  private final String internalId;
  private final Credentials credentials;

  public User(Credentials credentials) {
    this.internalId = (credentials instanceof CryptedSimpleCredentials) ? ((CryptedSimpleCredentials) credentials)
        .getUserID() : credentials.toString();
    this.credentials = credentials;
  }

  public String getId() {
    return internalId;
  }

  public Credentials credentials() {
    return this.credentials;
  }

}