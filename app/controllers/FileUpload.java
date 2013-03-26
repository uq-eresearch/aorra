package controllers;

import com.google.inject.Inject;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import org.apache.jackrabbit.commons.JcrUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import play.libs.F.Function;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import securesocial.core.Identity;
import securesocial.core.java.SecureSocial;
import service.ContributionFolderProvider;
import service.JcrSessionFactory;

public final class FileUpload extends Controller {

  private final ContributionFolderProvider contributionFolderProvider;
  private final JcrSessionFactory sessionFactory;

  @Inject
  public FileUpload(final ContributionFolderProvider cfp,
      final JcrSessionFactory sessionFactory) {
    this.contributionFolderProvider = cfp;
    this.sessionFactory = sessionFactory;
  }

  @SecureSocial.SecuredAction
  public Result postUpload(final String folderName) {
    return sessionFactory.inSession(new Function<Session, Result>() {
      public final Result apply(Session session) {
        final Identity user = getUser();
        final Node folder = getFolder(session, folderName);
        if (folder == null) {
          return badRequest("A valid folder must be specified.")
              .as("text/plain");
        }
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
              updateFileContents(folder, filePart);
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
    });
  }

  @SecureSocial.SecuredAction
  public Result getUpload() {
    return sessionFactory.inSession(new Function<Session, Result>() {
      public final Result apply(Session session) {
        return ok(views.html.upload.render(contributionFolderProvider
            .getWritable(session)));
      }
    });
  }

  private Identity getUser() {
    return (Identity) ctx().args.get(SecureSocial.USER_KEY);
  }

  private Node getFolder(Session session, String folderName) {
    try {
      for (Node folder : contributionFolderProvider.getWritable(session)) {
        if (folder.getName().equals(folderName)) {
          return folder;
        }
      }
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private void updateFileContents(Node folder,
      MultipartFormData.FilePart filePart) {
    try {
      JcrUtils.putFile(folder, filePart.getFilename(),
          filePart.getContentType(),
          new FileInputStream(filePart.getFile()));
    } catch (RepositoryException e) {
      throw new RuntimeException(e);
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

}
