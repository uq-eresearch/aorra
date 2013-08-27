package boxrenderer;

import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;

public class Spacing {

    private int top = -1;
    private int left = -1;
    private int bottom = -1;
    private int right = -1;

    private int size;

    private Paint paint;

    private boolean render = true;

    private boolean applicable = true;

    public Spacing() {
        this(0);
    }

    public Spacing(int s) {
        size = s;
    }

    public Spacing(int top, int left, int bottom, int right) {
        this.top = top;
        this.left = left;
        this.bottom = bottom;
        this.right = right;
    }

    public Graphics2D render(Graphics2D g2) {
        int width = g2.getClipBounds().width;
        int height = g2.getClipBounds().height;
        if(isApplicable()) {
            if(render && (paint !=null)) {
                g2.setPaint(paint);
                g2.fill(makeShape(width, height));
            }
//            Unfortunately this is not working properly.
//            It produces ugly gaps if used with a border-radius and rendered with suns graphics2d
//            but with batiks graphics2d it seems to be ok
            Graphics2D g0 = (Graphics2D)g2.create();
            g0.setClip(new Area(makeInnerShape(width, height)));
            g0.translate(getLeft(), getTop());
            return g0;
        } else {
            return (Graphics2D)g2.create(0, 0, width, height);
        }
    }

    protected Shape makeShape(int w, int h) {
        Path2D.Double outer = new Path2D.Double();
        outer.moveTo(0, 0);
        outer.lineTo(w, 0);
        outer.lineTo(w, h);
        outer.lineTo(0, h);
        outer.lineTo(0, 0);
        outer.closePath();
        Area shape = new Area(outer);
        shape.subtract(new Area(makeInnerShape(w, h)));
        return shape;
    }

    protected Shape makeInnerShape(int w, int h) {
        Path2D.Double inner = new Path2D.Double();
        inner.moveTo(getLeft(), getTop());
        inner.lineTo(w-getRight(), getTop());
        inner.lineTo(w-getRight(), h-getBottom());
        inner.lineTo(getLeft(), h-getBottom());
        inner.closePath();
        return inner;
    }

    public void setPaint(Paint paint) {
        this.paint = paint;
    }

    public boolean isRender() {
        return render;
    }

    public void setRender(boolean render) {
        this.render = render;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getTop() {
        return top!=-1?top:size;
    }

    public void setTop(int top) {
        this.top = top;
    }

    public int getLeft() {
        return left!=-1?left:size;
    }

    public void setLeft(int left) {
        this.left = left;
    }

    public int getBottom() {
        return bottom!=-1?bottom:size;
    }

    public void setBottom(int bottom) {
        this.bottom = bottom;
    }

    public int getRight() {
        return right!=-1?right:size;
    }

    public void setRight(int right) {
        this.right = right;
    }

    public boolean isApplicable() {
        return applicable;
    }

    public void setApplicable(boolean applicable) {
        this.applicable = applicable;
    }

    public Paint getPaint() {
        return paint;
    }

    @Override
    public String toString() {
        return String.format("top:%s, bottom: %s, left: %s, right: %s",
                getTop(), getBottom(), getLeft(), getRight());
    }

}
