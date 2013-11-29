package controllers;

import static org.apache.commons.httpclient.util.URIUtil.encodeQuery;
import static service.filestore.roles.Admin.isAdmin;
import helpers.ExtractionHelper;
import helpers.FileStoreHelper;
import helpers.FileStoreHelper.FileOrFolderException;
import jackrabbit.AorraAccessManager;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

import javax.jcr.AccessDeniedException;
import javax.jcr.ItemExistsException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.GroupManager;
import models.Flag;
import models.User;
import models.UserDAO;

import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.jackrabbit.api.security.user.Group;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.jcrom.Jcrom;
import org.jcrom.util.PathUtils;

import play.Logger;
import play.api.http.MediaRange;
import play.api.libs.MimeTypes;
import play.api.mvc.AnyContentAsRaw;
import play.api.mvc.AnyContentAsText;
import play.libs.F;
import play.libs.Json;
import play.mvc.Http.MultipartFormData;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.EventManager;
import service.JcrSessionFactory;
import service.EventManager.Event;
import service.filestore.FileStore;
import service.filestore.FileStore.Folder;
import service.filestore.FileStore.Permission;
import service.filestore.FileStoreImpl;
import service.filestore.FlagStore;
import service.filestore.FlagStore.FlagType;
import service.filestore.JsonBuilder;

import com.google.inject.Inject;

@With(UncacheableAction.class)
public final class FileStoreController extends SessionAwareController {

  private final FileStore fileStoreImpl;
  private final FlagStore flagStoreImpl;

  @Inject
  public FileStoreController(final JcrSessionFactory sessionFactory,
      final Jcrom jcrom,
      final CacheableUserProvider sessionHandler,
      final FileStore fileStoreImpl,
      final FlagStore flagStoreImpl) {
    super(sessionFactory, jcrom, sessionHandler);
    this.fileStoreImpl = fileStoreImpl;
    this.flagStoreImpl = flagStoreImpl;
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
            jb.toJson(fm.getFolders()),
            getUsersJson(session))).as("text/html; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result mkdir(final String folderId, final String path) {
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStoreHelper fh = new FileStoreHelper(session);
        try {
          final FileStore.Folder baseFolder =
              (FileStore.Folder) fm.getByIdentifier(folderId);
          final String absPath =
              baseFolder.getPath() + "/" + PathUtils.relativePath(path);
          final FileStore.Folder newFolder = fh.mkdir(absPath, true);
          final ArrayNode json = JsonNodeFactory.instance.arrayNode();
          FileStore.Folder f = newFolder;
          while (f != null && !f.getIdentifier().equals(folderId)) {
            // Shift onto the front, because we need to return in order
            // of creation.
            json.insert(0, jb.toJsonShallow(f, false));
            f = f.getParent();
          }
          return created(json).as("application/json; charset=utf-8");
        } catch (FileOrFolderException e) {
          return badRequest(e.getMessage());
        }
      }
    });
  }

  @SubjectPresent
  public Result showFolder(final String folderId) {
    for (final MediaRange m : ctx().request().acceptedTypes()) {
      if (m.accepts("text/html")) {
        return index();
      } else if (m.accepts("application/json") || m.accepts("text/javascript")){
        return folderJson(folderId);
      }
    }
    return status(UNSUPPORTED_MEDIA_TYPE);
  }

  @SubjectPresent
  public Result showFile(final String fileId) {
    for (final MediaRange m : ctx().request().acceptedTypes()) {
      if (m.accepts("text/html")) {
        return index();
      } else if (m.accepts("application/json") || m.accepts("text/javascript")){
        return fileJson(fileId);
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
        return ok(jb.toJson(fm.getFolders()))
            .as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result flagsJson(final String flagTypeName) {
    final FlagType t = FlagType.valueOf(flagTypeName.toUpperCase());
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final ArrayNode json = JsonNodeFactory.instance.arrayNode();
        for (final Flag flag : flagStoreImpl.getManager(session).getFlags(t)) {
          json.add(jb.toJson(flag));
        }
        return ok(json).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result flagJson(final String flagTypeName, final String flagId) {
    final FlagType t = FlagType.valueOf(flagTypeName.toUpperCase());
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final Flag flag = flagStoreImpl.getManager(session).getFlag(t, flagId);
        if (flag == null) {
          return notFound();
        }
        return ok(jb.toJson(flag)).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result addFlag(final String flagTypeName) {
    final FlagType t = FlagType.valueOf(flagTypeName.toUpperCase());
    final JsonNode params = ctx().request().body().asJson();
    if (!params.has("targetId") || !params.has("userId"))
      return badRequest();
    final String targetId = params.get("targetId").asText();
    final String userId = params.get("userId").asText();
    if (!getUser().getId().equals(userId))
      return forbidden();
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final User user = (new UserDAO(session, jcrom)).get(getUser());
        final JsonBuilder jb = new JsonBuilder();
        final Flag flag = flagStoreImpl.getManager(session)
            .setFlag(t, targetId, user);
        fileStoreImpl.getEventManager()
          .tell(FlagStore.Events.create(flag, t, user));
        return created(jb.toJson(flag)).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result deleteFlag(final String flagTypeName, final String flagId) {
    final FlagType t = FlagType.valueOf(flagTypeName.toUpperCase());
    return inUserSession(new F.Function<Session, Result>() {
      @Override
      public final Result apply(Session session) throws RepositoryException {
        final FlagStore.Manager flm = flagStoreImpl.getManager(session);
        final Flag flag = flm.getFlag(t, flagId);
        if (flag == null)
          return notFound();
        flm.unsetFlag(t, flagId);
        fileStoreImpl.getEventManager()
          .tell(FlagStore.Events.delete(flag, t, getUser()));
        return status(204);
      }
    });
  }

  protected Result folderJson(final String folderId) {
    return folderBasedResult(folderId, new FolderOp() {
      @Override
      public final Result apply(Session session, FileStore.Folder f)
          throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        return ok(jb.toJsonShallow(f, false));
      }
    });
  }

  protected Result fileJson(final String fileId) {
    return fileBasedResult(fileId, new FileOp() {
      @Override
      public final Result apply(Session session, FileStore.File f)
          throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        return ok(jb.toJsonShallow(f)).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result modifyFolder(final String folderId) {
    return folderBasedResult(folderId, new FolderOp() {
      @Override
      public final Result apply(Session session, FileStore.Folder f)
          throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final JsonNode params = ctx().request().body().asJson();
        try {
          f.rename(params.get("name").asText());
          return ok(jb.toJsonShallow(f, false));
        } catch (ItemExistsException e) {
          return badRequest(e.getMessage());
        }
      }
    });
  }

  @SubjectPresent
  public Result modifyFile(final String fileId) {
    return fileBasedResult(fileId, new FileOp() {
      @Override
      public final Result apply(Session session, FileStore.File f)
          throws RepositoryException {
        final JsonBuilder jb = new JsonBuilder();
        final JsonNode params = ctx().request().body().asJson();
        try {
          f.rename(params.get("name").asText());
          return ok(jb.toJsonShallow(f));
        } catch (ItemExistsException e) {
          return badRequest(e.getMessage());
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
        final UserDAO dao = getUserDAO(session);
        if (!isAdmin(session, dao, dao.get(getUser())))
          return forbidden();
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
  public Result deleteVersion(final String fileId, final String versionName) {
    return versionBasedResult(fileId, versionName, new FileVersionOp() {
      @Override
      public final Result apply(
          final Session session,
          final FileStore.File file,
          final FileStore.File version)
          throws RepositoryException, IOException {
        final UserDAO dao = getUserDAO(session);
        final User u = dao.get(getUser());
        if (!isAdmin(session, dao, u) && !version.getAuthor().equals(u))
          return forbidden();
        try {
          version.delete();
          return noContent();
        } catch (AccessDeniedException e) {
          return forbidden(e.getMessage());
        }
      }
    });
  }

  @SubjectPresent
  public Result downloadFolder(final String folderId) {
    return folderBasedResult(folderId, new FolderOp() {
      @Override
      public Result apply(Session session, Folder folder)
          throws RepositoryException, IOException {
        final FileStoreHelper fh = new FileStoreHelper(session);
        final java.io.File zipFile = fh.createZipFile(folder);
        ctx().response().setContentType("application/zip");
        ctx().response().setHeader("Content-Disposition",
            "attachment; filename="+encodeQuery(folder.getName())+".zip");
        ctx().response().setHeader("Content-Length", zipFile.length()+"");
        return ok(new FileInputStream(zipFile) {
          @Override
          public void close() throws IOException {
            super.close();
            zipFile.delete();
          }
        });
      }
    });
  }

  @SubjectPresent
  public Result downloadFile(final String fileId, final String versionName) {
    return versionBasedResult(fileId, versionName, new FileVersionOp() {
      @Override
      public final Result apply(
          final Session session,
          final FileStore.File file,
          final FileStore.File version)
          throws RepositoryException, IOException {
        final String authorName = version.getAuthor() != null ?
            version.getAuthor().getName() : "unknown";
        final String versionStamp =
            String.format("(%1$tY%1$tm%1$tdT%1$tH%1$tM%1$tS %2$s)",
                version.getModificationTime(),
                authorName);
        final String filename = file.getName().replaceAll(
            "(\\.?[^\\.]+$)", versionStamp + "$1" );
        ctx().response().setContentType(version.getMimeType());
        ctx().response().setHeader("Content-Disposition",
            "attachment; filename="+encodeQuery(filename));
        return ok(version.getData());
      }
    });
  }

  @SubjectPresent
  public Result fileTextSummary(final String fileId, final String versionName) {
    return versionBasedResult(fileId, versionName, new FileVersionOp() {
      @Override
      public final Result apply(
          final Session session,
          final FileStore.File file,
          final FileStore.File version)
          throws RepositoryException, IOException {
        final ExtractionHelper eh = new ExtractionHelper(version);
        return ok(eh.getPlainText()).as("text/plain; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result groupPermissionList(final String folderId) {
    return folderBasedResult(folderId, new FolderOp() {
      @Override
      public final Result apply(Session session, Folder folder)
          throws RepositoryException {
        final ArrayNode perms = JsonNodeFactory.instance.arrayNode();
        for (final Map.Entry<String, Permission> e :
            folder.getGroupPermissions().entrySet()) {
          perms.add(groupJson(e.getKey(), e.getValue()));
        }
        return ok(perms).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result groupPermission(final String folderId, final String groupName) {
    return folderBasedResult(folderId, new FolderOp() {
      @Override
      public final Result apply(Session session, Folder folder)
          throws RepositoryException {
        final Map<String, Permission> permissions =
            folder.getGroupPermissions();
        if (!permissions.containsKey(groupName)) {
          return notFound();
        }
        final Permission access = permissions.get(groupName);
        return ok(groupJson(groupName, access))
            .as("application/json; charset=utf-8");
      }
    });
  }

  protected ObjectNode groupJson(String groupName, Permission permission) {
    final ObjectNode obj = Json.newObject();
    obj.put("name", groupName);
    obj.put("access", permission.toString());
    return obj;
  }

  @SubjectPresent
  public Result permissionUpdate(final String folderId) {
    final JsonNode params = ctx().request().body().asJson();
    if (!params.has("name") || !params.has("access")) {
      return badRequest();
    }
    final String groupName = params.get("name").asText();
    final Permission accessLevel = Permission.valueOf(
        params.get("access").asText());
    final String id = inUserSession(new F.Function<Session, String>() {
      @Override
      public final String apply(Session session) throws RepositoryException,
          IOException {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStore.FileOrFolder fof = fm.getByIdentifier(folderId);
        if (!(fof instanceof FileStoreImpl.Folder)) {
          return null;
        }
        // Check this user has appropriate permissions
        if (fof.getAccessLevel() != FileStore.Permission.RW) {
          return null;
        }
        return fof.getIdentifier();
      }
    });
    if (id == null) {
      return notFound();
    }
    // Try granting the permission
    final Result result =
        sessionFactory.inSession(new F.Function<Session, Result>() {
      @Override
      public Result apply(final Session session) throws Throwable {
        final GroupManager gm = new GroupManager(session);
        final Group group = gm.find(groupName);
        if (group == null)
          return notFound();
        final AorraAccessManager acm = (AorraAccessManager)
            session.getAccessControlManager();
        acm.grant(group.getPrincipal(),
            session.getNodeByIdentifier(id).getPath(),
            accessLevel.toJackrabbitPermission());
        fileStoreImpl.getEventManager().tell(FileStore.Events.updateFolder(id));
        return null;
      }
    });
    if (result != null)
      return result;
    return groupPermission(folderId, groupName);
  }

  @SubjectPresent
  public Result fileInfo(final String fileId) {
    return fileBasedResult(fileId, new FileOp() {
      @Override
      public final Result apply(final Session session, final FileStore.File file)
          throws RepositoryException {
        final ObjectNode json = Json.newObject();
        {
          final ArrayNode aNode = json.putArray("versions");
          for (String versionName : file.getVersions().keySet()) {
            final FileStore.File version =
                file.getVersions().get(versionName);
            final User author = version.getAuthor();
            final ObjectNode versionInfo = Json.newObject();
            versionInfo.put("name", versionName);
            if (author != null) {
              final ObjectNode authorInfo = Json.newObject();
              authorInfo.put("name", author.getName());
              authorInfo.put("email", author.getEmail());
              versionInfo.put("author", authorInfo);
            }
            versionInfo.put("timestamp",
                DateFormatUtils.ISO_DATETIME_FORMAT.format(
                    version.getModificationTime()));
            aNode.add(versionInfo);
          }
        }
        return ok(json).as("application/json; charset=utf-8");
      }
    });
  }

  @SubjectPresent
  public Result updateFile(final String fileID) {
    final MultipartFormData body = request().body().asMultipartFormData();
    if (body == null || body.getFiles().size() != 1) {
      if (request().headers().containsKey("Content-Type")) {
        if (request().body().asRaw() != null) {
          return updateToFile(fileID, request().body().asRaw().asBytes());
        } else if (request().body().asText() != null) {
          return updateToFile(fileID, request().body().asText().getBytes());
        }
      }
      return badRequest("Request must contain a single file to upload.")
          .as("text/plain");
    } else {
      return uploadToFile(fileID, body.getFiles().get(0));
    }
  }

  protected Result updateToFile(final String fileID, final byte[] buf) {
    return fileBasedResult(fileID, new FileOp() {
      @Override
      public final Result apply(Session session, FileStore.File f)
          throws RepositoryException, FileNotFoundException {
        try {
          final String mimeType = request().headers().get("Content-Type")[0];
          final JsonBuilder jb = new JsonBuilder();
          f.update(mimeType, new ByteArrayInputStream(buf));
          session.save();
          Logger.info(String.format(
            "file %s content type %s uploaded to %s by %s",
            f.getName(), mimeType,
            f.getPath(), getUser()));
          return ok(jb.toJsonShallow(f)).as("application/json; charset=utf-8");
        } catch (ItemExistsException iee) {
          return forbidden(iee.getMessage());
        } catch (AccessDeniedException ade) {
          return forbidden(
              "Insufficient permissions to upload files to folder.");
        }
      }
    });
  }

  protected Result uploadToFile(final String fileID,
      final MultipartFormData.FilePart filePart) {
    return fileBasedResult(fileID, new FileOp() {
      @Override
      public final Result apply(Session session, FileStore.File f)
          throws RepositoryException, FileNotFoundException {
        try {
          final JsonBuilder jb = new JsonBuilder();
          f.update(getMimeType(filePart),
              new FileInputStream(filePart.getFile()));
          session.save();
          Logger.info(String.format(
            "file %s content type %s uploaded to %s by %s",
            filePart.getFilename(), getMimeType(filePart),
            f.getPath(), getUser()));
          return ok(jb.toJsonShallow(f)).as("application/json; charset=utf-8");
        } catch (ItemExistsException iee) {
          return forbidden(iee.getMessage());
        } catch (AccessDeniedException ade) {
          return forbidden(
              "Insufficient permissions to upload files to folder.");
        }
      }
    });
  }

  @SubjectPresent
  public Result uploadToFolder(final String folderID) {
    return folderBasedResult(folderID, new FolderOp() {
      @Override
      public final Result apply(Session session, FileStore.Folder folder)
          throws RepositoryException {
        final MultipartFormData body = request().body().asMultipartFormData();
        if (body == null || body.getFiles().size() != 1) {
          return badRequest("Request must contain a single file to upload.")
              .as("text/plain");
        }
        final MultipartFormData.FilePart filePart = body.getFiles().get(0);
        try {
          final JsonBuilder jb = new JsonBuilder();
          FileStore.File f = updateFileContents(folder, filePart);
          session.save();
          Logger.info(String.format(
            "file %s content type %s uploaded to %s by %s",
            filePart.getFilename(), getMimeType(filePart),
            f.getPath(), getUser()));
          return created(jb.toJsonShallow(f))
              .as("application/json; charset=utf-8");
        } catch (ItemExistsException iee) {
          return forbidden(iee.getMessage());
        } catch (AccessDeniedException ade) {
          return forbidden(
              "Insufficient permissions to upload files to folder.");
        }
      }
    });
  }

  protected ArrayNode getUsersJson(Session session) throws RepositoryException {
    final JsonBuilder jb = new JsonBuilder();
    final ArrayNode json = JsonNodeFactory.instance.arrayNode();
    final UserDAO dao = new UserDAO(session, jcrom);
    for (final User user : dao.list()) {
      json.add(jb.toJson(user, isAdmin(session, dao, user)));
    }
    return json;
  }

  protected UserDAO getUserDAO(Session session) {
    return new UserDAO(session, jcrom);
  }

  private interface FofOp<T extends FileStore.FileOrFolder>
    extends F.Function2<Session, T, Result> {}
  private interface FolderOp extends FofOp<FileStore.Folder> {}
  private interface FileOp extends FofOp<FileStore.File> {}
  private interface FileVersionOp
    extends F.Function3<Session, FileStore.File, FileStore.File, Result> {}

  protected Result folderBasedResult(
      final String folderId,
      final FolderOp operation) {
    return fofBasedResult(folderId, FileStore.Folder.class, operation);
  }

  protected Result fileBasedResult(
      final String folderId,
      final FileOp operation) {
    return fofBasedResult(folderId, FileStore.File.class, operation);
  }


  public Result versionBasedResult(
      final String fileId,
      final String versionName,
      final FileVersionOp operation) {
    return fileBasedResult(fileId, new FileOp() {
      @Override
      public final Result apply(Session session, FileStore.File file)
          throws Throwable {
        final Map<String, FileStore.File> versions = file.getVersions();
        if (versionName.equals("latest")) {
          return operation.apply(session, file, file.getLatestVersion());
        }
        if (versions.containsKey(versionName)) {
          return operation.apply(session, file, versions.get(versionName));
        }
        return notFound(versionName + " is not a valid version for this file.");
      }
    });
  }

  protected <T extends FileStore.FileOrFolder> Result fofBasedResult(
      final String fofId,
      final Class<T> fofClass,
      final FofOp<T> operation) {
    final String typeName = fofClass.getSimpleName().toLowerCase();
    return inUserSession(new F.Function<Session, Result>() {
      @SuppressWarnings("unchecked")
      @Override
      public final Result apply(Session session) throws Throwable {
        final FileStore.Manager fm = fileStoreImpl.getManager(session);
        final FileStore.FileOrFolder fof = fm.getByIdentifier(fofId);
        if (fof == null) {
          return notFound("The "+typeName+" specified does not exist.")
              .as("text/plain");
        } else if (!fofClass.isInstance(fof)) {
          return badRequest(fofId+" is not a "+typeName+".")
              .as("text/plain");
        } else {
          return operation.apply(session, (T) fof);
        }
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
            getMimeType(filePart),
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

  protected String getMimeType(MultipartFormData.FilePart filePart) {
    if (filePart.getContentType().equals("application/octet-stream")) {
      final scala.Option<String> guessed =
          MimeTypes.forFileName(filePart.getFilename());
      return guessed.nonEmpty() ? guessed.get() : "application/octet-stream";
    }
    return filePart.getContentType();
  }

}