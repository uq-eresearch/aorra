package charts.graphics;

import graphics.HatchedRectangle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.text.NumberFormat;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableMap;

public class GrazingPracticeSystems implements ManagementPracticeSystems {

    private static final Color COLOR_A = new Color(0,118,70);
    private static final Color COLOR_B = new Color(168,198,162); 
    private static final Color COLOR_C = new Color(252,203,38);
    private static final Color COLOR_D = new Color(233,44,48);
    private static final Color COLOR_A_TRANS = new Color(0,118,70,90);
    private static final Color COLOR_B_TRANS = new Color(168,198,162,90); 
    private static final Color COLOR_C_TRANS = new Color(252,203,38,90);
    private static final Color COLOR_D_TRANS = new Color(233,44,48,90);

    private static final Color AXIS_LABEL_COLOR = new Color(6, 76, 132);
    private static final Font TITLE_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font AXIS_LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font CAXIS_LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 12);

    private static class CustomBarRenderer extends BarRenderer {

        private static final Map<Pair<Integer, Integer>, Color> BAR_COLORS =
                new ImmutableMap.Builder<Pair<Integer, Integer>, Color>()
                    .put(Pair.of(0, 0), COLOR_A_TRANS)
                    .put(Pair.of(1, 0), COLOR_A)
                    .put(Pair.of(0, 1), COLOR_B_TRANS)
                    .put(Pair.of(1, 1), COLOR_B)
                    .put(Pair.of(0, 2), COLOR_C_TRANS)
                    .put(Pair.of(1, 2), COLOR_C)
                    .put(Pair.of(0, 3), COLOR_D_TRANS)
                    .put(Pair.of(1, 3), COLOR_D)
                    .build();

        // copied and modified from jfreechart-1.0.14 org.jfree.chart.renderer.category.BarRenderer
        private void drawItemInternal(Graphics2D g2,
                CategoryItemRendererState state,
                Rectangle2D dataArea,
                CategoryPlot plot,
                CategoryAxis domainAxis,
                ValueAxis rangeAxis,
                CategoryDataset dataset,
                int row,
                int column,
                int pass) {

            // nothing is drawn if the row index is not included in the list with
            // the indices of the visible rows...
            int visibleRow = state.getVisibleSeriesIndex(row);
            if (visibleRow < 0) {
                return;
            }
            // nothing is drawn for null values...
            Number dataValue = dataset.getValue(row, column);
            if (dataValue == null) {
                return;
            }

            final double value = dataValue.doubleValue();
            PlotOrientation orientation = plot.getOrientation();
            double barW0 = calculateBarW0(plot, orientation, dataArea, domainAxis,
                    state, visibleRow, column);
            double[] barL0L1 = calculateBarL0L1(value);
            if (barL0L1 == null) {
                return;  // the bar is not visible
            }

            RectangleEdge edge = plot.getRangeAxisEdge();
            double transL0 = rangeAxis.valueToJava2D(barL0L1[0], dataArea, edge);
            double transL1 = rangeAxis.valueToJava2D(barL0L1[1], dataArea, edge);

            // in the following code, barL0 is (in Java2D coordinates) the LEFT
            // end of the bar for a horizontal bar chart, and the TOP end of the
            // bar for a vertical bar chart.  Whether this is the BASE of the bar
            // or not depends also on (a) whether the data value is 'negative'
            // relative to the base value and (b) whether or not the range axis is
            // inverted.  This only matters if/when we apply the minimumBarLength
            // attribute, because we should extend the non-base end of the bar
            boolean positive = (value >= this.getBase());
            boolean inverted = rangeAxis.isInverted();
            double barL0 = Math.min(transL0, transL1);
            double barLength = Math.abs(transL1 - transL0);
            double barLengthAdj = 0.0;
            if (barLength > 0.0 && barLength < getMinimumBarLength()) {
                barLengthAdj = getMinimumBarLength() - barLength;
            }
            double barL0Adj = 0.0;
            RectangleEdge barBase;
            if (orientation == PlotOrientation.HORIZONTAL) {
                if (positive && inverted || !positive && !inverted) {
                    barL0Adj = barLengthAdj;
                    barBase = RectangleEdge.RIGHT;
                }
                else {
                    barBase = RectangleEdge.LEFT;
                }
            }
            else {
                if (positive && !inverted || !positive && inverted) {
                    barL0Adj = barLengthAdj;
                    barBase = RectangleEdge.BOTTOM;
                }
                else {
                    barBase = RectangleEdge.TOP;
                }
            }

            // draw the bar...
            RectangularShape bar = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
                bar = getBarShape(row, barL0 - barL0Adj, barW0,
                        barLength + barLengthAdj, state.getBarWidth());
            }
            else {
                bar = getBarShape(row, barW0, barL0 - barL0Adj,
                        state.getBarWidth(), barLength + barLengthAdj);
            }

            if (getShadowsVisible()) {
                this.getBarPainter().paintBarShadow(g2, this, row, column, bar, barBase,
                        true);
            }
            this.getBarPainter().paintBar(g2, this, row, column, bar, barBase);

//          FIXME
//            CategoryItemLabelGenerator generator = getItemLabelGenerator(row,
//                    column);
//            if (generator != null && isItemLabelVisible(row, column)) {
//                drawItemLabel(g2, dataset, row, column, plot, generator, bar,
//                        (value < 0.0));
//            }

            // submit the current data point as a crosshair candidate
            int datasetIndex = plot.indexOf(dataset);
            updateCrosshairValues(state.getCrosshairState(),
                    dataset.getRowKey(row), dataset.getColumnKey(column), value,
                    datasetIndex, barW0, barL0, orientation);

            // add an item entity, if this information is being collected
            EntityCollection entities = state.getEntityCollection();
            if (entities != null) {
                addItemEntity(entities, dataset, row, column, bar);
            }

        }

        @Override
        public void drawItem(Graphics2D g2, CategoryItemRendererState state,
                Rectangle2D dataArea, CategoryPlot plot,
                CategoryAxis domainAxis, ValueAxis rangeAxis,
                CategoryDataset dataset, int row, int column, int pass) {
            drawItemInternal(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row,
                    column, pass);
//            System.out.println(String.format("row %s, column %s, pass %s", row, column, pass));
            if((pass == 0) && (row == 1)&& (column == 3)) {
                // Workaround: because the dataArea sits on the the Axis the 0% gridline gets drawn 
                // over the category axis making it gray. To fix this as we draw another black line
                // to restore the black axis.
                g2.setColor(Color.black);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine((int)dataArea.getMinX(), (int)dataArea.getMaxY(), (int)dataArea.getMaxX(), (int)dataArea.getMaxY());
                g2.drawLine((int)dataArea.getMinX(), (int)dataArea.getMinY(), (int)dataArea.getMinX(), (int)dataArea.getMaxY());
            }
        }
        
        @Override
        public Paint getItemPaint(int row, int column) {
            return BAR_COLORS.get(Pair.of(row, column));
        }

        private RectangularShape getBarShape(int row,
                double x, double y, double width, double height) {
            if(row == 0) {
                return new HatchedRectangle(x, y, width, height, 5, 5);
            } else {
                return new Rectangle2D.Double(x, y, width, height);
            }
        }
    }

    private static LegendTitle createLegend(String legend1Text, String legend2Text) {
        final LegendItemCollection legendItems = new LegendItemCollection();
        FontRenderContext frc = new FontRenderContext(null, true, true);
        Font legenfont = new Font(Font.SANS_SERIF, Font.BOLD, 12);
        GlyphVector gv = legenfont.createGlyphVector(frc, new char[] {'X', 'X'});
        Shape shape = gv.getVisualBounds();
        Rectangle2D bounds = shape.getBounds2D();
        HatchedRectangle hatchShape = new HatchedRectangle(bounds.getX(), bounds.getY(),
                bounds.getWidth(), bounds.getHeight(), 5, 5);
        {
            LegendItem li = new LegendItem(legend1Text, null, null, null, hatchShape, Color.black);
            li.setLabelFont(legenfont);
            legendItems.add(li);

        }
        {
            LegendItem li = new LegendItem(legend2Text, null, null, null, shape, Color.black);
            li.setLabelFont(legenfont);
            legendItems.add(li);
        }
        LegendTitle legend = new LegendTitle(new LegendItemSource() {
            @Override
            public LegendItemCollection getLegendItems() {
                return legendItems;
            }});
        legend.setPosition(RectangleEdge.BOTTOM);
        legend.setMargin(new RectangleInsets(0,30,0,0));
        legend.setPadding(RectangleInsets.ZERO_INSETS);
        legend.setLegendItemGraphicPadding(new RectangleInsets(0,20,0,0));
        return legend;
    }

    private static NumberFormat percentFormatter() {
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(0);
        return percentFormat;
    }

    public Drawable createChart(ADCDataset dataset, Dimension dimension) {
        JFreeChart chart = ChartFactory.createBarChart(
                "",  // chart title
                "",  // domain axis label
                "",  // range axis label
                dataset,                     // data
                PlotOrientation.VERTICAL,    // the plot orientation
                false,                       // legend
                false,                        // tooltips
                false                        // urls
                );
        TextTitle textTitle = new TextTitle(dataset.<String>get(Attribute.TITLE), TITLE_FONT);
        textTitle.setPadding(new RectangleInsets(10,0,0,0));
        chart.setTitle(textTitle);
        
        chart.addLegend(createLegend(
                dataset.getRowKey(0).toString(), dataset.getRowKey(1).toString()));
        CategoryPlot plot = (CategoryPlot) chart.getPlot();
        plot.setBackgroundPaint(Color.white);
        plot.setOutlineVisible(false);
        plot.setAxisOffset(new RectangleInsets(0,0,0,0));
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(true);
        plot.setRangeGridlinePaint(Color.gray);
        plot.setRangeGridlineStroke(new BasicStroke(2));

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setAutoTickUnitSelection(true);
        rangeAxis.setTickUnit(new NumberTickUnit(0.2, percentFormatter()));
        rangeAxis.setAxisLineVisible(true);
        rangeAxis.setLabel(dataset.<String>get(Attribute.RANGE_AXIS_LABEL));
        rangeAxis.setAxisLineStroke(new BasicStroke(2));
        rangeAxis.setAxisLinePaint(Color.black);
        rangeAxis.setTickMarksVisible(false);
        rangeAxis.setLabelPaint(AXIS_LABEL_COLOR);
        rangeAxis.setLabelFont(AXIS_LABEL_FONT);
        rangeAxis.setLabelInsets(new RectangleInsets(0,0,0,0));
        rangeAxis.setUpperMargin(0);
        rangeAxis.setAutoRange(false);
        rangeAxis.setRange(0, 1);

        CategoryAxis cAxis = plot.getDomainAxis();
        cAxis.setTickMarksVisible(false);
        cAxis.setAxisLinePaint(Color.black);
        cAxis.setAxisLineStroke(new BasicStroke(2));
        cAxis.setLabel(dataset.<String>get(Attribute.DOMAIN_AXIS_LABEL));
        cAxis.setTickLabelsVisible(true);
        cAxis.setUpperMargin(0.05);
        cAxis.setLowerMargin(0.05);
        cAxis.setTickLabelFont(CAXIS_LABEL_FONT);
        cAxis.setTickLabelPaint(Color.black);
        CustomBarRenderer renderer = new CustomBarRenderer();
        plot.setRenderer(renderer);
        renderer.setDrawBarOutline(false);
        renderer.setBaseItemLabelsVisible(false);
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setMaximumBarWidth(0.08);
        renderer.setItemMargin(0.01);
        return new JFreeChartDrawable(chart, dimension);
    }
}

