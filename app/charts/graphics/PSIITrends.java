package charts.graphics;

import static charts.graphics.Colors.BLACK;
import static charts.graphics.Colors.BLUE;
import static charts.graphics.Colors.BROWN;
import static charts.graphics.Colors.GREEN;
import static charts.graphics.Colors.LIGHT_BLUE;
import static charts.graphics.Colors.LIGHT_BROWN;
import static charts.graphics.Colors.LIGHT_PURPLE;
import static charts.graphics.Colors.ORANGE;
import static charts.graphics.Colors.PINK;
import static charts.graphics.Colors.RED;
import static charts.graphics.Colors.YELLOW;

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
import org.jfree.chart.block.GridArrangement;
import org.jfree.chart.entity.EntityCollection;
import org.jfree.chart.labels.CategoryItemLabelGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.StackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.DataUtilities;
import org.jfree.data.Range;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import svg.AorraSvgGraphics2D;
import charts.Drawable;
import charts.graphics.AutoSubCategoryAxis.Border;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableMap;

public class PSIITrends {

    public static final String SEPARATOR = AutoSubCategoryAxis.DEFAULT_SEPARATOR;

    private static final Paint[] SERIES_PAINT = new Paint[] {
        PINK, LIGHT_BROWN, BLUE, LIGHT_PURPLE, YELLOW, BROWN, ORANGE, LIGHT_BLUE, GREEN, RED, BLACK };

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
                if(g2 instanceof AorraSvgGraphics2D) {
                    g2.setPaint(getItemPaint(row, column));
                    ((AorraSvgGraphics2D) g2).fillWithTooltip(bar, String.format("%s %s ng/L",
                            dataset.getRowKey(row).toString(), value));
                } else {
                    getBarPainter().paintBar(g2, this, row, column, bar, barBase);
                }

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
    
    public static Drawable createChart(ADCDataset dataset, Dimension dimension) {
        JFreeChart chart = createStackBarChart(dataset, dataset.get(Attribute.TITLE));
        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        {
            plot.setBackgroundPaint(Color.white);
            plot.setRangeGridlinesVisible(true);
            plot.setRangeGridlinePaint(Color.lightGray);
            plot.setRangeGridlineStroke(new BasicStroke(1.0f));
        }
        {
            // TODO the partition range should be determined automatically!
            if(plot.getRangeAxis() instanceof PartitionedNumberAxis) {
                PartitionedNumberAxis vAxis = (PartitionedNumberAxis)plot.getRangeAxis();
                vAxis.addPartition(new PartitionedNumberAxis.Partition(new Range(0,110.0),1.0/3.0*2.0));
                vAxis.addPartition(new PartitionedNumberAxis.Partition(new Range(120.0,750.0),1.0/3.0));
            }
        }
        {
            StackedBarRenderer renderer = (StackedBarRenderer)plot.getRenderer();
            Paint[] sp = seriesPaint(dataset);
            for(int i=0;i<sp.length;i++) {
              if(sp[i] != null) {
                renderer.setSeriesPaint(i, sp[i]);
              }
            }
            renderer.setBarPainter(new StandardBarPainter());
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
        {
            final LegendItemCollection items = new LegendItemCollection();
            int i = 0;
            for(Object p : dataset.getRowKeys()) {
                items.add(new LegendItem(p.toString(), null, null, null,
                        new Rectangle2D.Double(-6.0, -6.0, 10.0,10.0),
                        plot.getRenderer().getSeriesPaint(i++)));
            }
            LegendTitle legend = new LegendTitle(new LegendItemSource() {
                @Override
                public LegendItemCollection getLegendItems() {
                    return items;
                }}, new GridArrangement(dataset.getRowCount()/6+1,6), null);
            legend.setMargin(new RectangleInsets(1.0, 50.0, 1.0, 1.0));
            legend.setBackgroundPaint(Color.white);
            legend.setPosition(RectangleEdge.BOTTOM);
            chart.addLegend(legend);
        }
        return new JFreeChartDrawable(chart, dimension);
    }

    private static Paint[] seriesPaint(ADCDataset dataset) {
      Color[] colors = dataset.get(Attribute.SERIES_COLORS);
      if(colors == null) {
        return SERIES_PAINT;
      }
      Paint[] p = new Paint[colors.length];
      for(int i=0;i<colors.length;i++) {
        if(colors[i] == null) {
          if(i < SERIES_PAINT.length) {
            p[i] = SERIES_PAINT[i];
          }
        } else {
          p[i] = colors[i];
        }
      }
      return p;
    }

    private static JFreeChart createStackBarChart(ADCDataset dataset,String title) {
        AutoSubCategoryAxis dAxis = new AutoSubCategoryAxis(dataset);
        dAxis.setCategoryLabelPositionOffset(0);
        dAxis.setLabel(dataset.get(Attribute.X_AXIS_LABEL));
        PartitionedNumberAxis vAxis = new PartitionedNumberAxis(
            dataset.get(Attribute.Y_AXIS_LABEL));
        CategoryPlot plot = new CategoryPlot(dAxis.getFixedDataset(), dAxis, vAxis, new Renderer());
        plot.setOrientation(PlotOrientation.VERTICAL);
        JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        new StandardChartTheme("JFree").apply(chart);
        return chart;
    }
}
