package charts.builder.spreadsheet;

import static com.google.common.collect.Lists.newLinkedList;
import static java.lang.String.format;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;
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
import charts.graphics.HSLandPracticeSystems;
import charts.graphics.LandPracticeSystems;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class LandPracticeSystemsBuilder extends AbstractBuilder {

    private static enum PS {

        NUTRIENTS("Nutrients", 1), HERBICIDES("Herbicides", 5), SOIL("Soil", 9);

        private String label;
        private int colstart;

        private PS(String label, int colstart) {
            this.label = label;
            this.colstart = colstart;
        }

        public String getLabel() {
            return label;
        }

        public int getColstart() {
            return colstart;
        }
    }

    private static final List<String> CATEGORIES = Lists.newArrayList("A", "B", "C", "D");

    private static interface Converter {
      void addData(SpreadsheetDataSource ds, DefaultCategoryDataset dataset, String sheetname,
          int row, PS ps, String series) throws MissingDataException;
    }

    private static class HSConverter implements Converter {
      @Override
      public void addData(SpreadsheetDataSource ds, DefaultCategoryDataset dataset, String sheetname,
          int row, PS ps, String series) throws MissingDataException {
        for (int col = 0; col < 4; col++) {
          dataset.addValue(ds.select(sheetname, row, col + ps.getColstart()).asDouble(),
              String.format("%s_%s", CATEGORIES.get(col), series), ps.getLabel());
        }
      }
    }

    private static class GrainsConverter implements Converter {
      @Override
      public void addData(SpreadsheetDataSource ds, DefaultCategoryDataset dataset, String sheetname,
          int row, PS ps, String series) throws MissingDataException {
        double a = ds.select(sheetname, row, ps.getColstart()).asDouble();
        double b = ds.select(sheetname, row, 1 + ps.getColstart()).asDouble();
        double c = ds.select(sheetname, row, 2 + ps.getColstart()).asDouble();
        Double d = ds.select(sheetname, row, 3 + ps.getColstart()).asDouble();
        if(d!= null) {
          c+=d;
        }
        dataset.addValue(a,"A_"+series, ps.getLabel());
        dataset.addValue(b,"B_"+series, ps.getLabel());
        dataset.addValue(c,"C/D_"+series, ps.getLabel());
      }
    }

    private static final ImmutableMap<ChartType, String> SHEETS =
            new ImmutableMap.Builder<ChartType, String>()
                .put(ChartType.HORTICULTURE_PS, "horticulture")
                .put(ChartType.SUGARCANE_PS, "sugarcane")
                .put(ChartType.GRAINS_PS, "sugarcane")
                .build();

    public LandPracticeSystemsBuilder() {
        super(Lists.newArrayList(ChartType.HORTICULTURE_PS,
                ChartType.SUGARCANE_PS, ChartType.GRAINS_PS));
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return "land practice systems".equalsIgnoreCase(StringUtils.strip(datasource.select("A1")
                    .asString()));
        } catch (MissingDataException e) {
            return false;
        }
    }

    @Override
    public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region) {
        if(region == Region.CAPE_YORK ||
            (type == ChartType.GRAINS_PS && region != Region.FITZROY) ||
            (type == ChartType.SUGARCANE_PS && region == Region.FITZROY)) {
          return null;
        }
        final CategoryDataset dataset = createDataset(datasource, type, region);
        // Unsupported region
        if (dataset == null) {
          return null;
        }
        LandPracticeSystems lps;
        if(type == ChartType.GRAINS_PS) {
          lps = new GrainsPracticeSystems();
        } else {
          lps = new HSLandPracticeSystems();
        }
        final Drawable drawable =
            lps.createChart(dataset, type.getLabel() + " - " + region.getProperName(),
                new Dimension(750, 500));

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

    private CategoryDataset createDataset(SpreadsheetDataSource ds, ChartType type, Region region) {
        try {
            String sheetname = SHEETS.get(type);
            if(sheetname == null) {
                throw new RuntimeException(String.format("chart type %s not supported", type.getLabel()));
            }
            Integer row = null;
            String regionText = region.getName().toUpperCase();
            for (int i = 0; i < 65525; i++) {
              final String cellText = ds.select(sheetname, i, 0).asString();
              if (cellText == null) {
                continue;
              }
              if (cellText.toUpperCase().equals(regionText)) {
                row = i;
                break;
              }
            }
            // Unsupported region
            if (row == null) {
              return null;
            }
            row += 2;
            DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            String series1 = ds.select(sheetname, row, 0).asString();
            String series2 = ds.select(sheetname, row+1, 0).asString();
            Converter converter = (type == ChartType.GRAINS_PS) ?
                new GrainsConverter() : new HSConverter();
            converter.addData(ds, dataset, sheetname, row, PS.NUTRIENTS, series1);
            converter.addData(ds, dataset, sheetname, row+1, PS.NUTRIENTS, series2);
            converter.addData(ds, dataset, sheetname, row, PS.HERBICIDES, series1);
            converter.addData(ds, dataset, sheetname, row+1, PS.HERBICIDES, series2);
            converter.addData(ds, dataset, sheetname, row, PS.SOIL, series1);
            converter.addData(ds, dataset, sheetname, row+1, PS.SOIL, series2);
            return dataset;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }
}
