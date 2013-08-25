package boxrenderer.xhtml;

import boxrenderer.Box;

public class BorderWidthCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) {
        int size = Sizes.getPixelSize(getProperty().getValue());
        String prop = getProperty().getProperty();
        if("border-width".equals(prop)) {
            box.getBorder().setSize(size);
        } else if("border-top-width".equals(prop)) {
            box.getBorder().setTop(size);
        } else if("border-bottom-width".equals(prop)) {
            box.getBorder().setBottom(size);
        } else if("border-left-width".equals(prop)) {
            box.getBorder().setLeft(size);
        } else if("border-right-width".equals(prop)) {
            box.getBorder().setRight(size);
        }
    }

}
