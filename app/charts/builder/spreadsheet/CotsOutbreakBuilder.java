package charts.builder.spreadsheet;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.time.TimeSeries;
import org.jfree.data.time.TimeSeriesDataItem;
import org.jfree.data.time.Year;
import org.supercsv.io.CsvListWriter;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.CotsOutbreak;
import charts.jfree.ATSCollection;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class CotsOutbreakBuilder extends JFreeBuilder {

  private static final String TITLE = "Crown-of-thorns starfish outbreaks";

  public CotsOutbreakBuilder() {
    super(ChartType.COTS_OUTBREAK);
  }

  private boolean isCotsOutbreakSpreadsheet(SpreadsheetDataSource datasource) {
    try {
      return "TOTALOUTBREAKS".equalsIgnoreCase(datasource.select("B1")
          .getValue());
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  protected ATSCollection createDataset(Context ctx) {
    if(ctx.region() == Region.GBR) {
      TimeSeries s1 = new TimeSeries("outbreaks");
      try {
        for (int i = 2; true; i++) {
          String year = ctx.datasource().select("A" + i).getValue();
          String outbreaks = ctx.datasource().select("B" + i).getValue();
          if (StringUtils.isBlank(year) || StringUtils.isBlank(outbreaks))
            break;
          double val = Double.parseDouble(outbreaks);
          s1.add(new Year(parseYear(year)), val);
        }
      } catch (Exception e) {
      }
      ATSCollection dataset = new ATSCollection();
      dataset.addSeries(s1);
      return dataset;
    } else {
      return null;
    }
  }

  private int parseYear(String y) {
    y = StringUtils.strip(y);
    if (y.contains(".")) {
      y = StringUtils.substringBefore(y, ".");
    }
    return Integer.parseInt(y);
  }

  @Override
  public boolean canHandle(SpreadsheetDataSource datasource) {
    return isCotsOutbreakSpreadsheet(datasource);
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE, TITLE).
        put(Attribute.X_AXIS_LABEL, "Year").
        put(Attribute.Y_AXIS_LABEL, "Outbreaks").
        put(Attribute.SERIES_COLOR, Color.blue).
        build();
  }

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    return new CotsOutbreak().createChart(
        (ATSCollection)ctx.dataset(), new Dimension(750, 500));
  }

  @SuppressWarnings("unchecked")
  @Override
  protected String getCsv(final JFreeContext ctx) {
    return Csv.write(new CsvWriter() {
      @Override
      public void write(CsvListWriter csv) throws IOException {
        final DateFormat yearOnly = new SimpleDateFormat("YYYY");
        csv.write("Year", "Outbreaks");
        final List<TimeSeriesDataItem> items = ((ATSCollection)ctx.dataset()).getSeries(0)
            .getItems();
        for (TimeSeriesDataItem i : items) {
          csv.write(yearOnly.format(i.getPeriod().getStart()), i.getValue()
              .intValue() + "");
        }
      }});
  }

}
