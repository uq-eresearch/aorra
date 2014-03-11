package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.Colors;
import charts.graphics.RiparianFLT;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableSet;

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
    protected ADCDataset createDataset(Context ctx) {
      final SpreadsheetDataSource ds = ctx.datasource();
      if(canHandle(ds) && matchesRegion(ds, ctx.region())) {
        try {
          ADCDataset d = new ADCDataset();
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
      return RiparianFLT.createChart((ADCDataset)ctx.dataset(), new Dimension(750,500));
    }

    @Override
    protected String getCsv(final JFreeContext ctx) {
      final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
      return Csv.write(new CsvWriter() {
        @Override
        public void write(CsvListWriter csv) throws IOException {
          csv.writeHeader((ctx.region() == Region.GBR?"Region":"Catchment"), TITLE);
          for(int cat=0;cat<dataset.getColumnCount();cat++) {
              Object o = dataset.getColumnKey(cat);
              Number n = dataset.getValue(0, cat);
              csv.write(o,n);
          }
        }});
    }

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.TITLE, TITLE+"\n${region}").
          put(Attribute.X_AXIS_LABEL, "${catchment}").
          put(Attribute.Y_AXIS_LABEL, "Riparian forest loss (%)").
          put(Attribute.SERIES_COLOR, Colors.BLUE).
          build();
    }

    @Override
    public Set<SubstitutionKey> substitutionKeys() {
      return ImmutableSet.<SubstitutionKey>builder().
          addAll(super.substitutionKeys()).add(SubstitutionKeys.CATCHMENT).build();
    }

}
