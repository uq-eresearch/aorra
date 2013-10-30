package charts.builder.spreadsheet;

import static charts.ChartType.MARINE_CT;
import static charts.ChartType.MARINE_ST;
import static charts.ChartType.MARINE_WQT;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.MarineTrends;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class MarineTrendsBuilder extends AbstractBuilder {

    private static final String TIME_SERIES = "Time Series";
    private static final String WATER_QUALITY = "Water Quality";
    private static final String SEAGRASS = "Seagrass";
    private static final String CORAL = "Coral";

    private static final ImmutableMap<ChartType, Integer> ROWS = ImmutableMap.of(
            MARINE_WQT, 2, MARINE_ST, 14, MARINE_CT, 26);

    private static enum Indicator {
        OWQ_SCORE("Overall water quality score", "WQI", MARINE_WQT),
        CHLOROPHYLL_A("Chlorophyll \u03b1", "Chlorophyll a", MARINE_WQT),
        TOTAL_SUSPENDED_SOLIDS("Total suspended solids", "TSS", MARINE_WQT),
        OS_SCORE("Overall seagrass score", "Seagrass Index", MARINE_ST),
        REPRODUCTION("Reproduction", "Reproductive Effort", MARINE_ST),
        ABUNDANCE("Abundance", "Abundance", MARINE_ST),
        NUTRIENT_STATUS("Nutrient status", "C:N Ratio", MARINE_ST),
        OC_SCORE("Overall coral score", "Coral Index", MARINE_CT),
        COVER("Cover", "Cover", MARINE_CT),
        CHANGE("Change", "Change", MARINE_CT),
        MACROALGAE("Macroalgae", "Algal Cover", MARINE_CT),
        JUVENILE("Juveniles", "Juvenile", MARINE_CT),
        ;

        private String label;
        private String heading;
        private ChartType type;

        private Indicator(String label, String heading, ChartType type) {
            this.label = label;
            this.heading = heading;
            this.type = type;
        }

        public String getLabel() {
            return label;
        }

        public String getHeading() {
            return heading;
        }

        public ChartType getChartType() {
            return type;
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
            return datasource.hasSheet(TIME_SERIES) &&
              equalsIgnoreCase(datasource.select(TIME_SERIES, "B2").asString(), WATER_QUALITY) &&
              equalsIgnoreCase(datasource.select(TIME_SERIES, "B14").asString(), SEAGRASS) &&
              equalsIgnoreCase(datasource.select(TIME_SERIES, "B26").asString(), CORAL);
        } catch(MissingDataException e) {}
        return false;
    }

  @Override
  public Chart
      build(final SpreadsheetDataSource datasource, final ChartType type,
          final Region region, final Dimension queryDimensions) {
    if (type == MARINE_CT
        && (region == Region.CAPE_YORK || region == Region.BURNETT_MARY)) {
      return null;
    } else {
      return new AbstractChart(queryDimensions) {

        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(type, region);
        }

        @Override
        public Drawable getChart() {
          return MarineTrends.createChart(getDataset(datasource, type, region),
              type, region, new Dimension(750, 500));
        }

        @Override
        public String getCSV() throws UnsupportedFormatException {
          final StringWriter sw = new StringWriter();
          try {
            final CategoryDataset dataset =
                getDataset(datasource, type, region);
            final CsvListWriter csv = new CsvListWriter(sw,
                CsvPreference.STANDARD_PREFERENCE);
            @SuppressWarnings("unchecked")
            List<String> columnKeys = dataset.getColumnKeys();
            @SuppressWarnings("unchecked")
            List<String> rowKeys = dataset.getRowKeys();
            final List<String> heading = ImmutableList.<String>builder()
                .add(format("%s %s", region.getProperName(), type.getLabel()))
                .addAll(columnKeys)
                .build();
            csv.write(heading);
            for (String row : rowKeys) {
              List<String> line = newLinkedList();
              line.add(row);
              for (String col : columnKeys) {
                final Number n = dataset.getValue(row, col);
                line.add(n == null ? "" : format("%.1f", n.doubleValue()));
              }
              csv.write(line);
            }
            csv.close();
          } catch (IOException e) {
            // How on earth would you get an IOException with a StringWriter?
            throw new RuntimeException(e);
          }
          return sw.toString();
        }

        @Override
        public String getCommentary() throws UnsupportedFormatException {
          throw new UnsupportedFormatException();
        }

      };
    }
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
                if(equalsIgnoreCase(ds.select(row, col).asString(), indicator.getHeading())) {
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
            l.add(ds.select(row, col).asString());
        }
        return l;
    }

    private CategoryDataset getDataset(SpreadsheetDataSource ds, ChartType type, Region region) {
        try {
            ds.setDefaultSheet(TIME_SERIES);
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            int row = getRow(ds, type, region);
            for(Indicator i : Indicator.forType(type)) {
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

}
