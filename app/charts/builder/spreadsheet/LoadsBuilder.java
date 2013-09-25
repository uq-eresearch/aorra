package charts.builder.spreadsheet;

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

    private static final String SHEETNAME = "Summary Table RRC 2_3";

    private static final String PERIOD = "period";

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

    public LoadsBuilder() {
        super(ChartType.LOADS);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        return datasource.hasSheet(SHEETNAME);
    }

    @Override
    public Chart build(SpreadsheetDataSource datasource, final ChartType type,
            final Region region, final Dimension queryDimensions) {
        throw new RuntimeException("parameters missing");
    }

    @Override
    protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
        return new ImmutableMap.Builder<String, List<String>>()
                .put(PERIOD, Lists.newArrayList(getPeriods(datasource)))
                .build();
    }

    private List<String> getPeriods(SpreadsheetDataSource ds) {
        try {
            List<String> periods = Lists.newArrayList();
            int row = 2;
            for(int col = 3;true;col++) {
                String s = ds.select(SHEETNAME, row, col).asString();
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

    public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
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
                        dataset, new Dimension(750, 500));
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
        return ds.select(SHEETNAME, row, col).asDouble();
    }

    private String getTitle(SpreadsheetDataSource ds, Region region, String period) {
        String title = region.getProperName() + " total load reductions from\n";
        List<String> periods = getPeriods(ds);
        if(StringUtils.equalsIgnoreCase(period, "total")) {
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
