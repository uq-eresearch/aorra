package charts.builder;

import static charts.builder.ChartBuilderTest.getDatasource;
import static charts.builder.ChartBuilderTest.getDefaultTestingRegion;
import static com.google.common.collect.Lists.newArrayList;
import static java.util.Arrays.asList;
import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.fail;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
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

  private final static ChartBuilder chartBuilder = new DefaultChartBuilder(
      new DataSourceFactory() {
        @Override
        public DataSource getDataSource(String id) throws Exception {
          return getDatasource(ChartType.valueOf(id));
        }
      });

  @Test
  public void svgAndPngChartSize() throws Exception {
    final List<ChartType> chartTypes = newArrayList(ChartType.values());
    Collections.sort(chartTypes, new Comparator<ChartType>() {
      @Override
      public int compare(ChartType o1, ChartType o2) {
        return o1.name().compareTo(o2.name());
      }
    });
    for (final ChartType ct : chartTypes) {
      try {
        svgAndPngChartSize(ct);
      } catch (UnsupportedFormatException e) {
        fail(e.getMessage());
      }
    }
  }

  private void memstat(String msg) {
    Runtime r = Runtime.getRuntime();
    System.out.println(String.format("XXX %s, free %s, total %s, max %s",msg, hr(r.freeMemory()),
        hr(r.totalMemory()), hr(r.maxMemory())));
  }

  private String hr(long bytes) {
    return humanReadableByteCount(bytes, false);
  }

  // from http://stackoverflow.com/a/3758880
  private String humanReadableByteCount(long bytes, boolean si) {
    int unit = si ? 1000 : 1024;
    if (bytes < unit) return bytes + " B";
    int exp = (int) (Math.log(bytes) / Math.log(unit));
    String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp-1) + (si ? "" : "i");
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
  }

  public void svgAndPngChartSize(ChartType chartType) throws Exception {
    memstat("testing "+chartType.name());
    long start = System.currentTimeMillis();
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
    final charts.Chart chart = chartBuilder.getCharts(chartType.name(), chartType,
        regions, parameters).get(0);
    {
      try {
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
      Element svg = getSvgRoot(chart, new Dimension(0, 127));
      assertThat(svg.attr("width"))
        .as(chartType+" unspecified width")
        .isNotEqualTo("0");
      assertThat(svg.attr("height")).isEqualTo("127");
      checkDimensionsMatch(chartType, svg, getPngImage(chart, new Dimension(0, 127)));
    }
    {
      Element svg = getSvgRoot(chart, new Dimension(383, 0));
      assertThat(svg.attr("width")).isEqualTo("383");
      assertThat(svg.attr("height"))
        .as(chartType+" unspecified height")
        .isNotEqualTo("0");
      checkDimensionsMatch(chartType, svg, getPngImage(chart, new Dimension(383, 0)));
    }
    {
      Element svg = getSvgRoot(chart, new Dimension(383, 127));
      assertThat(svg.attr("width")).isEqualTo("383");
      assertThat(svg.attr("height")).isEqualTo("127");
      checkDimensionsMatch(chartType, svg, getPngImage(chart, new Dimension(383, 127)));
    }
    memstat(String.format("%s test took %s ms ",
        chartType.name(), System.currentTimeMillis()-start));
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
