package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.MarineBarChart;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableList;

public abstract class MarineBarChartBuilder extends JFreeBuilder {

  public MarineBarChartBuilder(ChartType type) {
    super(type);
  }

  @Override
  protected boolean canHandle(SpreadsheetDataSource datasource) {
    try {
      return StringUtils.startsWithIgnoreCase(datasource.select("A1").asString(),
          defaults(type()).get(Attribute.TITLE));
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  protected ADCDataset createDataset(Context ctx) {
    if(ctx.region() == Region.GBR) {
      ADCDataset dataset = new ADCDataset();
      int column = 1;
      for(String series : new String[] {"Inshore", "Midshelf", "Offshore"}) {
        try {
          for(int row=2;true;row++) {
            Region r = Region.lookup(ctx.datasource().select(row,0).asString());
            if(r == null) {
              break;
            }
            Double val = ctx.datasource().select(row, column).asDouble();
            dataset.addValue(val, series, r.getProperName());
          }
          column++;
        } catch(MissingDataException e) {
          e.printStackTrace();
        }
      }
      return dataset;
    } else {
      return null;
    }
  }

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    return new MarineBarChart().createChart((ADCDataset)ctx.dataset());
  }

  @Override
  protected String getCsv(JFreeContext ctx) {
    ADCDataset dataset = (ADCDataset)ctx.dataset();
    final StringWriter sw = new StringWriter();
    try {
      final CsvListWriter csv = new CsvListWriter(sw,
          CsvPreference.STANDARD_PREFERENCE);
      @SuppressWarnings("unchecked")
      List<String> columnKeys = dataset.getColumnKeys();
      @SuppressWarnings("unchecked")
      List<String> rowKeys = dataset.getRowKeys();
      final List<String> heading = ImmutableList.<String>builder()
          .add(format("%s %s", ctx.region().getProperName(), ctx.type().getLabel()))
          .addAll(columnKeys)
          .build();
      csv.write(heading);
      for (String row : rowKeys) {
        List<String> line = newLinkedList();
        line.add(row);
        for (String col : columnKeys) {
          final Number n = dataset.getValue(row, col);
          line.add(n == null ? "" : format("%.1f", n.doubleValue()));
        }
        csv.write(line);
      }
      csv.close();
    } catch (IOException e) {
      // How on earth would you get an IOException with a StringWriter?
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

}
