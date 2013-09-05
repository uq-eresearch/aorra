package graphics;

import java.awt.geom.AffineTransform;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;

public class HatchedRectangle extends RectangularShape {

    private Path2D.Double shape = new Path2D.Double();

    private double x;

    private double y;

    private double w;

    private double h;

    private double hatchWidth;

    private double hatchDistance;

    public HatchedRectangle(double x, double y, double w, double h, double hatchWidth, double hatchDistance) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.hatchWidth = hatchWidth;
        this.hatchDistance = hatchDistance;
        makeShape();
    }

    private void makeShape() {
        double startY = hatchDistance;
        while((startY - w) < h) {
            if(((startY+hatchWidth) < h)) {
                // start left
                // i don't think we can have partially visible hatch shapes in this case
                if((startY+hatchWidth) < w) {
                    // hit top
                    makeShape(0d, startY, startY, 0d, startY+hatchWidth, 0d, 0d, startY+hatchWidth);
                } else if(startY<w) {
                    // hit corner
                    makeShape(0d, startY, startY, 0d, w, 0d, w, hatchWidth-(w-startY), 0d, startY+hatchWidth);
                } else {
                    // hit right
                    makeShape(0d, startY, w, startY-w, w, startY-w+hatchWidth, 0d, startY+hatchWidth);
                }
            } else if(startY < h) {
                if((startY-w+hatchWidth) > h) {
                    // start corner but only partially visible hatch
                    if((startY+hatchWidth) < w) {
                        // hit top
                        // not sure if this can happen, ignore
                    } else if(startY<w) {
                        // hit corner
                        makeShape(0d, startY, startY, 0d, w, 0d, w, h, 0d, h);
                    } else {
                        // hit right
                        makeShape(0d, startY, w, startY-w, w, h, 0d, h);
                    }
                } else {
                    // start corner
                    if((startY+hatchWidth) < w) {
                        // hit top
                        makeShape(0d, startY, startY, 0d, startY+hatchWidth, 0d, hatchWidth-(h-startY), h, 0d, h);
                    } else if(startY<w) {
                        // hit corner
                        makeShape(
                                0d, startY,
                                startY, 0d,
                                w,0d,
                                w, hatchWidth-(w-startY),
                                hatchWidth-(h-startY), h,
                                0d, h
                                );
                    } else {
                        // hit right
                        makeShape(0d, startY, w, startY-w, w, startY-w+hatchWidth, hatchWidth-(h-startY), h, 0d, h);
                    }
                }
            } else {
                if((startY-w+hatchWidth) > h) {
                    // start bottom but only partially visible hatch
                    // start bottom
                    if((startY+hatchWidth) < w) {
                        // hit top
                        // not sure if this can happen, ignore
                    } else if(startY<w) {
                        // hit corner
                        makeShape(startY-h, h, startY, 0d, w, 0d, w, h);
                    } else {
                        // hit right
                        makeShape(startY-h, h, w, startY-w, w, h);
                    }
                } else {
                    // start bottom
                    if((startY+hatchWidth) < w) {
                        // hit top
                        makeShape(startY-h, h, startY, 0d, startY+hatchWidth, 0d, startY-h+hatchWidth, h);
                    } else if(startY<w) {
                        // hit corner
                        makeShape(startY-h, h, startY, 0d, w, 0d, w, hatchWidth-(w-startY), startY-h+hatchWidth, h);
                    } else {
                        // hit right
                        makeShape(startY-h, h, w, startY-w, w, startY-w+hatchWidth, startY-h+hatchWidth, h);
                    }
                }
            }
            startY += hatchWidth + hatchDistance;
        }
    }

    private void makeShape(double x1, double y1, double x2, double y2, double x3, double y3) {
        shape.moveTo(x1+x, y1+y);
        shape.lineTo(x2+x, y2+y);
        shape.lineTo(x3+x, y3+y);
        shape.closePath();
    }

    private void makeShape(double x1, double y1, double x2, double y2, double x3, double y3,
            double x4, double y4) {
        shape.moveTo(x1+x, y1+y);
        shape.lineTo(x2+x, y2+y);
        shape.lineTo(x3+x, y3+y);
        shape.lineTo(x4+x, y4+y);
        shape.closePath();
    }

    private void makeShape(double x1, double y1, double x2, double y2, double x3, double y3,
            double x4, double y4, double x5, double y5) {
        shape.moveTo(x1+x, y1+y);
        shape.lineTo(x2+x, y2+y);
        shape.lineTo(x3+x, y3+y);
        shape.lineTo(x4+x, y4+y);
        shape.lineTo(x5+x, y5+y);
        shape.closePath();
    }

    private void makeShape(double x1, double y1, double x2, double y2, double x3, double y3,
            double x4, double y4, double x5, double y5, double x6, double y6) {
        shape.moveTo(x1+x, y1+y);
        shape.lineTo(x2+x, y2+y);
        shape.lineTo(x3+x, y3+y);
        shape.lineTo(x4+x, y4+y);
        shape.lineTo(x5+x, y5+y);
        shape.lineTo(x6+x, y6+y);
        shape.closePath();
    }

    @Override
    public Rectangle2D getBounds2D() {
        return shape.getBounds2D();
    }

    @Override
    public boolean contains(double x, double y) {
        return shape.contains(x, y);
    }

    @Override
    public boolean intersects(double x, double y, double w, double h) {
        return shape.intersects(x, y, w, h);
    }

    @Override
    public boolean contains(double x, double y, double w, double h) {
        return shape.contains(x, y, w, h);
    }

    @Override
    public PathIterator getPathIterator(AffineTransform at) {
        return shape.getPathIterator(at);
    }

    @Override
    public double getX() {
        return x;
    }

    @Override
    public double getY() {
        return y;
    }

    @Override
    public double getWidth() {
        return w;
    }

    @Override
    public double getHeight() {
        return h;
    }

    @Override
    public boolean isEmpty() {
        throw new RuntimeException("not implemented");
    }

    @Override
    public void setFrame(double x, double y, double w, double h) {
        throw new RuntimeException("not implemented");
    }

}
