package charts.builder.spreadsheet;

import static charts.ChartType.PSII_TRENDS;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.isBlank;
import static org.apache.commons.lang.StringUtils.isNotBlank;

import java.awt.Color;
import java.awt.Dimension;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.PSIITrends;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class PSIITrendsBuilder extends JFreeBuilder {

    private static final String TITLE = "Maximum concentration of individual PSII herbicides";

    private static final Pattern YEAR_PATTERN = Pattern.compile(".*?(\\d+-\\d+).*?");

    public PSIITrendsBuilder() {
        super(PSII_TRENDS);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return StringUtils.startsWithIgnoreCase(datasource.select("A2").asString(), TITLE);
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    private String getYear(String s) {
        Matcher m = YEAR_PATTERN.matcher(s);
        if(m.matches()) {
            String year = m.group(1);
            String[] y = StringUtils.split(year, '-');
            if(y.length == 2 && y[0].length() == 4 && y[1].length() == 4) {
                return y[0].substring(2)+"-"+y[1].substring(2);
            } else {
                return year;
            }
        } else {
            throw new RuntimeException("trouble extracting year from "+s);
        }
    }

    private boolean eof(SpreadsheetDataSource ds, int row) throws MissingDataException {
        for(int i=0;i<3;i++) {
            if(isNotBlank(ds.select(row+i, 1).asString())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected ADCDataset createDataset(Context ctx) {
      if(ctx.region() != Region.GBR) {
        return null;
      }
      final SpreadsheetDataSource ds = ctx.datasource();
        try {
          ADCDataset dataset = new ADCDataset();
            String cRegion = null;
            String year = null;
            List<String> pesticides = Lists.newArrayList();
            Map<String, Color> seriesColors = Maps.newHashMap();
            for(int row = 1;!eof(ds, row);row++) {
                String col1 = ds.select(row, 0).asString();
                if(StringUtils.startsWithIgnoreCase(col1, TITLE)) {
                    year = getYear(col1);
                    continue;
                } else if(StringUtils.equalsIgnoreCase("region", col1)) {
                    pesticides = Lists.newArrayList();
                    for(int col=2;col<13;col++) {
                        String p = ds.select(row, col).asString();
                        if(isBlank(p)) {
                            break;
                        } else {
                            pesticides.add(p);
                            Color c = ds.select(row,col).asColor();
                            if(c!= null && !Color.white.equals(c)) {
                              seriesColors.put(p, c);
                            }
                        }
                    }
                    continue;
                }
                if(isNotBlank(col1)) {
                    cRegion = col1;
                }
                String site = ds.select(row, 1).asString();
                if(isBlank(site)) {
                    continue;
                }
                for(int i=0;i<pesticides.size();i++) {
                    String p = pesticides.get(i);
                    Double val = ds.select(row, i+2).asDouble();
                    double v = val!=null?val:0.0;
                    String key = StringUtils.join(
                            Lists.newArrayList(cRegion, site, year), PSIITrends.SEPARATOR);
                    dataset.addValue(v, p, key);
                }
            }
            Color[] colors = new Color[pesticides.size()];
            for(int i=0;i<pesticides.size();i++) {
              Color c = seriesColors.get(pesticides.get(i));
              if(c!=null) {
                colors[i] = c;
              }
            }
            dataset.attrMap().put(Attribute.SERIES_COLORS, colors);
            return dataset;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public AttributeMap defaults(ChartType type) {
      return new AttributeMap.Builder().
          put(Attribute.TITLE, TITLE).
          put(Attribute.X_AXIS_LABEL, "").
          put(Attribute.Y_AXIS_LABEL, "Concentration in water (ng/L)").
          build();
    }

    @Override
    protected Drawable getDrawable(JFreeContext ctx) {
      return PSIITrends.createChart((ADCDataset)ctx.dataset(), new Dimension(1000, 500));
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
              .add(format("%s %s", ctx.region(), ctx.type().getLabel()))
              .add("Region")
              .add("Subregion")
              .add("Period")
              .addAll(rowKeys)
              .build();
          csv.write(heading);
          for (String col : columnKeys) {
            List<String> line = newLinkedList();
            line.add("");
            line.addAll(asList(col.split("\\_\\|\\|\\_")));
            for (String row : rowKeys) {
              line.add(format("%.1f",
                  dataset.getValue(row, col).doubleValue()));
            }
            csv.write(line);
          }
        }});
    }
}
