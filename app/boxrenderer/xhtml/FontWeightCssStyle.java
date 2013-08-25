package boxrenderer.xhtml;

import boxrenderer.Box;

public class FontWeightCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) {
        box.setBold("bold".equals(getProperty().getValue()));
    }

}
