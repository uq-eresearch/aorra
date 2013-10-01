package charts.builder.spreadsheet;

import static charts.ChartType.LOADS;
import static charts.ChartType.LOADS_DIN;
import static charts.ChartType.LOADS_PSII;
import static charts.ChartType.LOADS_TN;
import static charts.ChartType.LOADS_TSS;

import java.awt.Dimension;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.Loads;

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
    public boolean canHandle(SpreadsheetDataSource datasource) {
        return sheet(datasource) != -1;
    }

    private int sheet(SpreadsheetDataSource ds) {
        for(int i=0;i<ds.sheets();i++) {
            try {
                String name = ds.getSheetname(i);
                if(name != null) {
                    if(StringUtils.equalsIgnoreCase("Region", ds.select(name, "C3").asString()) && 
                        StringUtils.equalsIgnoreCase("Total Change (%)", ds.select(name, "C12").asString())) {
                        return i;
                    }
                }
            } catch(Exception e) {}
        }
        return -1;
    }

    @Override
    public Chart build(SpreadsheetDataSource datasource, final ChartType type,
            final Region region, final Dimension queryDimensions) {
        throw new RuntimeException("parameters missing");
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
            final Region region, Dimension queryDimensions, final Map<String, ?> parameters) {
        int sheet = sheet(datasource);
        if(sheet == -1) {
            return null;
        } else {
            datasource.setDefaultSheet(sheet);
        }
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
        final CategoryDataset dataset = getRegionsDataset(datasource, indicator, period);
        return new AbstractChart(queryDimensions) {

            @Override
            public ChartDescription getDescription() {
                return new ChartDescription(type, region, parameters);
            }

            @Override
            public Drawable getChart() {
                return Loads.createChart(getTitle(datasource, indicator, period),
                        "Region", dataset, new Dimension(750, 500));
            }

            @Override
            public String getCSV() throws UnsupportedFormatException {
                throw new UnsupportedFormatException();
            }

            @Override
            public String getCommentary() throws UnsupportedFormatException {
                throw new UnsupportedFormatException();
            }}; 
    }

    private Chart buildLoads(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension queryDimensions, final Map<String, ?> parameters) {
        final String period = (String)parameters.get(PERIOD);
        if(StringUtils.isBlank(period)) {
            return null;
        }
        final CategoryDataset dataset = getDataset(datasource, region, period);
        return new AbstractChart(queryDimensions) {

            @Override
            public ChartDescription getDescription() {
                return new ChartDescription(type, region, parameters);
            }

            @Override
            public Drawable getChart() {
                return Loads.createChart(getTitle(datasource, region, period),
                        "Pollutants", dataset, new Dimension(750, 500));
            }

            @Override
            public String getCSV() throws UnsupportedFormatException {
                throw new UnsupportedFormatException();
            }

            @Override
            public String getCommentary() throws UnsupportedFormatException {
                throw new UnsupportedFormatException();
            }};
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
