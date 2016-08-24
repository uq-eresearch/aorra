package charts.builder.spreadsheet;

import static charts.ChartType.MARINE_CT;
import static charts.ChartType.MARINE_ST;
import static charts.ChartType.MARINE_WQT;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.Colors;
import charts.graphics.MarineTrends;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class MarineTrendsBuilder extends JFreeBuilder {

    private static final String WATER_QUALITY = "Water Quality";
    private static final String SEAGRASS = "Seagrass";
    private static final String CORAL = "Coral";

    private static final ImmutableMap<ChartType, Integer> ROWS = ImmutableMap.of(
            MARINE_WQT, 2, MARINE_ST, 14, MARINE_CT, 26);

    private static enum Indicator {
        OWQ_SCORE("Overall water quality score", new String[] 
            {"Water quality index", "WQI"}, MARINE_WQT),
        CHLOROPHYLL_A("Chlorophyll a", "Chlorophyll a", MARINE_WQT),
        TOTAL_SUSPENDED_SOLIDS("Total suspended solids", new String[]
            {"Total suspended solids", "TSS"}, MARINE_WQT),
        OS_SCORE("Overall seagrass score", "Seagrass Index", MARINE_ST),
        REPRODUCTION("Reproduction", "Reproductive Effort", MARINE_ST),
        ABUNDANCE("Abundance", "Abundance", MARINE_ST),
        NUTRIENT_STATUS("Nutrient status", "C:N Ratio", MARINE_ST),
        OC_SCORE("Overall coral score", "Coral Index", MARINE_CT),
        COVER("Cover", "Cover", MARINE_CT),
        CHANGE("Change", "Change", MARINE_CT),
        MACROALGAE("Macroalgae", "Algal Cover", MARINE_CT),
        JUVENILE("Juveniles", new String[] {"Juvenile", "Juvenile density"}, MARINE_CT),
        ;

        private String label;
        private String[] headers;
        private ChartType type;

        private Indicator(String label, String heading, ChartType type) {
          this(label, new String[] {heading}, type);
        }

        private Indicator(String label, String[] headers, ChartType type) {
            this.label = label;
            this.headers = headers;
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public ChartType getChartType() {
            return type;
        }

        public boolean match(String header) {
          for(String h : headers) {
            if(equalsIgnoreCase(h, header)) {
              return true;
            }
          }
          return equalsIgnoreCase(header, label);
        }

        public static List<Indicator> forType(ChartType type) {
            List<Indicator> l = Lists.newArrayList();
            for(Indicator i : Indicator.values()) {
                if(i.getChartType() == type) {
                    l.add(i);
                }
            }
            return l;
        }
    }

    public MarineTrendsBuilder() {
        super(ImmutableList.of(MARINE_CT, MARINE_ST, MARINE_WQT));
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
              return equalsIgnoreCase(datasource.select("B2").asString(), WATER_QUALITY) &&
              equalsIgnoreCase(datasource.select("B14").asString(), SEAGRASS) &&
              equalsIgnoreCase(datasource.select("B26").asString(), CORAL);
        } catch(MissingDataException e) {}
        return false;
    }

    private int getRow(SpreadsheetDataSource ds, ChartType type) {
        return ROWS.get(type);
    }

    private int getRow(SpreadsheetDataSource ds, ChartType type, Region region) {
        int offset = 2;
        if(region == Region.GBR) {
            offset += 6;
        } else {
            offset += region.ordinal() - 1;
        }
        return getRow(ds, type) + offset;
    }

    private int getColumnStart(SpreadsheetDataSource ds, Indicator indicator) {
        int row = getRow(ds, indicator.getChartType());
        for(int col = 1;col < 100;col++) {
            try {
                if(indicator.match(ds.select(row, col).asString())) {
                    return col;
                }
            } catch(MissingDataException e) {}
        }
        throw new RuntimeException(String.format(
                "column start for indicator '%s' not found", indicator.getLabel()));
    }

    private int getColumnEnd(SpreadsheetDataSource ds, Indicator indicator) {
        int row = getRow(ds, indicator.getChartType()) + 1;
        for(int col = getColumnStart(ds, indicator) + 1; true; col++) {
            try {
                String s = ds.select(row, col).asString();
                if(isBlank(s)) {
                    return col - 1;
                }
                String s2 = ds.select(row-1, col).asString();
                if(!isBlank(s2)) {
                    return col - 1;
                }
            } catch(MissingDataException e) {}
        }
    }

    private List<String> getCategories(SpreadsheetDataSource ds, Indicator indicator) throws MissingDataException {
        List<String> l = Lists.newArrayList();
        int row = getRow(ds, indicator.getChartType()) + 1;
        for(int col = getColumnStart(ds, indicator);col <= getColumnEnd(ds, indicator);col++) {
            l.add(StringUtils.substringBefore(ds.select(row, col).asString(), "."));
        }
        return l;
    }

    @Override
    protected ADCDataset createDataset(Context ctx) {
      if (ctx.type() == MARINE_CT
          && (ctx.region() == Region.CAPE_YORK || ctx.region() == Region.BURNETT_MARY)) {
        return null;
      }
      SpreadsheetDataSource ds = ctx.datasource();
      try {
        ADCDataset dataset = new ADCDataset();
        int row = getRow(ds, ctx.type(), ctx.region());
        for(Indicator i : Indicator.forType(ctx.type())) {
          int col = getColumnStart(ds, i);
          for(String s : getCategories(ds, i)) {
            try {
              double d = ds.select(row, col).asDouble();
              dataset.addValue(d, i.getLabel(), s);
            } catch(Exception e) {
              dataset.addValue(null, i.getLabel(), s);
            }
            col++;
          }
        }
        return dataset;
      } catch(MissingDataException e) {
        throw new RuntimeException(e);
      }
    }

    @Override
    protected Drawable getDrawable(JFreeContext ctx) {
      return MarineTrends.createChart((ADCDataset)ctx.dataset(), new Dimension(750, 500), ctx);
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
              .add(String.format("%s %s", ctx.region().getProperName(), ctx.type().getLabel()))
              .addAll(columnKeys)
              .build();
          csv.write(heading);
          for (String row : rowKeys) {
            List<String> line = Lists.newArrayList();
            line.add(row);
            for (String col : columnKeys) {
              final Number n = dataset.getValue(row, col);
              line.add(n == null ? "" : String.format("%.1f", n.doubleValue()));
            }
            csv.write(line);
          }
        }});
    }

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.TITLE, type.equals(ChartType.MARINE_WQT)?"${region} remote sensed water"
              + " quality score":"${region} inshore ${type}").
          put(Attribute.Y_AXIS_LABEL, "Score").
          put(Attribute.X_AXIS_LABEL, "Reporting Year").
          put(Attribute.SERIES_COLORS, new Color[] {Colors.BLUE, Colors.DARK_RED,
              Colors.RED, Colors.VIOLET, Colors.GREEN}).
          build();
    }
}
