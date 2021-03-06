package charts.builder;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.LinkedList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import charts.Chart.UnsupportedFormatException;
import charts.ChartType;
import charts.Region;
import charts.builder.spreadsheet.XlsDataSource;
import charts.builder.spreadsheet.XlsxDataSource;
import charts.representations.Format;

@RunWith(Parameterized.class)
public class ChartBuilderTest {

  private static List<ChartType> skippedChartTypes = asList(new ChartType[] {
    ChartType.MARINE_V2
  });

  private final static ChartBuilder chartBuilder = new DefaultChartBuilder(
      new DataSourceFactory() {
        @Override
        public DataSource getDataSource(String id) throws Exception {
          return getDatasource(ChartType.valueOf(id));
        }
      });

  @Parameters(name = "{0} {1}")
  public static Collection<Object[]> data() {
    List<Object[]> params = new LinkedList<Object[]>();
    for (final ChartType ct : ChartType.values()) {
      if (skippedChartTypes.contains(ct))
        continue;
      for (final Format f : new Format[]{Format.CSV, Format.SVG}) {
        params.add(new Object[] { ct, f });
      }
    }
    return params;
  }

  @Parameter
  public ChartType ct;

  @Parameter(1)
  public Format f;

  @Test
  public void format() throws Exception {
    format(ct, f);
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
    case GRAINS_PS:
      return Region.FITZROY;
    case CORAL_HCC:
    case CORAL_SCC:
    case CORAL_JUV:
    case CORAL_MA:
      return Region.WET_TROPICS;
    case GROUNDCOVER:
    case GROUNDCOVER_BELOW_50:
      return Region.WET_TROPICS;
    //$CASES-OMITTED$
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
    case HORTICULTURE_PS:
    case SUGARCANE_PS:
    case GRAINS_PS:
      return "test/management_practice_systems.xlsx";
    case MARINE:
    case MARINE_CT:
    case MARINE_ST:
    case MARINE_WQT:
      return "test/marine.xls";
    case PROGRESS_TABLE:
    case PROGRESS_TABLE_REGION:
    case PROGRESS_TABLE_TILE:
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
    case LOADS_PN:
    case LOADS_PP:
    case LOADS_TSS:
      return "test/loads.xlsx";
    case CORAL_HCC:
    case CORAL_SCC:
    case CORAL_MA:
    case CORAL_JUV:
    case CORAL_HCC_GBR:
    case CORAL_SCC_GBR:
    case CORAL_MA_GBR:
    case CORAL_JUV_GBR:
      return "test/coral.xls";
    case PSII_MAX_HEQ:
    case PSII_TRENDS:
      return "test/pesticides.xlsx";
    case RIPARIAN_FOREST_LOSS_TOTAL:
    case RIPARIAN_FOREST_LOSS:
      return "test/riparian_2010.xlsx";
    case WETLANDS_LOSS:
    case WETLANDS_REMAINING:
      return "test/wetlands.xlsx";
    case TOTAL_SUSPENDED_SEDIMENT:
      return "test/TSS.xlsx";
    case CHLOROPHYLL_A:
      return "test/Chloro.xlsx";
    default:
      throw new RuntimeException("Unknown chart type: "+t);
    }
  }

}
