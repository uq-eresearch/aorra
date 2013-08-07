package charts.builder;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import org.jfree.ui.Drawable;

import charts.Dimensions;

public class DimensionsWrapper implements Dimensions {

    private Dimension dimension;

    private Drawable drawable;

    public DimensionsWrapper(Drawable drawable) {
        this.drawable = drawable;
    }

    public DimensionsWrapper(Drawable drawable, Dimension dimension) {
        this(drawable);
        this.dimension = dimension;
    }

    @Override
    public void draw(Graphics2D g2, Rectangle2D area) {
        this.drawable.draw(g2, area);
    }

    @Override
    public Dimension getDimension() {
        return dimension;
    }

    @Override
    public void setDimension(Dimension dimension) {
        this.dimension = dimension;
    }

}
