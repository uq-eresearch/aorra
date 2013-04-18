package models;

import javax.jcr.Credentials;
import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;

public class User {

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