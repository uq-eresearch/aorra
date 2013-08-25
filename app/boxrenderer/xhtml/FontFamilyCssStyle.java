package boxrenderer.xhtml;

import boxrenderer.Box;

public class FontFamilyCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) {
        box.setFontFamily(getProperty().getValue());
    }

}
