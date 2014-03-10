package charts.builder.spreadsheet;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.supercsv.io.CsvListWriter;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.CoralCover;
import charts.jfree.ADSCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public abstract class CoralCoverBuilder extends JFreeBuilder {

    protected static final String HC_MEAN = "HC mean";
    protected static final String HC_SE = "HC se";
    protected static final String SC_MEAN = "SC mean";
    protected static final String SC_SE = "SC se";
    protected static final String MA_MEAN = "MA mean";
    protected static final String MA_SE = "MA se";
    protected static final String JUV_DEN = "Juv density";
    protected static final String JUV_DEN_SE = "Juv Den (se)";

    protected static final String COVER = "Cover (%)";
    protected static final String JUVENILE = "Juveniles/m\u00b2";

    private static final  ImmutableMap<String, Region> REGIONS = ImmutableMap.of(
             "Wet Tropics", Region.WET_TROPICS,
             "Burdekin", Region.BURDEKIN,
             "Whitsunday", Region.MACKAY_WHITSUNDAY,
             "Fitzroy", Region.FITZROY);

    protected abstract String getMeanColumn();
    protected abstract String getSeColumn();

    public CoralCoverBuilder(ChartType type) {
      super(type);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource ds) {
        try {
            return StringUtils.equalsIgnoreCase(ds.select(0, 2).asString(), HC_MEAN) &&
                    StringUtils.equalsIgnoreCase(ds.select(0, 3).asString(), HC_SE);
        } catch(MissingDataException e) {
            return false;
        }
    }

    private boolean eof(SpreadsheetDataSource ds, int row) throws MissingDataException {
        if(StringUtils.isBlank(ds.select(row, 0).asString()) &&
                StringUtils.isBlank(ds.select(row+1, 0).asString())) {
            return true;
        } else {
            return false;
        }
    }

    private List<Region> regions(SpreadsheetDataSource ds) {
        try {
            Set<Region> regions = Sets.newLinkedHashSet();
            for(int row=0;!eof(ds, row); row++) {
                if(REGIONS.containsKey(ds.select(row, 0).asString())) {
                    regions.add(REGIONS.get(ds.select(row, 0).asString()));
                }
            }
            return Lists.newArrayList(regions);
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean containsRegion(SpreadsheetDataSource ds, Region region) {
        return regions(ds).contains(region);
    }

    private boolean containsChart(SpreadsheetDataSource ds, ChartType type) {
        return getColumn(ds, getMeanColumn()) != null &&
                getColumn(ds, getSeColumn()) != null;
    }

    @Override
    protected ADSCDataset createDataset(Context ctx) {
      SpreadsheetDataSource ds = ctx.datasource();
      ChartType type = ctx.type();
      Region region = ctx.region();
      if (((region == Region.GBR) || containsRegion(ds, region)) &&
          containsChart(ds, type)) {
        Integer meanColumn = getColumn(ds, getMeanColumn());
        Integer deviationColumn = getColumn(ds, getSeColumn());
        if(meanColumn == null || deviationColumn == null) {
            throw new RuntimeException(String.format("data not found for ChartType %s", type.name()));
        }
        List<Region> regions;
        if(region == Region.GBR) {
            regions = regions(ds);
        } else {
            if(containsRegion(ds, region)) {
                regions = Collections.singletonList(region);
            } else {
                throw new RuntimeException(String.format("no entries for region %s",
                        region.getProperName()));
            }
        }
        try {
          ADSCDataset d = new ADSCDataset();
            for(Region r : regions) {
                for(int row = getRegionStart(ds, r);
                        StringUtils.isNotBlank(ds.select(row, 0).asString());row++) {
                    Double mean = ds.select(row, meanColumn).asDouble();
                    Double deviation = ds.select(row, deviationColumn).asDouble();
                    String series = ds.select(row, 0).asString();
                    String year = ds.select(row, 1).asInteger().toString();
                    d.add(mean, deviation, series, year);
                }
            }
            return d;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
      } else {
        return null;
      }
    }

    private int getRegionStart(SpreadsheetDataSource ds, Region region) throws MissingDataException {
        for(int row = 0;!eof(ds, row);row++) {
            if(REGIONS.get(ds.select(row, 0).asString()) == region) {
                return row;
            }
        }
        throw new RuntimeException(String.format("region %s not found", region.getProperName()));
    }

    private Integer getColumn(SpreadsheetDataSource ds, String heading) {
        Integer cols = ds.getColumnCount(0);
        if(cols != null) {
            for(int c=0;c<cols;c++) {
                try {
                    if(StringUtils.equalsIgnoreCase(ds.select(0, c).asString(), heading)) {
                        return c;
                    }
                } catch (MissingDataException e) {}
            }
        }
        return null;
    }

    @Override
    protected Drawable getDrawable(JFreeContext ctx) {
      return CoralCover.createChart((ADSCDataset)ctx.dataset(), ctx.type(),
          ctx.region(), new Dimension(750, 500));
    }

    @Override
    protected String getCsv(final JFreeContext ctx) {
      final DefaultStatisticalCategoryDataset dataset = 
          (DefaultStatisticalCategoryDataset)ctx.dataset();
      return Csv.write(new CsvWriter() {
        @Override
        public void write(CsvListWriter csv) throws IOException {
          @SuppressWarnings("unchecked")
          List<String> columnKeys = dataset.getColumnKeys();
          @SuppressWarnings("unchecked")
          List<String> rowKeys = dataset.getRowKeys();
          final List<String> heading = ImmutableList
              .<String> builder()
              .add(String.format("%s %s", ctx.type().getLabel(), ctx.region()))
              .addAll(columnKeys)
              .build();
          csv.write(heading);
          for (String row : rowKeys) {
            {
              List<String> line = Lists.newLinkedList();
              line.add(row + " (Mean)");
              for (String col : columnKeys) {
                line.add(formatNumber("%.3f", dataset.getMeanValue(row, col)));
              }
              csv.write(line);
            }
            {
              List<String> line = Lists.newLinkedList();
              line.add(row + " (Std Dev)");
              for (String col : columnKeys) {
                line.add(formatNumber("%.3f", dataset.getStdDevValue(row, col)));
              }
              csv.write(line);
            }
          }
        }});
    }

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.SERIES_COLOR, new Color(187, 34, 51)).
          build();
    }

}
