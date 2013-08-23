package charts.builder;

import static charts.builder.ChartBuilder.getFloat;
import static charts.builder.ChartBuilder.renderPNG;
import static charts.builder.ChartBuilder.renderSVG;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.util.CellReference;
import org.jfree.chart.JFreeChart;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import charts.AnnualRainfall;
import charts.representations.Format;
import charts.representations.Representation;
import charts.spreadsheet.DataSource;

import com.google.common.collect.ImmutableMap;

public class AnnualRainfallChartBuilder extends DefaultSpreadsheetChartBuilder {

    private static final ImmutableMap<Region, Integer> ROW =
            new ImmutableMap.Builder<Region, Integer>()
                .put(Region.BURDEKIN, 1)
                .put(Region.FITZROY, 2)
                .put(Region.MACKAY_WHITSUNDAYS, 3)
                .put(Region.BURNETT_MARY, 4)
                .put(Region.WET_TROPICS, 5)
                .put(Region.GBR, 6)
                .build();

    public AnnualRainfallChartBuilder() {
        super(ChartType.ANNUAL_RAINFALL);
    }

    private CategoryDataset createDataset(DataSource datasource, Region region) {
        int i = 1;
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        final String series = "rainfall";
        Integer row = ROW.get(region);
        while(true) {
            try {
                String year = datasource.select(new CellReference(0, i).formatAsString()).format("value");
                if(StringUtils.equalsIgnoreCase("Annual Average", year)) {
                    break;
                }
                String outbreaks = datasource.select(new CellReference(row, i).formatAsString()).format("value");
                if(StringUtils.isNotBlank(year) && StringUtils.isNotBlank(outbreaks)) {
                    double val = Double.parseDouble(outbreaks);
                    dataset.addValue(val, series, Integer.toString(parseYear(year)));
                } else {
                    break;
                }
                i++;
            } catch(Exception e) {
                e.printStackTrace();
                break;
            }
        }
        return dataset;
    }

    private int parseYear(String y) {
        y = StringUtils.strip(y);
        if(y.contains(".")) {
            y = StringUtils.substringBefore(y, ".");
        }
        return Integer.parseInt(y);
    }

    @Override
    boolean canHandle(DataSource datasource) {
        try {
            return "Great Barrier Reef".equalsIgnoreCase(datasource.select("A7").format("value"));
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    Chart build(DataSource datasource, final Region region,
            final Map<String, String[]> query) {
        if(ROW.containsKey(region)) {
            CategoryDataset dataset = createDataset(datasource, region);
            final JFreeChart jfreechart = new AnnualRainfall().createChart(
                region.getName(), dataset);
            final Chart chart = new Chart() {
              @Override
              public ChartDescription getDescription() {
                return new ChartDescription(ChartType.ANNUAL_RAINFALL, region);
              }
              @Override
              public Representation outputAs(Format format)
                  throws UnsupportedFormatException {
                switch (format) {
                case CSV:
                  // TODO: Replace with real implementation
                  return format.createRepresentation("");
                case SVG:
                  return format.createRepresentation(
                      renderSVG(createDimensions(jfreechart, query)));
                case PNG:
                  return format.createRepresentation(
                      renderPNG(createDimensions(jfreechart, query),
                          getFloat(query.get("width")),
                          getFloat(query.get("height"))));
                }
                throw new Chart.UnsupportedFormatException();
              }
            };
            return chart;
        } else {
            return null;
        }
    }

}
