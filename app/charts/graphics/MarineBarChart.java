package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
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
        dataset.get(Attribute.TITLE),
        dataset.get(Attribute.DOMAIN_AXIS_LABEL),
        dataset.get(Attribute.RANGE_AXIS_LABEL),
        dataset,
        PlotOrientation.VERTICAL,
        true, false, false);
    final CategoryPlot plot = chart.getCategoryPlot();
    plot.setBackgroundPaint(Color.white);
    plot.setDomainGridlinePaint(Color.lightGray);
    plot.setRangeGridlinePaint(Color.lightGray);
    ValueAxis raxis = plot.getRangeAxis();
    raxis.setRange(0, 100.0);
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
