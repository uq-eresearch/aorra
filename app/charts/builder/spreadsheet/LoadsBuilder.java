package charts.builder.spreadsheet;

import static charts.ChartType.LOADS;
import static charts.ChartType.LOADS_DIN;
import static charts.ChartType.LOADS_PSII;
import static charts.ChartType.LOADS_TN;
import static charts.ChartType.LOADS_TSS;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
import charts.graphics.Loads;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class LoadsBuilder extends AbstractBuilder {

    private static final String PERIOD = "period";

    private static final String TOTAL = "Total";

    private static enum Indicator {
        TSS("Total suspended solids", true),
        TP("Total phosphorus", true),
        PP("Particulate phosphorus", true),
        DIP(false),
        DOP(false),
        TN("Total nitrogen", true),
        PN("Particulate nitrogen", true),
        DIN("Dissolved Inorganic nitrogen", true),
        DON(false),
        PSII("PSII pesticides", true);

        private String label;
        private boolean include;

        private Indicator(boolean include) {
            this.include = include;
        }

        private Indicator(String label, boolean include) {
            this(include);
            this.label = label;
        }

        public boolean include() {
            return include;
        }

        public String getLabel() {
            return label!=null?label:name();
        }
    }

    private static final ImmutableMap<Region, Integer> ROWS =
            new ImmutableMap.Builder<Region, Integer>()
              .put(Region.CAPE_YORK, 5)
              .put(Region.WET_TROPICS, 6)
              .put(Region.BURDEKIN, 7)
              .put(Region.MACKAY_WHITSUNDAY, 8)
              .put(Region.FITZROY, 9)
              .put(Region.BURNETT_MARY, 10)
              .put(Region.GBR, 12)
              .build();

    private static final ImmutableMap<ChartType, Indicator> INDICATORS =
            new ImmutableMap.Builder<ChartType, Indicator>()
              .put(LOADS_DIN, Indicator.DIN)
              .put(LOADS_TN, Indicator.TN)
              .put(LOADS_PSII, Indicator.PSII)
              .put(LOADS_TSS, Indicator.TSS)
              .build();

    public LoadsBuilder() {
        super(Lists.newArrayList(LOADS, LOADS_DIN, LOADS_TN, LOADS_PSII, LOADS_TSS));
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource ds) {
        try {
            if(StringUtils.equalsIgnoreCase("Region", ds.select("C3").asString()) &&
                    StringUtils.equalsIgnoreCase("Total Change (%)", ds.select("C12").asString())) {
                return true;
            }
        } catch(MissingDataException e) {}
        return false;
    }

    @Override
    protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
        return new ImmutableMap.Builder<String, List<String>>()
                .put(PERIOD, Lists.newArrayList(TOTAL))
                .build();
    }

    private List<String> getPeriods(SpreadsheetDataSource ds) {
        try {
            List<String> periods = Lists.newArrayList();
            int row = 2;
            for(int col = 3;true;col++) {
                String s = ds.select(row, col).asString();
                if(StringUtils.isBlank(s) || periods.contains(s)) {
                    return periods;
                } else {
                    periods.add(s);
                }
            }
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension queryDimensions, final Map<String, String> parameters) {
        if(type == LOADS) {
            return buildLoads(datasource, type, region, queryDimensions, parameters);
        } else if(region == Region.GBR) {
            return buildLoadRegions(datasource, type, region, queryDimensions, parameters);
        } else {
            return null;
        }
    }

    public Chart buildLoadRegions(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, final Dimension queryDimensions, final Map<String, ?> parameters ) {
        final String period = (String)parameters.get(PERIOD);
        if(StringUtils.isBlank(period)) {
            return null;
        }
        final Indicator indicator = INDICATORS.get(type);
        if(indicator == null) {
            throw new RuntimeException(String.format("chart type %s not implemented", type.name()));
        }
        return new AbstractChart(queryDimensions) {

            @Override
            public ChartDescription getDescription() {
                return new ChartDescription(type, region, parameters);
            }

            @Override
            public Drawable getChart() {
                return Loads.createChart(getTitle(datasource, indicator, period),
                        "Region",
                        getRegionsDataset(datasource, indicator, period),
                        new Dimension(750, 500));
            }

            @Override
            public String getCSV() throws UnsupportedFormatException {
              final StringWriter sw = new StringWriter();
              try {
                final CategoryDataset dataset =
                    getRegionsDataset(datasource, indicator, period);
                final CsvListWriter csv = new CsvListWriter(sw,
                    CsvPreference.STANDARD_PREFERENCE);
                @SuppressWarnings("unchecked")
                List<String> columnKeys = dataset.getColumnKeys();
                @SuppressWarnings("unchecked")
                List<String> rowKeys = dataset.getRowKeys();
                final List<String> heading = ImmutableList.<String>builder()
                    .add(format("%s %s %s load reductions",
                        region, indicator, period))
                    .addAll(rowKeys)
                    .build();
                csv.write(heading);
                for (String col : columnKeys) {
                  List<String> line = newLinkedList();
                  line.add(col);
                  for (String row : rowKeys) {
                    line.add(format("%.1f",
                        dataset.getValue(row, col).doubleValue()));
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
        };
    }

    private Chart buildLoads(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension queryDimensions, final Map<String, ?> parameters) {
        final String period = (String)parameters.get(PERIOD);
        if(StringUtils.isBlank(period)) {
            return null;
        }
        return new AbstractChart(queryDimensions) {

            @Override
            public ChartDescription getDescription() {
                return new ChartDescription(type, region, parameters);
            }

            @Override
            public Drawable getChart() {
                return Loads.createChart(getTitle(datasource, region, period),
                        "Pollutants", getDataset(datasource, region, period),
                        new Dimension(750, 500));
            }

            @Override
            public String getCSV() throws UnsupportedFormatException {
              final StringWriter sw = new StringWriter();
              try {
                final CategoryDataset dataset =
                    getDataset(datasource, region, period);
                final CsvListWriter csv = new CsvListWriter(sw,
                    CsvPreference.STANDARD_PREFERENCE);
                @SuppressWarnings("unchecked")
                List<String> columnKeys = dataset.getColumnKeys();
                @SuppressWarnings("unchecked")
                List<String> rowKeys = dataset.getRowKeys();
                final List<String> heading = ImmutableList.<String>builder()
                    .add(format("%s %s load reductions", region, period))
                    .addAll(rowKeys)
                    .build();
                csv.write(heading);
                for (String col : columnKeys) {
                  List<String> line = newLinkedList();
                  line.add(col);
                  for (String row : rowKeys) {
                    line.add(format("%.1f",
                        dataset.getValue(row, col).doubleValue()));
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

        };
    }

    private CategoryDataset getRegionsDataset(SpreadsheetDataSource ds,
            Indicator indicator, String period) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for(Region region : Region.values()) {
            Integer row = ROWS.get(region);
            if(row == null) {
                throw new RuntimeException(String.format("region %s not configured", region.getName()));
            }
            try {
                double val = selectAsDouble(ds, region, indicator, period);
                dataset.addValue(val, indicator.getLabel(), region.getProperName());
            } catch(Exception e) {
                throw new RuntimeException("region "+region.getName(), e);
            }
        }
        return dataset;
    }

    private CategoryDataset getDataset(SpreadsheetDataSource ds, Region region, String period) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        Integer row = ROWS.get(region);
        if(row == null) {
            throw new RuntimeException("unknown region "+region);
        }
        row--;
        try {
            for(Indicator indicator : Indicator.values()) {
                if(indicator.include()) {
                    dataset.addValue(selectAsDouble(ds, region, indicator, period),
                            region.getProperName() , indicator.getLabel());
                }
            }
            return dataset;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    private Double selectAsDouble(SpreadsheetDataSource ds, Region region,
            Indicator indicator, String period) throws MissingDataException {
        List<String> periods = getPeriods(ds);
        int row = ROWS.get(region) - 1;
        int col = indicator.ordinal() * periods.size() + periods.indexOf(period) + 3;
        return ds.select(row, col).asDouble();
    }

    private String getTitle(SpreadsheetDataSource ds, Indicator indicator, String period) {
        return getTitle(ds, indicator.getLabel() + " load reductions from\n", period);
    }

    private String getTitle(SpreadsheetDataSource ds, Region region, String period) {
        String title = region.getProperName() + " total load reductions from\n";
        return getTitle(ds, title, period);
    }

    private String getTitle(SpreadsheetDataSource ds, String prefix, String period) {
        String title = prefix;
        List<String> periods = getPeriods(ds);
        if(StringUtils.equalsIgnoreCase(period, TOTAL)) {
            title += formatPeriods(periods, -1, periods.size()-2);
        } else {
            int start = periods.indexOf(period);
            title += formatPeriods(periods, start-1, start);
        }
        return title;
    }

    private String formatPeriods(List<String> periods, int start, int end) {
        if(start == -1) {
            return " the baseline (2008-2009) to " + formatPeriod(periods.get(end));
        } else {
            return formatPeriod(periods.get(start)) + " to " +
                    formatPeriod(periods.get(end));
        }
    }

    private String formatPeriod(String period) {
        try {
            String p = StringUtils.substringBetween(period, "(", ")");
            String[] sa =  StringUtils.split(p, "/");
            if(sa[0].length() == 2 && sa[1].length() == 2) {
                return "20"+sa[0]+"-"+"20"+sa[1];
            }
            return period;
        } catch(Exception e) {
            return period;
        }
    }

}
