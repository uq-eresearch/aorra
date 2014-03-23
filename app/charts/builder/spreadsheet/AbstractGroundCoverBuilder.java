package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;

import play.Logger;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.Groundcover;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public abstract class AbstractGroundCoverBuilder extends JFreeBuilder {

  protected static final String GC_SHEETNAME = "avgCover_ordered";
  protected static final String GCB50_SHEETNAME = "areaBelow50_ordered";

  private static final int MAX_ROW = 27;

  private static final ImmutableMap<Region, Set<String>> CATCHMENTS =
      new ImmutableMap.Builder<Region, Set<String>>()
      .put(Region.WET_TROPICS, ImmutableSet.of("Herbert"))
      .put(Region.BURDEKIN, ImmutableSet.of("Black", "Burdekin",
          "Don", "Haughton", "Ross"))
      .put(Region.MACKAY_WHITSUNDAY, ImmutableSet.of("Proserpine",
          "Pioneer", "Plane", "Oconnell", "O'Connell"))
      .put(Region.FITZROY, ImmutableSet.of("Boyne", "Calliope",
          "Fitzroy", "Shoalwater", "Styx", "Waterpark"))
      .put(Region.BURNETT_MARY, ImmutableSet.of("Baffle", "Burnett",
          "Burrum", "Kolan", "Mary"))
      .build();

    private static final SubstitutionKey FIRST_YEAR = new SubstitutionKey("firstYear",
        "first year of groundcover data in the spreadsheet",
        new SubstitutionKey.Val() {
          @Override
          public String value(Context ctx) {
            try {
              return getFirstYear(ctx.datasource());
            } catch (MissingDataException e) {
              throw new RuntimeException(e);
            }
          }
        });
    private static final SubstitutionKey LAST_YEAR = new SubstitutionKey("lastYear",
        "last year of groundcover data in the spreadsheet",
        new SubstitutionKey.Val() {
          @Override
          public String value(Context ctx) {
            try {
              return getLastYear(ctx.datasource());
            } catch (MissingDataException e) {
              throw new RuntimeException(e);
            }
          }
        });
    private static final SubstitutionKey GC_REGION = new SubstitutionKey("gcRegion",
        "groundcover region copied from spreadsheet A2-A28",
        new SubstitutionKey.Val() {
      @Override
      public String value(Context ctx) {
        Integer row = findRow(ctx, ctx.region());
        if(row != null) {
          try {
            return ctx.datasource().select(row, 0).asString();
          } catch (MissingDataException e) {}
        }
        Logger.debug("gcRegion not found for region "+ctx.region().name());
        return ctx.region().getProperName();
      }
    });

    public AbstractGroundCoverBuilder(ChartType type) {
      super(type);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            if(datasource.hasSheet(GC_SHEETNAME) || datasource.hasSheet(GCB50_SHEETNAME)) {
                List<Value> row0 = datasource.selectRow(0);
                List<Value> col1 = datasource.selectColumn(1);
                return SpreadsheetDataSource.containsString(row0, "Region") &&
                        SpreadsheetDataSource.containsString(row0, "Type") &&
                        SpreadsheetDataSource.containsString(row0, "AllYearMean") &&
                        SpreadsheetDataSource.containsString(col1, "Catchment") &&
                        SpreadsheetDataSource.containsString(col1, "Region");
            }
        } catch (MissingDataException e) {}
        return false;
    }

    private static Integer findRow(Context ctx, Region region) {
      for(int row=1;row<=MAX_ROW;row++) {
        try {
          String type = ctx.datasource().select(row, 1).asString();
          if(StringUtils.isBlank(type) || StringUtils.equalsIgnoreCase("Region", type)) {
            String r = ctx.datasource().select(row, 0).asString();
            if(StringUtils.containsIgnoreCase(r, StringUtils.split(region.getProperName())[0])) {
              return row;
            }
          }
        } catch(MissingDataException e) {}
      }
      return null;
    }

    private boolean contains(Region region, String catchment) {
      if(CATCHMENTS.get(region) != null) {
        for(String s : CATCHMENTS.get(region)) {
          if(StringUtils.containsIgnoreCase(catchment, s)) {
            return true;
          }
        }
      }
      return false;
    }

    private void addRow(Context ctx, ADCDataset dataset, int row) throws MissingDataException {
      SpreadsheetDataSource ds = ctx.datasource();
      String rowKey = ds.select(row, 0).asString();
      for(int column = 2;column<=getLastColumn(ds);column++) {
        String columnKey = ds.select(0, column).asInteger() + "";
        if(ds.select(row, column).getValue() == null) {
          dataset.addValue(null, rowKey, columnKey);
        } else {
          dataset.addValue(ds.select(row, column).asDouble(), rowKey, columnKey);
        }
      }
    }

    @Override
    protected ADCDataset createDataset(Context ctx) {
      try {
        SpreadsheetDataSource ds = ctx.datasource();
        ADCDataset dataset = new ADCDataset();
        if(ctx.region() == Region.GBR) {
          for(Region region : Region.values()) {
            Integer row = findRow(ctx, region);
            if(row!=null) {
              addRow(ctx, dataset, row);
            }
          }
        } else {
          if(!CATCHMENTS.containsKey(ctx.region())) {
            return null;
          }
          for(int row=1;row<=MAX_ROW;row++) {
            String type = ds.select(row, 1).asString();
            if(StringUtils.equalsIgnoreCase("catchment", type)) {
              String catchment = ds.select(row, 0).asString();
              if(contains(ctx.region(), catchment)) {
                addRow(ctx, dataset, row);
              }
            }
          }
        }
        return dataset;
      } catch(MissingDataException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.X_AXIS_LABEL, "Year").
          put(Attribute.SERIES_COLORS, new Color[] {
              new Color(29,107,171), new Color(134,177,56),
              new Color(87,88,71), new Color(150,116,52),
              new Color(103,42,4), new Color(208,162,33)}).
          build();
    }

    private static String getFirstYear(SpreadsheetDataSource ds) throws MissingDataException {
        return ds.select(0, 2).asInteger() + "";
    }

    private static String getLastYear(SpreadsheetDataSource ds) throws MissingDataException {
        return ds.select(0, getLastColumn(ds)).asInteger() + "";
    }

    private static int getLastColumn(SpreadsheetDataSource ds) throws MissingDataException {
      for(int col=ds.getColumns(0)-1;col > 0;col--) {
        if(ds.select(0, col).asInteger() != null) {
          return col;
        }
      }
      return 0;
    }

    @Override
    protected Drawable getDrawable(JFreeContext ctx) {
      return Groundcover.createChart(
          (ADCDataset)ctx.dataset(), ctx.type(), new Dimension(750,500));
    }

    @Override
    protected String getCsv(final JFreeContext ctx) {
      final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
      return Csv.write(new CsvWriter() {
        @Override
        public void write(CsvListWriter csv) throws IOException {
          @SuppressWarnings("unchecked")
          List<String> columnKeys = dataset.getColumnKeys();
          @SuppressWarnings("unchecked")
          List<String> rowKeys = dataset.getRowKeys();
          final List<String> heading = ImmutableList.<String>builder()
              .add(String.format("%s %s", ctx.region(), ctx.type()))
              .addAll(columnKeys)
              .build();
          csv.write(heading);
          for (String row : rowKeys) {
            List<String> line = newLinkedList();
            line.add(row);
            for (String col : columnKeys) {
              line.add(formatNumber("%.1f", dataset.getValue(row, col)));
            }
            csv.write(line);
          }
        }});
    }

    @Override
    public Set<SubstitutionKey> substitutionKeys() {
      return ImmutableSet.<SubstitutionKey>builder().
          addAll(super.substitutionKeys()).add(FIRST_YEAR).add(LAST_YEAR).add(GC_REGION).build();
    }
}
