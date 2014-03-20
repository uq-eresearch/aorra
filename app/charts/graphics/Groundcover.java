package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import charts.ChartType;
import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

public class Groundcover {

    public static Drawable createChart(final ADCDataset dataset,
        ChartType type, Dimension dimension) {
        JFreeChart chart = ChartFactory.createLineChart(
            dataset.get(Attribute.TITLE),
            dataset.get(Attribute.X_AXIS_LABEL),
            dataset.get(Attribute.Y_AXIS_LABEL),
            dataset, PlotOrientation.VERTICAL, false, false, false);
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setRangeGridlineStroke(new BasicStroke(1));
        CategoryItemRenderer r = plot.getRenderer();
        for(int i = 0;i<dataset.get(Attribute.SERIES_COLORS).length;i++) {
          r.setSeriesPaint(i, dataset.get(Attribute.SERIES_COLORS)[i]);
        }
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickMarksVisible(false);
        if(type == ChartType.GROUNDCOVER_BELOW_50) {
          rangeAxis.setTickUnit(new NumberTickUnit(
              (Math.round(Math.floor(getMaxValue(dataset)))/10)+1, new DecimalFormat("0")));
        } else {
          rangeAxis.setRange(0, 100.0);
        }
        final CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        LegendTitle legend = new LegendTitle(plot, new TwoColumnArrangement(), null);
        legend.setLegendItemGraphicPadding(new RectangleInsets(0,20,0,0));
        legend.setPosition(RectangleEdge.BOTTOM);
        chart.addLegend(legend);
        return new JFreeChartDrawable(chart, dimension);
    }

    private static double getMaxValue(CategoryDataset dataset) {
      double max = 0;
      for(int r = 0;r<dataset.getRowCount();r++) {
        for(int c = 0;c<dataset.getColumnCount();c++) {
          Number n = dataset.getValue(r, c);
          if(n!=null) {
            max = Math.max(n.doubleValue(), max);
          }
        }
      }
      return max;
    }
}
