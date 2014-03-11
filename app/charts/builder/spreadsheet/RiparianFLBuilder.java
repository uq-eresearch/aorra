package charts.builder.spreadsheet;

import static charts.ChartType.RIPARIAN_FOREST_LOSS;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;

import com.google.common.collect.ImmutableSet;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.Value;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.Colors;
import charts.graphics.RiparianFL;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

public class RiparianFLBuilder extends JFreeBuilder {

    private static final String TITLE = "Riparian forest loss (%)";

    public RiparianFLBuilder() {
        super(RIPARIAN_FOREST_LOSS);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return StringUtils.equalsIgnoreCase(TITLE,
                    StringUtils.strip(datasource.select("B1").asString())) ||
                    StringUtils.equalsIgnoreCase(TITLE,
                    StringUtils.strip(datasource.select("L1").asString()));
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
            String[] cols;
            if(StringUtils.equalsIgnoreCase(ds.select("A2").asString(), "Region")) {
                cols = new String[] {"B", "C"};
            } else {
                cols = new String[] {"L", "N"};
            }
            for(int col=0;col<cols.length;col++) {
                String series = ds.select(cols[col]+"2").asString();
                for(int row=3;StringUtils.isNotBlank(ds.select("A"+Integer.toString(row)).asString());row++) {
                    String category = ds.select("A"+Integer.toString(row)).asString();
                    Double val = ds.select(cols[col]+Integer.toString(row)).asDouble();
                    d.addValue(val, series, category);
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
      return RiparianFL.createChart((ADCDataset)ctx.dataset(), new Dimension(750,500));
    }

    @Override
    protected String getCsv(JFreeContext ctx) {
      final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
      return Csv.write(new CsvWriter() {
        @Override
        public void write(CsvListWriter csv) throws IOException {
          csv.writeHeader(TITLE, (String)dataset.getRowKey(0), (String)dataset.getRowKey(1));
          for(int cat=0;cat<dataset.getColumnCount();cat++) {
              csv.write(dataset.getColumnKey(cat),
                      dataset.getValue(0, cat), dataset.getValue(1, cat));
          }
        }});
    }

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.TITLE, TITLE+"\n${region}").
          put(Attribute.X_AXIS_LABEL, "${catchment}").
          put(Attribute.Y_AXIS_LABEL, TITLE).
          put(Attribute.SERIES_COLORS, new Color[] {Colors.BLUE, Colors.RED}).
          build();
    }

    @Override
    public Set<SubstitutionKey> substitutionKeys() {
      return ImmutableSet.<SubstitutionKey>builder().
          addAll(super.substitutionKeys()).add(SubstitutionKeys.CATCHMENT).build();
    }
}
