package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.text.NumberFormat;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRendererState;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.CategoryDataset;
import org.jfree.ui.RectangleInsets;

import boxrenderer.ContentBoxImpl;
import boxrenderer.TableBox;
import boxrenderer.TableCellBox;
import boxrenderer.TableRowBox;
import boxrenderer.TextBox;
import charts.ChartType;
import charts.Drawable;
import charts.jfree.ADCDataset;
import charts.jfree.Attribute;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class TrackingTowardsTargets {

    private static final Color GRADIENT_START = new Color(227, 246, 253);
    private static final Color GRADIENT_END = Color.white;

    private static final Color SERIES_1_COLOR = new Color(30, 172, 226);
    private static final Color SERIES_2_COLOR = new Color(187, 34, 51);

    private static final float LINE_WIDTH = 8.0f;
    private static final double ARROWHEAD_EDGE_LENGTH = 30.0;

    private static double sqr(double a) {
        return a*a;
    }

    private static Point2D rotate(double x, double y, double a) {
      double x2 = x * Math.cos(a) - y * Math.sin(a);
      double y2 = x * Math.sin(a) + y * Math.cos(a);
      return new Point2D.Double(x2, y2);
    }

    private static Point2D translate(Point2D point, double x, double y) {
        return new Point2D.Double(point.getX() + x, point.getY() + y);
    }

    private static double gradient(Point2D p1, Point2D p2) {
        return Math.atan((p2.getY() - p1.getY()) / (p2.getX() - p1.getX()));
    }

    private static class LineSection {

        private Point2D w1Start;
        private Point2D w1End;

        private Point2D w2Start;
        private Point2D w2End;

        private boolean first;

        public LineSection(Point2D p1, Point2D p2, boolean first, boolean last, double lh) {
            this.first = first;
            double gradient = gradient(p1, p2);
            Point2D p3 = rotate(lh, 0, gradient + (-Math.PI/2));
//          Point p3 is on the parallel line of p1 -> p2 with the sought line width
            if(last) {
                Point2D pTipTrans = translate(p2, -p1.getX(), -p1.getY());
                double length = Math.sqrt(sqr(pTipTrans.getX())+sqr(pTipTrans.getY()));
                double c3 = length-(ARROWHEAD_EDGE_LENGTH/2*Math.sqrt(3));
                double x3 = Math.cos(gradient) * c3;
                double y3 = Math.sin(gradient) * c3;
                // line p1 -> p(x3,y3) is the same direction as line p1 -> p2 but shortened so
                // that there is some space for the arrowhead

                // point(ahx1,ahy1) and point(ahx2,ahy2) are both 
                // orthogonally translated from line p1 -> point(x3, y3) each by LINE_WIDTH / 2
                double ograd = gradient+(-Math.PI/2);
                Point2D p1Trans = rotate(lh, 0, ograd);
                Point2D p2Trans = rotate(lh, 0, ograd+Math.PI);
                double ahx1 = x3+p1.getX()+p1Trans.getX();
                double ahy1 = y3+p1.getY()+p1Trans.getY();
                double ahx2 = x3+p1.getX()+p2Trans.getX();
                double ahy2 = y3+p1.getY()+p2Trans.getY();
                w1End = new Point2D.Double(ahx1, ahy1);
                w2Start = new Point2D.Double(ahx2, ahy2);
            } else {
                w1End = new Point2D.Double(p2.getX()+p3.getX(), p2.getY()+p3.getY());
                w2Start = new Point2D.Double(p2.getX()-p3.getX(), p2.getY()-p3.getY());
            }
            if(first) {
//              The formula for a line with a point (p3) and the gradient known is
//              f(x) = gradient * ( x - p.x ) + p.y
//              The formula below calculates the intersection of that line and the x-axis (f(x) = 0)
                double x = p3.getX() - p3.getY() / Math.tan(gradient);
                w1Start = new Point2D.Double(p1.getX()+x, p1.getY());
                w2End = new Point2D.Double(p1.getX()-x, p1.getY());
            } else {
                w1Start = new Point2D.Double(p1.getX()+p3.getX(), p1.getY()+p3.getY());
                w2End = new Point2D.Double(p1.getX()-p3.getX(), p1.getY()-p3.getY());
            }
        }

        public void addW1(Path2D path) {
            if(first) {
                path.moveTo(w1Start.getX(), w1Start.getY());
            } else {
                path.lineTo(w1Start.getX(), w1Start.getY());
            }
            path.lineTo(w1End.getX(), w1End.getY());
        }

        public void addW2(Path2D path) {
            path.lineTo(w2Start.getX(), w2Start.getY());
            path.lineTo(w2End.getX(), w2End.getY());
        }
    }

    private static class Renderer extends LineAndShapeRenderer {

        private Font legendFont;

        public Renderer(Font legendFont) {
            super(true, false);
            this.legendFont = legendFont;
        }

        @Override
        public void drawItem(Graphics2D g2,
                CategoryItemRendererState state, Rectangle2D dataArea,
                CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis,
                CategoryDataset dataset, int row, int column, int pass) {
            if(column == 0) {
                Shape shape = createShape(state, dataArea, plot, domainAxis, rangeAxis, dataset, row);
                if(shape != null) {
                    if(pass == 0) {
                        // render all drop shadows first (pass 0)
                        Graphics2D g0 = (Graphics2D)g2.create();
                        g0.translate(2, 2);
                        g0.setPaint(Color.darkGray);
                        g0.fill(shape);
                        g0.dispose();
                    } else if(pass == 1) {
                        g2.setPaint(getSeriesPaint(row));
                        g2.fill(shape);
                    }
                }
            }
        }

        private Shape createShape(CategoryItemRendererState state, Rectangle2D dataArea,
                CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis,
                CategoryDataset dataset, int row) {
            List<Point2D> points = getPoints(row, dataArea, dataset, plot, domainAxis, rangeAxis, state);
            double lineWidth = ((BasicStroke)getSeriesStroke(row)).getLineWidth();
            double lh = lineWidth / 2;
            if(points.isEmpty()) {
                return null;
            }
            if(points.size() == 1) {
                Point2D p = points.get(0);
                return new Ellipse2D.Double(p.getX()-lh,p.getY()-lh,lineWidth,lineWidth);
            }
            Path2D path = new Path2D.Double();
            List<LineSection> sections = Lists.newArrayList();
            for(int i=0; i<points.size()-1; i++) {
                Point2D p1 = points.get(i);
                Point2D p2 = points.get(i+1);
                LineSection section = new LineSection(p1, p2, i==0, i==(points.size()-2), lh);
                sections.add(section);
            }
            for(LineSection section : sections) {
                section.addW1(path);
            }
            addArrowhead(path, points.get(points.size()-2), points.get(points.size()-1), lh);
            for(LineSection section : Lists.reverse(sections)) {
                section.addW2(path);
            }
            path.closePath();
            return path;
        }

        private void addArrowhead(Path2D path, Point2D p1, Point2D p2, double lh) {
            double gradient = gradient(p1, p2);
            Point2D pTipTrans = translate(p2, -p1.getX(), -p1.getY());
            double length = Math.sqrt(sqr(pTipTrans.getX())+sqr(pTipTrans.getY()));
            double c3 = length-(ARROWHEAD_EDGE_LENGTH/2*Math.sqrt(3));
            double x3 = Math.cos(gradient) * c3;
            double y3 = Math.sin(gradient) * c3;
            // line p1 -> p(x3,y3) is the same direction as line p1 -> p2 but shortened so
            // that there is some space for the arrowhead

            // point(ahx1,ahy1) and point(ahx2,ahy2) are both 
            // orthogonally translated from line p1 -> point(x3, y3) each by LINE_WIDTH / 2
            double ograd = gradient+(-Math.PI/2);
            Point2D p1Trans = rotate(lh, 0, ograd);
            Point2D p2Trans = rotate(lh, 0, ograd+Math.PI);
            double ahx1 = x3+p1.getX()+p1Trans.getX();
            double ahy1 = y3+p1.getY()+p1Trans.getY();
            double ahx2 = x3+p1.getX()+p2Trans.getX();
            double ahy2 = y3+p1.getY()+p2Trans.getY();
            path.lineTo(ahx1, ahy1);
            
            Point2D pV1Trans = rotate(ARROWHEAD_EDGE_LENGTH/2, 0, ograd);
            Point2D pV2Trans = rotate(ARROWHEAD_EDGE_LENGTH/2, 0, ograd+Math.PI);
            double ahx3 = x3+p1.getX()+pV1Trans.getX();
            double ahy3 = y3+p1.getY()+pV1Trans.getY();
            // line to one of the base vertices of arrowhead
            path.lineTo(ahx3, ahy3);
            // line to tip of arrowhead
            path.lineTo(p2.getX(), p2.getY());
            double ahx4 = x3+p1.getX()+pV2Trans.getX();
            double ahy4 = y3+p1.getY()+pV2Trans.getY();
            path.lineTo(ahx4, ahy4);
            path.lineTo(ahx2, ahy2);
        }

        private List<Point2D> getPoints(int row, Rectangle2D dataArea, CategoryDataset dataset,
                CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryItemRendererState state) {
            List<Point2D> points = Lists.newArrayList();
            for(int column = 0; true; column++) {
                Number v = null;
                try {
                    v = dataset.getValue(row, column);
                } catch(Exception e) {}
                if (v == null) {
                    break;
                }
                int visibleRow = state.getVisibleSeriesIndex(row);
                if (visibleRow < 0) {
                    break;
                }
                int visibleRowCount = state.getVisibleSeriesCount();
                double x1;
                if (this.getUseSeriesOffset()) {
                    x1 = domainAxis.getCategorySeriesMiddle(column,
                            dataset.getColumnCount(), visibleRow, visibleRowCount,
                            getItemMargin(), dataArea, plot.getDomainAxisEdge());
                }
                else {
                    x1 = domainAxis.getCategoryMiddle(column, getColumnCount(),
                            dataArea, plot.getDomainAxisEdge());
                }
                double value = v.doubleValue();
                double y1 = rangeAxis.valueToJava2D(value, dataArea,
                        plot.getRangeAxisEdge());
                points.add(new Point2D.Double(x1, y1));
            }
            return points;
        }

        @Override
        public void drawBackground(Graphics2D g2, CategoryPlot plot,
                Rectangle2D dataArea) {
            super.drawBackground(g2, plot, dataArea);
            Graphics2D g0 = null;
            try {
                CategoryDataset dataset = this.getPlot().getDataset();
                if(dataset.getRowCount() == 2) {
                    TableRowBox row = new TableRowBox();
                    for(int i = 0; i < 2;i++) {
                        ContentBoxImpl c = new ContentBoxImpl();
                        c.setBackground(this.getSeriesPaint(i));
                        c.setWidth(25);
                        c.setHeight(2);
                        c.getMargin().setRight(5);
                        TableCellBox legendLineBox = new TableCellBox(c);
                        TableCellBox labelBox = new TableCellBox(
                                new TextBox(dataset.getRowKey(i).toString(), legendFont));
                        labelBox.getMargin().setRight(i==0?20:40);
                        row.addCell(legendLineBox);
                        row.addCell(labelBox);
                    }
                    TableBox legendBox = new TableBox();
                    legendBox.addRow(row);
                    Dimension d = legendBox.getDimension(g2);
                    g0 = (Graphics2D)g2.create((int)(dataArea.getMaxX()-d.getWidth()), 75, d.width, d.height);
                    legendBox.render(g0); 
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(g0 != null) {
                    g0.dispose();
                }
            }
        }
    }

    private static NumberFormat percentFormatter() {
      NumberFormat percentFormat = NumberFormat.getPercentInstance();
      percentFormat.setMaximumFractionDigits(0);
      return percentFormat;
  }

    public Drawable createChart(ChartType type, double target, 
        final ADCDataset dataset, Dimension dimension) {
        if(dataset.getRowCount() == 0) {
            throw new RuntimeException("no series");
        }
        if(dataset.getRowCount() > 2) {
            throw new RuntimeException("too many series");
        }
        final JFreeChart chart = ChartFactory.createLineChart(
                dataset.<String>get(Attribute.TITLE),
                dataset.<String>get(Attribute.DOMAIN_AXIS_TITLE),
                dataset.<String>get(Attribute.RANGE_AXIS_TITLE),
                dataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false);
        final CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis caxis = plot.getDomainAxis();
        caxis.setTickMarksVisible(false);
        plot.setRenderer(new Renderer(caxis.getTickLabelFont()));
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(LINE_WIDTH));
        plot.getRenderer().setSeriesStroke(1, new BasicStroke(LINE_WIDTH));
        Color[] seriesColors = dataset.get(Attribute.SERIES_COLORS);
        for(int i=0;i<2;i++) {
          if(seriesColors != null && i < seriesColors.length && seriesColors[i] != null) {
            plot.getRenderer().setSeriesPaint(i, seriesColors[i]);
          } else {
            plot.getRenderer().setSeriesPaint(i, ImmutableList.of(SERIES_1_COLOR,
                SERIES_2_COLOR).get(i));
          }
        }
        plot.getRenderer().setBaseOutlinePaint(Color.black);
        plot.setBackgroundPaint(new GradientPaint(0, 0, GRADIENT_END, 0, 0, GRADIENT_START));
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);
        plot.setOutlinePaint(Color.black);
        plot.setOutlineVisible(true);
        plot.setOutlineStroke(new BasicStroke(2.0f));
        plot.setAxisOffset(RectangleInsets.ZERO_INSETS);
        NumberAxis vaxis = (NumberAxis)plot.getRangeAxis();
        vaxis.setRange(0, target);
        vaxis.setAutoTickUnitSelection(true);
        double tickSize;
        if(target <= 0.2) {
            tickSize = 0.02;
        } else if(target <= 0.5) {
            tickSize = 0.05;
        } else {
            tickSize = 0.1;
        }
        vaxis.setTickUnit(new NumberTickUnit(tickSize, percentFormatter()));
        vaxis.setTickMarksVisible(false);
        return new JFreeChartDrawable(chart, dimension);
    }

}
