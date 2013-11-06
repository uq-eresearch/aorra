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
import org.jfree.data.category.CategoryDataset;

import charts.Drawable;

public class WetlandsRemaing {

    public static Drawable createChart(String title, String cAxisTitle, String vAxisTitle,
            CategoryDataset dataset, Dimension dimension) {
        final JFreeChart chart = ChartFactory.createBarChart(
                title,       // chart title
                cAxisTitle,               // domain axis label
                vAxisTitle,                  // range axis label
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
        renderer.setSeriesPaint(0, Colors.BLUE);
        renderer.setSeriesPaint(1, Colors.RED);
        renderer.setItemMargin(0);
        final CategoryAxis cAxis = plot.getDomainAxis();
        cAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        chart.getTitle().setFont(cAxis.getLabelFont());
        chart.getLegend().setMargin(2, 60, 2, 20);
        return new JFreeChartDrawable(chart, dimension);
    }

}
