package boxrenderer;

import java.awt.Dimension;
import java.awt.Graphics2D;

public class BreakBox extends AbstractBox {

    public BreakBox() {
        getMargin().setApplicable(false);
        getBorder().setApplicable(false);
        getPadding().setApplicable(false);
    }

    @Override
    public Dimension getContentDimension(Graphics2D g2) throws Exception {
        return new Dimension();
    }

    @Override
    public void renderContent(Graphics2D g2) throws Exception {
    }

}
