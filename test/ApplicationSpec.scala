package test

import test.AorraTestUtils.fakeApp
import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._

/**
 * Add your spec here.
 * You can mock out a whole application including requests, plugins etc.
 * For more information, consult the wiki.
 */
class ApplicationSpec extends Specification {

  "Application" should {

    "send 404 on a bad request" in {
      running(fakeApp) {
        route(FakeRequest(GET, "/boom")) must beNone
      }
    }

    "send 200 for index page without login" in {
      running(fakeApp) {
        val home = route(FakeRequest(GET, "/")).get
        status(home) must equalTo(OK)
      }
    }

    "send 303 for user info page without login" in {
      running(fakeApp) {
        val home = route(FakeRequest(GET, "/user/info")).get
        status(home) must equalTo(SEE_OTHER)
      }
    }

    "render the login page" in {
      running(fakeApp) {
        val home = route(FakeRequest(GET, "/login")).get

        status(home) must equalTo(OK)
        contentType(home) must beSome.which(_ == "text/html")
        contentAsString(home) must contain ("Login")
      }
    }
  }
}