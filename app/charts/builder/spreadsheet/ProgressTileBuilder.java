package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.supercsv.io.CsvListWriter;
import org.supercsv.prefs.CsvPreference;

import charts.AbstractChart;
import charts.Chart;
import charts.ChartDescription;
import charts.ChartType;
import charts.Drawable;
import charts.Region;
import charts.builder.DataSource.MissingDataException;
import charts.graphics.ProgressTable;
import charts.graphics.ProgressTable.Indicator;

import com.google.common.collect.Lists;

public class ProgressTileBuilder extends AbstractProgressTableBuilder {

  public ProgressTileBuilder() {
    super(Lists.newArrayList(ChartType.PROGRESS_TABLE_TILE));
  }

  @Override
  protected Map<String, List<String>> getParameters(SpreadsheetDataSource datasource, ChartType type) {
    if (type != null && !type.equals(ChartType.PROGRESS_TABLE_TILE)) {
      return Collections.emptyMap();
    }
    final List<String> indicators = newArrayList();
    for (Indicator i : ProgressTable.Indicator.values()) {
      indicators.add(i.toString());
    }
    return Collections.singletonMap("indicator", indicators);
  }

  @Override
  protected Chart build(SpreadsheetDataSource datasource, final ChartType type, final Region region, final Map<String, String> parameters) {
    if (!parameters.containsKey("indicator")) {
      return null;
    }
    final ProgressTable.Indicator indicator =
        ProgressTable.Indicator.valueOf(parameters.get("indicator"));
    try {
      final ProgressTable.Cell cell =
          getIndicatorCell(datasource, region, indicator);
      if (cell == null) {
        return null;
      }
      final ProgressTable pt = new ProgressTable(cell);
      return new AbstractChart() {
        @Override
        public ChartDescription getDescription() {
          return new ChartDescription(type, region, parameters,
              "Progress Tile - "+indicator.getLabel());
        }

        @Override
        public Drawable getChart() {
          return pt;
        }

        @Override
        public String getCSV() throws UnsupportedFormatException {
          final StringWriter sw = new StringWriter();
          try {
            final CsvListWriter csv = new CsvListWriter(sw,
                CsvPreference.STANDARD_PREFERENCE);
            {
              final List<String> cList = Lists.newLinkedList();
              cList.add("Indicator");
              cList.add(indicator.getLabel());
              csv.write(cList);
            }
            {
              final List<String> cList = Lists.newLinkedList();
              cList.add("Progress");
              cList.add(cell.progress.toString());
              csv.write(cList);
            }
            {
              final List<String> cList = Lists.newLinkedList();
              cList.add("Condition");
              cList.add(cell.condition.toString());
              csv.write(cList);
            }
            csv.close();
          } catch (IOException e) {
            // How on earth would you get an IOException with a StringWriter?
            throw new RuntimeException(e);
          }
          return sw.toString();
        }
      };
    } catch (MissingDataException e) {
      return null;
    }
  }

  private ProgressTable.Cell getIndicatorCell(SpreadsheetDataSource datasource,
      Region region, Indicator indicator) throws MissingDataException {
    final List<ProgressTable.Column> columns = getColumns(datasource, region);
    for (ProgressTable.Cell cell : getRow(columns, datasource, region).cells) {
      if (cell.indicator != null && cell.indicator.equals(indicator)) {
        return cell;
      }
    }
    return null;
  }

}
