package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;

import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

public class WetlandsRemaing {

    public static Drawable createChart(ADCDataset dataset, Dimension dimension) {
        final JFreeChart chart = ChartFactory.createBarChart(
                dataset.get(Attribute.TITLE),// chart title
                dataset.get(Attribute.X_AXIS_LABEL),// domain axis label
                dataset.get(Attribute.Y_AXIS_LABEL),// range axis label
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                true,                    // include legend
                false,                     // tooltips?
                false                     // URLs?
            );
        chart.setBackgroundPaint(Color.white);
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        final BarRenderer renderer = (BarRenderer)plot.getRenderer();
        Colors.setSeriesPaint(renderer, dataset.get(Attribute.SERIES_COLORS));
        renderer.setItemMargin(0);
        renderer.setBarPainter(new StandardBarPainter());
        final CategoryAxis cAxis = plot.getDomainAxis();
        cAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        chart.getTitle().setFont(cAxis.getLabelFont());
        chart.getLegend().setMargin(2, 60, 2, 20);
        return new JFreeChartDrawable(chart, dimension);
    }

}
