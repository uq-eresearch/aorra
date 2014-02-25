package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.text.TextBlock;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleEdge;

import charts.Drawable;
import charts.jfree.ADSCDataset;
import charts.jfree.Attribute;

public class TrendsSeagrassAbundance {

    public static Drawable createChart(final ADSCDataset dataset, Dimension dimension) {
        JFreeChart chart = ChartFactory.createLineChart(
                dataset.get(Attribute.TITLE),  // title
                "",             // x-axis label
                dataset.get(Attribute.RANGE_AXIS_LABEL),   // y-axis label
                dataset,            // data
                PlotOrientation.VERTICAL,
                false,               // create legend?
                false,               // generate tooltips?
                false               // generate URLs?
            );
        chart.setBackgroundPaint(Color.white);
        StatisticalLineAndShapeRenderer renderer = new StatisticalLineAndShapeRenderer();
        renderer.setErrorIndicatorPaint(ErrorIndicator.ERROR_INDICATOR_COLOR);
        renderer.setDrawOutlines(true);
        renderer.setUseOutlinePaint(true);
        renderer.setBaseOutlinePaint(Color.black);
        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        plot.setRenderer(renderer);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setRangeGridlineStroke(new BasicStroke(1.0f));
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Color.white);
        renderer.setSeriesPaint(0, new Color(30, 172, 226));
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickUnit(new NumberTickUnit(5));
        rangeAxis.setRange(0.0, upperRange(dataset));
        rangeAxis.setTickMarksVisible(false);
        chart.getTitle().setFont(rangeAxis.getLabelFont());
        CategoryAxis cAxis = new CategoryAxis() {
            @SuppressWarnings("rawtypes")
            @Override
            protected TextBlock createLabel(Comparable category, float width,
                    RectangleEdge edge, Graphics2D g2) {
                int mod = dataset.getColumnCount() / 25 + 1;
                int col = dataset.getColumnIndex(category);
                String label = "";
                if(col % mod == 0) {
                    label = category.toString();
                }
                return TextUtilities.createTextBlock(label,
                        getTickLabelFont(category), getTickLabelPaint(category));
            }
        };
        cAxis.setLabel(dataset.get(Attribute.DOMAIN_AXIS_LABEL));
        cAxis.setLabelFont(rangeAxis.getLabelFont());
        cAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        plot.setDomainAxis(cAxis);
        chart.addLegend(ErrorIndicatorLegend.createLegend());
        return new JFreeChartDrawable(chart, dimension);
    }

    private static double upperRange(CategoryDataset dataset) {
        double max = 0.0;
        for(int i = 0;i<dataset.getColumnCount();i++) {
            max = Math.max(max, dataset.getValue(0, i).doubleValue());
        }
        double result = ((int)(max / 10.0)+1)*10;
        if(result > 100.0 && max < 100.0) {
            return 100.0;
        } else {
            return result;
        }
    }

}
