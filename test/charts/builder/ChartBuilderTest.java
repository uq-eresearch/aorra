package charts.builder;

import static org.fest.assertions.Assertions.assertThat;

import java.awt.Dimension;
import java.io.FileInputStream;
import java.util.List;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.Test;

import charts.Chart.UnsupportedFormatException;
import charts.Region;
import charts.builder.spreadsheet.XlsxDataSource;
import charts.representations.Format;

import com.google.common.collect.ImmutableList;

public class ChartBuilderTest {

  public final ChartBuilder chartBuilder;

  public ChartBuilderTest() {
    this.chartBuilder = new ChartBuilder();
  }

  @Test
  public void svgChartSize() throws UnsupportedFormatException{
    final List<DataSource> ds = ImmutableList.of(getMarineChartDatasource());
    final List<Region> regions = ImmutableList.of(Region.GBR);
    {
      final charts.Chart chart = chartBuilder.getCharts(ds, regions,
          new Dimension(9832, 1337)).get(0);
      Element svg = getSvgRoot(chart);
      assertThat(svg.attr("width")).isEqualTo("9832");
      assertThat(svg.attr("height")).isEqualTo("1337");
    }
    {
      final charts.Chart chart = chartBuilder.getCharts(ds, regions,
          new Dimension(0, 1337)).get(0);
      Element svg = getSvgRoot(chart);
      assertThat(svg.attr("width")).isNotEqualTo("0");
      assertThat(svg.attr("height")).isEqualTo("1337");
    }
    {
      final charts.Chart chart = chartBuilder.getCharts(ds, regions,
          new Dimension(9832, 0)).get(0);
      Element svg = getSvgRoot(chart);
      assertThat(svg.attr("width")).isEqualTo("9832");
      assertThat(svg.attr("height")).isNotEqualTo("0");
    }
  }

  private Element getSvgRoot(charts.Chart chart)
      throws UnsupportedFormatException {
    return Jsoup.parse(new String(chart.outputAs(Format.SVG).getContent()))
        .select("svg").get(0);
  }

  private DataSource getMarineChartDatasource() {
    try {
      return new XlsxDataSource(new FileInputStream("test/marine.xlsx"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
