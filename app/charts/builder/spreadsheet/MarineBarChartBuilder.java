package charts.builder.spreadsheet;

import java.util.Map;

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
      ADCDataset dataset = createDataset(datasource);
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
          // TODO 
          return null;
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
