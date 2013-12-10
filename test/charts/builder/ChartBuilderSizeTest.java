package charts.builder;

import static charts.builder.ChartBuilderTest.getDatasource;
import static charts.builder.ChartBuilderTest.getDefaultTestingRegion;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Test;

import charts.Chart.UnsupportedFormatException;
import charts.ChartType;
import charts.Region;
import charts.builder.spreadsheet.TrendsSeagrassAbundanceBuilder;
import charts.representations.Format;

import com.google.common.collect.ImmutableMap;

public class ChartBuilderSizeTest {

  private final static ChartBuilder chartBuilder = new ChartBuilder();

  private final static DataSourceFactory dsf = new DataSourceFactory() {
    @Override
    public DataSource getDataSource(String id) throws Exception {
      return getDatasource(ChartType.valueOf(id));
    }};

  @Test
  public void svgAndPngChartSize() throws Exception {
    for (final ChartType ct : ChartType.values()) {
      try {
        svgAndPngChartSize(ct);
      } catch (UnsupportedFormatException e) {
        fail(e.getMessage());
      }
    }
  }

  public void svgAndPngChartSize(ChartType chartType) throws Exception {
    final List<Region> regions;
    final Map<String, String> parameters;
    switch (chartType) {
    case TSA:
      regions = asList(Region.CAPE_YORK);
      parameters = ImmutableMap.of(TrendsSeagrassAbundanceBuilder.SUBREGION,
        TrendsSeagrassAbundanceBuilder.Subregion.AP.name());
      break;
      //$CASES-OMITTED$
    default:
      regions = asList(getDefaultTestingRegion(chartType));
      parameters = Collections.<String, String>emptyMap();
    }
    {
      try {
        final charts.Chart chart = chartBuilder.getCharts(chartType.name(), dsf, chartType, 
            regions, parameters).get(0);
        Element svg = getSvgRoot(chart, new Dimension());
        String[] viewBox = svg.attr("viewBox").split(" ");
        assertThat(svg.attr("width")).isEqualTo(viewBox[2]);
        assertThat(svg.attr("height")).isEqualTo(viewBox[3]);
        checkDimensionsMatch(chartType, svg, getPngImage(chart, new Dimension()));
      } catch(IndexOutOfBoundsException e) {
        throw new RuntimeException(String.format(
          "caught exception while testing chart type %s", chartType), e);
      }
    }
    {
      final charts.Chart chart = chartBuilder.getCharts(chartType.name(), dsf, chartType, regions,
          parameters).get(0);
      Element svg = getSvgRoot(chart, new Dimension(0, 127));
      assertThat(svg.attr("width"))
        .as(chartType+" unspecified width")
        .isNotEqualTo("0");
      assertThat(svg.attr("height")).isEqualTo("127");
      checkDimensionsMatch(chartType, svg, getPngImage(chart, new Dimension(0, 127)));
    }
    {
      final charts.Chart chart = chartBuilder.getCharts(chartType.name(), dsf, chartType, regions,
          parameters).get(0);
      Element svg = getSvgRoot(chart, new Dimension(383, 0));
      assertThat(svg.attr("width")).isEqualTo("383");
      assertThat(svg.attr("height"))
        .as(chartType+" unspecified height")
        .isNotEqualTo("0");
      checkDimensionsMatch(chartType, svg, getPngImage(chart, new Dimension(383, 0)));
    }
    {
      final charts.Chart chart = chartBuilder.getCharts(chartType.name(), dsf, chartType, regions,
          parameters).get(0);
      Element svg = getSvgRoot(chart, new Dimension(383, 127));
      assertThat(svg.attr("width")).isEqualTo("383");
      assertThat(svg.attr("height")).isEqualTo("127");
      checkDimensionsMatch(chartType, svg, getPngImage(chart, new Dimension(383, 127)));
    }
  }

  private void checkDimensionsMatch(
      ChartType ct, Element svg, BufferedImage png) {
    String svgDimensions = svg.attr("width") + " x " + svg.attr("height");
    String pngDimensions = png.getWidth() + " x " + png.getHeight();
    assertThat(pngDimensions).as(ct+" PNG dimensions match SVG")
      .isEqualTo(svgDimensions);
  }

  private Element getSvgRoot(charts.Chart chart, Dimension dimension)
      throws UnsupportedFormatException {
    return Jsoup.parse(new String(chart.outputAs(Format.SVG, dimension).getContent()))
        .select("svg").get(0);
  }

  private BufferedImage getPngImage(charts.Chart chart, Dimension dimension)
      throws UnsupportedFormatException {
    try {
      return ImageIO.read(new ByteArrayInputStream(chart.outputAs(Format.PNG, dimension)
          .getContent()));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
