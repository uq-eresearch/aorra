package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.KeyToGroupMap;

import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

public class WetlandLoss {

    public static Drawable createChart(ADCDataset dataset, Dimension dimension) {
        final JFreeChart chart = ChartFactory.createBarChart(
                dataset.get(Attribute.TITLE),       // chart title
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
        GroupedStackedBarRenderer renderer = new GroupedStackedBarRenderer();
        KeyToGroupMap map = new KeyToGroupMap();
        map.mapKeyToGroup(dataset.getRowKey(0), "Y1");
        map.mapKeyToGroup(dataset.getRowKey(1), "Y1");
        map.mapKeyToGroup(dataset.getRowKey(2), "Y2");
        map.mapKeyToGroup(dataset.getRowKey(3), "Y2");
        renderer.setSeriesToGroupMap(map);
        Colors.setSeriesPaint(renderer, dataset.get(Attribute.SERIES_COLORS));
        renderer.setItemMargin(0);
        renderer.setBarPainter(new StandardBarPainter());
        plot.setRenderer(renderer);
        final CategoryAxis cAxis = plot.getDomainAxis();
        cAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        chart.getTitle().setFont(cAxis.getLabelFont());
        chart.getLegend().setMargin(2, 60, 2, 20);
        return new JFreeChartDrawable(chart, dimension);
    }

}
