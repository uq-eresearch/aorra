package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.text.NumberFormat;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.text.TextBlock;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleEdge;

import charts.Drawable;

public class TrendsSeagrassAbundance {

    private static NumberFormat percentFormatter() {
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(0);
        return percentFormat;
    }

    public static Drawable createChart(final CategoryDataset dataset, String title, Dimension dimension) {
        JFreeChart chart = ChartFactory.createLineChart(
                title,  // title
                "",             // x-axis label
                "Seagrass abundance",   // y-axis label
                dataset,            // data
                PlotOrientation.VERTICAL,
                false,               // create legend?
                false,               // generate tooltips?
                false               // generate URLs?
            );
        chart.setBackgroundPaint(Color.white);
        StatisticalLineAndShapeRenderer renderer = new StatisticalLineAndShapeRenderer();
        renderer.setErrorIndicatorPaint(new Color(147,112,45));
        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        plot.setRenderer(renderer);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setRangeGridlineStroke(new BasicStroke(1.0f));
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Color.white);
        renderer.setSeriesPaint(0, Color.blue);
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickUnit(new NumberTickUnit(0.05, percentFormatter()));
        rangeAxis.setTickMarksVisible(false);
        CategoryAxis cAxis = new CategoryAxis() {
            @Override
            protected TextBlock createLabel(Comparable category, float width,
                    RectangleEdge edge, Graphics2D g2) {
                String label = "";
                if(StringUtils.contains(category.toString(), "July")) {
                    label = category.toString();
                }
                return TextUtilities.createTextBlock(label,
                        getTickLabelFont(category), getTickLabelPaint(category));

            }
        };
        cAxis.setLabel("Year");
        cAxis.setLabelFont(rangeAxis.getLabelFont());
        chart.getTitle().setFont(rangeAxis.getLabelFont());
        plot.setDomainAxis(cAxis);
        return new JFreeChartDrawable(chart, dimension);
    }

}
