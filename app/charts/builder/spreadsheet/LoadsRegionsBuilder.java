package charts.builder.spreadsheet;

import static charts.ChartType.LOADS_DIN;
import static charts.ChartType.LOADS_PSII;
import static charts.ChartType.LOADS_TN;
import static charts.ChartType.LOADS_TSS;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.Dataset;
import org.supercsv.io.CsvListWriter;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.Loads;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class LoadsRegionsBuilder extends LoadsBuilder {

  public LoadsRegionsBuilder() {
    super(Lists.newArrayList(LOADS_DIN, LOADS_TN, LOADS_PSII, LOADS_TSS));
  }

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    final String period = ctx.parameters().get(PERIOD);
    return Loads.createChart(getTitle(ctx.datasource(), INDICATORS.get(ctx.type()), period),
        "Region",
        (CategoryDataset)ctx.dataset(),
        new Dimension(750, 500));
  }

  @Override
  protected String getCsv(final JFreeContext ctx) {
    final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
    return Csv.write(new CsvWriter() {
      @Override
      public void write(CsvListWriter csv) throws IOException {
        @SuppressWarnings("unchecked")
        List<String> columnKeys = dataset.getColumnKeys();
        @SuppressWarnings("unchecked")
        List<String> rowKeys = dataset.getRowKeys();
        final List<String> heading = ImmutableList.<String>builder()
            .add(String.format("%s %s %s load reductions",
                ctx.region(), INDICATORS.get(ctx.type()), ctx.parameters().get(PERIOD)))
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
  protected Dataset createDataset(Context ctx) {
    if(ctx.region() != Region.GBR) {
      return null;
    }
    ChartType type = ctx.type();
    final String period = ctx.parameters().get(PERIOD);
    if(StringUtils.isBlank(period)) {
        return null;
    }
    final Indicator indicator = INDICATORS.get(type);
    if(indicator == null) {
        throw new RuntimeException(String.format("chart type %s not implemented", type.name()));
    }
    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
    for(Region region : Region.values()) {
        Integer row = ROWS.get(region);
        if(row == null) {
            throw new RuntimeException(String.format("region %s not configured", region.getName()));
        }
        try {
            double val = selectAsDouble(ctx.datasource(), region, indicator, period);
            dataset.addValue(val, indicator.getLabel(), region.getProperName());
        } catch(Exception e) {
            throw new RuntimeException("region "+region.getName(), e);
        }
    }
    return dataset;
  }

}
