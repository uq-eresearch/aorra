package charts.builder.spreadsheet;

import static charts.ChartType.RIPARIAN_FOREST_LOSS;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
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
import charts.builder.Value;
import charts.graphics.RiparianFL;

public class RiparianFLBuilder extends AbstractBuilder {

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
    public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension queryDimensions) {
        if(canHandle(datasource) && matchesRegion(datasource, region)) {
            final CategoryDataset dataset = getDataset(datasource);
            return new AbstractChart(queryDimensions) {

                @Override
                public ChartDescription getDescription() {
                    return new ChartDescription(type, region);
                }

                @Override
                public Drawable getChart() {
                    return RiparianFL.createChart(String.format("%s\n%s",TITLE,
                            region.getProperName()), region == Region.GBR?"Region":"Catchment", TITLE,
                            dataset, new Dimension(750,500));
                }

                @Override
                public String getCSV() throws UnsupportedFormatException {
                    final StringWriter sw = new StringWriter();
                    try {
                      final CsvListWriter csv = new CsvListWriter(sw,
                          CsvPreference.STANDARD_PREFERENCE);
                      csv.writeHeader(TITLE, (String)dataset.getRowKey(0), (String)dataset.getRowKey(1));
                      for(int cat=0;cat<dataset.getColumnCount();cat++) {
                          csv.write(dataset.getColumnKey(cat),
                                  dataset.getValue(0, cat), dataset.getValue(1, cat));
                      }
                      csv.close();
                    } catch (IOException e) {
                      // How on earth would you get an IOException with a StringWriter?
                      throw new RuntimeException(e);
                    }
                    return sw.toString();
                }

            };
        } else {
            return null;
        }
    }

    protected CategoryDataset getDataset(SpreadsheetDataSource ds) {
        try {
            DefaultCategoryDataset d = new DefaultCategoryDataset();
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
    }

}
