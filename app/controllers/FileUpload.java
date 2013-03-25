package controllers;

import java.io.File;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import securesocial.core.Identity;
import securesocial.core.java.SecureSocial;

public final class FileUpload extends Controller {

  @SecureSocial.SecuredAction
  public final static Result postUpload() {
    final Identity user = getUser();
    final MultipartFormData body = request().body().asMultipartFormData();
    if (body == null) {
      return badRequest("POST must contain multipart form data.")
          .as("text/plain");
    }
    // Assemble the JSON response
    final ObjectNode json = Json.newObject();
    {
      final ArrayNode aNode = json.putArray("files");
      for (MultipartFormData.FilePart filePart : body.getFiles()) {
        final ObjectNode jsonFileData = Json.newObject();
        {
          final String fileName = filePart.getFilename();
          final File file = filePart.getFile();
          Logger.info(String.format(
            "file %s content type %s uploaded to %s by %s",
            fileName, filePart.getContentType(), file.getAbsolutePath(),
            user.id().id()));
          jsonFileData.put("name", fileName);
          jsonFileData.put("size", file.length());
        }
        aNode.add(jsonFileData);
      }
    }
    // even though we return json set the content type to text/html
    // to prevent IE/Opera from opening a download dialog as described here:
    // https://github.com/blueimp/jQuery-File-Upload/wiki/Setup
    return ok(json).as("text/html");
  }

  @SecureSocial.SecuredAction
  public final static Result getUpload() {
    return ok(views.html.upload.render());
  }

  private final static Identity getUser() {
    return (Identity) ctx().args.get(SecureSocial.USER_KEY);
  }

}
