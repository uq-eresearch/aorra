package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

public class AnnualRainfall {

    public Drawable createChart(final ADCDataset dataset, Dimension dimension) {
        final JFreeChart chart = ChartFactory.createBarChart(
            dataset.get(Attribute.TITLE),       // chart title
            dataset.get(Attribute.X_AXIS_LABEL),               // domain axis label
            dataset.get(Attribute.Y_AXIS_LABEL),                  // range axis label
            dataset,                  // data
            PlotOrientation.VERTICAL, // orientation
            false,                    // include legend
            false,                     // tooltips?
            false                     // URLs?
        );
        chart.setBackgroundPaint(Color.white);
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        // set the range axis to display integers only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setUpperMargin(0.15);
        final CategoryItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesItemLabelsVisible(0, Boolean.TRUE);
        final Color c = dataset.get(Attribute.SERIES_COLOR);
        renderer.setSeriesPaint(0, c != null?c:Color.blue);
        ((BarRenderer)renderer).setBarPainter(new StandardBarPainter());
        final CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        return new JFreeChartDrawable(chart, dimension);
    }
}
