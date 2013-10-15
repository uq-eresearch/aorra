package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.axis.AxisState;
import org.jfree.chart.axis.NumberTick;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotRenderingInfo;
import org.jfree.data.Range;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.TextAnchor;

import com.google.common.collect.Lists;

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
        if(size != 1) {
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
        List<NumberTick> ticks = Lists.newArrayList();
        // TODO generate them automatically
        ticks.add(new NumberTick(0, "0", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        ticks.add(new NumberTick(10, "10", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        ticks.add(new NumberTick(20, "20", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        ticks.add(new NumberTick(30, "30", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        ticks.add(new NumberTick(40, "40", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        ticks.add(new NumberTick(50, "50", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        ticks.add(new NumberTick(100, "100", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        ticks.add(new NumberTick(400, "400", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        ticks.add(new NumberTick(500, "500", TextAnchor.CENTER_RIGHT, TextAnchor.CENTER_RIGHT, 0));
        return ticks;
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

}
