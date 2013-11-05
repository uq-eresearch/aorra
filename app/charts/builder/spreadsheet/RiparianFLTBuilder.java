package charts.builder.spreadsheet;

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
import charts.graphics.RiparianFLT;

public class RiparianFLTBuilder extends AbstractBuilder {

    private static final String TITLE = "Loss of riparian forest since pre-European extent (%)";

    public RiparianFLTBuilder() {
        super(ChartType.RIPARIAN_FOREST_LOSS_TOTAL);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            return StringUtils.equalsIgnoreCase(TITLE,
                    StringUtils.strip(datasource.select("B1").asString()));
            
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

    private boolean setupDataSource(SpreadsheetDataSource ds, Region region) {
        for(int i=0;i<ds.sheets();i++) {
            ds.setDefaultSheet(i);
            if(canHandle(ds) && matchesRegion(ds, region)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Chart build(final SpreadsheetDataSource datasource, final ChartType type,
            final Region region, Dimension queryDimensions) {
        
        if(setupDataSource(datasource, region)) {
            final CategoryDataset dataset = getDataset(datasource);
            return new AbstractChart(queryDimensions) {

                @Override
                public ChartDescription getDescription() {
                    return new ChartDescription(type, region);
                }

                @Override
                public Drawable getChart() {
                    return RiparianFLT.createChart(String.format("%s\n%s",TITLE,
                            region.getProperName()), region == Region.GBR?"Region":"Catchment",
                            dataset, new Dimension(750,500));
                }

                @Override
                public String getCSV() throws UnsupportedFormatException {
                    final StringWriter sw = new StringWriter();
                    try {
                      final CsvListWriter csv = new CsvListWriter(sw,
                          CsvPreference.STANDARD_PREFERENCE);
                      csv.writeHeader((region == Region.GBR?"Region":"Catchment"), TITLE);
                      for(int cat=0;cat<dataset.getColumnCount();cat++) {
                          Object o = dataset.getColumnKey(cat);
                          Number n = dataset.getValue(0, cat);
                          csv.write(o,n);
                      }
                      csv.close();
                    } catch (IOException e) {
                      // How on earth would you get an IOException with a StringWriter?
                      throw new RuntimeException(e);
                    }
                    return sw.toString();
                }

                @Override
                public String getCommentary() throws UnsupportedFormatException {
                    throw new UnsupportedFormatException();
                }};
        } else {
            return null;
        }
    }

    private CategoryDataset getDataset(SpreadsheetDataSource ds) {
        try {
            DefaultCategoryDataset d = new DefaultCategoryDataset();
            for(int row = 1;StringUtils.isNotBlank(ds.select(row, 0).asString());row++) {
                String region = ds.select(row, 0).asString();
                Double val = ds.select(row, 1).asDouble();
                d.addValue(val, TITLE, region);
            }
            return d;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

}
