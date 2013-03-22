package models;

import securesocial.core.AuthenticationMethod;
import securesocial.core.Identity;
import securesocial.core.OAuth1Info;
import securesocial.core.OAuth2Info;
import securesocial.core.PasswordInfo;
import securesocial.core.SocialUser;
import securesocial.core.UserId;

import javax.jcr.Credentials;
import javax.jcr.LoginException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;

import org.apache.jackrabbit.core.security.authentication.CryptedSimpleCredentials;
import scala.Option;

/**
 * A immutable wrapper for SocialUser which allows JackRabbit session control.
 **/
public class User implements Identity {

  private org.apache.jackrabbit.api.security.user.User jackrabbitUser;
  private SocialUser socialUser;

  public User(org.apache.jackrabbit.api.security.user.User jackrabbitUser,
              SocialUser socialUser) {
    this.jackrabbitUser = jackrabbitUser;
    this.socialUser = socialUser;
  }

  public org.apache.jackrabbit.api.security.user.User jackrabbitUser() {
    return this.jackrabbitUser;
  }

  public UserId id() { return socialUser.id(); }
  public String firstName() { return socialUser.firstName(); }
  public String lastName() { return socialUser.lastName(); }
  public String fullName() { return socialUser.fullName(); }
  public Option<String> email() { return socialUser.email(); }
  public Option<String> avatarUrl() { return socialUser.avatarUrl(); }
  public AuthenticationMethod authMethod() { return socialUser.authMethod(); }
  public Option<OAuth1Info> oAuth1Info() { return socialUser.oAuth1Info(); }
  public Option<OAuth2Info> oAuth2Info() { return socialUser.oAuth2Info(); }

  public Option<PasswordInfo> passwordInfo() {
    return socialUser.passwordInfo();
  }

  /**
   * Get session impersonating this user.
   */
  public Session impersonate(Session session)
      throws LoginException, RepositoryException {
    Credentials usableCreds;
    {
      final Credentials creds = jackrabbitUser.getCredentials();
      if (creds instanceof CryptedSimpleCredentials) {
        usableCreds = new SimpleCredentials(
            ((CryptedSimpleCredentials) creds).getUserID(), "".toCharArray());
      } else if (creds instanceof SimpleCredentials) {
        usableCreds = creds;
      } else {
        throw new RuntimeException(
            "You can't impersonate with those credentials.");
      }
    }
    return session.impersonate(usableCreds);
  }

}