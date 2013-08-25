package boxrenderer.xhtml;

import boxrenderer.Box;

public class RotationPointCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) {
        box.setRotationPoint(getProperty().getValue());
    }

}
