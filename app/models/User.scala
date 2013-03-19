package models

import securesocial.core._
import org.apache.jackrabbit.api.security.user.{User => JackrabbitUser}
import javax.jcr.{Session, SimpleCredentials}
import org.apache.jackrabbit.core.security.authentication._

/**
 * A immutable wrapper for SocialUser which allows JackRabbit session control.
 **/
case class User(
    val jackrabbitUser: JackrabbitUser,
    val socialUser: SocialUser)
    extends Identity {

  override def id: UserId = socialUser.id
  override def firstName: String = socialUser.firstName
  override def lastName: String = socialUser.lastName
  override def fullName: String = socialUser.fullName
  override def email: Option[String] = socialUser.email
  override def avatarUrl: Option[String] = socialUser.avatarUrl
  override def authMethod: AuthenticationMethod = socialUser.authMethod
  override def oAuth1Info: Option[OAuth1Info] = socialUser.oAuth1Info
  override def oAuth2Info: Option[OAuth2Info] = socialUser.oAuth2Info
  override def passwordInfo: Option[PasswordInfo] = socialUser.passwordInfo

  /**
   * Get session impersonating this user.
   */
  def impersonate(session: Session) = {
    val creds = jackrabbitUser.getCredentials() match {
      case c: CryptedSimpleCredentials => crypted2simple(c)
      case sc: SimpleCredentials => sc
      // TODO: Use better exception
      case _ => sys.error("You can't impersonate without those credentials.")
    }
    session.impersonate(creds)
  }

  def crypted2simple(creds: CryptedSimpleCredentials) = {
    new SimpleCredentials(creds.getUserID(), "".toCharArray())
  }

}