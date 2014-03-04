package charts.builder.spreadsheet;

import java.util.List;
import java.util.Map;

import org.jfree.data.general.Dataset;

import play.Logger;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.jfree.AttributedDataset;

public abstract class JFreeBuilder extends AbstractBuilder {

  public JFreeBuilder(ChartType type) {
    super(type);
  }

  public JFreeBuilder(List<ChartType> types) {
    super(types);
  }

  @Override
  protected Chart build(final Context context) {
    if(context == null) {
      return null;
    } else {
      final JFreeContext ctx = (JFreeContext)context;
      if(ctx.dataset() instanceof AttributedDataset) {
        configurator(ctx).configure((AttributedDataset)ctx.dataset(), ctx.type());
      }
      final ChartDescription description = new ChartDescription(ctx.type(), ctx.region(),
          ctx.parameters(), title(ctx));
      final Drawable drawable = getDrawable(ctx);
      final String csv = fetchCsv(ctx);
      return new AbstractChart() {
        @Override
        public ChartDescription getDescription() {
          return description;
        }

        @Override
        public Drawable getChart() {
          return drawable;
        }

        @Override
        public String getCSV() throws UnsupportedFormatException {
          return csv;
        }};
    }
  }

  private String fetchCsv(JFreeContext ctx) {
    try {
      return getCsv(ctx);
    } catch(RuntimeException e) {
      Logger.debug("while fetching csv for "+ctx.toString(), e);
      return "";
    }
  }

  protected String title(JFreeContext ctx) {
    return null;
  }

  protected abstract Drawable getDrawable(JFreeContext ctx);

  protected abstract String getCsv(JFreeContext ctx);

  protected abstract Dataset createDataset(Context ctx);

  @Override
  protected charts.builder.spreadsheet.Context context(
      SpreadsheetDataSource datasource, ChartType type, Region region,
      Map<String, String> parameters) {
    Dataset dataset = createDataset(new Context(datasource, type, region, parameters));
    return dataset != null?new JFreeContext(datasource, type, region, parameters, dataset) : null;
  }

  protected Dataset getDataset(Context context) {
    return ((JFreeContext)context).dataset();
  }

}
