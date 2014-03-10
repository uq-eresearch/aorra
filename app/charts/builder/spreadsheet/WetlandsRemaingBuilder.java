package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
import org.supercsv.io.CsvListWriter;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.WetlandsRemaing;

public class WetlandsRemaingBuilder extends JFreeBuilder {

    private static final String SWAMPS = "Vegetated freshwater swamps extent remaining";
    private static final String FLATS = "Mangroves/salt flats extent";
    private static final String TITLE = "Wetlands (vegetated freshwater swamps and" +
            " mangroves/salt flats)\nremaining from pre-European extent";


    public WetlandsRemaingBuilder() {
        super(ChartType.WETLANDS_REMAINING);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource ds) {
        try {
            return (startsWith(ds.select(0, 0), "Region") || startsWith(ds.select(0, 0), "Catchment")) &&
                    startsWith(ds.select(0, 1), SWAMPS) &&
                    startsWith(ds.select(0, 2), FLATS);
        } catch(MissingDataException e) {}
        return false;
    }

    private boolean startsWith(Value v, String prefix) {
        return StringUtils.startsWithIgnoreCase(StringUtils.strip(v.asString()), prefix);
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

    private String title(SpreadsheetDataSource ds, Region region) {
        return String.format("%s\n%s",TITLE, region.getProperName());
    }

    @Override
    protected CategoryDataset createDataset(Context ctx) {
      final SpreadsheetDataSource ds = ctx.datasource();
      if(canHandle(ds) && matchesRegion(ds, ctx.region())) {
        try {
          DefaultCategoryDataset d = new DefaultCategoryDataset();
          for(int col=1;col<3;col++) {
            String series = ds.select(0, col).asString();
            for(int row = 1;StringUtils.isNotBlank(ds.select(row, 0).asString());row++) {
              String region = ds.select(row, 0).asString();
              Double val = ds.select(row, col).asDouble();
              d.addValue(val, series, region);
            }
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
      return WetlandsRemaing.createChart(title(ctx.datasource(), ctx.region()),
          "Region", "Wetlands remaining (%)",
          (CategoryDataset)ctx.dataset(), new Dimension(750, 500));
    }

    @Override
    protected String getCsv(final JFreeContext ctx) {
      final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
      return Csv.write(new CsvWriter() {
        @Override
        public void write(CsvListWriter csv) throws IOException {
          csv.writeHeader((ctx.region() == Region.GBR?"Region":"Catchment"),
              (String)dataset.getRowKey(0), (String)dataset.getRowKey(1));
          DecimalFormat f = new DecimalFormat(".##");
          for(int cat=0;cat<dataset.getColumnCount();cat++) {
              csv.write(dataset.getColumnKey(cat), f.format(dataset.getValue(0, cat)),
                  f.format(dataset.getValue(1, cat)));
          }
        }});
    }

}
