package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.strip;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jfree.data.statistics.StatisticalCategoryDataset;
import org.supercsv.io.CsvListWriter;

import play.Logger;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.TrendsSeagrassAbundance;
import charts.jfree.ADSCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class TrendsSeagrassAbundanceBuilder extends JFreeBuilder {

  private final SubstitutionKey S_SUBREGION = new SubstitutionKey("subregion",
      "Subregion e.g. Archer Point", new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          return getSubregion(ctx.datasource(), ctx.parameters()).getLabel();
        }
      });

  private final SubstitutionKey S_HABITAT = new SubstitutionKey("habitat",
      "The habitat type of the location (fringing reef, coastal, reef and estuarine)",
      new SubstitutionKey.Val() {
        @Override
        public String value(Context ctx) {
          return getSubregion(ctx.datasource(), ctx.parameters()).getHabitat().getLabel();
        }
      });

    public static final String SUBREGION = "subregion";

    public static enum Habitat {
      REEF, COASTAL, ESTUARINE;

      public String getLabel() {
        return name().toLowerCase();
      }
    }

    public static enum Subregion {

        AP(Region.CAPE_YORK, "Archer Point", Habitat.REEF),
        YP(Region.WET_TROPICS, "Yule Point", Habitat. COASTAL),
        LB(Region.WET_TROPICS, "Lugger Bay", Habitat.COASTAL),
        GI(Region.WET_TROPICS, "Green Island", Habitat.REEF),
        DI(Region.WET_TROPICS, "Dunk Island", Habitat.REEF),
        TSV(Region.BURDEKIN, "Bushland and Shelly Beaches", Habitat.COASTAL),
        MI(Region.BURDEKIN, "Magnetic Island", Habitat.REEF),
        SI(Region.MACKAY_WHITSUNDAY, "Sarina Inlet", Habitat.COASTAL),
        PI(Region.MACKAY_WHITSUNDAY, "Pioneer Bay", Habitat.COASTAL),
        HM(Region.MACKAY_WHITSUNDAY, "Hamilton Island", Habitat.REEF),
        GH(Region.FITZROY, "Gladstone Harbour", Habitat.ESTUARINE),
        GK(Region.FITZROY, "Great Keppel Island", Habitat.REEF),
        SWB(Region.FITZROY, "Shoalwater Bay", Habitat.COASTAL),
        UG(Region.BURNETT_MARY, "Urangan", Habitat.ESTUARINE),
        RD(Region.BURNETT_MARY, "Rodds Bay", Habitat.ESTUARINE),
        ;

        private final Region region;
        private final String label;
        private final Habitat habitat;

        private Subregion(Region region, String label, Habitat habitat) {
            this.region = region;
            this.label = label;
            this.habitat = habitat;
        }

        public Region getRegion() {
            return region;
        }

        public String getLabel() {
            return label;
        }

        public Habitat getHabitat() {
          return habitat;
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

  @Override
  protected ADSCDataset createDataset(Context ctx) {
    SpreadsheetDataSource ds = ctx.datasource();
    final Subregion subregion = getSubregion(ds, ctx.parameters());
    if (subregion == null) {
      return null;
    }
    if (subregion.getRegion() == ctx.region()) {
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
        Logger.debug("while building tracking towards targets dataset "+ctx.toString(), e);
      }
      return d;
    } else {
      return null;
    }
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE, "Seagrass abundance at inshore intertidal ${habitat} habitat"
            + "\nat ${subregion} in the ${region} region").
        put(Attribute.X_AXIS_LABEL, "").
        put(Attribute.Y_AXIS_LABEL, "Seagrass abundance").
        put(Attribute.SERIES_COLOR, new Color(30, 172, 226)).
        build();
  }

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    return TrendsSeagrassAbundance.createChart(
        (ADSCDataset)ctx.dataset(), new Dimension(750, 500));
  }

  @Override
  protected String getCsv(final JFreeContext ctx) {
    final StatisticalCategoryDataset dataset = (StatisticalCategoryDataset)ctx.dataset();
    return Csv.write(new CsvWriter() {
      @Override
      public void write(CsvListWriter csv) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> columnKeys = dataset.getColumnKeys();
        @SuppressWarnings("unchecked")
        List<Subregion> rowKeys = dataset.getRowKeys();
        final List<String> heading = ImmutableList.<String> builder()
            .add(format("%s %s %s", ctx.type(), ctx.region(),
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
      }});
  }

  @Override
  protected String title(JFreeContext ctx) {
    return "TSA at "+getSubregion(ctx.datasource(), ctx.parameters()).getLabel();
  }

  @Override
  public Set<SubstitutionKey> substitutionKeys() {
    return ImmutableSet.<SubstitutionKey>builder().
        addAll(super.substitutionKeys()).add(S_SUBREGION).add(S_HABITAT).build();
  }

}
