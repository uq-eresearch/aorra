package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.MarineBarChart;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableList;

public abstract class MarineBarChartBuilder extends AbstractBuilder {

  public MarineBarChartBuilder(ChartType type) {
    super(type);
  }

  abstract Map<Attribute, Object> defaults();

  @Override
  protected boolean canHandle(SpreadsheetDataSource datasource) {
    try {
      return StringUtils.startsWithIgnoreCase(datasource.select("A1").asString(),
          (String)defaults().get(Attribute.TITLE));
    } catch (MissingDataException e) {
      return false;
    }
  }

  @Override
  protected Chart build(SpreadsheetDataSource datasource, final ChartType type,
      final Region region) {
    if(region == Region.GBR && supports(type)) {
      final ADCDataset dataset = createDataset(datasource);
      configurator(datasource, defaults(), type, region).configure(dataset);
      final Drawable d = new MarineBarChart().createChart(dataset);
      return new AbstractChart() {
        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(type, region);
        }

        @Override
        public Drawable getChart() {
          return d;
        }

        @Override
        public String getCSV() {
          final StringWriter sw = new StringWriter();
          try {
            final CsvListWriter csv = new CsvListWriter(sw,
                CsvPreference.STANDARD_PREFERENCE);
            @SuppressWarnings("unchecked")
            List<String> columnKeys = dataset.getColumnKeys();
            @SuppressWarnings("unchecked")
            List<String> rowKeys = dataset.getRowKeys();
            final List<String> heading = ImmutableList.<String>builder()
                .add(format("%s %s", region.getProperName(), type.getLabel()))
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
      };
    } else {
      return null;
    }
  }

  private ADCDataset createDataset(SpreadsheetDataSource ds) {
    ADCDataset dataset = new ADCDataset();
    int column = 1;
    for(String series : new String[] {"Inshore", "Midshelf", "Offshore"}) {
      try {
        for(int row=2;true;row++) {
          Region r = Region.lookup(ds.select(row,0).asString());
          if(r == null) {
            break;
          }
          Double val = ds.select(row, column).asDouble();
          dataset.addValue(val, series, r.getProperName());
        }
        column++;
      } catch(MissingDataException e) {
        e.printStackTrace();
      }
    }
    return dataset;
  }
}
