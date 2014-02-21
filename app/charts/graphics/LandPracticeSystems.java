package charts.graphics;

import graphics.GraphUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
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
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.GroupedStackedBarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.LegendTitle;
import org.jfree.data.KeyToGroupMap;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.Maps;

public abstract class LandPracticeSystems implements ManagementPracticeSystems {

  protected static final Color COLOR_A = new Color(0,118,70);
  protected static final Color COLOR_B = new Color(168,198,162); 
  protected static final Color COLOR_C = new Color(252,203,38);
  protected static final Color COLOR_D = new Color(233,44,48);
  protected static final Color COLOR_A_TRANS = new Color(0,118,70,90);
  protected static final Color COLOR_B_TRANS = new Color(168,198,162,90); 
  protected static final Color COLOR_C_TRANS = new Color(252,203,38,90);
  protected static final Color COLOR_D_TRANS = new Color(233,44,48,90);
  protected static final Color COLOR_CD = new Color(215, 136, 70);
  protected static final Color COLOR_CD_TRANS = new Color(215, 136, 70, 90);

    private static final Color AXIS_LABEL_COLOR = new Color(6, 76, 132);
    protected static final Font LEGEND_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font AXIS_LABEL_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);

    private static class BarLabelRenderer extends GroupedStackedBarRenderer {

        private boolean isSeriesComplete(CategoryDataset dataset, int row) {
          return (dataset.getRowCount()/2-1 == row) || (dataset.getRowCount()-1 == row);
        }

        private boolean isLastRow(CategoryDataset dataset, int row) {
          return dataset.getRowCount()-1 == row;
        }

        private boolean renderBarLabel(CategoryDataset dataset, int row, int column) {
          return dataset.getValue(row, column) != null;
        }

        @Override
        public void drawItem(Graphics2D g2, CategoryItemRendererState state,
                Rectangle2D dataArea, CategoryPlot plot,
                CategoryAxis domainAxis, ValueAxis rangeAxis,
                CategoryDataset dataset, int row, int column, int pass) {
            super.drawItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row,
                    column, pass);
            // after the stacked bar is completely rendered draw the glow text into it.
            if((pass == 2) && isSeriesComplete(dataset, row) && renderBarLabel(dataset, row, column)) {
                GraphUtils g = new GraphUtils(g2);
                String rowKey = dataset.getRowKey(row).toString();
                String colKey = dataset.getColumnKey(column).toString();
                String label = String.format("%s %s", colKey, getSeries(rowKey));
                double barW0 = calculateBarW0(plot, plot.getOrientation(), dataArea, domainAxis,
                        state, row, column);
                // centre the label
                double labelx = barW0 + state.getBarWidth()/2;
                double labely = (dataArea.getMinY()+dataArea.getHeight() / 2) -
                        (g.getBounds(label).getWidth() / 2);
                AffineTransform saveT = g2.getTransform();
                AffineTransform transform = new AffineTransform();
                // jfree chart seem to be using the transform on the Graphics2D object 
                // for scaling when the window gets very small or large
                // therefore we can not just overwrite the transform but have to factor it into 
                // our rotation and translation transformations.
                transform.concatenate(saveT);
                transform.concatenate(AffineTransform.getRotateInstance(-Math.PI/2, labelx, labely));
                g2.setTransform(transform);
                g2.setFont(LEGEND_FONT);
                g2.setColor(Color.black);
                g.drawGlowString(label, Color.white, 6, (int)labelx, (int)labely);
                g2.setTransform(saveT);
            }
            if((pass == 2) && isLastRow(dataset, row)) {
                // Workaround: because the dataArea sits on the the Axis the 0% gridline gets drawn 
                // over the category axis making it gray. To fix this as we draw another black line
                // to restore the black axis.
                g2.setColor(Color.black);
                g2.setStroke(new BasicStroke(2));
                g2.drawLine((int)dataArea.getMinX(), (int)dataArea.getMaxY(), (int)dataArea.getMaxX(), (int)dataArea.getMaxY());
                g2.drawLine((int)dataArea.getMinX(), (int)dataArea.getMinY(), (int)dataArea.getMinX(), (int)dataArea.getMaxY());
            }
        }
    }

    private Paint[] seriesPaint;

    public LandPracticeSystems(Paint... seriesPaint) {
      this.seriesPaint = seriesPaint;
    }
    
    private LegendTitle createLegend() {
      final LegendItemCollection legendItems = new LegendItemCollection();
      FontRenderContext frc = new FontRenderContext(null, true, true);
      GlyphVector gv = LEGEND_FONT.createGlyphVector(frc, new char[] {'X'});
      Shape shape = gv.getGlyphVisualBounds(0);
      for(Pair<String, Color> p : getLegend()) {
        LegendItem li = new LegendItem(p.getLeft(), null, null, null, shape, p.getRight());
        li.setLabelFont(LEGEND_FONT);
        legendItems.add(li);
      }
      LegendTitle legend = new LegendTitle(new LegendItemSource() {
        @Override
        public LegendItemCollection getLegendItems() {
          return legendItems;
        }});
      legend.setPosition(RectangleEdge.BOTTOM);
      return legend;
    }

    protected abstract List<Pair<String, Color>> getLegend();

    private NumberFormat percentFormatter() {
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(0);
        return percentFormat;
    }

    @Override
    public Drawable createChart(ADCDataset dataset, Dimension dimension) {
        JFreeChart chart = ChartFactory.createStackedBarChart(
            dataset.<String>get(Attribute.TITLE),  // chart title
            "",  // domain axis label
            "",  // range axis label
            dataset,                     // data
            PlotOrientation.VERTICAL,    // the plot orientation
            false,                       // legend
            false,                        // tooltips
            false                        // urls
        );
        chart.addLegend(createLegend());
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
        rangeAxis.setRange(0, 1);
        
        CategoryAxis cAxis = plot.getDomainAxis();
        cAxis.setTickMarksVisible(false);
        cAxis.setAxisLinePaint(Color.black);
        cAxis.setAxisLineStroke(new BasicStroke(2));
        cAxis.setLabel(dataset.<String>get(Attribute.DOMAIN_AXIS_LABEL));
        cAxis.setTickLabelsVisible(false);
        cAxis.setCategoryMargin(0.05);
        cAxis.setUpperMargin(0.1);
        cAxis.setLowerMargin(0);
        GroupedStackedBarRenderer renderer = new BarLabelRenderer();
        plot.setRenderer(renderer);
        for(int i=0;i<seriesPaint.length;i++) {
          renderer.setSeriesPaint(i, seriesPaint[i]);
        }
        renderer.setRenderAsPercentages(true);
        renderer.setDrawBarOutline(false);
        renderer.setBaseItemLabelsVisible(false);
        renderer.setShadowVisible(false);
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setItemMargin(0.10);
        renderer.setSeriesToGroupMap(createKeyToGroupMap(dataset));
        return new JFreeChartDrawable(chart, dimension);
    }

    private KeyToGroupMap createKeyToGroupMap(CategoryDataset dataset) {
        KeyToGroupMap map = new KeyToGroupMap();
        Map<String, String> groups = Maps.newHashMap();
        int groupCounter = 1;
        for(int i=0;i<dataset.getRowCount();i++) {
            String key = (String)dataset.getRowKey(i);
            String series = getSeries(key);
            String group = groups.get(series);
            if(group == null) {
                group = "G"+groupCounter++;
                groups.put(series, group);
            }
            map.mapKeyToGroup(key, group);
        }
        return map;
    }

    private static String getSeries(String rowKey) {
      return StringUtils.substringAfter(rowKey, "_");
    }

}
