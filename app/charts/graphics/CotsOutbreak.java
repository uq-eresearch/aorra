package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import charts.Drawable;
import charts.jfree.ATSCollection;
import charts.jfree.Attribute;

public class CotsOutbreak {

    public Drawable createChart(final ATSCollection dataset, Dimension dimension) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                dataset.get(Attribute.TITLE),  // title
                dataset.get(Attribute.DOMAIN_AXIS_LABEL), // x-axis label
                dataset.get(Attribute.RANGE_AXIS_LABEL),   // y-axis label
                dataset,            // data
                false,               // create legend?
                false,               // generate tooltips?
                false               // generate URLs?
            );
        chart.setBackgroundPaint(Color.white);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoRangeIncludesZero(true);
        rangeAxis.setTickUnit(new NumberTickUnit(
            (Math.round(Math.floor(getMaxOutbreaks(dataset)))/20)+1, new DecimalFormat("0")));
        XYItemRenderer r = plot.getRenderer();
        Color seriesColor = dataset.get(Attribute.SERIES_COLOR);
        r.setSeriesPaint(0, ((seriesColor != null) && !Color.white.equals(seriesColor))?
            seriesColor:Color.blue);
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("yyyy"));
        DateTickUnit unit = new DateTickUnit(DateTickUnitType.YEAR, 1, new SimpleDateFormat("yyyy"));
        axis.setTickUnit(unit);
        return new JFreeChartDrawable(chart, dimension);
    }

    public double getMaxOutbreaks(XYDataset dataset) {
      double result = 0;
      for(int i=0;i<dataset.getItemCount(0);i++) {
        Number n = dataset.getY(0, i);
        if(n!= null) {
          result = Math.max(n.doubleValue(), result);
        }
      }
      return result;
    }

}
