package test

import play.api.mvc.MultipartFormData
import play.api.mvc.MultipartFormData.FilePart
import org.apache.commons.io.IOUtils
import java.io.FileOutputStream
import play.api.libs.Files.TemporaryFile
import play.api.mvc.AnyContentAsMultipartFormData
import java.io.ByteArrayInputStream
import org.specs2.execute.AsResult
import play.api.test.WithApplication
import service.GuiceInjectionPlugin
import service.filestore.FileStore

object AorraScalaHelper {

  def fakeAorraApp = AorraTestUtils.fakeAorraApp().getWrappedApplication

  class FakeAorraApp extends WithApplication(fakeAorraApp)

  def filestore = injector.getInstance(classOf[FileStore])
  def injector = GuiceInjectionPlugin.getInjector(play.Play.application())

  def testMultipartFormBody(content: String) = {
    val tf = TemporaryFile(java.io.File.createTempFile("multipart", "test"))
    IOUtils.copy(new ByteArrayInputStream(content.getBytes),
        new FileOutputStream(tf.file))
    AnyContentAsMultipartFormData(
      new MultipartFormData(Map(), List(
        FilePart("files", "test.txt",
          Some("Content-Type: multipart/form-data"),
          tf)
        ), List(), List()))
  }

}