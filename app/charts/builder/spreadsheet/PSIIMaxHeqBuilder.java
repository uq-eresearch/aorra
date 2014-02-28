package charts.builder.spreadsheet;

import static charts.ChartType.PSII_MAX_HEQ;
import static charts.graphics.PSIIMaxHeq.SEPARATOR;
import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.lang.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.PSIIMaxHeq;
import charts.graphics.PSIIMaxHeq.Condition;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class PSIIMaxHeqBuilder extends JFreeBuilder {

  private static final String TITLE = "Maximum PSII Herbicide Equivalent Concentrations";

    private static final Pattern YEAR_PATTERN = Pattern.compile(".*?(\\d+-\\d+).*?");

    public PSIIMaxHeqBuilder() {
        super(PSII_MAX_HEQ);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return StringUtils.startsWithIgnoreCase(datasource.select("A1").asString(), TITLE);
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

    private Condition getCondition(String s) {
        if(isBlank(s)) {
            return Condition.NOT_EVALUATED;
        } else if(equalsIgnoreCase("red", s)) {
            return Condition.VERY_POOR;
        } else if(equalsIgnoreCase("orange", s)) {
            return Condition.POOR;
        } else if(equalsIgnoreCase("yellow", s)) {
            return Condition.MODERATE;
        } else if(equalsIgnoreCase("light green", s)) {
            return Condition.GOOD;
        } else if(equalsIgnoreCase("dark green", s)) {
            return Condition.VERY_GOOD;
        } else {
            throw new RuntimeException(String.format("trouble mapping color %s to condition", s));
        }
    }

    @Override
    protected ADCDataset createDataset(Context ctx) {
      if(ctx.region() != Region.GBR) {
        return null;
      }
      SpreadsheetDataSource ds = ctx.datasource();
        try {
          ADCDataset dataset = new ADCDataset();
            String year = "";
            String region = "";
            for(int row = 0;true;row++) {
                String col1 = ds.select(row, 0).asString();
                if(StringUtils.equalsIgnoreCase("region", col1)) {
                    continue;
                }
                if(StringUtils.startsWithIgnoreCase(col1, TITLE)) {
                    year = getYear(col1);
                    continue;
                }
                if(StringUtils.isNotBlank(col1)) {
                    region = col1;
                }
                String site = ds.select(row, 1).asString();
                if(StringUtils.isBlank(site)) {
                    if(StringUtils.isBlank(ds.select(row+1, 0).asString())) {
                        break;
                    } else {
                        continue;
                    }
                }
                Double val = ds.select(row, 2).asDouble();
                double v = val!=null?val:0.0;
                String condition = ds.select(row, 3).asString();
                String key = StringUtils.join(Lists.newArrayList(region, site, year), SEPARATOR);
                dataset.addValue(v, getCondition(condition), key);
            }
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
          put(Attribute.Y_AXIS_LABEL, "Max PSII Heq ng/L").
          build();
    }

    @Override
    protected Drawable getDrawable(JFreeContext ctx) {
      return PSIIMaxHeq.createChart((ADCDataset)ctx.dataset(),new Dimension(1000, 500));
    }

    @Override
    protected String getCsv(JFreeContext ctx) {
      final StringWriter sw = new StringWriter();
      try {
        final CategoryDataset dataset = (CategoryDataset)ctx.dataset();
        final CsvListWriter csv = new CsvListWriter(sw,
            CsvPreference.STANDARD_PREFERENCE);
        @SuppressWarnings("unchecked")
        List<String> columnKeys = dataset.getColumnKeys();
        @SuppressWarnings("unchecked")
        List<Condition> rowKeys = dataset.getRowKeys();
        final List<String> heading = ImmutableList.<String>builder()
            .add(format("%s %s", ctx.region(), ctx.type().getLabel()))
            .add("Region")
            .add("Subregion")
            .add("Period")
            .add("ng/L")
            .add("Rating")
            .build();
        csv.write(heading);
        for (String col : columnKeys) {
          List<String> line = newLinkedList();
          line.add("");
          line.addAll(asList(col.split("\\_\\|\\|\\_")));
          for (Condition row : rowKeys) {
            final Number n = dataset.getValue(row, col);
            if (n == null)
              continue;
            line.add(format("%.1f", n.doubleValue()));
            line.add(row.getLabel());
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

}
