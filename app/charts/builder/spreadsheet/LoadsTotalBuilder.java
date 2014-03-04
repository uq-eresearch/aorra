package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.Loads;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class LoadsTotalBuilder extends LoadsBuilder {

  public LoadsTotalBuilder() {
    super(ChartType.LOADS);
  }

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    final String period = ctx.parameters().get(PERIOD); 
    return Loads.createChart(getTitle(ctx.datasource(), ctx.region(), period),
        "Pollutants", (CategoryDataset)ctx.dataset(),
        new Dimension(750, 500));
  }

  @Override
  protected String getCsv(JFreeContext ctx) {
    final Region region = ctx.region();
    final String period = ctx.parameters().get(PERIOD);
    final StringWriter sw = new StringWriter();
    try {
      final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
      final CsvListWriter csv = new CsvListWriter(sw,
          CsvPreference.STANDARD_PREFERENCE);
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
      csv.close();
    } catch (IOException e) {
      // How on earth would you get an IOException with a StringWriter?
      throw new RuntimeException(e);
    }
    return sw.toString();
  }

  @Override
  public CategoryDataset createDataset(Context ctx) {
    final String period = ctx.parameters().get(PERIOD); 
    if(StringUtils.isBlank(period)) {
        return null;
    }
    SpreadsheetDataSource ds = ctx.datasource();
    Region region = ctx.region();
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
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

}