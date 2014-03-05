package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;

import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

public class MarineTrends {

    public static Drawable createChart(final ADCDataset dataset, Dimension dimension) {
        final JFreeChart chart = ChartFactory.createLineChart(
                dataset.get(Attribute.TITLE),
                dataset.get(Attribute.X_AXIS_LABEL),
                dataset.get(Attribute.Y_AXIS_LABEL),
                dataset,
                PlotOrientation.VERTICAL,
                true, false, false);
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        ValueAxis raxis = plot.getRangeAxis();
        raxis.setRange(0, 100.0);
        CategoryItemRenderer renderer = plot.getRenderer();
        for(int i=0;i<dataset.get(Attribute.SERIES_COLORS).length;i++) {
          renderer.setSeriesPaint(i, dataset.get(Attribute.SERIES_COLORS)[i]);
        }
        return new JFreeChartDrawable(chart, dimension);
    }

}
