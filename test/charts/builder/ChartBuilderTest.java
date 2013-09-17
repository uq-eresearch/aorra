package charts.builder;

import static org.fest.assertions.Assertions.assertThat;

import java.awt.Dimension;
import java.io.FileInputStream;

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
    final charts.Chart chart = chartBuilder.getCharts(ImmutableList.of(
        getMarineChartDatasource()), ImmutableList.of(Region.GBR),
        new Dimension(9832, 1337)).get(0);
    final Document doc = Jsoup.parse(
        new String(chart.outputAs(Format.SVG).getContent()));
    Element svg = doc.select("svg").get(0);
    assertThat(svg.attr("width")).isEqualTo("9832");
    assertThat(svg.attr("height")).isEqualTo("1337");
  }

  private DataSource getMarineChartDatasource() {
    try {
      return new XlsxDataSource(new FileInputStream("test/marine.xlsx"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
