package charts.graphics;

import static views.html.chart.progresstable.render;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.io.InputStream;
import java.io.StringReader;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.xml.sax.InputSource;

import boxrenderer.Box;
import boxrenderer.Resolver;
import boxrenderer.XmlUtils;
import boxrenderer.xhtml.Parser;
import charts.Chart;
import charts.Drawable;

import com.google.common.collect.ImmutableList;

public class ProgressTable implements Drawable {

  public static enum Indicator {
    GRAZING, SUGARCANE, GRAIN, HORTICULTURE, GROUNDCOVER,
    NITROGEN, SEDIMENT, PESTICIDES, DAIRY, PHOSPHORUS;

    public String getLabel() {
      return StringUtils.capitalize(StringUtils.lowerCase(name()));
    }
  }

  public static enum Condition {
    VERYPOOR ("Very Poor"),
    POOR ("Poor"),
    MODERATE ("Moderate"),
    GOOD ("Good"),
    VERYGOOD ("Very Good");

    private final String label;

    private Condition(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }
  }

  public static class Cell {
    public final Indicator indicator;
    public final Condition condition;
    public final String progress;

    public Cell() {
      this.indicator = null;
      this.condition = null;
      this.progress = null;
    }

    public Cell(Indicator indicator, Condition condition, String progress) {
      this.indicator = indicator;
      this.condition = condition;
      this.progress = progress;
    }
  }

  public static class Column {
    public final String header;
    public final String description;
    public final String target;

    public Column(String header, String description, String target) {
      this.header = header;
      this.description = description;
      this.target = target;
    }
  }

  public static class Row {
    public final String header;
    public final String description;
    public final List<Cell> cells;

    public Row(String header, String description, List<Cell> cells) {
      this.header = header;
      this.description = description;
      this.cells = ImmutableList.copyOf(cells);
    }
  }

  public static class Dataset {
    public final List<Column> columns;
    public final List<Row> rows;

    public Dataset(List<Column> columns, List<Row> rows) {
      this.columns = ImmutableList.copyOf(columns);
      this.rows = ImmutableList.copyOf(rows);
    }
  }

  private final Box box;

  public ProgressTable(Dataset dataset) {
    try {
      final Resolver resolver = new Resolver() {
        @Override
        public InputStream resolve(String source) throws Exception {
          if (StringUtils.startsWith(source, "url('")) {
            source = StringUtils.substringBetween(source, "('", "')");
          }
          final InputStream stream =
              Chart.class.getResourceAsStream(source);
          if (stream == null) {
            throw new Exception("failed to load resource " + source);
          }
          return stream;
        }
      };
      box = new Parser(resolver).parse(XmlUtils.parse(new InputSource(
          new StringReader(render(dataset.columns, dataset.rows).toString()))));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public Dimension getDimension(Graphics2D graphics) {
    try {
      return box.getDimension(graphics);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void draw(Graphics2D g2) {
    try {
      Dimension d = box.getDimension(g2);
      g2.setClip(0, 0, d.width, d.height);
      box.render(g2);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
