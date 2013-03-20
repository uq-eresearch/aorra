package controllers

import java.io.File
import java.util.Iterator
import java.util.List
import play.Logger
import play.api._
import play.api.mvc._
import securesocial.core.SecureSocial

object FileUploadController extends Controller with SecureSocial {

  def postUpload = SecuredAction { implicit request =>
    val body = request.body.asMultipartFormData
    val buf = new StringBuilder("{\"files\": [")
    val files = body.get.files // TODO: Handle no body
    // TODO: Do this without a while loop
    val iter = files.iterator
    while (iter.hasNext) {
      val filePart = iter.next
      val fileName = filePart.filename
      val file = filePart.ref.file
      // TODO: Consider using JsonHelper
      buf.append("{")
      buf.append(s""""name": "$fileName",""")
      buf.append(s""""size": "${file.length}"""")
      buf.append("}")
      if (iter.hasNext) {
        buf.append(",")
      }
      Logger.info(String.format("file %s content type %s uploaded to %s",
        fileName, filePart.contentType, file.getAbsolutePath()))
    }
    buf.append("]}");
    // even though we return json set the content type to text/html to prevent IE/Opera
    // from opening a download dialog as described here:
    // https://github.com/blueimp/jQuery-File-Upload/wiki/Setup
    Ok(buf.toString()).as("text/html")
  }

  def getUpload = SecuredAction { implicit request =>
    Ok(views.html.upload.render())
  }

}
