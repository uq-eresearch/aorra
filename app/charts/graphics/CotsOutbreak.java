package charts.graphics;

import java.awt.Color;
import java.awt.Dimension;
import java.text.SimpleDateFormat;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.DateTickUnit;
import org.jfree.chart.axis.DateTickUnitType;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.data.xy.XYDataset;
import org.jfree.ui.RectangleInsets;

import charts.Drawable;

public class CotsOutbreak {

  private static final String TITLE = "Crown-of-thorns starfish outbreaks";

    public Drawable createChart(final XYDataset dataset, final String title,
        Color seriesColor, Dimension dimension) {
        JFreeChart chart = ChartFactory.createTimeSeriesChart(
                StringUtils.isNotBlank(title)?title:TITLE,  // title
                "Year",             // x-axis label
                "Outbreaks",   // y-axis label
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
        XYItemRenderer r = plot.getRenderer();
        r.setSeriesPaint(0, ((seriesColor != null) && !Color.white.equals(seriesColor))?
            seriesColor:Color.blue);
        DateAxis axis = (DateAxis) plot.getDomainAxis();
        axis.setDateFormatOverride(new SimpleDateFormat("yyyy"));
        DateTickUnit unit = new DateTickUnit(DateTickUnitType.YEAR, 1, new SimpleDateFormat("yyyy"));
        axis.setTickUnit(unit);
        return new JFreeChartDrawable(chart, dimension);
    }

}
