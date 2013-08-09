package charts;

import java.awt.Color;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;

public class AnnualRainfall {

    public JFreeChart createChart(final String region, final CategoryDataset dataset) {
        final JFreeChart chart = ChartFactory.createBarChart(
            getTitle(dataset, region),       // chart title
            "Year",               // domain axis label
            "Rainfall (mm)",                  // range axis label
            dataset,                  // data
            PlotOrientation.VERTICAL, // orientation
            false,                    // include legend
            false,                     // tooltips?
            false                     // URLs?
        );
        chart.setBackgroundPaint(Color.white);
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        // set the range axis to display integers only...
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
        rangeAxis.setUpperMargin(0.15);
        final CategoryItemRenderer renderer = plot.getRenderer();
        renderer.setSeriesItemLabelsVisible(0, Boolean.TRUE);
        renderer.setSeriesPaint(0, Color.blue);
        ((BarRenderer)renderer).setBarPainter(new StandardBarPainter());
        final CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        return chart;
    }

    private String getTitle(CategoryDataset dataset, String region) {
        return String.format("Mean annual rainfall for %s-%s - %s",
                getMinYear(dataset), getMaxYear(dataset), region);
    }

    private String getMinYear(CategoryDataset dataset) {
        if(!dataset.getColumnKeys().isEmpty()) {
            return dataset.getColumnKeys().get(0).toString();
        } else {
            return "";
        }
    }

    private String getMaxYear(CategoryDataset dataset) {
        if(!dataset.getColumnKeys().isEmpty()) {
            return dataset.getColumnKeys().get(dataset.getColumnKeys().size()-1).toString();
        } else {
            return "";
        }
    }

}
