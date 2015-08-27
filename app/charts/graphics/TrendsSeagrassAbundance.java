package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.statistics.StatisticalCategoryDataset;
import org.jfree.text.TextBlock;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleEdge;

import boxrenderer.ContentBoxImpl;
import boxrenderer.TableBox;
import boxrenderer.TableCellBox;
import boxrenderer.TableRowBox;
import boxrenderer.TextBox;
import charts.Drawable;
import charts.jfree.ADSCDataset;
import charts.jfree.Attribute;

public class TrendsSeagrassAbundance {

  // This renderer does not break the line in case the previous data point is missing but instead 
  // searches the dataset until it finds a previous value further back.
  // Some code was copied from StatisticalLineAndShapeRenderer::drawItem to make this work.
  private static class SLSRenderer extends StatisticalLineAndShapeRenderer {

    private Font legendFont;

    public SLSRenderer(Font legendFont) {
      this.legendFont = legendFont;
    }

    private Integer findPreviousIndex(StatisticalCategoryDataset statDataset, int row, int column) {
      for(int i = column - 1; i >=0;i--) {
        if(statDataset.getValue(row, i) != null) {
          return i;
        }
      }
      return null;
    }

    private void drawLegend(Graphics2D g2, Rectangle2D dataArea) {
      Graphics2D g0 = null;
      try {
        TableBox legend = new TableBox();
        legend.getMargin().setRight(5);
        for(int i = 0; i < 2;i++) {
          ContentBoxImpl c = new ContentBoxImpl();
          c.setBackground(this.getSeriesPaint(i));
          c.setWidth(25);
          c.setHeight(2);
          c.getMargin().setRight(5);
          TableCellBox legendLineBox = new TableCellBox(c);
          TableCellBox labelBox = new TableCellBox(
              new TextBox(i==0?"Intertidal":"Subtidal", legendFont));
          legend.addRow(new TableRowBox(legendLineBox, labelBox));
        }
        Dimension d = legend.getDimension(g2);
        g0 = (Graphics2D)g2.create((int)(dataArea.getMaxX()-d.getWidth()), 80, d.width, d.height);
        legend.render(g0);
      } catch(Exception e) {
        e.printStackTrace();
      } finally {
        if(g0 != null) {
          g0.dispose();
        }
      }
    }

    private void _drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
        CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset,
        int row, int column, int pass) {
      StatisticalCategoryDataset statDataset = (StatisticalCategoryDataset) dataset;
      int visibleRow = state.getVisibleSeriesIndex(row);
      int visibleRowCount = state.getVisibleSeriesCount();
      PlotOrientation orientation = plot.getOrientation();
      // current data point...
      double x1;
      if (getUseSeriesOffset()) {
          x1 = domainAxis.getCategorySeriesMiddle(column,
                  dataset.getColumnCount(),
                  visibleRow, visibleRowCount,
                  getItemMargin(), dataArea, plot.getDomainAxisEdge());
      }
      else {
          x1 = domainAxis.getCategoryMiddle(column, getColumnCount(),
                  dataArea, plot.getDomainAxisEdge());
      }
      Number meanValue = statDataset.getMeanValue(row, column);
      if (meanValue == null) {
        return;
      }
      double y1 = rangeAxis.valueToJava2D(meanValue.doubleValue(), dataArea,
              plot.getRangeAxisEdge());
      if (pass == 0 && getItemLineVisible(row, column)) {
        if (column != 0) {
          Number previousValue = statDataset.getValue(row, column - 1);
          if(previousValue != null) {
            // ignore this case here because the call to super.drawItem(...) took care if it.
            return;
          } else {
            // previous data point...
            Integer pIdx = findPreviousIndex(statDataset, row, column);
            if(pIdx == null) {
              return;
            }
            previousValue = statDataset.getValue(row, pIdx.intValue());
            double previous = previousValue.doubleValue();
            double x0;
            if (getUseSeriesOffset()) {
              x0 = domainAxis.getCategorySeriesMiddle(
                  pIdx.intValue(), dataset.getColumnCount(),
                  visibleRow, visibleRowCount,
                  getItemMargin(), dataArea,
                  plot.getDomainAxisEdge());
            }
            else {
              x0 = domainAxis.getCategoryMiddle(pIdx.intValue(),
                  getColumnCount(), dataArea,
                  plot.getDomainAxisEdge());
            }
            double y0 = rangeAxis.valueToJava2D(previous, dataArea,
                plot.getRangeAxisEdge());

            Line2D line = null;
            if (orientation == PlotOrientation.HORIZONTAL) {
              line = new Line2D.Double(y0, x0, y1, x1);
            }
            else if (orientation == PlotOrientation.VERTICAL) {
              line = new Line2D.Double(x0, y0, x1, y1);
            }
            g2.setPaint(getItemPaint(row, column));
            g2.setStroke(getItemStroke(row, column));
            g2.draw(line);
          }
        }
      }
    }

    public void drawItem(Graphics2D g2, CategoryItemRendererState state, Rectangle2D dataArea,
        CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryDataset dataset,
        int row, int column, int pass) {
      super.drawItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column, pass);
      _drawItem(g2, state, dataArea, plot, domainAxis, rangeAxis, dataset, row, column, pass);
      // draw the legend last and only if the chart contains the subtidal series (row == 1)
      if((row == 1) && (pass == 1) && (column == dataset.getColumnCount()-1)) {
        drawLegend(g2, dataArea);
      }
    }
  }

    public static Drawable createChart(final ADSCDataset dataset, Dimension dimension) {
        JFreeChart chart = ChartFactory.createLineChart(
                dataset.get(Attribute.TITLE),  // title
                "",             // x-axis label
                dataset.get(Attribute.Y_AXIS_LABEL),   // y-axis label
                dataset,            // data
                PlotOrientation.VERTICAL,
                false,               // create legend?
                false,               // generate tooltips?
                false               // generate URLs?
            );
        chart.setBackgroundPaint(Color.white);
        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        CategoryAxis caxis = plot.getDomainAxis();
        StatisticalLineAndShapeRenderer renderer = new SLSRenderer(caxis.getTickLabelFont());
        renderer.setErrorIndicatorPaint(ErrorIndicator.ERROR_INDICATOR_COLOR);
        renderer.setDrawOutlines(true);
        renderer.setUseOutlinePaint(true);
        renderer.setBaseOutlinePaint(Color.black);
        plot.setRenderer(renderer);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setRangeGridlineStroke(new BasicStroke(1.0f));
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Color.white);
        renderer.setSeriesPaint(0, dataset.get(Attribute.SERIES_COLOR));
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickUnit(new NumberTickUnit(10));
        rangeAxis.setRange(0.0, 100.0);
        rangeAxis.setTickMarksVisible(false);
        chart.getTitle().setFont(rangeAxis.getLabelFont());
        CategoryAxis cAxis = new CategoryAxis() {
            @SuppressWarnings("rawtypes")
            @Override
            protected TextBlock createLabel(Comparable category, float width,
                    RectangleEdge edge, Graphics2D g2) {
                int mod = dataset.getColumnCount() / 25 + 1;
                int col = dataset.getColumnIndex(category);
                String label = "";
                if(col % mod == 0) {
                    label = category.toString();
                }
                return TextUtilities.createTextBlock(label,
                        getTickLabelFont(category), getTickLabelPaint(category));
            }
        };
        cAxis.setLabel(dataset.get(Attribute.X_AXIS_LABEL));
        cAxis.setLabelFont(rangeAxis.getLabelFont());
        cAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
        plot.setDomainAxis(cAxis);
        chart.addLegend(ErrorIndicatorLegend.createLegend());
        return new JFreeChartDrawable(chart, dimension);
    }

}
