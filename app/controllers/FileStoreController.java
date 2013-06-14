package controllers;

import helpers.FileStoreHelper;
import helpers.FileStoreHelper.FileExistsException;
import helpers.FileStoreHelper.FolderExistsException;
import helpers.FileStoreHelper.FolderNotFoundException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.User;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.jcrom.Jcrom;
import org.jcrom.util.PathUtils;

import play.Logger;
import play.Play;
import play.api.http.MediaRange;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import providers.CacheableUserProvider;
import service.GuiceInjectionPlugin;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import service.filestore.FileStore.Folder;
import service.filestore.FileStoreImpl;
import service.filestore.JsonBuilder;
import be.objectify.deadbolt.java.actions.SubjectPresent;

import com.google.inject.Inject;
import com.google.inject.Injector;

public final class FileStoreController extends SessionAwareController {

  private final FileStore fileStoreImpl;

  @Inject
  public FileStoreController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom,
      final CacheableUserProvider sessionHandler,
      final FileStore fileStoreImpl) {
    super(sessionFactory, jcrom, sessionHandler);
    this.fileStoreImpl = fileStoreImpl;
  }

  @SubjectPresent
  public Result index() {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        return ok(views.html.FileStoreController.index.render(
            fileStoreImpl.getEventManager().getLastEventId(),
            jb.toJson(fm.getFolders()))).as("text/html");
      }
    });
  }

  @SubjectPresent
  public Result mkdir(final String folderId, final String path) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStoreHelper fh = new FileStoreHelper(session);
        try {
          final FileStore.Folder baseFolder =
              (FileStore.Folder) fm.getByIdentifier(folderId);
          final String absPath =
              baseFolder.getPath() + "/" + PathUtils.relativePath(path);
          fh.mkdir(absPath, true);
        } catch (FileExistsException e) {
          return badRequest(e.getMessage());
        } catch (FolderExistsException e) {
          return badRequest(e.getMessage());
        } catch (FolderNotFoundException e) {
          return badRequest(e.getMessage());
        }
        return created();
      }
    });
  }

  @SubjectPresent
  public Result showFolder(final String folderId) {
    for (final MediaRange m : ctx().request().acceptedTypes()) {
      if (m.accepts("application/json") || m.accepts("text/javascript")) {
        return folderJson(folderId);
      } else if (m.accepts("text/html")) {
        return index();
      }
    }
    return status(UNSUPPORTED_MEDIA_TYPE);
  }

  @SubjectPresent
  public Result showFile(final String fileId) {
    for (final MediaRange m : ctx().request().acceptedTypes()) {
      if (m.accepts("application/json") || m.accepts("text/javascript")) {
        return fileJson(fileId);
      } else if (m.accepts("text/html")) {
        return index();
      }
    }
    return status(UNSUPPORTED_MEDIA_TYPE);
  }

  @SubjectPresent
  public Result filestoreJson() {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        ctx().response().setHeader("Cache-Control", "no-cache");
        return ok(jb.toJson(fm.getFolders())).as("application/json");
      }
    });
  }

  protected Result folderJson(final String folderId) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        FileStore.FileOrFolder fof = fm.getByIdentifier(folderId);
        ctx().response().setHeader("Cache-Control", "no-cache");
        if (fof instanceof FileStore.Folder) {
          return ok(jb.toJsonShallow((FileStore.Folder) fof))
              .as("application/json");
        } else if (fof instanceof FileStore.File) {
          return ok(jb.toJsonShallow((FileStore.File) fof))
              .as("application/json");
        } else {
          return notFound();
        }
      }
    });
  }

  protected Result fileJson(final String fileId) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        FileStore.FileOrFolder fof = fm.getByIdentifier(fileId);
        if (fof instanceof FileStore.File) {
          return ok(jb.toJsonShallow((FileStore.File) fof))
              .as("application/json");
        } else {
          return notFound();
        }
      }
    });
  }

  @SubjectPresent
  public Result delete(final String fileOrFolderId) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        FileStore.FileOrFolder fof = fm.getByIdentifier(fileOrFolderId);
        if (fof == null) {
          return notFound();
        } else {
          fof.delete();
          return noContent();
        }
      }
    });
  }

  @SubjectPresent
  public Result downloadFolder(final String folderId) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException,
          IOException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStore.FileOrFolder fof;
        if (folderId == null) {
          fof = fm.getRoot();
        } else {
          fof = fm.getByIdentifier(folderId);
        }
        if (fof instanceof FileStoreImpl.Folder) {
          final FileStore.Folder folder = (FileStore.Folder) fof;
          final FileStoreHelper fh = new FileStoreHelper(session);
          final java.io.File zipFile = fh.createZipFile(folder);
          ctx().response().setContentType("application/zip");
          ctx().response().setHeader("Content-Disposition",
              "attachment; filename="+fof.getName()+".zip");
          ctx().response().setHeader("Content-Length", zipFile.length()+"");
          return ok(new FileInputStream(zipFile) {
            @Override
            public void close() throws IOException {
              super.close();
              zipFile.delete();
            }
          });
        } else {
          return notFound();
        }
      }
    });
  }

  @SubjectPresent
  public Result downloadFile(final String fileId, final String versionName) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException,
          IOException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStore.FileOrFolder fof = fm.getByIdentifier(fileId);
        if (fof instanceof FileStoreImpl.File) {
          FileStore.File file = (FileStoreImpl.File) fof;
          if (versionName.equals("latest")) {
            ctx().response().setContentType(file.getMimeType());
            ctx().response().setHeader("Content-Disposition",
                "attachment; filename="+file.getName());
            return ok(file.getData());
          } else {
            if (!file.getVersions().containsKey(versionName)) {
              return notFound();
            }
            final FileStore.File version = file.getVersions().get(versionName);
            ctx().response().setContentType(version.getMimeType());
            ctx().response().setHeader("Content-Disposition",
                "attachment; filename="+file.getName());
            return ok(version.getData());
          }
        } else {
          return notFound();
        }
      }
    });
  }

  @SubjectPresent
  public Result fileInfo(final String fileId) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException,
          IOException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStore.FileOrFolder fof = fm.getByIdentifier(fileId);
        if (fof instanceof FileStoreImpl.File) {
          final FileStore.File file = (FileStoreImpl.File) fof;
          final ObjectNode json = Json.newObject();
          {
            final ArrayNode aNode = json.putArray("versions");
            for (String versionName : file.getVersions().keySet()) {
              final FileStore.File version =
                  file.getVersions().get(versionName);
              final User author = version.getAuthor();
              final ObjectNode authorInfo = Json.newObject();
              authorInfo.put("name", author.getName());
              authorInfo.put("email", author.getEmail());
              final ObjectNode versionInfo = Json.newObject();
              versionInfo.put("name", versionName);
              versionInfo.put("author", authorInfo);
              versionInfo.put("timestamp",
                  DateFormatUtils.ISO_DATETIME_FORMAT.format(
                      version.getModificationTime()));
              aNode.add(versionInfo);
            }
          }
          return ok(json).as("application/json");
        } else {
          return notFound();
        }
      }
    });
  }

  @SubjectPresent
  public Result updateFile(final String fileID) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session)
          throws RepositoryException, FileNotFoundException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStore.FileOrFolder fof = fm.getByIdentifier(fileID);
        if (fof == null) {
          return badRequest("A valid folder must be specified.")
              .as("text/plain");
        }
        final FileStore.File f;
        if (fof instanceof FileStore.File) {
          f = (FileStore.File) fof;
        } else {
          return badRequest("Specified destination is not a folder.")
              .as("text/plain");
        }
        final MultipartFormData body = request().body().asMultipartFormData();
        if (body == null) {
          return badRequest("POST must contain multipart form data.")
              .as("text/plain");
        }
        if (body.getFiles().size() > 1) {
          return badRequest("POST cannot contain multiple files.")
              .as("text/plain");
        }
        // Assemble the JSON response
        final ObjectNode json = Json.newObject();
        {
          final ArrayNode aNode = json.putArray("files");
          for (MultipartFormData.FilePart filePart : body.getFiles()) {
            final ObjectNode jsonFileData = Json.newObject();
            final String fileName = filePart.getFilename();
            final File file = filePart.getFile();
            jsonFileData.put("name", fileName);
            jsonFileData.put("size", file.length());
            try {
              f.update(filePart.getContentType(),
                  new FileInputStream(filePart.getFile()));
              session.save();
              Logger.info(String.format(
                "file %s content type %s uploaded to %s by %s",
                fileName, filePart.getContentType(),
                f.getPath(),
                getUser()));
            } catch (ItemExistsException iee) {
              jsonFileData.put("error", iee.getMessage());
            } catch (AccessDeniedException ade) {
              jsonFileData.put("error",
                  "Insufficient permissions to upload files to folder.");
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

  @SubjectPresent
  public Result uploadToFolder(final String folderID) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStore.FileOrFolder fof = fm.getByIdentifier(folderID);
        if (fof == null) {
          return badRequest("A valid folder must be specified.")
              .as("text/plain");
        }
        final FileStore.Folder folder;
        if (fof instanceof FileStore.Folder) {
          folder = (Folder) fof;
        } else {
          return badRequest("Specified destination is not a folder.")
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
            final String fileName = filePart.getFilename();
            final File file = filePart.getFile();
            jsonFileData.put("name", fileName);
            jsonFileData.put("size", file.length());
            try {
              FileStore.File f = updateFileContents(folder, filePart);
              session.save();
              Logger.info(String.format(
                "file %s content type %s uploaded to %s by %s",
                fileName, filePart.getContentType(),
                f.getPath(),
                getUser()));
            } catch (ItemExistsException iee) {
              jsonFileData.put("error", iee.getMessage());
            } catch (AccessDeniedException ade) {
              jsonFileData.put("error",
                  "Insufficient permissions to upload files to folder.");
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

  private FileStore.File updateFileContents(FileStore.Folder folder,
      MultipartFormData.FilePart filePart) throws RepositoryException {
    try {
      final FileStore.FileOrFolder fof =
          folder.getFileOrFolder(filePart.getFilename());
      final FileStore.File f;
      if (fof == null) {
        f = folder.createFile(filePart.getFilename(),
            filePart.getContentType(),
            new FileInputStream(filePart.getFile()));
      } else if (fof instanceof FileStore.File) {
        throw new ItemExistsException(
            "File already exists. Add a new version instead.");
      } else {
        throw new ItemExistsException("Item exists and is not a file.");
      }
      return f;
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private static String decodePath(String path) {
    try {
      return URLDecoder.decode(path, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      // Should never happen
      throw new RuntimeException(e);
    }
  }

  private Injector getInjector() {
    return GuiceInjectionPlugin.getInjector(Play.application());
  }

}