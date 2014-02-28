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
import charts.builder.Value;
import charts.graphics.RiparianFLT;

public class RiparianFLTBuilder extends JFreeBuilder {

    private static final String TITLE = "Loss of riparian forest since pre-European extent (%)";

    public RiparianFLTBuilder() {
        super(ChartType.RIPARIAN_FOREST_LOSS_TOTAL);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return StringUtils.equalsIgnoreCase(TITLE,
                    StringUtils.strip(datasource.select("B1").asString()));

        } catch(MissingDataException e) {}
        return false;
    }

    private boolean matchesRegion(SpreadsheetDataSource ds, Region region) {
        try {
            String cmp = region.getProperName() + (region == Region.GBR?" total":" region");
            List<Value> col0 = ds.selectColumn(0, 20);
            for(Value v : col0) {
                if(StringUtils.equalsIgnoreCase(
                        StringUtils.strip(v.asString()), cmp)) {
                    return true;
                }
            }
            return false;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected CategoryDataset createDataset(Context ctx) {
      final SpreadsheetDataSource ds = ctx.datasource();
      if(canHandle(ds) && matchesRegion(ds, ctx.region())) {
        try {
          DefaultCategoryDataset d = new DefaultCategoryDataset();
          for(int row = 1;StringUtils.isNotBlank(ds.select(row, 0).asString());row++) {
            String region = ds.select(row, 0).asString();
            Double val = ds.select(row, 1).asDouble();
            d.addValue(val, TITLE, region);
          }
          return d;
        } catch(MissingDataException e) {
          throw new RuntimeException(e);
        }
      } else {
        return null;
      }
    }

    @Override
    protected Drawable getDrawable(JFreeContext ctx) {
      Region region = ctx.region();
      return RiparianFLT.createChart(String.format("%s\n%s",TITLE,
          region.getProperName()), region == Region.GBR?"Region":"Catchment",
          (CategoryDataset)ctx.dataset(), new Dimension(750,500));
    }

    @Override
    protected String getCsv(JFreeContext ctx) {
      CategoryDataset dataset = (CategoryDataset)ctx.dataset();
      final StringWriter sw = new StringWriter();
      try {
        final CsvListWriter csv = new CsvListWriter(sw,
            CsvPreference.STANDARD_PREFERENCE);
        csv.writeHeader((ctx.region() == Region.GBR?"Region":"Catchment"), TITLE);
        for(int cat=0;cat<dataset.getColumnCount();cat++) {
            Object o = dataset.getColumnKey(cat);
            Number n = dataset.getValue(0, cat);
            csv.write(o,n);
        }
        csv.close();
      } catch (IOException e) {
        // How on earth would you get an IOException with a StringWriter?
        throw new RuntimeException(e);
      }
      return sw.toString();
    }

}
