package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.data.category.CategoryDataset;

import charts.Drawable;
import charts.graphics.AutoSubCategoryAxis.Border;

public class PSIITrends {

    public static final String SEPARATOR = AutoSubCategoryAxis.DEFAULT_SEPARATOR;

    private static final Color BLUE = new Color(30, 172, 226);
    private static final Color GREEN = new Color(51,151,79);
    private static final Color GRAY = new Color(137,137,137);
    private static final Color BROWN = new Color(146, 116, 80);
    private static final Color VIOLET = new Color(50, 52, 109);
    private static final Color YELLOW = new Color(246, 219, 68);
    private static final Color RED = new Color(187, 34, 51);

    private static final Paint[] SERIES_PAINT = new Paint[] {
        BLUE, GREEN, GRAY, BROWN, VIOLET, YELLOW, RED };

    private static class Renderer extends StackedBarRenderer {

        public Renderer() {
            setShadowVisible(false);
        }

        @Override
        protected void calculateBarWidth(CategoryPlot plot,
                Rectangle2D dataArea,
                int rendererIndex,
                CategoryItemRendererState state) {
            CategoryAxis xAxis = plot.getDomainAxisForDataset(rendererIndex);
            if(xAxis instanceof AutoSubCategoryAxis) {
                state.setBarWidth(((AutoSubCategoryAxis)xAxis).calculateCategorySize(dataArea));
            } else {
                super.calculateBarWidth(plot, dataArea, rendererIndex, state);
            }
        }
    }
    
    public static Drawable createChart(CategoryDataset dataset, String title, Dimension dimension) {
        JFreeChart chart = ChartFactory.createStackedBarChart(
            title,  // chart title
            "",  // domain axis label
            "Maxium photosystem II herbicide equivalent concentrations ng/L",  // range axis label
            dataset,                     // data
            PlotOrientation.VERTICAL,    // the plot orientation
            false,                       // legend
            false,                        // tooltips
            false                        // urls
        );
        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        //plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setRangeGridlineStroke(new BasicStroke(1.0f));
        plot.setRenderer(new Renderer());
        ValueAxis vAxis = plot.getRangeAxis();
        Font f = vAxis.getTickLabelFont();
        StackedBarRenderer renderer = (StackedBarRenderer)plot.getRenderer();
        for(int i=0;i<SERIES_PAINT.length;i++) {
            renderer.setSeriesPaint(i, SERIES_PAINT[i]);
        }
        AutoSubCategoryAxis cAxis = new AutoSubCategoryAxis(dataset); 
        cAxis.setLowerMargin(0.01);
        cAxis.setUpperMargin(0.01);
        cAxis.setItemMargin(0, 1.0);
        cAxis.setItemMargin(1, 1.0);
        cAxis.setItemMargin(2, 0.10);
        cAxis.setCategoryLabelConfig(0, new AutoSubCategoryAxis.CategoryLabelConfig(
                f,2,2, Border.ALL, Color.black));
        cAxis.setCategoryLabelConfig(1, new AutoSubCategoryAxis.CategoryLabelConfig(
                f,2,2, Border.BETWEEN, Color.lightGray));
        cAxis.setCategoryLabelConfig(2, new AutoSubCategoryAxis.CategoryLabelConfig(
                CategoryLabelPositions.UP_90,f, Color.black,2.0,2.0));
        plot.setDomainAxis(cAxis);
        renderer.setBarPainter(new StandardBarPainter());
        return new JFreeChartDrawable(chart, dimension);
    }

}
