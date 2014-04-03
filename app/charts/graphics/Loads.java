package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;

import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

public class Loads {

    public static Drawable createChart(ADCDataset dataset, Dimension dimension) {
        final JFreeChart chart = ChartFactory.createBarChart(
            dataset.get(Attribute.TITLE),       // chart title
            dataset.get(Attribute.X_AXIS_LABEL),               // domain axis label
            dataset.get(Attribute.Y_AXIS_LABEL), // range axis label
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
        rangeAxis.setTickUnit(new NumberTickUnit(5));
        rangeAxis.setRange(0.0, upperRange(dataset));
        final CategoryItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesItemLabelsVisible(0, Boolean.TRUE);
        renderer.setSeriesPaint(0, dataset.get(Attribute.SERIES_COLOR));
        ((BarRenderer)renderer).setBarPainter(new StandardBarPainter());
        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setMaximumCategoryLabelLines(3);
        return new JFreeChartDrawable(chart, dimension);
    }

    private static double upperRange(CategoryDataset dataset) {
        double max = 0.0;
        for(int i = 0;i<dataset.getColumnCount();i++) {
          if(dataset.getValue(0, i) != null) {
            max = Math.max(max, dataset.getValue(0, i).doubleValue());
          }
        }
        double result = ((int)(max / 10.0)+1)*10;
        if(result > 100.0 && max < 100.0) {
            return 100.0;
        } else {
            return result;
        }
    }

}
