package controllers;

import java.net.URLDecoder;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import models.UserDAO;

import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;
import org.jcrom.Jcrom;

import play.libs.F;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import service.JcrSessionFactory;
import service.filestore.FileStore;
import au.edu.uq.aorra.charts.ChartRenderer;

import com.feth.play.module.pa.PlayAuthenticate;
import com.feth.play.module.pa.user.AuthUser;
import com.feth.play.module.pa.user.EmailIdentity;
import com.google.inject.Inject;

import ereefs.charts.ChartDescription;
import ereefs.spreadsheet.Spreadsheet;

public class Chart extends SessionAwareController {

    private final FileStore fileStore;

    @Inject
    public Chart(final JcrSessionFactory sessionFactory,
            final Jcrom jcrom,
            final FileStore fileStore) {
      super(sessionFactory, jcrom);
      this.fileStore = fileStore;
    }

    @Security.Authenticated(Secured.class)
    public Result charts(final String path) {
        final AuthUser user = PlayAuthenticate.getUser(ctx());
        return inUserSession(user, new F.Function<Session, Result>() {
            @Override
            public final Result apply(Session session) throws RepositoryException {
                try {
                    String decoded = URLDecoder.decode(path, "UTF-8");
                    final FileStore.Manager fm = fileStore.getManager(session);
                    FileStore.FileOrFolder fof = fm.getFileOrFolder("/"+decoded);
                    if (fof instanceof FileStore.File) {
                        FileStore.File file = (FileStore.File) fof;
                        // Check this is an OpenXML document (no chance otherwise)
                        if (!file.getMimeType().equals(
                            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")) {
                          return notFound();
                        }
                        Spreadsheet spreadsheet = new Spreadsheet(file.getData(), file.getMimeType());
                        List<ereefs.charts.Chart> charts = spreadsheet.getCharts(request().queryString());
                        if(charts.isEmpty()) {
                            return notFound();
                        } else if(charts.size() == 1) {
                            ChartRenderer renderer = new ChartRenderer(charts.get(0).getChart());
                            String svg = renderer.render();
                            return ok(svg).as("image/svg+xml");
                        } else {
                            final ObjectNode json = Json.newObject();
                            final ArrayNode aNode = json.putArray("charts");
                            for(ereefs.charts.Chart chart : charts) {
                                ChartDescription desc = chart.getDescription();
                                final ObjectNode chartNode = Json.newObject();
                                chartNode.put("type", desc.getType().toString());
                                for(Map.Entry<String, String> me : desc.getProperties().entrySet()) {
                                    chartNode.put(me.getKey(), me.getValue());
                                }
                                aNode.add(chartNode);
                            }
                            return ok(json).as("application/json");
                        }
                    } else {
                        return notFound();
                    }
                } catch(Exception e) {
                    throw new RepositoryException(e);
                }
            }
        });
    }

}
