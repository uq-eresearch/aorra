package charts.reference;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import play.Logger;
import charts.Chart;
import charts.ChartType;
import charts.Region;
import charts.builder.DataSource;
import charts.builder.DataSourceFactory;
import charts.builder.DefaultChartBuilder;
import charts.builder.spreadsheet.XlsDataSource;
import charts.builder.spreadsheet.XlsxDataSource;
import charts.representations.Format;
import charts.representations.Representation;

import com.google.common.collect.Maps;
import com.google.inject.Singleton;

@Singleton
public class ChartRefCache {

  private static final String[] RESOURCES = new String[] {
    "coral.xls",
    "annual_rainfall.xlsx",
    "Chloro.xlsx",
    "cots_outbreak.xlsx",
    "groundcover_below_50.xlsx",
    "groundcover.xlsx",
    "loads.xlsx",
    "management_practice_systems.xlsx",
    "marine2.xlsx",
    "progress_table.xlsx",
    "riparian_2010.xlsx",
    "seagrass_cover2.xlsx",
    "tracking_towards_targets2.xlsx",
    "total_suspended_solids.xlsx",
    "wetlands.xlsx",
    "pesticides.xlsx",
  };

  private final DefaultChartBuilder chartBuilder;

  private Map<ChartType, File> cache = Maps.newHashMap();

  private volatile boolean run;

  private volatile boolean initialized;

  private Thread t;

  public ChartRefCache() {
    this.chartBuilder = new DefaultChartBuilder(new DataSourceFactory() {
      @Override
      public DataSource getDataSource(String id) throws Exception {
        final String resource = "/chartref/"+id;
        InputStream in = this.getClass().getResourceAsStream(resource);
        if(in != null) {
          if(StringUtils.endsWithIgnoreCase(resource, "xlsx")) {
            return new XlsxDataSource(in);
          } else {
            return new XlsDataSource(in);
          }
        } else {
          Logger.warn("resource not found "+resource);
          return null;
        }
      }});
  }

  public void start() {
    if(t != null) {
      return;
    }
    run = true;
    final Runnable r = new Runnable() {
      @Override
      public void run() {
        while(run) {
          if(needsRebuild()) {
            rebuildCache();
          }
          if(run) {
            initialized = true;
            try {
              TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {}
          }
        }
        clearCache();
      }
    };
    t = new Thread(r, "chart cache reference");
    t.start();
  }

  public void stop() {
    run = false;
    if(t!=null) {
      t.interrupt();
      t = null;
    }
  }

  public CacheResult cached(final ChartType type) {
    final File f;
    synchronized(cache) {
      f = cache.get(type);
    }
    if(f == null || !f.exists()) {
      return null;
    } else {
      return new CacheResult() {
        @Override
        public Date created() {
          return new Date(f.lastModified());
        }
        @Override
        public InputStream content() {
          try {
            return FileUtils.openInputStream(f);
          } catch (IOException e) {
            Logger.debug(String.format("while getting chart type %s"
                + " from chart reference cache",type.name()), e);
            return null;
          }
        }
      };
    }
  }

  public boolean initialized() {
    return initialized;
  }

  DefaultChartBuilder builder() {
    return chartBuilder;
  }

  private boolean needsRebuild() {
    if(cache.isEmpty()) {
      return true;
    }
    synchronized(cache) {
      for(File f : cache.values()) {
        if(!f.exists()) {
          return true;
        }
      }
    }
    return false;
  }

  private void rebuildCache() {
    for(String r : RESOURCES) {
      try {
        List<Chart> charts = chartBuilder.getCharts(r, null,
            Collections.<Region>emptyList(), null);
        for(Chart c : charts) {
          if(!run) {
            return;
          }
          updateCache(c);
        }
      } catch (Exception e) {
        Logger.warn("while rebuilding chart reference cache", e);
      }
    }
    checkComplete();
  }

  private void checkComplete() {
    for(ChartType type : ChartType.values()) {
      synchronized(cache) {
        final File f = cache.get(type);
        if(f == null) {
          Logger.warn(String.format("chart type %s missing in chart reference."
              + " please add a spreadsheet into resources/chartref"
              + " that yields this type of chart.", type.name()));
        } else if(!f.exists()) {
          Logger.debug(String.format("chart type %s exists in chart cache"
              + " but the file %s does not"), type.name(), f.getAbsolutePath());
        }
      }
    }
  }

  private void clearCache() {
    synchronized(cache) {
      for(File f : cache.values()) {
        FileUtils.deleteQuietly(f);
      }
    }
  }

  private void updateCache(final Chart chart) {
    // TODO instead of synchronized try ReadWriteLock
    synchronized(cache) {
      final ChartType type = chart.getDescription().getType();
      File f = cache.get(type);
      if(f == null || !f.exists()) {
        try {
          Representation r = chart.outputAs(Format.SVG, new Dimension());
          f = File.createTempFile(type.name(), ".svg");
          f.deleteOnExit();
          FileUtils.writeByteArrayToFile(f, r.getContent());
          cache.put(type, f);
        } catch(Exception e) {
          Logger.warn("while updating chart reference cache", e);
        }
      }
    }
  }

}
