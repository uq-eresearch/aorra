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

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.GrainsPracticeSystems;
import charts.graphics.GrazingPracticeSystems;
import charts.graphics.HSLandPracticeSystems;
import charts.graphics.ManagementPracticeSystems;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;
import charts.jfree.AttributeMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public abstract class ManagementPracticeBuilder extends AbstractBuilder {

  public ManagementPracticeBuilder(ChartType type) {
    super(type);
  }

  @Override
  protected Chart build(SpreadsheetDataSource datasource,
      final ChartType type, final Region region) {
    final ADCDataset dataset = createDataset(datasource, type, region);
    if(dataset == null) {
      return null;
    }
    configurator(datasource, type, region).configure(dataset);
    ManagementPracticeSystems mps;
    if(type == ChartType.HORTICULTURE_PS || type == ChartType.SUGARCANE_PS) {
      mps = new HSLandPracticeSystems();
    } else if(type == ChartType.GRAINS_PS) {
      mps = new GrainsPracticeSystems();
    } else if(type == ChartType.GRAZING_PS) {
      mps = new GrazingPracticeSystems();
    } else {
      return null;
    }
    final Drawable drawable = mps.createChart(dataset, new Dimension(750, 500));
    return new AbstractChart() {
      @Override
      public ChartDescription getDescription() {
        return new ChartDescription(type, region);
      }

      @Override
      public Drawable getChart() {
        return drawable;
      }

      @Override
      public String getCSV() {
        final StringWriter sw = new StringWriter();
        try {
          final CsvListWriter csv = new CsvListWriter(sw,
              CsvPreference.STANDARD_PREFERENCE);
          @SuppressWarnings("unchecked")
          List<String> columnKeys = dataset.getColumnKeys();
          @SuppressWarnings("unchecked")
          List<String> rowKeys = dataset.getRowKeys();
          final List<String> heading = ImmutableList.<String>builder()
              .add(format("%s %s practices", region, type))
              .addAll(rowKeys)
              .build();
          csv.write(heading);
          for (String col : columnKeys) {
            List<String> line = newLinkedList();
            line.add(col);
            for (String row : rowKeys) {
              line.add(format("%.1f",
                  dataset.getValue(row, col).doubleValue() * 100));
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
    };
  }

  private boolean eof(int row, SpreadsheetDataSource ds) throws MissingDataException {
    return StringUtils.isBlank(ds.select(row, 0).asString()) &&
        StringUtils.isBlank(ds.select(row+1, 0).asString());
  }

  private ADCDataset createDataset(SpreadsheetDataSource ds, ChartType type, Region region) {
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
  protected AttributeMap defaults(ChartType type) {
    return new AttributeMap.Builder().
        put(Attribute.TITLE, "${type} - ${region}").
        put(Attribute.X_AXIS_LABEL, "").
        build();
  }

}
