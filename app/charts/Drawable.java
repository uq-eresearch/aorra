package charts;

import java.awt.Dimension;
import java.awt.Graphics2D;

public interface Drawable {

    public Dimension getDimension(Graphics2D graphics);

    public void draw(Graphics2D graphics);

}
