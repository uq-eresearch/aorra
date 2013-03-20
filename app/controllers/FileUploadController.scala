package controllers

import java.io.File
import java.util.Iterator
import java.util.List
import play.Logger
import play.api._
import play.api.libs.json.Json
import play.api.mvc._
import securesocial.core.SecureSocial

object FileUploadController extends Controller with SecureSocial {

  def postUpload = SecuredAction { implicit request =>
    val body = request.body.asMultipartFormData
    body match {
      case Some(body) =>
        val json = Json.obj(
          "files" -> body.files.map { filePart =>
            val fileName = filePart.filename
            val file = filePart.ref.file
            Logger.info(String.format(
              "file %s content type %s uploaded to %s by %s",
              fileName, filePart.contentType, file.getAbsolutePath(),
              request.user.id.id))
            Json.obj(
              "name" -> fileName,
              "size" -> file.length)
          })
        // even though we return json set the content type to text/html
        // to prevent IE/Opera from opening a download dialog as described here:
        // https://github.com/blueimp/jQuery-File-Upload/wiki/Setup
        Ok(json).as("text/html")
      case None =>
        BadRequest("POST must contain multipart form data.").as("text/plain")
    }
  }

  def getUpload = SecuredAction { implicit request =>
    Ok(views.html.upload.render)
  }

}
