package charts.builder.spreadsheet;

import java.awt.Dimension;
import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
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
import charts.graphics.WetlandLoss;

public class WetlandsLossBuilder extends AbstractBuilder {

    private static final String SWAMPS = "Vegetated freshwater swamps loss";
    private static final String FLATS = "Mangroves/salt flats loss";
    private static final String WETLANDS_LOSS = "Wetlands loss (%)";
    private static final String TITLE = "Wetlands (vegetated freshwater swamps and" +
            " mangroves/salt flats) loss";

    public WetlandsLossBuilder() {
        super(ChartType.WETLANDS_LOSS);
    }

    @Override
    public boolean canHandle(SpreadsheetDataSource datasource) {
        try {
            List<Value> values = datasource.selectRow(0);
            return (startsWith(values.get(0), "Region") || startsWith(values.get(0), "Catchment")) &&
                    startsWith(values.get(1), SWAMPS) &&
                    startsWith(values.get(2), FLATS) &&
                    startsWith(values.get(3), SWAMPS) &&
                    startsWith(values.get(4), FLATS);
        } catch(MissingDataException e) {}
        return false;
    }

    private boolean startsWith(Value v, String prefix) {
        return StringUtils.startsWithIgnoreCase(StringUtils.strip(v.asString()), prefix);
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
                    return WetlandLoss.createChart(title(datasource, region), WETLANDS_LOSS, dataset, new Dimension(750, 500));
                }

                @Override
                public String getCSV() throws UnsupportedFormatException {
                    final StringWriter sw = new StringWriter();
                    try {
                      final CsvListWriter csv = new CsvListWriter(sw,
                          CsvPreference.STANDARD_PREFERENCE);
                      csv.writeHeader((region == Region.GBR?"Region":"Catchment"),
                          (String)dataset.getRowKey(0), (String)dataset.getRowKey(1),
                          (String)dataset.getRowKey(2), (String)dataset.getRowKey(3));
                      DecimalFormat f = new DecimalFormat(".##");
                      for(int cat=0;cat<dataset.getColumnCount();cat++) {
                          csv.write(dataset.getColumnKey(cat), f.format(dataset.getValue(0, cat)),
                              f.format(dataset.getValue(1, cat)), f.format(dataset.getValue(2, cat)), 
                              f.format(dataset.getValue(3, cat)));
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

    private String title(SpreadsheetDataSource ds, Region region) {
        return String.format("%s\n%s",TITLE, region.getProperName());
    }

    private CategoryDataset getDataset(SpreadsheetDataSource ds) {
        try {
            DefaultCategoryDataset d = new DefaultCategoryDataset();
            for(int col=1;col<5;col++) {
                String series = ds.select(0, col).asString();
                for(int row = 1;StringUtils.isNotBlank(ds.select(row, 0).asString());row++) {
                    String region = ds.select(row, 0).asString();
                    Double val = ds.select(row, col).asDouble();
                    d.addValue(val, series, region);
                }
            }
            return d;
        } catch(MissingDataException e) {
            throw new RuntimeException(e);
        }
    }

}
