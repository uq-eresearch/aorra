package charts.builder;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.jfree.data.category.DefaultCategoryDataset;

import charts.Drawable;
import charts.TrackingTowardsTargets;
import charts.spreadsheet.DataSource;
import charts.spreadsheet.SpreadsheetHelper;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class TrackingTowardsTagetsBuilder extends DefaultSpreadsheetChartBuilder {

    private static enum Series {
        CANE("Cane"),
        HORTICULTURE("Horticulture"),
        GRAZING("Grazing"),
        SEDIMENT("Sediment"),
        TOTAL_NITROGEN("Total nitrogen"),
        PESTICIDES("Pesticides");

        private String name;

        private Series(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    private static final ImmutableMap<Series, Integer> ROW =
            new ImmutableMap.Builder<Series, Integer>()
                .put(Series.CANE, 1)
                .put(Series.HORTICULTURE, 2)
                .put(Series.GRAZING, 3)
                .put(Series.SEDIMENT, 4)
                .put(Series.TOTAL_NITROGEN, 5)
                .put(Series.PESTICIDES, 6)
                .build();

    public TrackingTowardsTagetsBuilder() {
        super(Lists.newArrayList(ChartType.TTT_CANE_AND_HORT, ChartType.TTT_GRAZING,
                ChartType.TTT_NITRO_AND_PEST, ChartType.TTT_SEDIMENT));
    }

    @Override
    boolean canHandle(DataSource datasource) {
        try {
            return "tracking towards tagets".equalsIgnoreCase(
                    datasource.select("A1").format("value"));
        } catch(Exception e) {
            return false;
        }
    }

    @Override
    Chart build(DataSource datasource, final ChartType type, final Region region,
            final Map<String, String[]> query) {
        if(region == Region.GBR) {
            SpreadsheetHelper helper = new SpreadsheetHelper(datasource);
            final DefaultCategoryDataset dataset = new DefaultCategoryDataset();
            final double target;
            final String targetBy;
            switch(type) {
            case TTT_CANE_AND_HORT:
                addSeries(helper, dataset, Series.CANE);
                addSeries(helper, dataset, Series.HORTICULTURE);
                target = getTarget(helper, Series.CANE);
                targetBy = getTargetBy(helper, Series.CANE);
                break;
            case TTT_GRAZING:
                addSeries(helper, dataset, Series.GRAZING);
                target = getTarget(helper, Series.GRAZING);
                targetBy = getTargetBy(helper, Series.GRAZING);
                break;
            case TTT_NITRO_AND_PEST:
                addSeries(helper, dataset, Series.TOTAL_NITROGEN);
                addSeries(helper, dataset, Series.PESTICIDES);
                target = getTarget(helper, Series.TOTAL_NITROGEN);
                targetBy = getTargetBy(helper, Series.TOTAL_NITROGEN);
                break;
            case TTT_SEDIMENT:
                addSeries(helper, dataset, Series.SEDIMENT);
                target = getTarget(helper, Series.SEDIMENT);
                targetBy = getTargetBy(helper, Series.SEDIMENT);
                break;
            default:
                throw new RuntimeException("chart type not supported "+type.toString());
            }
            return new AbstractChart(query) {

                @Override
                public ChartDescription getDescription() {
                    return new ChartDescription(type, region);
                }

                @Override
                public Drawable getChart() {
                    return new TrackingTowardsTargets().createChart(type, target, targetBy,
                            dataset, getChartSize(query, 750, 500));
                }

                @Override
                public String getCSV() throws UnsupportedFormatException {
                    throw new Chart.UnsupportedFormatException();
                }};
        }
        return null;
    }

    private void addSeries(SpreadsheetHelper helper, DefaultCategoryDataset dataset, Series series) {
        Integer row = ROW.get(series);
        if(row == null) {
            throw new RuntimeException("no row configured for series "+series);
        }
        List<String> columns = getColumns(helper);
        for(int col=0; col<columns.size(); col++) {
            String s = helper.selectText(row, col+3);
            try {
                Double value = new Double(s);
                dataset.addValue(value, series.toString(), columns.get(col));
            } catch(Exception e) {
                dataset.addValue(null, series.toString(), columns.get(col));
            }
        }

    }

    private List<String> getColumns(SpreadsheetHelper helper) {
        List<String> columns = Lists.newArrayList();
        for(int col=3; true; col++) {
            String s = helper.selectText(0, col);
            if(StringUtils.isBlank(s)) {
                break;
            }
            columns.add(s);
        }
        return columns;
    }

    private double getTarget(SpreadsheetHelper helper, Series series) {
        return helper.selectDouble(ROW.get(series), 1);
    }

    private String getTargetBy(SpreadsheetHelper helper, Series series) {
        return helper.selectInt(ROW.get(series), 2).toString();
    }

}
