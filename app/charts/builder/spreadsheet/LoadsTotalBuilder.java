package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.Loads;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class LoadsTotalBuilder extends LoadsBuilder {

  public LoadsTotalBuilder() {
    super(ChartType.LOADS);
  }

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    return Loads.createChart((ADCDataset)ctx.dataset(), ctx.type(), new Dimension(800, 500));
  }

  @Override
  protected String getCsv(JFreeContext ctx) {
    final Region region = ctx.region();
    final String period = ctx.parameters().get(PERIOD);
    final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
    return Csv.write(new CsvWriter() {
      @Override
      public void write(CsvListWriter csv) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> columnKeys = dataset.getColumnKeys();
        @SuppressWarnings("unchecked")
        List<String> rowKeys = dataset.getRowKeys();
        final List<String> heading = ImmutableList.<String>builder()
            .add(String.format("%s %s load reductions", region, period))
            .addAll(rowKeys)
            .build();
        csv.write(heading);
        for (String col : columnKeys) {
          List<String> line = Lists.newLinkedList();
          line.add(col);
          for (String row : rowKeys) {
            line.add(formatNumber("%.1f",
                dataset.getValue(row, col)));
          }
          csv.write(line);
        }
      }});
  }

  @Override
  public ADCDataset createDataset(Context ctx) {
    final String period = ctx.parameters().get(PERIOD); 
    if(StringUtils.isBlank(period)) {
      return null;
    }
    SpreadsheetDataSource ds = ctx.datasource();
    Region region = ctx.region();
    ADCDataset dataset = new ADCDataset();
    Integer row = ROWS.get(region);
    if(row == null) {
      throw new RuntimeException("unknown region "+region);
    }
    row--;
    try {
      for(Indicator indicator : Indicator.values()) {
        if(indicator.include()) {
          dataset.addValue(selectAsDouble(ds, region, indicator, period),
              region.getProperName() , indicator.getLabel());
        }
      }
      return dataset;
    } catch(MissingDataException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        putAll(super.defaults(type)).
        put(Attribute.TITLE, "${region} cumulative load reductions to ${lastPeriodyyyy}").
        put(Attribute.X_AXIS_LABEL, "Pollutants").
        build();
  }

}
