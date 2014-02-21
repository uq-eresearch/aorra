package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.strip;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.jfree.data.statistics.StatisticalCategoryDataset;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.TrendsSeagrassAbundance;
import charts.jfree.ADSCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class TrendsSeagrassAbundanceBuilder extends AbstractBuilder {

  private static final Map<Attribute, Object> ccDefaults = ImmutableMap.<Attribute, Object>of(
      Attribute.TITLE, "Trends in seagrass abundance (mean) at ${subregion}",
      Attribute.DOMAIN_AXIS_LABEL, "Year",
      Attribute.RANGE_AXIS_LABEL, "Seagrass abundance"
      );

    public static final String SUBREGION = "subregion";

    public static enum Subregion {

        AP(Region.CAPE_YORK, "Archer Point"),
        YP(Region.WET_TROPICS, "Yule Point"),
        LB(Region.WET_TROPICS, "Lugger Bay"),
        GI(Region.WET_TROPICS, "Green Island"),
        DI(Region.WET_TROPICS, "Dunk Island"),
        TSV(Region.BURDEKIN, "Bushland and Shelly Beaches"),
        MI(Region.BURDEKIN, "Magnetic Island"),
        SI(Region.MACKAY_WHITSUNDAY, "Sarina Inlet"),
        PI(Region.MACKAY_WHITSUNDAY, "Pioneer Bay"),
        HM(Region.MACKAY_WHITSUNDAY, "Hamilton Island"),
        GH(Region.FITZROY, "Gladstone Harbour"),
        GK(Region.FITZROY, "Great Keppel Island"),
        SWB(Region.FITZROY, "Shoalwater Bay"),
        UG(Region.BURNETT_MARY, "Urangan"),
        RD(Region.BURNETT_MARY, "Rodds Bay"),
        ;

        private Region region;
        private String label;

        private Subregion(Region region, String label) {
            this.region = region;
            this.label = label;
        }

        public Region getRegion() {
            return region;
        }

        public String getLabel() {
            return label;
        }

        public static Subregion fromName(String name) {
            try {
                return valueOf(name);
            } catch(IllegalArgumentException e) {
                return null;
            }
        }
    }

    public TrendsSeagrassAbundanceBuilder() {
        super(ChartType.TSA);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource ds) {
        try {
            if(stripEqualsIgnoreCase(ds, "site", "A1") &&
                    stripEqualsIgnoreCase(ds, "date", "B1") &&
                    stripEqualsIgnoreCase(ds, "mean", "C1") &&
                    stripEqualsIgnoreCase(ds, "se", "D1")) {
                return true;
            }
        } catch(MissingDataException e) {}
        return false;
    }

    private boolean stripEqualsIgnoreCase(SpreadsheetDataSource ds, String s,
            String cellref) throws MissingDataException {
        return equalsIgnoreCase(s, strip(ds.select(cellref).asString()));
    }

    @Override
    protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
        return Collections.singletonMap(SUBREGION, getSubregions(datasource));
    }

    private List<String> getSubregions(SpreadsheetDataSource ds) {
        List<String> subregions = Lists.newArrayList();
        for(int row = 1; true; row++) {
            try {
                String s = strip(ds.select(row, 0).asString());
                if(isBlank(s)) {
                    if(isBlank(strip(ds.select(row+1, 0).asString()))) {
                        break;
                    } else {
                        continue;
                    }
                }
                if(Subregion.fromName(s.toUpperCase()) != null && !subregions.contains(s)) {
                    subregions.add(s);
                }
            } catch(MissingDataException e ) {}
        }
        return subregions;
    }

    private Subregion getSubregion(final SpreadsheetDataSource ds, Map<String, ?> parameters) {
        String subregion = (String)parameters.get(SUBREGION);
        if(isBlank(subregion)) {
            return null;
        }
        if(!getSubregions(ds).contains(subregion)) {
            return null;
        }
        return Subregion.valueOf(subregion.toUpperCase());
    }

    @Override
  public Chart build(final SpreadsheetDataSource ds, final ChartType type,
      final Region region, final Map<String, String> parameters) {
    final Subregion subregion = getSubregion(ds, parameters);
    if (subregion == null) {
      return null;
    }
    if (subregion.getRegion() == region) {
      final ADSCDataset dataset = createDataset(ds, subregion);
      configurator(ds, ccDefaults, type, region,
          ImmutableMap.of("subregion", subregion.getLabel())).configure(dataset);
      final Drawable d = TrendsSeagrassAbundance.createChart(dataset, new Dimension(750, 500));
      return new AbstractChart() {
        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(type, region, parameters, "TSA at "+subregion.getLabel());
        }

        @Override
        public Drawable getChart() {
          return d;
        }

        @Override
        public String getCSV() throws UnsupportedFormatException {
          final StringWriter sw = new StringWriter();
          try {
            final StatisticalCategoryDataset dataset =
                createDataset(ds, subregion);
            final CsvListWriter csv = new CsvListWriter(sw,
                CsvPreference.STANDARD_PREFERENCE);
            @SuppressWarnings("unchecked")
            List<String> columnKeys = dataset.getColumnKeys();
            @SuppressWarnings("unchecked")
            List<Subregion> rowKeys = dataset.getRowKeys();
            final List<String> heading = ImmutableList.<String> builder()
                .add(format("%s %s %s", type, region,
                    rowKeys.get(0).getLabel()))
                .add("Mean")
                .add("Std Dev")
                .build();
            csv.write(heading);
            for (String col : columnKeys) {
              List<String> line = newLinkedList();
              line.add(col);
              line.add(format("%.3f",
                  dataset.getMeanValue(rowKeys.get(0), col).doubleValue()));
              line.add(format("%.3f",
                  dataset.getStdDevValue(rowKeys.get(0), col).doubleValue()));
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
    } else {
      return null;
    }
  }

  private int
      getSubregionRowStart(SpreadsheetDataSource ds, Subregion subregion) {
    for (int row = 1; true; row++) {
      try {
        String s = strip(ds.select(row, 0).asString());
        if (isBlank(s)) {
          if (isBlank(strip(ds.select(row + 1, 0).asString()))) {
            break;
          } else {
            continue;
          }
        }
        if (equalsIgnoreCase(s, subregion.name())) {
          return row;
        }
      } catch (MissingDataException e) {
      }
    }
    return -1;
  }

  private ADSCDataset createDataset(
      SpreadsheetDataSource ds, Subregion subregion) {
    final ADSCDataset d = new ADSCDataset();
    SimpleDateFormat sdf = new SimpleDateFormat("MMMMM yyyy");
    try {
      int row = getSubregionRowStart(ds, subregion);
      if (row == -1) {
        return d;
      }
      for (; true; row++) {
        String subr = strip(ds.select(row, 0).asString());
        if (!equalsIgnoreCase(subr, subregion.name())) {
          break;
        }
        Date date = ds.select(row, 1).asDate();
        Double mean = ds.select(row, 2).asDouble();
        Double deviation = ds.select(row, 3).asDouble();
        if (mean != null && deviation != null) {
          d.add(mean, deviation, subregion, sdf.format(date));
        }
      }
    } catch (MissingDataException e) {
      e.printStackTrace();
    }
    return d;
  }

}
