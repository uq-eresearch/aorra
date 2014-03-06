package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;

import org.apache.commons.lang3.StringUtils;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.DrawingSupplier;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.category.StatisticalLineAndShapeRenderer;
import org.jfree.data.statistics.DefaultStatisticalCategoryDataset;
import org.jfree.text.TextBlock;
import org.jfree.text.TextUtilities;
import org.jfree.ui.RectangleEdge;

import boxrenderer.Align;
import boxrenderer.Box;
import boxrenderer.TableBox;
import boxrenderer.TableCellBox;
import boxrenderer.TableRowBox;
import boxrenderer.TextBox;
import charts.ChartType;
import charts.Drawable;
import charts.Region;

public class CoralCover {

    private static final String COVER = "Cover (%)";
    private static final String JUVENILE = "Juveniles/m\u00b2";

    public static Drawable createChart(final DefaultStatisticalCategoryDataset dataset,
            ChartType type, Region region, Dimension dimension) {
        String title = String.format("%s (mean)", type.getLabel());
        String rangeAxisLabel = (type == ChartType.CORAL_JUV)?JUVENILE:COVER;
        JFreeChart chart;
        if(region == Region.GBR) {
            chart = createChart(rearrange(dataset), title, rangeAxisLabel, type);
            CategoryPlot plot = (CategoryPlot)chart.getPlot();
            CategoryAxis cAxis = getSubCategoryAxis(dataset);
            cAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
            cAxis.setLabel("Region");
            cAxis.setLabelFont(plot.getRangeAxis().getLabelFont());
            cAxis.setTickLabelFont(plot.getRangeAxis().getTickLabelFont());
            cAxis.setTickMarksVisible(false);
            plot.setDomainAxis(cAxis);
        } else {
            title = String.format(title + " in the %s region", region.getProperName());
            chart = createChart(dataset, title, rangeAxisLabel, type);
            CategoryPlot plot = (CategoryPlot)chart.getPlot();
            plot.getDomainAxis().setLabel("Year");
        }
        return new JFreeChartDrawable(chart, dimension);
    }

    private static JFreeChart createChart(final DefaultStatisticalCategoryDataset dataset, String title,
            String rangeAxisLabel, ChartType type) {
        JFreeChart chart = ChartFactory.createLineChart(
                title,  // title
                "",             // x-axis label
                rangeAxisLabel,   // y-axis label
                dataset,            // data
                PlotOrientation.VERTICAL,
                false,               // create legend?
                false,               // generate tooltips?
                false               // generate URLs?
            );
        chart.setBackgroundPaint(Color.white);
        StatisticalLineAndShapeRenderer renderer = new StatisticalLineAndShapeRenderer();
        renderer.setErrorIndicatorPaint(ErrorIndicator.ERROR_INDICATOR_COLOR);
        CategoryPlot plot = (CategoryPlot)chart.getPlot();
        plot.setRenderer(renderer);
        plot.setRangeGridlinePaint(Color.lightGray);
        plot.setDomainGridlinePaint(Color.lightGray);
        plot.setRangeGridlineStroke(new BasicStroke(1.0f));
        plot.setDomainGridlinesVisible(false);
        plot.setBackgroundPaint(Color.white);
        renderer.setDrawOutlines(true);
        renderer.setUseOutlinePaint(true);
        renderer.setUseFillPaint(false);
        plot.setDrawingSupplier(new DrawingSupplier() {
            @Override
            public Paint getNextPaint() {
                return new Color(187, 34, 51);
            }

            @Override
            public Paint getNextOutlinePaint() {
                return Color.black;
            }

            @Override
            public Paint getNextFillPaint() {
                return null;
            }

            @Override
            public Stroke getNextStroke() {
                return new BasicStroke(1.0f);
            }

            @Override
            public Stroke getNextOutlineStroke() {
                return new BasicStroke(1.0f);
            }

            @Override
            public Shape getNextShape() {
                return AbstractRenderer.DEFAULT_SHAPE;
            }});
        renderer.setBaseOutlinePaint(Color.black);
        final NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickMarksVisible(false);
        if(type == ChartType.CORAL_JUV) {
          rangeAxis.setRange(0, 10.0);
          rangeAxis.setTickUnit(new NumberTickUnit(1, new DecimalFormat("0")));
        } else {
          rangeAxis.setRange(0, 100.0);
          rangeAxis.setTickUnit(new NumberTickUnit(10, new DecimalFormat("0")));
        }
        chart.getTitle().setFont(rangeAxis.getLabelFont());
        chart.addLegend(ErrorIndicatorLegend.createLegend());
        return chart;
    }

    // rearrange the dataset so, that the lines are drawn next to each other
    // (and not on top of each other)
    private static DefaultStatisticalCategoryDataset rearrange(DefaultStatisticalCategoryDataset d) {
        DefaultStatisticalCategoryDataset ds = new DefaultStatisticalCategoryDataset();
        int space = 0;
        for(int r = 0;r<d.getRowCount();r++) {
            for(int c = 0;c<d.getColumnCount();c++) {
                Comparable<?> row = d.getRowKey(r);
                Comparable<?> col = d.getColumnKey(c);
                Number mean = d.getMeanValue(row, col);
                Number stddev = d.getStdDevValue(row, col);
                ds.add(mean, stddev, row, String.format("%s %s", col.toString(), row.toString()));
            }
            if((r+1) < d.getRowCount()) {
                ds.add(null, null, d.getRowKey(r), "space"+space++);
            }
        }
        return ds;
    }

    private static CategoryAxis getSubCategoryAxis(final DefaultStatisticalCategoryDataset dataset) {
        return new CategoryAxis() {
            @SuppressWarnings("rawtypes")
            @Override
            protected TextBlock createLabel(Comparable category, float width,
                    RectangleEdge edge, Graphics2D g2) {
                String label = "";
                try {
                    label = new Integer(StringUtils.split(category.toString())[0]).toString();
                } catch(Exception e) {}
                return TextUtilities.createTextBlock(label,
                        getTickLabelFont(category), getTickLabelPaint(category));
            }

            @Override
            protected AxisState drawLabel(String label, Graphics2D g2,
                    Rectangle2D plotArea, Rectangle2D dataArea,
                    RectangleEdge edge, AxisState state) {
                double labely = state.getCursor();
                Graphics2D g0 = null;
                try {
                    Box b = getLabelBox((int)dataArea.getWidth());
                    int height = (int)b.getDimension(g2).getHeight();
                    g0 = (Graphics2D)g2.create((int)dataArea.getX(), (int)labely,
                            (int)dataArea.getWidth(), height);
                    b.render(g0);
                    state.cursorDown(height);
                } catch(Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    g0.dispose(); 
                }
                return state;
            }

            @Override
            public AxisSpace reserveSpace(Graphics2D g2, Plot plot,
                    Rectangle2D plotArea, RectangleEdge edge, AxisSpace space) {
                AxisSpace s = super.reserveSpace(g2, plot, plotArea, edge, space);
                Box b = getLabelBox((int)plotArea.getWidth());
                try {
                  // workaround, divide by 4 so reduce the large gap to the standard error indicator legend
                    s.add(b.getDimension(g2).getHeight()/4, RectangleEdge.BOTTOM);
                } catch(Exception e) {
                    throw new RuntimeException(e);
                }
                return s;
            }

            private Box getLabelBox(int width) {
                TableRowBox r1 = new TableRowBox();
                for(int i=0;i<dataset.getRowCount();i++) {
                    String row = dataset.getRowKey(i).toString();
                    r1.addCell(newCell(row, width/dataset.getRowCount(), getTickLabelFont()));
                }
                TableRowBox r2 = new TableRowBox();
                TableCellBox b1 = newCell(getLabel(), width, getLabelFont());
                b1.setColspan(0);
                r2.addCell(b1);
                TableBox box = new TableBox();
                box.setWidth(width);
                box.addRow(r1);
                box.addRow(r2);
                box.getMargin().setTop(4);
                return box;
            }

            private TableCellBox newCell(String label, int width, Font font) {
                TextBox t = new TextBox(label, font);
                TableCellBox b = new TableCellBox(t);
                b.setWidth(width);
                b.setAlign(Align.CENTER);
                return b;
            }
        };
    }

}
