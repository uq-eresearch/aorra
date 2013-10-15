package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.GradientBarPainter;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.data.DataUtilities;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;

import charts.Drawable;
import charts.graphics.AutoSubCategoryAxis.Border;

public class PSIITrends {

    private static final String RANGE_AXIS_LABEL =
            "Maxium photosystem II herbicide equivalent concentrations ng/L";

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
            setDrawBarOutline(false);
            setShadowVisible(false);
        }

        // copied from superclass and added another pass that adds markers where the bar crosses
        // partition boundaries
        @Override
        public void drawItem(Graphics2D g2, CategoryItemRendererState state,
                Rectangle2D dataArea, CategoryPlot plot,
                CategoryAxis domainAxis, ValueAxis rangeAxis,
                CategoryDataset dataset, int row, int column, int pass) {

            if (!isSeriesVisible(row)) {
                return;
            }

            // nothing is drawn for null values...
            Number dataValue = dataset.getValue(row, column);
            if (dataValue == null) {
                return;
            }

            double value = dataValue.doubleValue();
            double total = 0.0;  // only needed if calculating percentages
            if (this.getRenderAsPercentages()) {
                total = DataUtilities.calculateColumnTotal(dataset, column,
                        state.getVisibleSeriesArray());
                value = value / total;
            }

            PlotOrientation orientation = plot.getOrientation();
            double barW0 = domainAxis.getCategoryMiddle(column, getColumnCount(),
                    dataArea, plot.getDomainAxisEdge())
                    - state.getBarWidth() / 2.0;

            double positiveBase = getBase();
            double negativeBase = positiveBase;

            for (int i = 0; i < row; i++) {
                Number v = dataset.getValue(i, column);
                if (v != null && isSeriesVisible(i)) {
                    double d = v.doubleValue();
                    if (this.getRenderAsPercentages()) {
                        d = d / total;
                    }
                    if (d > 0) {
                        positiveBase = positiveBase + d;
                    }
                    else {
                        negativeBase = negativeBase + d;
                    }
                }
            }

            double translatedBase;
            double translatedValue;
            boolean positive = (value > 0.0);
            boolean inverted = rangeAxis.isInverted();
            RectangleEdge barBase;
            if (orientation == PlotOrientation.HORIZONTAL) {
                if (positive && inverted || !positive && !inverted) {
                    barBase = RectangleEdge.RIGHT;
                }
                else {
                    barBase = RectangleEdge.LEFT;
                }
            }
            else {
                if (positive && !inverted || !positive && inverted) {
                    barBase = RectangleEdge.BOTTOM;
                }
                else {
                    barBase = RectangleEdge.TOP;
                }
            }

            RectangleEdge location = plot.getRangeAxisEdge();
            if (positive) {
                translatedBase = rangeAxis.valueToJava2D(positiveBase, dataArea,
                        location);
                translatedValue = rangeAxis.valueToJava2D(positiveBase + value,
                        dataArea, location);
            }
            else {
                translatedBase = rangeAxis.valueToJava2D(negativeBase, dataArea,
                        location);
                translatedValue = rangeAxis.valueToJava2D(negativeBase + value,
                        dataArea, location);
            }
            double barL0 = Math.min(translatedBase, translatedValue);
            double barLength = Math.max(Math.abs(translatedValue - translatedBase),
                    getMinimumBarLength());

            Rectangle2D bar = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                bar = new Rectangle2D.Double(barL0, barW0, barLength,
                        state.getBarWidth());
            }
            else {
                bar = new Rectangle2D.Double(barW0, barL0, state.getBarWidth(),
                        barLength);
            }
            if (pass == 0) {
                if (getShadowsVisible()) {
                    boolean pegToBase = (positive && (positiveBase == getBase()))
                            || (!positive && (negativeBase == getBase()));
                    getBarPainter().paintBarShadow(g2, this, row, column, bar,
                            barBase, pegToBase);
                }
            }
            else if (pass == 1) {
                getBarPainter().paintBar(g2, this, row, column, bar, barBase);

                // add an item entity, if this information is being collected
                EntityCollection entities = state.getEntityCollection();
                if (entities != null) {
                    addItemEntity(entities, dataset, row, column, bar);
                }
            }
            else if (pass == 2) {
                CategoryItemLabelGenerator generator = getItemLabelGenerator(row,
                        column);
                if (generator != null && isItemLabelVisible(row, column)) {
                    drawItemLabel(g2, dataset, row, column, plot, generator, bar,
                            (value < 0.0));
                }
            } else if(pass == 3) {
                if(rangeAxis instanceof PartitionedNumberAxis) {
                    Range r = new Range(positiveBase, positiveBase+value);
                    ((PartitionedNumberAxis)rangeAxis).drawPatitionBoundaries(
                            g2, r, dataArea, state.getBarWidth(), barW0);
                }
            }
        }

        @Override
        public int getPassCount() {
            return 4;
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
        JFreeChart chart = createStackBarChart(dataset, title);
        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        {
            plot.setBackgroundPaint(Color.white);
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.lightGray);
            plot.setRangeGridlineStroke(new BasicStroke(1.0f));
        }
        {
            if(plot.getRangeAxis() instanceof PartitionedNumberAxis) {
                PartitionedNumberAxis vAxis = (PartitionedNumberAxis)plot.getRangeAxis();
                // TODO create partitions from dataset
                vAxis.addPartition(new PartitionedNumberAxis.Partition(new Range(0,55), 0.75));
                vAxis.addPartition(new PartitionedNumberAxis.Partition(new Range(95, 105), 0.125));
                vAxis.addPartition(new PartitionedNumberAxis.Partition(new Range(375, 550), 0.125));
            }
        }
        {
            StackedBarRenderer renderer = (StackedBarRenderer)plot.getRenderer();
            for(int i=0;i<SERIES_PAINT.length;i++) {
                renderer.setSeriesPaint(i, SERIES_PAINT[i]);
            }
            renderer.setBarPainter(new GradientBarPainter(0.5,0.1,0.4));
        }
        {
            Font f = plot.getRangeAxis().getTickLabelFont();
            AutoSubCategoryAxis cAxis = (AutoSubCategoryAxis)plot.getDomainAxis();
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
        }
        return new JFreeChartDrawable(chart, dimension);
    }

    private static JFreeChart createStackBarChart(CategoryDataset dataset,String title) {
        CategoryAxis categoryAxis = new AutoSubCategoryAxis(dataset);
        PartitionedNumberAxis vAxis = new PartitionedNumberAxis(RANGE_AXIS_LABEL);
        CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, vAxis, new Renderer());
        plot.setOrientation(PlotOrientation.VERTICAL);
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        new StandardChartTheme("JFree").apply(chart);
        return chart;
    }

}
