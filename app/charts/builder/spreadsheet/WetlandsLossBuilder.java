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
import charts.graphics.WetlandLoss;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableSet;

public class WetlandsLossBuilder extends JFreeBuilder {

    private static final String SWAMPS = "Vegetated freshwater swamps loss";
    private static final String FLATS = "Mangroves/salt flats loss";
    private static final String WETLANDS_LOSS = "Wetlands loss (%)";
    private static final String TITLE = "Wetlands (vegetated freshwater swamps and" +
            " mangroves/salt flats) loss";

    public WetlandsLossBuilder() {
        super(ChartType.WETLANDS_LOSS);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource ds) {
        try {
            int fc = findFirstColumn(ds);
            return (startsWith(ds.select(0, fc), "Region") || startsWith(ds.select(0, fc), "Catchment")) &&
                    startsWith(ds.select(0, fc+1), SWAMPS) &&
                    startsWith(ds.select(0, fc+2), FLATS) &&
                    startsWith(ds.select(0, fc+3), SWAMPS) &&
                    startsWith(ds.select(0, fc+4), FLATS);
        } catch(MissingDataException e) {}
        return false;
    }

    private int findFirstColumn(SpreadsheetDataSource datasource) {
        Integer cc = datasource.getColumnCount(0);
        if(cc == null) {
            return 0;
        } else {
            try {
                for(int c = 0;c<cc;c++) {
                    if(StringUtils.isNotBlank(datasource.select(0, c).asString())) {
                        return c;
                    }
                }
            } catch(MissingDataException e) {
                throw new RuntimeException(e);
            }
            return 0;
        }
    }

    private boolean startsWith(Value v, String prefix) {
        return StringUtils.startsWithIgnoreCase(StringUtils.strip(v.asString()), prefix);
    }

    private boolean matchesRegion(SpreadsheetDataSource ds, Region region) {
        try {
            String cmp = region.getProperName() + (region == Region.GBR?" total":" region");
            int fc = findFirstColumn(ds);
            List<Value> col0 = ds.selectColumn(fc, 20);
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
      SpreadsheetDataSource ds = ctx.datasource();
      if(canHandle(ds) && matchesRegion(ds, ctx.region())) {
        try {
          ADCDataset d = new ADCDataset();
          int fc = findFirstColumn(ds);
          for(int col=1;col<5;col++) {
            String series = ds.select(0, fc+col).asString();
            for(int row = 1;StringUtils.isNotBlank(ds.select(row, fc).asString());row++) {
              String region = ds.select(row, fc).asString();
              Double val = ds.select(row, fc+col).asDouble();
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
      return WetlandLoss.createChart((ADCDataset)ctx.dataset(), new Dimension(750, 500));
    }

    @Override
    protected String getCsv(final JFreeContext ctx) {
      final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
      return Csv.write(new CsvWriter() {
        @Override
        public void write(CsvListWriter csv) throws IOException {
          csv.writeHeader((ctx.region() == Region.GBR?"Region":"Catchment"),
              (String)dataset.getRowKey(0), (String)dataset.getRowKey(1),
              (String)dataset.getRowKey(2), (String)dataset.getRowKey(3));
          DecimalFormat f = new DecimalFormat(".##");
          for(int cat=0;cat<dataset.getColumnCount();cat++) {
              csv.write(dataset.getColumnKey(cat), f.format(dataset.getValue(0, cat)),
                  f.format(dataset.getValue(1, cat)), f.format(dataset.getValue(2, cat)),
                  f.format(dataset.getValue(3, cat)));
          }
        }});
    }

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.TITLE, TITLE+"\n${region}").
          put(Attribute.X_AXIS_LABEL, "${catchment}").
          put(Attribute.Y_AXIS_LABEL, WETLANDS_LOSS).
          put(Attribute.SERIES_COLORS, new Color[] {Colors.RED, Colors.LIGHT_RED,
              Colors.BLUE, Colors.LIGHT_BLUE}).
          build();
    }

    @Override
    public Set<SubstitutionKey> substitutionKeys() {
      return ImmutableSet.<SubstitutionKey>builder().
          addAll(super.substitutionKeys()).add(SubstitutionKeys.CATCHMENT).build();
    }

}
