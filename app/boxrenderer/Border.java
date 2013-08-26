package boxrenderer;

import static java.lang.Math.PI;
import graphics.GraphUtils;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;


public class Border extends Spacing {

    // border-radius
    private int radius;

    @Override
    protected Shape makeShape(int w, int h) {
        if(radius == 0) {
            return super.makeShape(w, h);
        } else {
            Path2D.Double outer = new Path2D.Double();
            GraphUtils.addArc(outer, true, radius, -PI, -PI/2, radius, radius);
            outer.lineTo(w-radius, 0);
            GraphUtils.addArc(outer, false, radius, -PI/2, 0, w-radius, radius);
            outer.lineTo(w, h-radius);
            GraphUtils.addArc(outer, false, radius, 0, PI/2, w-radius, h-radius);
            outer.lineTo(radius, h);
            GraphUtils.addArc(outer, false, radius, PI/2, PI, radius, h-radius);
            outer.closePath();
            Area shape = new Area(outer);
            shape.subtract(new Area(makeInnerShape(w, h)));
            return shape;
        }
    }

    @Override
    protected Shape makeInnerShape(int w, int h) {
        // TODO border-radius is the same for all corners.
        // not good enough to test only against top but do it anyway for now
        // this it ok if the border has the same width on all sides 
        // but will fail otherwise
        if(radius > getTop()) {
            Path2D.Double inner = new Path2D.Double();
            int size = getTop();
            GraphUtils.addArc(inner, true, radius-size, -PI, -PI/2, radius, radius);
            inner.lineTo(w-radius, size);
            GraphUtils.addArc(inner, false, radius-size, -PI/2, 0, w-radius, radius);
            inner.lineTo(w-size, h-radius);
            GraphUtils.addArc(inner, false, radius-size, 0, PI/2, w-radius, h-radius);
            inner.lineTo(radius, h-size);
            GraphUtils.addArc(inner, false, radius-size, PI/2, PI, radius, h-radius);
            inner.closePath();
            return inner;
        } else {
            return super.makeInnerShape(w, h);
        }
    }

    public Border() {
        super();
    }

    public Border(int size) {
        super(size);
        setPaint(Color.black);
    }

    public Border(int size, Color color) {
        super(size);
        setPaint(color);
    }

    public Border(int top, int left, int bottom, int right) {
        super(top, left, bottom, right);
        setPaint(Color.black);
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

}
