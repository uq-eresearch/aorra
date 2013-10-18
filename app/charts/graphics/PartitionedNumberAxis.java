package charts.graphics;

import graphics.GraphUtils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.text.DecimalFormat;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.Range;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.math.DoubleMath;

public class PartitionedNumberAxis extends ValueAxis {

    public static class Partition {
        private Range range;
        private double size;

        public Partition(Range range, double size) {
            super();
            this.range = range;
            this.size = size;
        }

        public Range getRange() {
            return range;
        }

        public double getSize() {
            return size;
        }

    }

    private static final Double BOUNDARY_SIZE_PX = 5.0;

    private static final Double CURVATURE = 7.5;

    private static final Paint BOUNDARY_PAINT = Color.lightGray;

    private List<Partition> partitions = Lists.newArrayList();

    public PartitionedNumberAxis(String label) {
        super(label, null);
    }

    public void addPartition(Partition p) {
        if(p.range == null) {
            throw new RuntimeException("partition.range is null");
        }
        if(p.size <= 0) {
            throw new RuntimeException("partition.size <= 0");
        }
        if(p.size > 1) {
            throw new RuntimeException("partition.size > 1");
        }
        if(!partitions.isEmpty()) {
            for(Partition partition : partitions) {
                if(partition.range.intersects(p.range)) {
                    throw new RuntimeException("partitions intersect");
                }
            }
            if(partitions.get(partitions.size()-1).range.getLowerBound() > p.getRange().getLowerBound()) {
                throw new RuntimeException("partition range order");
            }
        }
        partitions.add(p);
    }

    @Override
    public double valueToJava2D(double value, Rectangle2D area, RectangleEdge edge) {
        return valueToJava2D(value, area);
    }

    private double valueToJava2D(double value, Rectangle2D area) {
        if(partitions.isEmpty()) {
            throw new RuntimeException("no partitions");
        }
        double axisLength = axisLength(area);
        double current = area.getMaxY();
        if(value < partitions.get(0).range.getLowerBound()) {
            return current;
        }
        for(int i=0;i<partitions.size();i++) {
            Partition p = partitions.get(i);
            Range r = p.getRange();
            double s = p.getSize();
            double length = axisLength * s;
            if(r.contains(value)) {
                return current - (length * (value - r.getLowerBound()) / (r.getUpperBound()-r.getLowerBound()));
            } else if(((i+1) < partitions.size()) && (value < partitions.get(i+1).range.getLowerBound())) {
                return current - length - BOUNDARY_SIZE_PX / 2.0;
            } else {
                current = current - length - BOUNDARY_SIZE_PX;
            }
        }
        return current;
    }

    @Override
    public double java2DToValue(double java2dValue, Rectangle2D area,
            RectangleEdge edge) {
        throw new RuntimeException("java2DToValue not implemented");
    }

    @Override
    protected void autoAdjustRange() {}

    @Override
    public void configure() {}

    private void checkSize() {
        double size = 0;
        for(Partition p : partitions) {
            size += p.getSize();
        }
        if(!DoubleMath.fuzzyEquals(size, 1.0, 0.001)) {
            throw new RuntimeException(String.format(
                    "sum of partitions size must be 1 dude! (%s)", size));
        }
    }

    @Override
    public AxisState draw(Graphics2D g2, double cursor, Rectangle2D plotArea,
            Rectangle2D dataArea, RectangleEdge edge,
            PlotRenderingInfo plotState) {
        if(partitions.isEmpty()) {
            throw new RuntimeException("no partitions");
        }
        checkSize();
        if(edge != RectangleEdge.LEFT) {
            throw new RuntimeException("axis not on left");
        }
        if (isAxisLineVisible()) {
            drawAxisLine(g2, cursor, dataArea, edge);
        }
        drawPartitions(g2, dataArea);
        AxisState state = drawTickMarksAndLabels(g2, cursor, plotArea, dataArea, edge);
        state = drawLabel(getLabel(), g2, plotArea, dataArea, edge, state);
        createAndAddEntity(cursor, state, dataArea, edge, plotState);
        return state;
    }

    

    @Override
    protected void drawAxisLine(Graphics2D g2, double cursor,
            Rectangle2D dataArea, RectangleEdge edge) {
        g2.setPaint(getAxisLinePaint());
        g2.setStroke(getAxisLineStroke());
        double start = dataArea.getMaxY();
        double x = cursor;
        for(Double boundary : getPartitionBoundaries(dataArea)) {
            double end = boundary+BOUNDARY_SIZE_PX;
            g2.draw(new Line2D.Double(x, start, x, end));
            drawAxisPartitionMark(g2, x, end);
            start = boundary;
            drawAxisPartitionMark(g2, x, start);
        }
        g2.draw(new Line2D.Double(x, start, x, dataArea.getY()));
    }

    private void drawAxisPartitionMark(Graphics2D g2, double x, double y) {
        g2.draw(new Line2D.Double(x-2, y, x+2, y));
    }

    private int gaps() {
        return partitions.size()-1;
    }

    private double axisLength(Rectangle2D dataArea) {
        return dataArea.getMaxY() - dataArea.getY() - (gaps() * BOUNDARY_SIZE_PX);
    }

    private void drawPartitions(Graphics2D g2, Rectangle2D dataArea) {
        for(Double boundary : getPartitionBoundaries(dataArea)) {
            Rectangle2D.Double r = new Rectangle2D.Double(dataArea.getX(), boundary, dataArea.getWidth(), BOUNDARY_SIZE_PX);
            g2.setPaint(BOUNDARY_PAINT);
            g2.fill(r);
        }
    }

    private List<Double> getPartitionBoundaries(Rectangle2D dataArea) {
        List<Double> result = Lists.newArrayList();
        double axisLength = axisLength(dataArea);
        double current = dataArea.getMaxY();
        for(int i=0;i<partitions.size()-1;i++) {
            Partition p = partitions.get(i);
            double s = p.getSize();
            current = current - axisLength * s - BOUNDARY_SIZE_PX;
            result.add(current);
        }
        return result;
    }

    @Override
    public List<NumberTick> refreshTicks(Graphics2D g2, AxisState state,
            Rectangle2D dataArea, RectangleEdge edge) {
        return createTicks(g2, dataArea);
    }

    private List<NumberTick> createTicks(Graphics2D g2, Rectangle2D dataArea) {
        List<NumberTick> ticks = Lists.newArrayList();
        for(Partition p : partitions) {
            ticks.addAll(createTicks(p, g2, dataArea));
        }
        return ticks;
    }

    private List<NumberTick> createTicks(Partition p, Graphics2D g2, Rectangle2D dataArea) {
        List<NumberTick> ticks = Lists.newArrayList();
        double space = this.axisLength(dataArea) * p.getSize();
        Range r = p.getRange();
        double l = r.getLength();
        double tickUnitMultiplier = 10.0;
        Set<Double> seen = Sets.newHashSet();
        if(DoubleMath.fuzzyEquals(l, 0.0, 0.001)) {
            ticks.add(tick(r.getLowerBound()));
        } else {
            Range numberOfTicks = getNumberOfTicks(g2, space);
            double tickUnit = getTickUnit(Math.abs(l)/ numberOfTicks.getUpperBound());
            List<Double> result = Lists.newArrayList();
            // TODO this gives bizarre tick units
            while(true) {
                if(Double.isInfinite(tickUnit) || Double.isNaN(tickUnit)) {
                    break;
                }
                seen.add(tickUnit);
                double count = l / tickUnit;
                if(numberOfTicks.contains(count)) {
                    result.add(tickUnit);
                    if(seen.contains(tickUnit/tickUnitMultiplier)) {
                        tickUnitMultiplier /= 2;
                    }
                    tickUnit /= tickUnitMultiplier;
                } else if(count > numberOfTicks.getCentralValue()) {
                    if(seen.contains(tickUnit*tickUnitMultiplier)) {
                        tickUnitMultiplier /= 2;
                    }
                    tickUnit *= tickUnitMultiplier;
                } else {
                    if(seen.contains(tickUnit/tickUnitMultiplier)) {
                        tickUnitMultiplier /= 2;
                    }
                    tickUnit /= tickUnitMultiplier;
                }
                if(tickUnitMultiplier<1) {
                    break;
                }
            }
            if(result.isEmpty()) {
                throw new RuntimeException("trouble calculating tick unit");
            }
            Collections.sort(result);
            tickUnit = result.get(0);
            // make range a bit smaller to remove ticks that are close to the partition border
            Range smaller = shrinkNotZero(r,0.01);
            double current = Math.round(smaller.getLowerBound()/tickUnit)*tickUnit;
            while(current <= smaller.getUpperBound()) {
                if(smaller.contains(current)) {
                    ticks.add(tick(current));
                }
                current += tickUnit;
            }
        }
        return ticks;
    }

    private Range shrinkNotZero(Range r, double factor) {
        double l = Math.abs(r.getLength());
        if(DoubleMath.fuzzyEquals(l, 0.0, 0.00001)) {
            return r;
        }
        double shrink = l * factor;
        if(r.getLowerBound() == 0.0) {
            return new Range(r.getLowerBound(), r.getUpperBound()-shrink);
        } else if(r.getUpperBound() == 0.0) {
            return new Range(r.getLowerBound()+shrink, r.getUpperBound());
        } else {
            return new Range(r.getLowerBound()+shrink/2, r.getUpperBound()-shrink/2);
        }
    }

    private NumberTick tick(double value) {
        return new NumberTick(value, new DecimalFormat("#.######").format(value),
                TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0);
    }

    private Range getNumberOfTicks(Graphics2D g2, double space) {
        GraphUtils g = new GraphUtils(g2);
        double tickLabelSize = g.getTextHeight(this.getTickLabelFont(), "0");
        double margin = getTickLabelInsets().getTop()+getTickLabelInsets().getBottom();
        return new Range(1, space/(tickLabelSize+margin));
    }

    @Override
    public Range getRange() {
        if(partitions.isEmpty()) {
            throw new RuntimeException("no partitions");
        }
        double lower = partitions.get(0).range.getLowerBound();
        double upper = partitions.get(0).range.getUpperBound();
        for(int i=1;i<partitions.size();i++) {
            Partition p = partitions.get(i);
            lower = Math.min(lower, p.range.getLowerBound());
            upper = Math.max(upper, p.range.getUpperBound());
        }
        return new Range(lower, upper);
    }

    public void drawPatitionBoundaries(Graphics2D g2, Range r,
            Rectangle2D dataArea, double width, double x) {
        List<Double> boundaries = getPartitionBoundaries(dataArea);
        for(int i=0;i<partitions.size()-1;i++) {
            Partition p = partitions.get(i);
            if(crossesBoundary(p.range.getUpperBound(), r)) {
                g2.setStroke(new BasicStroke(1.0f));
                g2.setPaint(BOUNDARY_PAINT);
                Shape closed = createClosedBoundaryShape(x, boundaries.get(i), width);
                g2.fill(closed);
                g2.draw(closed);
                g2.setPaint(Color.black);
                g2.draw(createOpenBoundaryShape(x, boundaries.get(i), width));
            }
        }
    }

    private Shape createOpenBoundaryShape(double x, double y, double width) {
        return createBoundaryShape(x,y,width, true);
    }

    private Shape createClosedBoundaryShape(double x, double y, double width) {
        return createBoundaryShape(x,y,width, false);
    }

    private Shape createBoundaryShape(double x, double y, double width, boolean open) {
        Path2D.Double p = new Path2D.Double();
        p.moveTo(x,y);
        p.curveTo(x+width*.25, y-CURVATURE, x+width*.75, y+CURVATURE, x+width, y);
        if(open) {
            p.moveTo(x+width, y+BOUNDARY_SIZE_PX);
        } else {
            p.lineTo(x+width, y+BOUNDARY_SIZE_PX);
        }
        p.curveTo(x+width*.75, y+BOUNDARY_SIZE_PX+CURVATURE, x+width*.25,
                y+BOUNDARY_SIZE_PX-CURVATURE, x, y+BOUNDARY_SIZE_PX);
        if(!open) {
            p.closePath();
        }
        return p;
    }

    private boolean crossesBoundary(double boundary, Range r) {
        return (r.getLowerBound() < boundary) && (r.getUpperBound() > boundary);
    }

    public static double getTickUnit(double value) {
        double v = Math.abs(value);
        int exp = 0;
        if(DoubleMath.fuzzyEquals(v, 0.0, 0.000000001)) {
            return 0.0;
        } else if(v >= 1.0) {
            while(v >= 1.0) {
                v /= 10.0;
                exp++;
            }
            exp--;
        } else {
            while( v < 1.0) {
                v *= 10.0;
                exp--;
            }
        }
        double t = Math.pow(10.0, exp);
        return t;
    }

}
