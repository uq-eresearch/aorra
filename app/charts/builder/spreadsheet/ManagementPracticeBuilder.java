package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.ManagementPracticeSystems;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public abstract class ManagementPracticeBuilder extends JFreeBuilder {

  public ManagementPracticeBuilder(ChartType type) {
    super(type);
  }

  private boolean eof(int row, SpreadsheetDataSource ds) throws MissingDataException {
    return StringUtils.isBlank(ds.select(row, 0).asString()) &&
        StringUtils.isBlank(ds.select(row+1, 0).asString());
  }

  @Override
  protected ADCDataset createDataset(Context ctx) {
    SpreadsheetDataSource ds = ctx.datasource();
    Region region = ctx.region();
    try {
      Region current = null;
      List<Pair<String, Double[]>> values = Lists.newArrayList();
      for(int row=1;!eof(row, ds);row++) {
        String r0 = ds.select(row, 0).asString();
        if(StringUtils.isBlank(r0)) {
          if(values.isEmpty()) {
            continue;
          } else {
            break;
          }
        }
        if(Region.lookup(r0) != null) {
          current = Region.lookup(r0);
          continue;
        }
        if(current == region) {
          String year = r0;
          values.add(Pair.of(year, readRow(ds, row)));
        }
      }
      if(values.size() >= 2) {
        ADCDataset dataset = new ADCDataset();
        for(Pair<String, Double[]> p :
          ImmutableList.of(values.get(0), values.get(values.size()-1))) {
          addData(dataset, p.getLeft(), p.getRight());
        }
        return dataset;
      } else {
        return null;
      }
    } catch(MissingDataException e) {
      throw new RuntimeException(e);
    }
  }

  protected abstract Double[] readRow(SpreadsheetDataSource ds,
      int row) throws MissingDataException;

  protected abstract void addData(ADCDataset dataset, String year, Double[] values);

  @Override
  public AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE, "${type} - ${region}").
        put(Attribute.X_AXIS_LABEL, "").
        build();
  }

  @Override
  protected String getCsv(JFreeContext ctx) {
    final ADCDataset dataset = (ADCDataset)ctx.dataset();
    final StringWriter sw = new StringWriter();
    try {
      final CsvListWriter csv = new CsvListWriter(sw,
          CsvPreference.STANDARD_PREFERENCE);
      @SuppressWarnings("unchecked")
      List<String> columnKeys = dataset.getColumnKeys();
      @SuppressWarnings("unchecked")
      List<String> rowKeys = dataset.getRowKeys();
      final List<String> heading = ImmutableList.<String>builder()
          .add(format("%s %s practices", ctx.region(), ctx.type()))
          .addAll(rowKeys)
          .build();
      csv.write(heading);
      for (String col : columnKeys) {
        List<String> line = newLinkedList();
        line.add(col);
        for (String row : rowKeys) {
          Number n = dataset.getValue(row, col);
          if(n != null) {
            line.add(format("%.1f",n.doubleValue() * 100));
          } else {
            line.add("");
          }
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

  protected abstract ManagementPracticeSystems renderer();

  @Override
  protected Drawable getDrawable(JFreeContext ctx) {
    return renderer().createChart((ADCDataset)ctx.dataset(), new Dimension(750, 500));
  }

}
