/**
 * Copyright 2012 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package service

import collection.JavaConversions._
import com.fasterxml.uuid.EthernetAddress
import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.Generators.timeBasedGenerator
import com.fasterxml.uuid.UUIDGenerator
import com.wingnest.play2.jackrabbit.Jcr
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts
import javax.jcr.SimpleCredentials
import javax.jcr.{Repository, Value, ValueFactory}
import org.apache.jackrabbit.api.JackrabbitSession
import org.apache.jackrabbit.api.security.user.{User => JackrabbitUser}
import org.apache.jackrabbit.api.security.user.Query
import org.apache.jackrabbit.api.security.user.QueryBuilder
import org.apache.jackrabbit.api.security.user.QueryBuilder.Direction
import org.apache.jackrabbit.api.security.user.UserManager
import org.apache.jackrabbit.core.value.InternalValue
import org.joda.time.DateTime
import org.joda.time.Duration
import org.mindrot.jbcrypt.BCrypt
import play.api.{Application, Logger}
import securesocial.core._
import securesocial.core.providers.Token
import javax.jcr.Session
import models.User

class JackrabbitUserService(implicit val application: Application)
    extends UserServicePlugin(application)
    with CachingTokenProvider
    with JackrabbitSocialUserProvider {

  override def inSession[A](op: (Session) => A): A = {
    val session = Jcr.login(
      confStr(ConfigConsts.CONF_JCR_USERID).get,
      confStr(ConfigConsts.CONF_JCR_PASSWORD).get)
    op(session)
    // Note: We don't close the session, because it constantly gets reused.
  }

  private def confStr(k: String) = {
    application.configuration.getString(k)
  }
}

trait JackrabbitSocialUserProvider {

  val UserIdPrefix = "user-"

  lazy val uuidGenerator = {
    Generators.timeBasedGenerator(EthernetAddress.fromInterface())
  }

  def inSession[A](op: (Session) => A): A

  case class SocialUserDao(val session: Session) {

    implicit def userIdStr(id: UserId): String = id.id + ":" + id.providerId
    def strToValue(s: String): Value = valueFactory.createValue(s)

    def userManager = {
      session.asInstanceOf[JackrabbitSession].getUserManager()
    }

    def valueFactory = {
      session.getValueFactory()
    }

    def load(authorizable: JackrabbitUser): Identity = {
      val a = StringPropertyHolder(authorizable)
      val userId = UserId(
        a.prop("id/user").getString.get, a.prop("id/provider").getString.get)
      val oAuth1Info = {
        a.prop("oauth1/token").getString flatMap { token =>
          a.prop("oauth1/secret").getString flatMap { secret =>
            Some(OAuth1Info(token, secret))
          }
        }
      }
      val oAuth2Info = {
        a.prop("oauth2/accessToken").getString flatMap { token =>
          Some(
            OAuth2Info(token, a.prop("oauth2/tokenType").getString,
              a.prop("oauth2/expiresIn").getInt,
              a.prop("refreshToken").getString))
        }
      }
      val passwordInfo = {
        a.prop("password/hasher").getString flatMap { hasher =>
          a.prop("password/password").getString flatMap { hash =>
            Some(PasswordInfo(hasher, hash, a.prop("password/salt").getString))
          }
        }
      }
      User(
          authorizable,
          SocialUser(
            userId,
            a.prop("names/first").getString.get,
            a.prop("names/last").getString.get,
            a.prop("names/full").getString.get,
            a.prop("email").getString,
            a.prop("avatarUrl").getString,
            AuthenticationMethod(a.prop("authMethod").getString.get),
            oAuth1Info,
            oAuth2Info,
            passwordInfo))
    }

    def save(authorizable: JackrabbitUser, user: Identity): Identity = {
      val a = StringPropertyHolder(authorizable)
      a prop("id/user")		set user.id.id
      a prop("id/provider")	set user.id.providerId
      a prop("names/first")	set user.firstName
      a prop("names/last") 	set	user.lastName
      a prop("names/full")	set	user.fullName
      a prop("email")			set user.email
      a prop("avatarUrl")		set user.avatarUrl
      a prop("authMethod")	set user.authMethod.method
      user.oAuth1Info foreach { info =>
        a prop("oauth1/token")	set info.token
        a prop("oauth1/secret")	set info.secret
      }
      user.oAuth2Info foreach { info =>
        a prop("oauth2/accessToken")	set info.accessToken
        a prop("oauth2/type")			set info.tokenType
        a prop("oauth2/expiresIn")		set info.expiresIn
        a prop("oauth2/refreshToken")	set info.refreshToken
      }
      user.passwordInfo foreach { info =>
        a prop("password/hasher")	set info.hasher
        a prop("password/password")	set info.password
        a prop("password/salt")		set info.salt
      }
      load(authorizable)
    }

    case class StringPropertyHolder(authorizable: JackrabbitUser) {
      def prop(relPath: String) = AuthProp(relPath)

      case class AuthProp(val relPath: String) {
        // We have to be clear about types
        def getInt: Option[Int] = getProp(relPath, v => v.getLong.toInt)
        def getString: Option[String] = getProp(relPath, v => v.getString)

        // We can set properties for values
        def set(v: Value) = authorizable.setProperty(relPath, v)
        // Assume everything is a potential value, and use a partial to catch
        // our errors.
        def set(a: Any): Unit = a match {
          case v: Integer => set(valueFactory.createValue(v.toLong))
          case v: String  => set(valueFactory.createValue(v))
        }
        // For simplicity, we handle options of all types
        def set(ov: Option[Any]): Unit = ov foreach { set(_) }

        // Helper for get
        private def getProp[A](relPath: String, f: Value => A): Option[A] = {
          authorizable.getProperty(relPath) match {
            case null => None
            case array => array.toSeq.headOption map f
          }
        }
      }
    }

    /**
     * Finds a user that maches the specified id
     *
     * @param id the user id
     * @return an optional user
     */
    def find(id: UserId): Option[Identity] = {
      findAuthorizable(id) flatMap { a => Some(load(a)) }
    }

    /**
     * Finds a user by email and provider id.
     *
     * @param email - the user email
     * @param providerId - the provider id
     * @return
     */
    def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = {
      // TODO: Implement
      None
    }

    /**
     * Saves the user.  This method gets called when a user logs in.
     * This is your chance to save the user information in your backing store.
     * @param user
     */
    def save(user: Identity): Identity = {

      val authorizable = findAuthorizable(user.id) match {
        case Some(a) => a
        case None => userManager.createUser(newUserId, "")
      }
      save(authorizable, user)
    }

    private def newUserId = {
      UserIdPrefix + uuidGenerator.generate()
    }

    private def findAuthorizable(id: UserId): Option[JackrabbitUser] = {
      val query = new Query() {
        override def build[C](qb: QueryBuilder[C]) {
          val userMatch: C = qb.eq("id/user", strToValue(id.id))
          val providerMatch: C = qb.eq("id/provider", strToValue(id.providerId))
          qb.setCondition(qb.and(userMatch, providerMatch))
          qb.setSortOrder("id/@user", Direction.ASCENDING);
          qb.setSelector(classOf[JackrabbitUser]);
        }
      }
      userManager.findAuthorizables(query).toSeq match {
        case Seq(user: JackrabbitUser, _*) => Some(user)
        case _ => None
      }
    }

  }

  /**
   * Finds a user that maches the specified id
   *
   * @param id the user id
   * @return an optional user
   */
  def find(id: UserId): Option[Identity] = {
    inSession { s => SocialUserDao(s).find(id) }
  }

  /**
   * Finds a user by email and provider id.
   *
   * @param email - the user email
   * @param providerId - the provider id
   * @return
   */
  def findByEmailAndProvider(email: String, providerId: String): Option[Identity] = {
    // TODO: Implement
    None
  }

  /**
   * Saves the user.  This method gets called when a user logs in.
   * This is your chance to save the user information in your backing store.
   * @param user
   */
  def save(user: Identity): Identity = {
    inSession { s => SocialUserDao(s).save(user) }
  }

}

trait CachingTokenProvider {
  import play.api.cache.Cache

  val application: Application

  val TokenPrefix = "securesocial-token-"

  /**
   * Saves a token.  This is needed for users that
   * are creating an account in the system instead of using one in a 3rd party system.
   *
   * @param token The token to save
   * @return A string with a uuid that will be embedded in the welcome email.
   */
  def save(token: Token) {
    val validFor = new Duration(DateTime.now, token.expirationTime)
    Cache.set(TokenPrefix+token.uuid, token,
        validFor.getStandardSeconds().toInt)(application)
  }

  /**
   * Finds a token
   *
   * @param uuid the token uuid
   * @return
   */
  def findToken(uuid: String): Option[Token] = {
    Cache.get(TokenPrefix+uuid)(application) match {
      case Some(t: Token) => Some(t)
      case _ => None
    }
  }

  /**
   * Deletes a token
   *
   * @param uuid the token uuid
   */
  def deleteToken(uuid: String) {
    Cache.remove(TokenPrefix+uuid)(application)
  }

  /**
   * Deletes all expired tokens
   *
   */
  def deleteExpiredTokens() {
    // Unnecessary - cache handles this
  }

}