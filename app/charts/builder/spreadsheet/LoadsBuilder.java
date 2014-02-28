package charts.builder.spreadsheet;

import static charts.ChartType.LOADS_DIN;
import static charts.ChartType.LOADS_PSII;
import static charts.ChartType.LOADS_TN;
import static charts.ChartType.LOADS_TSS;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import charts.ChartType;
import charts.Region;
import charts.builder.DataSource.MissingDataException;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public abstract class LoadsBuilder extends JFreeBuilder {

    protected static final String PERIOD = "period";

    protected static final String TOTAL = "Total";

    protected static enum Indicator {
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

    protected static final ImmutableMap<Region, Integer> ROWS =
            new ImmutableMap.Builder<Region, Integer>()
              .put(Region.CAPE_YORK, 5)
              .put(Region.WET_TROPICS, 6)
              .put(Region.BURDEKIN, 7)
              .put(Region.MACKAY_WHITSUNDAY, 8)
              .put(Region.FITZROY, 9)
              .put(Region.BURNETT_MARY, 10)
              .put(Region.GBR, 12)
              .build();

    protected static final ImmutableMap<ChartType, Indicator> INDICATORS =
            new ImmutableMap.Builder<ChartType, Indicator>()
              .put(LOADS_DIN, Indicator.DIN)
              .put(LOADS_TN, Indicator.TN)
              .put(LOADS_PSII, Indicator.PSII)
              .put(LOADS_TSS, Indicator.TSS)
              .build();

    public LoadsBuilder(ChartType type) {
      super(type);
    }

    public LoadsBuilder(List<ChartType> types) {
      super(types);
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

    protected Double selectAsDouble(SpreadsheetDataSource ds, Region region,
            Indicator indicator, String period) throws MissingDataException {
        List<String> periods = getPeriods(ds);
        int row = ROWS.get(region) - 1;
        int col = indicator.ordinal() * periods.size() + periods.indexOf(period) + 3;
        return ds.select(row, col).asDouble();
    }

    protected String getTitle(SpreadsheetDataSource ds, Indicator indicator, String period) {
        return getTitle(ds, indicator.getLabel() + " load reductions from\n", period);
    }

    protected String getTitle(SpreadsheetDataSource ds, Region region, String period) {
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
