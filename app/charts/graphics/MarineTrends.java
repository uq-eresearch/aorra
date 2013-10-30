package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.data.category.CategoryDataset;

import charts.ChartType;
import charts.Drawable;
import charts.Region;

public class MarineTrends {

    public static Drawable createChart(final CategoryDataset dataset, ChartType type,
            Region region, Dimension dimension) {
        final JFreeChart chart = ChartFactory.createLineChart(
                region.getProperName()+" "+type.getLabel().toLowerCase(),
                "Year",
                "Score",
                dataset,
                PlotOrientation.VERTICAL,
                true, false, false);
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        CategoryItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesPaint(0, Colors.BLUE);
        renderer.setSeriesPaint(1, Colors.DARK_RED);
        renderer.setSeriesPaint(2, Colors.RED);
        renderer.setSeriesPaint(3, Colors.VIOLET);
        renderer.setSeriesPaint(4, Colors.GREEN);
        return new JFreeChartDrawable(chart, dimension);
    }

}
