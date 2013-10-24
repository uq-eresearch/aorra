package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.StandardChartTheme;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.GradientBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import charts.Drawable;
import charts.graphics.AutoSubCategoryAxis.Border;

import com.google.common.collect.ImmutableMap;

public class PSIIMaxHeq {
    
    public static final String SEPARATOR = AutoSubCategoryAxis.DEFAULT_SEPARATOR;

    private static final String RANGE_AXIS_LABEL = "Max PSII Heq ng/L";

    public static enum Condition {

        NOT_EVALUATED("Not evaluated", new Color(229,229,229)),
        VERY_POOR("Very poor", new Color(233,44,48)),
        POOR("Poor", new Color(244,141,64)),
        MODERATE("Moderate", new Color(252,203,38)),
        GOOD("Good", new Color(168,198,162)),
        VERY_GOOD("Very good", new Color(0,118,70));

        private final String label;
        private final Color color;

        Condition(String label, Color color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public Color getColor() {
            return color;
        }

      }

    private static class Renderer extends BarRenderer {

        private CategoryDataset dataset;

        public Renderer(CategoryDataset dataset) {
            this.dataset = dataset;
        }

        @Override
        public void drawItem(Graphics2D g2, CategoryItemRendererState state,
                Rectangle2D dataArea, CategoryPlot plot,
                CategoryAxis domainAxis, ValueAxis rangeAxis,
                CategoryDataset dataset, int row, int column, int pass) {
            super.drawItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row,
                    column, pass);
            double barW0 = calculateBarW0(plot, plot.getOrientation(), dataArea, domainAxis,
                    state, state.getVisibleSeriesIndex(row), column);
            Number dataValue = dataset.getValue(row, column);
            if (dataValue == null) {
                return;
            }
            final double value = dataValue.doubleValue();
            if(rangeAxis instanceof PartitionedNumberAxis) {
                Range r = new Range(0, value);
                ((PartitionedNumberAxis)rangeAxis).drawPatitionBoundaries(
                        g2, r, dataArea, state.getBarWidth(), barW0);
            }
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

        @Override
        protected double calculateBarW0(CategoryPlot plot,
                PlotOrientation orientation, Rectangle2D dataArea,
                CategoryAxis domainAxis, CategoryItemRendererState state,
                int row, int column) {
            return domainAxis.getCategoryMiddle(column, getColumnCount(),
                    dataArea, plot.getDomainAxisEdge()) - state.getBarWidth() / 2.0;
        }

        @Override
        public Paint getItemPaint(int row, int column) {
            Condition c = (Condition)dataset.getRowKey(row);
            return c.getColor();
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
                vAxis.addPartition(new PartitionedNumberAxis.Partition(new Range(0,55.0),0.5));
                vAxis.addPartition(new PartitionedNumberAxis.Partition(new Range(85.0,500.0),0.5));
            }
        }
        {
            BarRenderer renderer = (BarRenderer)plot.getRenderer();
            renderer.setBarPainter(new GradientBarPainter(0.0,0.0,0.5));
        }
        {
            Font f = plot.getRangeAxis().getTickLabelFont();
            AutoSubCategoryAxis cAxis = (AutoSubCategoryAxis)plot.getDomainAxis();
            cAxis.setLowerMargin(0.01);
            cAxis.setUpperMargin(0.01);
            cAxis.setItemMargin(0, 1.0);
            cAxis.setItemMargin(1, 1.0);
            cAxis.setItemMargin(2, 0.10);
            // TODO would be nice if the AutoSubCategoryAxis could do the wrapping automatically
            ImmutableMap<String, String> labels = new ImmutableMap.Builder<String, String>()
                .put("Low Isles", "Low\nIsles")
                .put("Green Island", "Green\nIsland")
                .put("Fitzroy Island", "Fitzroy\nIsland")
                .put("Normanby Island", "Normanby\nIsland")
                .put("Dunk Island", "Dunk\nIsland")
                .put("Orpheus Island", "Orpheus\nIsland")
                .put("Magnetic Island", "Magnetic\nIsland")
                .put("Cape Cleveland", "Cape\nCleveland")
                .put("Pioneer Bay", "Pioneer\nBay")
                .put("Outer Whitsunday", "Outer\nWhitsunday")
                .put("Sarina Inlet", "Sarina\nInlet")
                .put("North Keppel Island", "North\nKeppel Is")
                .build();
            cAxis.setCategoryLabelConfig(0, new AutoSubCategoryAxis.CategoryLabelConfig(
                    f,2,2, Border.ALL, Color.black));
            cAxis.setCategoryLabelConfig(1, new AutoSubCategoryAxis.CategoryLabelConfig(
                    f,2,2, Border.BETWEEN, Color.lightGray, labels));
            cAxis.setCategoryLabelConfig(2, new AutoSubCategoryAxis.CategoryLabelConfig(
                    CategoryLabelPositions.UP_90,f, Color.black,2.0,2.0));
        }
        return new JFreeChartDrawable(chart, dimension);
    }

    private static JFreeChart createStackBarChart(CategoryDataset dataset,String title) {
        AutoSubCategoryAxis categoryAxis = new AutoSubCategoryAxis(dataset);
        CategoryDataset fixedDataset = categoryAxis.getFixedDataset();
        PartitionedNumberAxis vAxis = new PartitionedNumberAxis(RANGE_AXIS_LABEL);
        CategoryPlot plot = new CategoryPlot(fixedDataset,
                categoryAxis, vAxis, new Renderer(fixedDataset));
        plot.setOrientation(PlotOrientation.VERTICAL);
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        final LegendItemCollection items = new LegendItemCollection();
        for(Condition c : Condition.values()) {
            if(c != Condition.NOT_EVALUATED) {
                items.add(new LegendItem(c.getLabel(), null, null, null,
                        new Rectangle2D.Double(-6.0, -6.0, 10.0,10.0), c.getColor()));
            }
        }
        LegendTitle legend = new LegendTitle(new LegendItemSource() {
            @Override
            public LegendItemCollection getLegendItems() {
                return items;
            }});
        legend.setMargin(new RectangleInsets(1.0, 1.0, 1.0, 1.0));
        legend.setBackgroundPaint(Color.white);
        legend.setPosition(RectangleEdge.BOTTOM);
        chart.addLegend(legend);
        new StandardChartTheme("JFree").apply(chart);
        return chart;
    }

}
