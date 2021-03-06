package charts.builder.spreadsheet;

import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.supercsv.io.CsvListWriter;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.builder.csv.Csv;
import charts.builder.csv.CsvWriter;
import charts.graphics.ProgressTable;
import charts.graphics.ProgressTable.Dataset;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;

public class ProgressTableBuilder extends AbstractProgressTableBuilder {

  public Set<ProgressTable.Indicator> ALLOWED_INDICATORS =
      new ImmutableSet.Builder<ProgressTable.Indicator>()
        .add(ProgressTable.Indicator.GRAZING)
        .add(ProgressTable.Indicator.SUGARCANE)
        .add(ProgressTable.Indicator.GRAIN)
        .add(ProgressTable.Indicator.HORTICULTURE)
        .add(ProgressTable.Indicator.GROUNDCOVER)
        .add(ProgressTable.Indicator.NITROGEN)
        .add(ProgressTable.Indicator.SEDIMENT)
        .add(ProgressTable.Indicator.PESTICIDES)
        .build();

  public ProgressTableBuilder() {
    super(Lists.newArrayList(
        ChartType.PROGRESS_TABLE,
        ChartType.PROGRESS_TABLE_REGION));
  }

  public abstract static class ProgressTableChart extends AbstractChart {
    public abstract ProgressTable.Dataset dataset();
  }

  @Override
  public Chart build(Context ctx) {
    final SpreadsheetDataSource datasource = ctx.datasource();
    final ChartType type = ctx.type();
    final Region region = ctx.region();
    final ProgressTable.Dataset ds;
    try {
      ds = getDataset(datasource, type==ChartType.PROGRESS_TABLE_REGION?region:null);
    } catch (MissingDataException e) {
      // TODO: Handle this better
      throw new RuntimeException(e);
    }
    if (region == Region.GBR
        || type == ChartType.PROGRESS_TABLE_REGION) {
      final ProgressTable pt = new ProgressTable(ds);
      return new ProgressTableChart() {
        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(type, region);
        }

        @Override
        public Drawable getChart() {
          return pt;
        }

        @Override
        public Dataset dataset() {
          return ds;
        }

        @Override
        public String getCSV() throws UnsupportedFormatException {
          return Csv.write(new CsvWriter() {
            @Override
            public void write(CsvListWriter csv) throws IOException {
              {
                final List<String> cList = Lists.newLinkedList();
                cList.add("");
                for (ProgressTable.Column c : ds.columns) {
                  cList.add(c.header);
                }
                csv.write(cList);
              }
              for (ProgressTable.Row r : ds.rows) {
                final List<String> rList = Lists.newLinkedList();
                rList.add(r.header);
                for (ProgressTable.Cell c : r.cells) {
                  if (c.condition == null) {
                    rList.add("");
                  } else {
                    rList.add(String.format("%s (%s)", c.condition, c.progress));
                  }
                }
                csv.write(rList);
              }
            }});
        }
      };
    }
    return null;
  }

  @Override
  protected List<ProgressTable.Column> getColumns(SpreadsheetDataSource ds, Region region)
      throws MissingDataException {
    List<ProgressTable.Column> columns = Lists.newArrayList();
    int col = 2;
    while (StringUtils.isNotBlank(ds.select(0, col).asString())) {
      ProgressTable.Indicator indicator =
          getIndicator(ds.select(0, col).asString(), region);
      if (ALLOWED_INDICATORS.contains(indicator)) {
        columns.add(new ProgressTable.Column(
            indicator.getLabel(),
            ds.select(1, col).asString(),
            ds.select(2, col).asString()));
      }
      col++;
    }
    return columns;
  }

}
