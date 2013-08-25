package boxrenderer;

import java.awt.Dimension;
import java.awt.Graphics2D;

public class ImageRenderBox extends AbstractBox implements Box {

    private ImageRenderer renderer;

    public ImageRenderBox(ImageRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public Dimension getContentDimension(Graphics2D g2) throws Exception {
        return renderer.getDimension(g2);
    }

    @Override
    public void renderContent(Graphics2D g2) throws Exception {
        renderer.render(g2);
    }

}
