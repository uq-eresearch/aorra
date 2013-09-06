package charts.builder.spreadsheet;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.pegdown.PegDownProcessor;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.graphics.BeerCoaster;
import charts.graphics.BeerCoaster.Category;
import charts.graphics.BeerCoaster.Condition;
import charts.graphics.BeerCoaster.Indicator;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Table.Cell;

public class MarineBuilder extends AbstractBuilder {

  private static final ImmutableMap<Region, Integer> OFFSETS =
      new ImmutableMap.Builder<Region, Integer>()
        .put(Region.CAPE_YORK, 0)
        .put(Region.WET_TROPICS, 1)
        .put(Region.BURDEKIN, 2)
        .put(Region.MACKAY_WHITSUNDAY, 3)
        .put(Region.FITZROY, 4)
        .put(Region.BURNETT_MARY, 5)
        .put(Region.GBR, 6)
        .build();

  public MarineBuilder() {
    super(ChartType.MARINE);
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    return isMarineSpreadsheet(datasource);
  }

  private boolean isMarineSpreadsheet(DataSource datasource) {
    try {
      return "MARINE SUMMARY".equalsIgnoreCase(
          datasource.select("Summary!B18").getValue());
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  public Chart build(final SpreadsheetDataSource datasource,
      final ChartType type,
      final Region region, final Map<String, String[]> query) {
    final BeerCoaster beercoaster = getDrawable(datasource, region);
    final AbstractBuilder thisBuilder = this;
    if (beercoaster != null) {
      return new AbstractChart(query) {
        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(ChartType.MARINE, region);
        }

        @Override
        public Drawable getChart() {
          return beercoaster;
        }

        @Override
        public String getCSV() {
          final StringWriter sw = new StringWriter();
          try {
            final CsvListWriter csv = new CsvListWriter(sw,
                CsvPreference.STANDARD_PREFERENCE);
            csv.write("Overall marine condition", beercoaster
                .getOverallCondition().getLabel());
            for (Category c : Category.values()) {
              csv.write(c.getName(), beercoaster.getCondition(c).getLabel());
              for (Indicator i : Indicator.values()) {
                if (i.getCategory() == c) {
                  csv.write(i.getName(), beercoaster.getCondition(i).getLabel());
                }
              }
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
          return thisBuilder.getCommentary(datasource, region);
        }
      };
    } else {
      return null;
    }
  }

  private BeerCoaster getDrawable(DataSource datasource, Region region) {
    try {
      Integer offset = OFFSETS.get(region);
      if (offset == null) {
        throw new Exception("unknown region " + region);
      }
      Double wa = getValue(datasource, "E", 9, offset);
      Double coral = getValue(datasource, "P", 9, offset);
      Double seag = getValue(datasource, "J", 9, offset);
      Double chla = getValue(datasource, "C", 9, offset);
      Double tss = getValue(datasource, "D", 9, offset);
      Double cs = getValue(datasource, "M", 9, offset);
      Double juv = getValue(datasource, "O", 9, offset);
      Double alg = getValue(datasource, "N", 9, offset);
      Double cover = getValue(datasource, "L", 9, offset);
      Double abu = getValue(datasource, "G", 9, offset);
      Double rep = getValue(datasource, "H", 9, offset);
      Double nut = getValue(datasource, "I", 9, offset);
      Double mc = getValue(datasource, "F", 20, offset);
      BeerCoaster bc = configureInternal(wa, coral, seag, chla, tss, cs, juv,
          alg, cover, abu, rep, nut, mc);
      return bc;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private Condition determineCondition(Double index) {
    if (index == null) {
      return Condition.NOT_EVALUATED;
    } else if (index >= 80) {
      return Condition.VERY_GOOD;
    } else if (index >= 60) {
      return Condition.GOOD;
    } else if (index >= 40) {
      return Condition.MODERATE;
    } else if (index >= 20) {
      return Condition.POOR;
    } else {
      return Condition.VERY_POOR;
    }
  }

  private BeerCoaster configureInternal(Double wa, Double coral, Double seag,
      Double chla, Double tss, Double cs, Double juv, Double alg, Double cover,
      Double abu, Double rep, Double nut, Double mc) {
    BeerCoaster chart = new BeerCoaster();
    chart.setCondition(Category.WATER_QUALITY, determineCondition(wa));
    chart.setCondition(Category.CORAL, determineCondition(coral));
    chart.setCondition(Category.SEAGRASS, determineCondition(seag));
    chart.setCondition(Indicator.CHLOROPHYLL_A, determineCondition(chla));
    chart.setCondition(Indicator.TOTAL_SUSPENDED_SOLIDS,
        determineCondition(tss));
    chart.setCondition(Indicator.SETTLEMENT, determineCondition(cs));
    chart.setCondition(Indicator.JUVENILE, determineCondition(juv));
    chart.setCondition(Indicator.ALGAE, determineCondition(alg));
    chart.setCondition(Indicator.COVER, determineCondition(cover));
    chart.setCondition(Indicator.ABUNDANCE, determineCondition(abu));
    chart.setCondition(Indicator.REPRODUCTION, determineCondition(rep));
    chart.setCondition(Indicator.NUTRIENT_STATUS, determineCondition(nut));
    chart.setOverallCondition(determineCondition(mc));
    return chart;
  }

  private Double getValue(DataSource ds, String column, int row, int rowOffset)
      throws Exception {
    String str = ds.select("Summary!" + column + (row + rowOffset)).getValue();
    try {
      return Double.parseDouble(str);
    } catch (NumberFormatException e) {
      return null;
    }
  }

}
