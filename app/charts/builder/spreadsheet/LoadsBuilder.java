package charts.builder.spreadsheet;

import static charts.ChartType.LOADS_DIN;
import static charts.ChartType.LOADS_PSII;
import static charts.ChartType.LOADS_TN;
import static charts.ChartType.LOADS_TSS;

import java.awt.Color;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import charts.ChartType;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

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

    private final SubstitutionKey LAST_PERIOD = new SubstitutionKey("lastPeriod",
        "last period of loads data in the spreadsheet",
        new SubstitutionKey.Val() {
          @Override
          public String value(Context ctx) {
            return lastPeriod(ctx);
          }
        });

    private final SubstitutionKey LAST_PERIOD_YYYY = new SubstitutionKey("lastPeriodyyyy",
        "last period of loads data in the spreadsheet format with 4 digit year",
        new SubstitutionKey.Val() {
          @Override
          public String value(Context ctx) {
            return formatPeriod(lastPeriod(ctx));
          }
        });

    public LoadsBuilder(ChartType type) {
      super(type);
    }

    public LoadsBuilder(List<ChartType> types) {
      super(types);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource ds) {
        try {
          return StringUtils.containsIgnoreCase(ds.select("C1").asString(),
              "Summary of Load Reductions") && indicatorsOk(ds);
        } catch(MissingDataException e) {
          return false;
        }
    }

    private boolean indicatorsOk(SpreadsheetDataSource ds) throws MissingDataException {
      Set<Indicator> iset = Sets.newHashSet();
      for(Indicator i : INDICATORS.values()) {
        if(i.include()) {
          iset.add(i);
        }
      }
      for(int col = 0; col< ds.getColumnCount(1);col++) {
        String s = StringUtils.strip(ds.select(1, col).asString());
        if(StringUtils.isNotBlank(s)) {
          Indicator i = Indicator.valueOf(StringUtils.upperCase(s));
          if(i != null) {
            iset.remove(i);
          }
        }
      }
      return iset.isEmpty();
    }

    @Override
    protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
      // only the 'Total' period can be selected currently. 
        return new ImmutableMap.Builder<String, List<String>>()
                .put(PERIOD, Lists.newArrayList(TOTAL))
                .build();
    }

    private List<String> getPeriods(SpreadsheetDataSource ds, Indicator indicator) {
      try {
        List<String> periods = Lists.newArrayList();
        for(int col = findIndicatorStartColumn(ds, indicator);
            col<=findIndicatorEndColumn(ds, indicator);col++) {
          periods.add(ds.select(2, col).asString());
        }
        return periods;
      } catch(MissingDataException e) {
        throw new RuntimeException(e);
      }
    }

    protected Double selectAsDouble(SpreadsheetDataSource ds, Region region,
            Indicator indicator, String period) throws MissingDataException {
        List<String> periods = getPeriods(ds, indicator);
        int row = ROWS.get(region) - 1;
        int col =  findIndicatorStartColumn(ds, indicator) + periods.indexOf(period);
        Double val = ds.select(row, col).asDouble();
        return val;
    }

    private int findIndicatorStartColumn(SpreadsheetDataSource ds,
        Indicator indicator) throws MissingDataException {
      for(int i=0;i<ds.getColumns(1);i++) {
        if(StringUtils.equalsIgnoreCase(indicator.name(), ds.select(1, i).asString())) {
          return findIndicatorStartColumn(ds, i, indicator);
        }
      }
      throw new RuntimeException("can't find column start for indicator "+indicator);
    }

    private int findIndicatorStartColumn(SpreadsheetDataSource ds,
        int col, Indicator indicator) throws MissingDataException {
      for(int i=col;i>=0;i--) {
        String s = StringUtils.strip(ds.select(2, i-1).asString());
        if(StringUtils.equalsIgnoreCase("total", s) || (i <= 3)) {
          return i;
        }
      }
      throw new RuntimeException("can't find column start for indicator (0) "+indicator);
    }

    private int findIndicatorEndColumn(SpreadsheetDataSource ds,
        Indicator indicator) throws MissingDataException {
      int start = findIndicatorStartColumn(ds, indicator);
      for(int i = start;i<ds.getColumnCount(2);i++) {
        String s = StringUtils.strip(ds.select(2, i).asString());
        if(StringUtils.equalsIgnoreCase("total", s)) {
          return i;
        }
      }
      throw new RuntimeException("can't find column end for indicator "+indicator);
    }

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.Y_AXIS_LABEL, "% Load reduction").
          put(Attribute.SERIES_COLOR, Color.blue).
          build();
    }

    @Override
    public Set<SubstitutionKey> substitutionKeys() {
      return ImmutableSet.<SubstitutionKey>builder().
          addAll(super.substitutionKeys()).add(LAST_PERIOD).add(LAST_PERIOD_YYYY).build();
    }

    private String lastPeriod(Context ctx) {
      Indicator indicator = getIndicator(ctx.type());
      List<String> periods = getPeriods(ctx.datasource(), indicator);
      if(periods.size() >= 2) {
        return periods.get(periods.size()-2);
      } else {
        return "";
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

    protected Indicator getIndicator(ChartType type) {
      Indicator indicator = INDICATORS.get(type);
      return indicator != null ? indicator : Indicator.TSS;
    }

}
