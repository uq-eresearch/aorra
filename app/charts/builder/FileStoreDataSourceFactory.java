package charts.builder;

import java.io.IOException;

import helpers.FileStoreHelper;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import play.libs.F;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import charts.builder.spreadsheet.SpreadsheetDataSource;
import charts.builder.spreadsheet.XlsDataSource;
import charts.builder.spreadsheet.XlsxDataSource;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class FileStoreDataSourceFactory implements DataSourceFactory {

  private final JcrSessionFactory sessionFactory;
  private final FileStore fileStore;

  @Inject
  public FileStoreDataSourceFactory(
      JcrSessionFactory sessionFactory, FileStore fileStore) {
    this.fileStore = fileStore;
    this.sessionFactory = sessionFactory;
  }

  @Override
  public DataSource getDataSource(final String id)
      throws IOException, RepositoryException {
    return sessionFactory.inSession(new F.Function<Session, DataSource>() {
      @Override
      public final DataSource apply(final Session session) throws Exception {
        return getDatasourceFromID(session, id);
      }
    });
  }

  private DataSource getDatasourceFromID(Session session, String id)
      throws IOException, RepositoryException {
    final FileStore.Manager fm = fileStore.getManager(session);
    final FileStore.FileOrFolder fof = fm.getByIdentifier(id);
    if (fof instanceof FileStore.File) {
      return getDataSource((FileStore.File) fof);
    }
    return null;
  }

  public static SpreadsheetDataSource getDataSource(FileStore.File file)
      throws IOException {
    // Check this is a MS spreadsheet document (no chance otherwise)
    if (file.getMimeType().equals(FileStoreHelper.XLS_MIME_TYPE)) {
      return new XlsDataSource(file.getData());
    } else if (file.getMimeType().equals(FileStoreHelper.XLSX_MIME_TYPE)) {
      return new XlsxDataSource(file.getData());
    }
    return null;
  }

}
