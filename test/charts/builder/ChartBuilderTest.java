package charts.builder;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import charts.Chart.UnsupportedFormatException;
import charts.ChartType;
import charts.Region;
import charts.builder.spreadsheet.XlsDataSource;
import charts.builder.spreadsheet.XlsxDataSource;
import charts.representations.Format;

public class ChartBuilderTest {

  private final static ChartBuilder chartBuilder = new DefaultChartBuilder(
      new DataSourceFactory() {
        @Override
        public DataSource getDataSource(String id) throws Exception {
          return getDatasource(ChartType.valueOf(id));
        }
      });

  @Test
  public void format() throws Exception {
    for (final ChartType ct : ChartType.values()) {
      for (final Format f : new Format[]{Format.CSV, Format.EMF}) {
        format(ct, f);
      }
    }
  }

  public void format(ChartType chartType, Format format) throws Exception {
    final List<charts.Chart> charts = chartBuilder.getCharts(
        chartType.name(),
        chartType,
        asList(getDefaultTestingRegion(chartType)),
        Collections.<String, String>emptyMap());
    assertThat(charts).as("No chart generated for "+chartType).isNotEmpty();
    final charts.Chart chart = charts.get(0);
    assertThat(chart.getDescription().getType()).isEqualTo(chartType);
    try {
      chart.outputAs(format, new Dimension(0,0));
    } catch (UnsupportedFormatException ufe) {
      fail(chartType+" should support "+format+" output.");
    }
  }

  public static DataSource getDatasource(ChartType t) {
    try {
      String filename = getChartTypeFile(t);
      if(filename.endsWith(".xlsx")) {
          return new XlsxDataSource(new FileInputStream(getChartTypeFile(t)));
      } else {
          return new XlsDataSource(new FileInputStream(getChartTypeFile(t)));
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public static Region getDefaultTestingRegion(ChartType t) {
    switch (t) {
    case TSA:
      return Region.BURDEKIN;
      //$CASES-OMITTED$
    case GRAINS_PS:
      return Region.FITZROY;
    default:
      return Region.GBR;
    }
  }

  public static String getChartTypeFile(ChartType t) {
    switch (t) {
    case COTS_OUTBREAK:
      return "test/cots_outbreak.xlsx";
    case ANNUAL_RAINFALL:
      return "test/annual_rainfall.xlsx";
    case GRAZING_PS:
      return "test/grazing_practice_systems.xlsx";
    case HORTICULTURE_PS:
    case SUGARCANE_PS:
    case GRAINS_PS:
      return "test/land_practice_systems.xlsx";
    case MARINE:
    case MARINE_CT:
    case MARINE_ST:
    case MARINE_WQT:
      return "test/marine.xls";
    case PROGRESS_TABLE_REGION:
    case PROGRESS_TABLE:
      return "test/progress_table.xlsx";
    case TSA:
      return "test/seagrass_cover.xls";
    case TTT_CANE_AND_HORT:
    case TTT_GRAZING:
    case TTT_NITRO_AND_PEST:
    case TTT_SEDIMENT:
      return "test/tracking_towards_targets.xlsx";
    case GROUNDCOVER:
      return "test/groundcover.xlsx";
    case GROUNDCOVER_BELOW_50:
      return "test/groundcover_below_50.xlsx";
    case LOADS:
    case LOADS_DIN:
    case LOADS_PSII:
    case LOADS_TN:
    case LOADS_TSS:
      return "test/loads.xlsx";
    case CORAL_HCC:
    case CORAL_SCC:
    case CORAL_MA:
    case CORAL_JUV:
      return "test/coral.xls";
    case PSII_MAX_HEQ:
      return "test/PSII.xlsx";
    case PSII_TRENDS:
      return "test/Max conc.xlsx";
    case RIPARIAN_FOREST_LOSS_TOTAL:
    case RIPARIAN_FOREST_LOSS:
      return "test/riparian_2010.xlsx";
    case WETLANDS_LOSS:
    case WETLANDS_REMAINING:
      return "test/wetlands_2010.xls";
    default:
      throw new RuntimeException("Unknown chart type: "+t);
    }
  }

}
