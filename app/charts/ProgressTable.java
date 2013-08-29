package charts;

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

public class ProgressTable implements Drawable {

  public static enum Indicator {
    GRAZING, SUGARCANE, GRAIN, HORTICULTURE,
    GROUNDCOVER, NITROGEN, SEDIMENT, PESTICIDES;
  }

  public static enum Condition {
    VERYPOOR ("Very Poor"),
    POOR ("Poor"),
    MODERATE ("Moderate"),
    GOOD ("Good"),
    VERYGOOD ("Very Good");

    private String label;

    private Condition(final String label) {
      this.label = label;
    }

    @Override
    public String toString() {
      return label;
    }

  }

  public static class Cell {
    public Indicator indicator;
    public Condition condition;
    public String progress;

    public Cell() {
    }

    public Cell(Indicator indicator, Condition condition, String progress) {
      super();
      this.indicator = indicator;
      this.condition = condition;
      this.progress = progress;
    }
  }

  public static class Column {
    public String header;
    public String description;
    public String target;

    public Column(String header, String description, String target) {
      super();
      this.header = header;
      this.description = description;
      this.target = target;
    }
  }

  public static class Row {
    public String header;
    public String description;
    public List<Cell> cells;

    public Row(String header, String description, List<Cell> cells) {
      super();
      this.header = header;
      this.description = description;
      this.cells = cells;
    }
  }

  public static class Dataset {
    public List<Column> columns;
    public List<Row> rows;

    public Dataset(List<Column> columns, List<Row> rows) {
      super();
      this.columns = columns;
      this.rows = rows;
    }
  }

  private Box box;

  public ProgressTable(Dataset dataset) {
    try {
      Resolver resolver = new Resolver() {
        @Override
        public InputStream resolve(String source) throws Exception {
          if (StringUtils.startsWith(source, "url('")) {
            source = StringUtils.substringBetween(source, "('", "')");
          }
          InputStream stream = ProgressTable.class.getResourceAsStream(source);
          if (stream == null) {
            throw new Exception("failed to load resource " + source);
          }
          return stream;
        }
      };
      Parser parser = new Parser(resolver);
      box = parser.parse(XmlUtils.parse(new InputSource(new StringReader(
          views.html.chart.progresstable.render(dataset.columns, dataset.rows)
              .toString()))));
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
