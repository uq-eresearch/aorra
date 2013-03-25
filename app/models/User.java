package models;

import securesocial.core.AuthenticationMethod;
import securesocial.core.Identity;
import securesocial.core.OAuth1Info;
import securesocial.core.OAuth2Info;
import securesocial.core.PasswordInfo;
import securesocial.core.SocialUser;
import securesocial.core.UserId;

import javax.jcr.Credentials;
import scala.Option;

/**
 * A immutable wrapper for SocialUser which allows JackRabbit session control.
 **/
public class User implements Identity {

  private Credentials credentials;
  private SocialUser socialUser;

  public User(Credentials credentials, SocialUser socialUser) {
    this.credentials = credentials;
    this.socialUser = socialUser;
  }

  public Credentials credentials() {
    return this.credentials;
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

}