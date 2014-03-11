package charts.builder.spreadsheet;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.text.DecimalFormat;
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
import charts.graphics.WetlandsRemaing;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableSet;

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

    @Override
    protected ADCDataset createDataset(Context ctx) {
      final SpreadsheetDataSource ds = ctx.datasource();
      if(canHandle(ds) && matchesRegion(ds, ctx.region())) {
        try {
          ADCDataset d = new ADCDataset();
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
      return WetlandsRemaing.createChart((ADCDataset)ctx.dataset(), new Dimension(750, 500));
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

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.TITLE, TITLE+"\n${region}").
          put(Attribute.X_AXIS_LABEL, "${catchment}").
          put(Attribute.Y_AXIS_LABEL, "Wetlands remaining (%)").
          put(Attribute.SERIES_COLORS, new Color[] {Colors.BLUE, Colors.RED}).
          build();
    }

    @Override
    public Set<SubstitutionKey> substitutionKeys() {
      return ImmutableSet.<SubstitutionKey>builder().
          addAll(super.substitutionKeys()).add(SubstitutionKeys.CATCHMENT).build();
    }

}
