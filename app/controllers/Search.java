package controllers;

import java.util.List;
import java.util.Map;

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
import com.google.inject.Inject;

@With(UncacheableAction.class)
public class Search extends SessionAwareController {

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
    }

    @Inject
    public Search(final JcrSessionFactory sessionFactory, final Jcrom jcrom,
        final CacheableUserProvider sessionHandler, final FileStore fileStore) {
      super(sessionFactory, jcrom, sessionHandler);
    }

    @SubjectPresent
    public Result search(final String q) {
        try {
            return ok(Json.toJson(srch(q))).as("application/json; charset=utf-8");
        } catch(Exception e) {
            e.printStackTrace();
            return ok("[]").as("application/json; charset=utf-8");
        }
    }

    private List<SearchResult> srch(final String q) {
        return inUserSession(new F.Function<Session, List<SearchResult>>() {
            @Override
            public final List<SearchResult> apply(Session session) throws Exception {
                Map<String, SearchResult> srMap = Maps.newHashMap();
                List<SearchResult> slist = Lists.newArrayList();
                ValueFactory vf = session.getValueFactory();
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                // How do we get the excerpt with JCR_SQL2?
                Query query = queryManager.createQuery(
                        "SELECT * FROM [nt:resource] as s WHERE contains(s.*,$query)",
                        javax.jcr.query.Query.JCR_SQL2);
                query.bindValue("query", vf.createValue(q));
                QueryResult result = query.execute();
                RowIterator iter = result.getRows();
                while(iter.hasNext()) {
                    Row row = iter.nextRow();
                    Node n = row.getNode().getParent().getParent().getParent();
                    SearchResult sr = new SearchResult(n.getIdentifier(), row.getScore(), "content");
                    slist.add(sr);
                    srMap.put(row.getNode().getIdentifier(), sr);
                }
                iter = fulltextQuery(queryManager);
                while(iter.hasNext()) {
                    Row row = iter.nextRow();
                    SearchResult sr = srMap.get(row.getNode().getIdentifier());
                    if(sr != null) {
                        sr.setExcerpt(row.getValue("rep:excerpt(.)").getString());
                    }
                }
                searchFilename(session, slist, q);
                return slist;
            }

            @SuppressWarnings("deprecation")
            protected RowIterator fulltextQuery(QueryManager queryManager)
                throws RepositoryException, InvalidQueryException {
              // TODO figure out how to do bindValue with javax.jcr.query.Query.SQL
              return queryManager.createQuery(
                      "SELECT * FROM nt:resource WHERE contains(.,'"+q+"')",
                      javax.jcr.query.Query.SQL).execute().getRows();
            }

            private void searchFilename(Session session, List<SearchResult> slist,
                    String q) throws Exception {
                QueryManager queryManager = session.getWorkspace().getQueryManager();
                Query query = queryManager.createQuery(
                    "SELECT * FROM [nt:file] WHERE localname() LIKE $query",
                  javax.jcr.query.Query.JCR_SQL2);
                ValueFactory vf = session.getValueFactory();
                query.bindValue("query", vf.createValue(q));
                QueryResult result = query.execute();
                RowIterator iter = result.getRows();
                while(iter.hasNext()) {
                    Row row = iter.nextRow();
                    Node n = row.getNode().getParent().getParent();
                    SearchResult sr = new SearchResult(n.getIdentifier(),
                            row.getScore(), n.getPath(), "filename");
                    slist.add(sr);
                }
            }
        });
    }
}