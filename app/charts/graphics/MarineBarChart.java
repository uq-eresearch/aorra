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

public class MarineBarChart {

  public Drawable createChart(ADCDataset dataset) {
    final JFreeChart chart = ChartFactory.createBarChart(
        dataset.<String>get(Attribute.TITLE),
        dataset.<String>get(Attribute.DOMAIN_AXIS_TITLE),
        dataset.<String>get(Attribute.RANGE_AXIS_TITLE),
        dataset,
        PlotOrientation.VERTICAL,
        true, false, false);
    final CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinePaint(Color.lightGray);
    plot.setRangeGridlinePaint(Color.lightGray);
    CategoryAxis cAxis = plot.getDomainAxis();
    cAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
    BarRenderer renderer = (BarRenderer)plot.getRenderer();
    renderer.setSeriesPaint(0, Colors.fromHex("#0AA1D8"));
    renderer.setSeriesPaint(1, Colors.fromHex("#932832"));
    renderer.setSeriesPaint(2, Colors.fromHex("#94BA4D"));
    renderer.setBarPainter(new StandardBarPainter());
    renderer.setItemMargin(0.01);
    return new JFreeChartDrawable(chart, new Dimension(750, 500));
  }

}