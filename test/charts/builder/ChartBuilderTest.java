package charts.builder;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;

import charts.Chart.UnsupportedFormatException;
import charts.ChartType;
import charts.Region;
import charts.builder.spreadsheet.XlsDataSource;
import charts.builder.spreadsheet.XlsxDataSource;
import charts.representations.Format;

public class ChartBuilderTest {

  private final ChartBuilder chartBuilder;

  public ChartBuilderTest() {
    this.chartBuilder = new ChartBuilder();
  }

  @Test
  public void docxAndEmf() throws UnsupportedFormatException {
    for (final ChartType ct : ChartType.values()) {
      final List<DataSource> ds = asList(getDatasource(ct));
      final List<Region> regions = asList(Region.GBR);
      {
        final charts.Chart chart = chartBuilder.getCharts(ds, regions,
            new Dimension(0, 0)).get(0);
        // Shouldn't trigger UnsupportedFormatException
        chart.outputAs(Format.DOCX);
        // Shouldn't trigger UnsupportedFormatException
        chart.outputAs(Format.EMF);
      }
    }
  }

  @Test
  public void svgAndPngChartSize() throws UnsupportedFormatException{
    final List<Region> regions = asList(Region.GBR);
    for (final ChartType ct : ChartType.values()) {
      final List<DataSource> ds = asList(getDatasource(ct));
      {
        final charts.Chart chart = chartBuilder.getCharts(ds, regions,
            new Dimension(0, 0)).get(0);
        Element svg = getSvgRoot(chart);
        String[] viewBox = svg.attr("viewBox").split(" ");
        assertThat(svg.attr("width")).isEqualTo(viewBox[2]);
        assertThat(svg.attr("height")).isEqualTo(viewBox[3]);
        checkDimensionsMatch(ct, svg, getPngImage(chart));
      }
      {
        final charts.Chart chart = chartBuilder.getCharts(ds, regions,
            new Dimension(0, 127)).get(0);
        Element svg = getSvgRoot(chart);
        assertThat(svg.attr("width"))
          .as(ct+" unspecified width")
          .isNotEqualTo("0");
        assertThat(svg.attr("height")).isEqualTo("127");
        checkDimensionsMatch(ct, svg, getPngImage(chart));
      }
      {
        final charts.Chart chart = chartBuilder.getCharts(ds, regions,
            new Dimension(383, 0)).get(0);
        Element svg = getSvgRoot(chart);
        assertThat(svg.attr("width")).isEqualTo("383");
        assertThat(svg.attr("height"))
          .as(ct+" unspecified height")
          .isNotEqualTo("0");
        checkDimensionsMatch(ct, svg, getPngImage(chart));
      }
      {
        final charts.Chart chart = chartBuilder.getCharts(ds, regions,
            new Dimension(383, 127)).get(0);
        Element svg = getSvgRoot(chart);
        assertThat(svg.attr("width")).isEqualTo("383");
        assertThat(svg.attr("height")).isEqualTo("127");
        checkDimensionsMatch(ct, svg, getPngImage(chart));
      }
    }
  }

  private void checkDimensionsMatch(
      ChartType ct, Element svg, BufferedImage png) {
    String svgDimensions = svg.attr("width") + " x " + svg.attr("height");
    String pngDimensions = png.getWidth() + " x " + png.getHeight();
    assertThat(pngDimensions).as(ct+" PNG dimensions match SVG")
      .isEqualTo(svgDimensions);
  }

  private Element getSvgRoot(charts.Chart chart)
      throws UnsupportedFormatException {
    return Jsoup.parse(new String(chart.outputAs(Format.SVG).getContent()))
        .select("svg").get(0);
  }

  private BufferedImage getPngImage(charts.Chart chart)
      throws UnsupportedFormatException {
    try {
      return ImageIO.read(new ByteArrayInputStream(chart.outputAs(Format.PNG)
          .getContent()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private DataSource getDatasource(ChartType t) {
    try {
        String filename = getChartTypeFile(t);
        if(filename.endsWith("xlsx")) {
            return new XlsxDataSource(new FileInputStream(getChartTypeFile(t)));
        } else {
            return new XlsDataSource(new FileInputStream(getChartTypeFile(t)));
        }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private String getChartTypeFile(ChartType t) {
    switch (t) {
    case COTS_OUTBREAK:
      return "test/cots_outbreak.xlsx";
    case ANNUAL_RAINFALL:
      return "test/annual_rainfall.xlsx";
    case GRAZING_PS:
      return "test/grazing_practice_systems.xlsx";
    case GRAINS_PS:
    case HORTICULTURE_PS:
    case SUGARCANE_PS:
      return "test/land_practice_systems.xlsx";
    case MARINE:
      return "test/marine.xlsx";
    case PROGRESS_TABLE_REGION:
    case PROGRESS_TABLE:
      return "test/progress_table.xlsx";
    case TSA:
      return "test/trends_in_seagrass_abundance.xlsx";
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
    default:
      throw new RuntimeException("Unknown chart type.");
    }
  }

}
