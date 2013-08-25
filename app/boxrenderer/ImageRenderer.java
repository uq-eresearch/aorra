package boxrenderer;

import java.awt.Dimension;
import java.awt.Graphics2D;

public interface ImageRenderer {

    /**
     * size of the image to be rendered
     */
    public Dimension getDimension(Graphics2D g2) throws Exception;

    /**
     * renders the image through the Graphics2D methods
     */
    public void render(Graphics2D g2) throws Exception;
}
