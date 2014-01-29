package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.jfree.data.category.CategoryDataset;
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
import com.google.common.collect.ImmutableMap;

public class MarineBarChartBuilder extends AbstractBuilder {

  private static final Map<Attribute, Object> DEFAULTS_TSS = ImmutableMap.<Attribute, Object>of(
      Attribute.TITLE,"Area (%) where the annual mean value for total suspended solids" +
          " exceeded the Water Quality Guidelines",
      Attribute.RANGE_AXIS_TITLE, "Area (%)",
      Attribute.DOMAIN_AXIS_TITLE, "Region");

  private static final Map<Attribute, Object> DEFAULTS_CHLORO = ImmutableMap.<Attribute, Object>of(
      Attribute.TITLE,"Area (%) where the annual mean value for chlorophyll a" +
          " exceeded the Water Quality Guidelines",
      Attribute.RANGE_AXIS_TITLE, "Area (%)",
      Attribute.DOMAIN_AXIS_TITLE, "Region");

  private static final Map<ChartType, Map<Attribute, Object>> DEFAULTS = 
      ImmutableMap.of(ChartType.TOTAL_SUSPENDED_SEDIMENT, DEFAULTS_TSS,
          ChartType.CHLOROPHYLL_A, DEFAULTS_CHLORO);

  public MarineBarChartBuilder() {
    super(ImmutableList.of(ChartType.CHLOROPHYLL_A, ChartType.TOTAL_SUSPENDED_SEDIMENT));
  }

  @Override
  protected boolean canHandle(SpreadsheetDataSource datasource) {
    Map<Attribute, Object> cfg = new ChartConfigurator(datasource,14,0).getConfiguration(null);
    return supports((ChartType)cfg.get(Attribute.TYPE));
  }

  @Override
  protected Chart build(SpreadsheetDataSource datasource, final ChartType type,
      final Region region) {
    ChartConfigurator configurator = new ChartConfigurator(DEFAULTS.get(type), datasource, 14, 0);
    if(region == Region.GBR && supports(type) &&
        type.equals(configurator.getConfiguration(null).get(Attribute.TYPE))) {
      final ADCDataset dataset = createDataset(datasource);
      configurator.configure(dataset);
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
