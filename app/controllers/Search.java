package controllers;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.ValueFactory;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.StringUtils;
import org.jcrom.Jcrom;

import play.libs.F;
import play.libs.Json;
import play.mvc.Result;
import play.mvc.With;
import providers.CacheableUserProvider;
import service.JcrSessionFactory;
import service.filestore.FileStore;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.Inject;

@With(UncacheableAction.class)
public class Search extends SessionAwareController {

  private static final String CONTENT = "content:";
  private static final String FILE = "file:";

  public static class SearchResult {

    private final String id;
    private final double score;
    private String excerpt;
    private final String type;

    public SearchResult(String id, double score, String type) {
      this.id = id;
      this.score = score;
      this.type = type;
    }

    public SearchResult(String id, double score, String excerpt, String type) {
      this(id, score, type);
      this.excerpt = excerpt;
    }

    public String getId() {
      return id;
    }

    public double getScore() {
      return score;
    }

    public String getExcerpt() {
      return excerpt;
    }

    public void setExcerpt(String excerpt) {
      this.excerpt = excerpt;
    }

    public String getType() {
      return type;
    }

    @Override
    public boolean equals(Object other) {
      if (other instanceof SearchResult) {
        return getId().equals(((SearchResult) other).getId());
      }
      return false;
    }

    @Override
    public int hashCode() {
      return getId().hashCode();
    }

  }

  private final FileStore fileStore;
  @Inject
  public Search(final JcrSessionFactory sessionFactory, final Jcrom jcrom,
      final CacheableUserProvider sessionHandler, final FileStore fileStore) {
    super(sessionFactory, jcrom, sessionHandler);
    this.fileStore = fileStore;
  }

  @SubjectPresent
  public Result search(final String q) {
    if (q.isEmpty()) {
      return badRequest("Query string cannot be blank.");
    } else {
      return ok(Json.toJson(srch(q))).as("application/json; charset=utf-8");
    }
  }

  private List<SearchResult> srch(final String q) {
    return inUserSession(new F.Function<Session, List<SearchResult>>() {
      @Override
      public final List<SearchResult> apply(Session session) throws Exception {
        Set<SearchResult> sresults = Sets.newHashSet();
        if(StringUtils.startsWith(q, CONTENT)) {
          sresults.addAll(searchContent(session, StringUtils.substring(q, CONTENT.length())));
        } else if(StringUtils.startsWith(q, FILE)) {
          sresults.addAll(searchFilename(session, StringUtils.substring(q, FILE.length())));
        } else {
          sresults.addAll(searchContent(session, q));
          sresults.addAll(searchFilename(session, q));
        }
        List<SearchResult> l = Lists.newArrayList(sresults);
        Collections.sort(l, Collections.reverseOrder(new Comparator<SearchResult>() {
          @Override
          public int compare(SearchResult s0, SearchResult s1) {
            return Double.compare(s0.getScore(), s1.getScore());
          }}));
        return l;
      }

      private Set<SearchResult> searchContent(Session session, String q) throws Exception {
        if(StringUtils.isBlank(q)) {
          return Collections.emptySet();
        }
        Map<String, SearchResult> srMap = Maps.newHashMap();
        Set<SearchResult> result = Sets.newHashSet();
        ValueFactory vf = session.getValueFactory();
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        // How do we get the excerpt with JCR_SQL2?
        Query query = queryManager.createQuery(
            "SELECT * FROM [nt:resource] as s WHERE contains(s.*,$query) AND" +
            " ISDESCENDANTNODE(s, '/filestore')",
            javax.jcr.query.Query.JCR_SQL2);
        query.bindValue("query", vf.createValue(q));
        QueryResult qr = query.execute();
        RowIterator iter = qr.getRows();
        while (iter.hasNext()) {
          Row row = iter.nextRow();
          Node n = row.getNode().getParent().getParent().getParent();
          SearchResult sr = new SearchResult(n.getIdentifier(), row.getScore(),
              "content");
          result.add(sr);
          srMap.put(row.getNode().getIdentifier(), sr);
        }
        iter = fulltextQuery(queryManager, q);
        while (iter.hasNext()) {
          Row row = iter.nextRow();
          SearchResult sr = srMap.get(row.getNode().getIdentifier());
          if (sr != null) {
            sr.setExcerpt(row.getValue("rep:excerpt(.)").getString());
          }
        }
        return result;
      }

      @SuppressWarnings("deprecation")
      protected RowIterator fulltextQuery(QueryManager queryManager, String q)
          throws RepositoryException, InvalidQueryException {
        // TODO figure out how to do bindValue with javax.jcr.query.Query.SQL
        return queryManager
            .createQuery(
                "SELECT * FROM nt:resource WHERE contains(.,'" + q + "')",
                javax.jcr.query.Query.SQL).execute().getRows();
      }

      private Set<SearchResult> searchFilename(Session session, String q) throws Exception {
        if(StringUtils.isBlank(q)) {
          return Collections.emptySet();
        }
        Set<SearchResult> result = Sets.newHashSet();
        FileStore.Manager fm = fileStore.getManager(session);
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(
            "SELECT * FROM [nt:unstructured] as file " +
            "WHERE LOWER(localname()) LIKE $query AND ISDESCENDANTNODE(file, '/filestore')",
            javax.jcr.query.Query.JCR_SQL2);
        ValueFactory vf = session.getValueFactory();
        query.bindValue("query", vf.createValue("%" + StringUtils.lowerCase(q) + "%"));
        QueryResult qr = query.execute();
        RowIterator iter = qr.getRows();
        while (iter.hasNext()) {
          Row row = iter.nextRow();
          Node n = row.getNode();
          FileStore.FileOrFolder fof = fm.getByIdentifier(n.getIdentifier());
          if(fof!= null) {
            SearchResult sr = new SearchResult(n.getIdentifier(), row.getScore(),
                highlight(fof, q), "filename");
            result.add(sr);
          }
        }
        return result;
      }

      private String highlight(FileStore.FileOrFolder fof, String query) {
        int start = StringUtils.indexOfIgnoreCase(fof.getName(), query);
        if(start == -1) {
          return fof.getPath();
        }
        return StringUtils.join(new String[] {
            StringUtils.substring(fof.getName(), 0, start),
            "<strong>",
            StringUtils.substring(fof.getName(), start, start + query.length()),
            "</strong>",
            StringUtils.substring(fof.getName(), start + query.length())});
      }
    });
  }
}
