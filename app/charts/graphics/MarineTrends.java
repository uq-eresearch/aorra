package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DefaultDrawingSupplier;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import charts.Drawable;
import charts.graphics.BeerCoaster.Condition;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

public class MarineTrends {

  private static class MarineTrendsRenderer extends LineAndShapeRenderer {

    @Override
    public void drawBackground(Graphics2D g2, CategoryPlot plot,
        Rectangle2D dataArea) {
      super.drawBackground(g2, plot, dataArea);
      NumberAxis raxis = (NumberAxis)plot.getRangeAxis();
      int value = 100;
      for(Condition c : BeerCoaster.Condition.values()) {
        if(c == Condition.NOT_EVALUATED) {
          continue;
        }
        drawCondition(g2, c.getLabel(),
            raxis.valueToJava2D(value-20, dataArea, RectangleEdge.LEFT),
            raxis.valueToJava2D(value, dataArea, RectangleEdge.LEFT),
            c.getColor(), dataArea);
        value-=20;
      }
    }

    private void drawCondition(Graphics2D g2, String text,
        double y0, double y1, Color background, Rectangle2D dataArea) {
      double width = 20;
      double height = Math.abs(y1-y0);
      double yCenter = y1+height/2;
      double xCenter = dataArea.getMinX()+width/2;
      g2.setColor(background);
      g2.fill(new Rectangle2D.Double(dataArea.getMinX(), Math.min(y0, y1), width, height));
      g2.setColor(Color.black);
      TextUtilities.drawRotatedString(text, g2, (float)xCenter, (float)yCenter,
          TextAnchor.CENTER, -Math.PI/2, (float)xCenter, (float)yCenter);
    }
  }

    public static Drawable createChart(final ADCDataset dataset, Dimension dimension) {
        final JFreeChart chart = ChartFactory.createLineChart(
                dataset.get(Attribute.TITLE),
                dataset.get(Attribute.X_AXIS_LABEL),
                dataset.get(Attribute.Y_AXIS_LABEL),
                dataset,
                PlotOrientation.VERTICAL,
                true, false, false);
        final CategoryPlot plot = chart.getCategoryPlot();
        plot.setDrawingSupplier(drawingSupplier());
        plot.setBackgroundPaint(Color.white);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinePaint(Color.gray);
        plot.setRangeGridlineStroke(new BasicStroke(1));
        NumberAxis raxis = (NumberAxis)plot.getRangeAxis();
        raxis.setRange(0, 100.0);
        raxis.setTickUnit(new NumberTickUnit(20));
        MarineTrendsRenderer renderer = new MarineTrendsRenderer();
        plot.setRenderer(renderer);
        for(int i=0;i<dataset.get(Attribute.SERIES_COLORS).length;i++) {
          renderer.setSeriesPaint(i, dataset.get(Attribute.SERIES_COLORS)[i]);
        }
        renderer.setUseOutlinePaint(false);
        renderer.setBaseShapesVisible(true);
        return new JFreeChartDrawable(chart, dimension);
    }

    private static DrawingSupplier drawingSupplier() {
      Stroke[] strokes = new Stroke[] { stroke(), stroke(5,5),
          stroke(20,5,5,5), stroke(10,10), stroke(20,5,5,5,5,5)};
      return new DefaultDrawingSupplier(
          DefaultDrawingSupplier.DEFAULT_PAINT_SEQUENCE,
          DefaultDrawingSupplier.DEFAULT_FILL_PAINT_SEQUENCE,
          DefaultDrawingSupplier.DEFAULT_OUTLINE_PAINT_SEQUENCE,
          strokes,
          DefaultDrawingSupplier.DEFAULT_OUTLINE_STROKE_SEQUENCE,
          DefaultDrawingSupplier.DEFAULT_SHAPE_SEQUENCE);
    }

    private static Stroke stroke(float... dash) {
      return new BasicStroke(1.0f, BasicStroke.CAP_SQUARE,BasicStroke.JOIN_BEVEL,
          1f, dash.length > 0?dash:null, 0f);
    }

}
