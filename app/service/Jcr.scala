package service

import com.wingnest.play2.jackrabbit.{Jcr => PluginJcr}
import javax.jcr.Session
import play.api.Play
import com.wingnest.play2.jackrabbit.plugin.ConfigConsts
import models.User

object Jcr {

  def repository = PluginJcr.getRepository()

  /**
   * Perform work in session the admin user.
   */
  def session: SessionWrapper = {
    SessionWrapper({ () =>
      def confStr(k: String) = Play.current.configuration.getString(k)
      PluginJcr.login(
        confStr(ConfigConsts.CONF_JCR_USERID).get,
        confStr(ConfigConsts.CONF_JCR_PASSWORD).get)
    })
  }

  /**
   * Perform work in session as a particular user.
   */
  def session(user: User): SessionWrapper = {
    SessionWrapper({ () =>
      session { adminSession =>
        user.impersonate(adminSession)
      }
    })
  }

  case class SessionWrapper(initSession: () => Session) {
    def apply[A](op: (Session) => A): A = {
      val session = initSession()
      try {
        op(session)
      } finally {
        session.logout
      }
    }
  }
}