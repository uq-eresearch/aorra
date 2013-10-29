package charts.builder;

import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;
import static charts.builder.ChartBuilderTest.getChartTypeFile;
import static charts.builder.ChartBuilderTest.getDatasource;
import static charts.builder.ChartBuilderTest.getDefaultTestingRegion;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.imageio.ImageIO;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import charts.Chart.UnsupportedFormatException;
import charts.ChartType;
import charts.Region;
import charts.builder.spreadsheet.TrendsSeagrassAbundanceBuilder;
import charts.builder.spreadsheet.XlsDataSource;
import charts.builder.spreadsheet.XlsxDataSource;
import charts.representations.Format;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

@RunWith(Parameterized.class)
public class ChartBuilderSizeTest {

  private final static ChartBuilder chartBuilder = new ChartBuilder();
  private final ChartType chartType;

  public ChartBuilderSizeTest(ChartType ct) {
    this.chartType = ct;
  }

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    final List<Object[]> l = Lists.newLinkedList();
    for (final ChartType ct : ChartType.values()) {
      l.add(new Object[] { ct });
    }
    return l;
  }

  @Test
  public void svgAndPngChartSize() throws UnsupportedFormatException{
    final List<Region> regions;
    final Map<String, String> parameters;
    final List<DataSource> ds = asList(getDatasource(chartType));
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
        final charts.Chart chart = chartBuilder.getCharts(ds, chartType,
            regions, new Dimension(0, 0), parameters).get(0);
        Element svg = getSvgRoot(chart);
        String[] viewBox = svg.attr("viewBox").split(" ");
        assertThat(svg.attr("width")).isEqualTo(viewBox[2]);
        assertThat(svg.attr("height")).isEqualTo(viewBox[3]);
        checkDimensionsMatch(chartType, svg, getPngImage(chart));
      } catch(IndexOutOfBoundsException e) {
        throw new RuntimeException(String.format(
          "caught exception while testing chart type %s", chartType), e);
      }
    }
    {
      final charts.Chart chart = chartBuilder.getCharts(ds, chartType, regions,
          new Dimension(0, 127), parameters).get(0);
      Element svg = getSvgRoot(chart);
      assertThat(svg.attr("width"))
        .as(chartType+" unspecified width")
        .isNotEqualTo("0");
      assertThat(svg.attr("height")).isEqualTo("127");
      checkDimensionsMatch(chartType, svg, getPngImage(chart));
    }
    {
      final charts.Chart chart = chartBuilder.getCharts(ds, chartType, regions,
          new Dimension(383, 0), parameters).get(0);
      Element svg = getSvgRoot(chart);
      assertThat(svg.attr("width")).isEqualTo("383");
      assertThat(svg.attr("height"))
        .as(chartType+" unspecified height")
        .isNotEqualTo("0");
      checkDimensionsMatch(chartType, svg, getPngImage(chart));
    }
    {
      final charts.Chart chart = chartBuilder.getCharts(ds, chartType, regions,
          new Dimension(383, 127), parameters).get(0);
      Element svg = getSvgRoot(chart);
      assertThat(svg.attr("width")).isEqualTo("383");
      assertThat(svg.attr("height")).isEqualTo("127");
      checkDimensionsMatch(chartType, svg, getPngImage(chart));
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

}
