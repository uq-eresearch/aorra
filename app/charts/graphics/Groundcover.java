package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.GridArrangement;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import charts.Drawable;

public class Groundcover {

    public static Drawable createChart(final CategoryDataset dataset, String title,
            String valueAxisLabel, Dimension dimension) {
        JFreeChart chart = ChartFactory.createLineChart(title,
                "Year", valueAxisLabel, dataset, PlotOrientation.VERTICAL, false, false, false);
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setRangeGridlineStroke(new BasicStroke(1));
        CategoryItemRenderer r = plot.getRenderer();
        r.setSeriesPaint(0, new Color(29,107,171));
        r.setSeriesPaint(1, new Color(134,177,56));
        r.setSeriesPaint(2, new Color(87,88,71));
        r.setSeriesPaint(3, new Color(150,116,52));
        r.setSeriesPaint(4, new Color(103,42,4));
        r.setSeriesPaint(5, new Color(208,162,33));
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickMarksVisible(false);
        final CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        LegendTitle legend = new LegendTitle(plot, new GridArrangement(3, 2), new GridArrangement(3, 2));
        legend.setLegendItemGraphicPadding(new RectangleInsets(0,20,0,0));
        legend.setPosition(RectangleEdge.BOTTOM);
        chart.addLegend(legend);
        return new JFreeChartDrawable(chart, dimension);
    }
}
