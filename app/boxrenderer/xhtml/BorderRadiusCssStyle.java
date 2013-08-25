package boxrenderer.xhtml;

import boxrenderer.Box;

public class BorderRadiusCssStyle extends AbstractCssStyle {

    @Override
    public void style(Box box) throws Exception {
        int size = Sizes.getPixelSize(getProperty().getValue());
        box.getBorder().setRadius(size);
    }

}
