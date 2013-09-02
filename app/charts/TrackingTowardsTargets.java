package charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Shape;
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
import charts.builder.ChartType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

public class TrackingTowardsTargets {

    private static String IMPROVED_PRATICES = "% of farmers adopting improved practices";
    private static String POLLUTANT_REDUCTION = "% reduction in pollutant load";
    private static String TARGET = " target (%s by %s)";

    private static float LINE_SIZE = 8.0f;

    private static Color GRADIENT_START = new Color(227, 246, 253);
    private static Color GRADIENT_END = Color.white;

    private static Color SERIES_1_COLOR = new Color(30, 172, 226);
    private static Color SERIES_2_COLOR = new Color(187, 34, 51);

    private static class Title {
        private String title;
        private String valueAxisLabel;
        public Title(String title, String valueAxisLabel) {
            super();
            this.title = title;
            this.valueAxisLabel = valueAxisLabel;
        }
        public String getTitle(double target, String targetBy) {
            return String.format(title, percentFormatter().format(target), targetBy);
        }
        public String getValueAxisLabel() {
            return valueAxisLabel;
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
            if((column == 0) && (pass == 0)) {
                Shape shape = createShape(state, dataArea, plot, domainAxis, rangeAxis, dataset, row);
                g2.setPaint(getSeriesPaint(row));
                g2.fill(shape);
            }
        }

        private Shape createShape(CategoryItemRendererState state, Rectangle2D dataArea,
                CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis,
                CategoryDataset dataset, int row) {
            double lineWidth = ((BasicStroke)getSeriesStroke(row)).getLineWidth();
            double lh = lineWidth / 2;
            List<Point2D> points = getPoints(row, dataArea, dataset, plot, domainAxis, rangeAxis, state);
            Path2D path = new Path2D.Double();
            for(int i=0; i<points.size(); i++) {
                Point2D p = points.get(i);
                if(i == 0) {
                    path.moveTo(p.getX()-lh, p.getY());
                } else {
                    path.lineTo(p.getX(), p.getY()-lh);
                }
            }
            for(int i=points.size()-1; i >= 0; i--) {
                Point2D p = points.get(i);
                if(i == 0) {
                    path.lineTo(p.getX()+lh, p.getY());
                } else {
                    path.lineTo(p.getX(), p.getY()+lh);
                }
            }
            path.closePath();
            return path;
        }

        private List<Point2D> getPoints(int row, Rectangle2D dataArea, CategoryDataset dataset,
                CategoryPlot plot, CategoryAxis domainAxis, ValueAxis rangeAxis, CategoryItemRendererState state) {
            List<Point2D> points = Lists.newArrayList();
            for(int column = 0; true; column++) {
                Number v = dataset.getValue(row, column);
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

    private static final ImmutableMap<ChartType, Title> TITLES =
            new ImmutableMap.Builder<ChartType, Title>()
                .put(ChartType.TTT_CANE_AND_HORT, new Title(
                        "Cane and horticulture"+TARGET, IMPROVED_PRATICES))
                .put(ChartType.TTT_GRAZING, new Title(
                        "Grazing"+TARGET, IMPROVED_PRATICES))
                .put(ChartType.TTT_NITRO_AND_PEST, new Title(
                        "Total nitrogen and pesticide"+TARGET, POLLUTANT_REDUCTION))
                .put(ChartType.TTT_SEDIMENT, new Title(
                        "Sediment"+TARGET, POLLUTANT_REDUCTION))
                .build();

    private static NumberFormat percentFormatter() {
        NumberFormat percentFormat = NumberFormat.getPercentInstance();
        percentFormat.setMaximumFractionDigits(0);
        return percentFormat;
    }

    public Drawable createChart(ChartType type, double target, String targetBy, 
            final CategoryDataset dataset, Dimension dimension) {
        if(dataset.getRowCount() == 0) {
            throw new RuntimeException("no series");
        }
        if(dataset.getRowCount() > 2) {
            throw new RuntimeException("too many series");
        }
        Title title = TITLES.get(type);
        if(title == null) {
            throw new RuntimeException("no title configured for chart type "+type.toString());
        }
        final JFreeChart chart = ChartFactory.createLineChart(
                title.getTitle(target, targetBy),
                null,
                title.getValueAxisLabel(),
                dataset,
                PlotOrientation.VERTICAL,
                false,
                false,
                false);
        final CategoryPlot plot = chart.getCategoryPlot();
        CategoryAxis caxis = plot.getDomainAxis();
        caxis.setTickMarksVisible(false);
        plot.setRenderer(new Renderer(caxis.getTickLabelFont()));
        plot.getRenderer().setSeriesStroke(0, new BasicStroke(LINE_SIZE));
        plot.getRenderer().setSeriesStroke(1, new BasicStroke(LINE_SIZE));
        plot.getRenderer().setSeriesPaint(0, SERIES_1_COLOR);
        plot.getRenderer().setSeriesPaint(1, SERIES_2_COLOR);
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
