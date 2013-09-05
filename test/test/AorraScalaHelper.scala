package test

import java.io.ByteArrayInputStream
import java.io.FileOutputStream
import org.apache.commons.io.IOUtils
import org.specs2.execute.PendingException
import org.specs2.matcher.MatchFailureException
import javax.jcr.Session
import models.User
import play.api.libs.Files.TemporaryFile
import play.api.mvc.AnyContentAsMultipartFormData
import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import play.api.test.FakeHeaders
import play.api.test.WithApplication
import play.libs.F
import play.test.{FakeRequest => JFakeRequest}
import service.GuiceInjectionPlugin
import service.filestore.FileStore
import play.api.mvc.AnyContentAsMultipartFormData

object AorraScalaHelper {

  def fakeAorraApp = AorraTestUtils.fakeAorraApp().getWrappedApplication

  class FakeAorraApp extends WithApplication(fakeAorraApp)

  def filestore = injector.getInstance(classOf[FileStore])
  def injector = GuiceInjectionPlugin.getInjector(play.Play.application())


  def testMultipartFormBody(
      filename: String, content: String, mimeType: String
      ): AnyContentAsMultipartFormData  =
    testMultipartFormBody(filename, content.getBytes, mimeType)

  def testMultipartFormBody(
      filename: String, content: Array[Byte], mimeType: String
      ): AnyContentAsMultipartFormData = {
    val tf = TemporaryFile("multipart", "test")
    IOUtils.copy(new ByteArrayInputStream(content),
        new FileOutputStream(tf.file))
    AnyContentAsMultipartFormData(
      new MultipartFormData(Map(), List(
        FilePart("files", filename,
          Some(mimeType),
          tf)
        ), List(), List()))
  }

  // Convert asAdminUser to work well with Scala
  def asAdminUser(f: (Session, User, FakeHeaders) => Unit) = {
    try {
      AorraTestUtils.asAdminUser(
          new F.Function3[Session, User, JFakeRequest, Session]() {
        def apply(session: Session, u: User, jfr: JFakeRequest) = {
          val headers = jfr.getWrappedRequest().headers match {
            case fakeHeaders: FakeHeaders => fakeHeaders
          }
          f(session, u, headers)
          session
        }
      });
    } catch {
      // Catch and rethrow Specs2 exceptions
      case e: RuntimeException =>
        e.getCause() match {
          case pe: PendingException => throw pe
          case mfe: MatchFailureException[_] => throw mfe
          case _ => throw e
        }
    }
  }

}