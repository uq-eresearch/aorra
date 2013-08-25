package boxrenderer.xhtml;

import org.apache.commons.lang3.StringUtils;

import boxrenderer.Box;


public class PaddingCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) {
        int size = Integer.parseInt(StringUtils.removeEnd(getProperty().getValue(), "px"));
        String prop = getProperty().getProperty();
        if("padding".equals(prop)) {
            box.getPadding().setSize(size);
        } else if("padding-top".equals(prop)) {
            box.getPadding().setTop(size);
        } else if("padding-bottom".equals(prop)) {
            box.getPadding().setBottom(size);
        } else if("padding-left".equals(prop)) {
            box.getPadding().setLeft(size);
        } else if("padding-right".equals(prop)) {
            box.getPadding().setRight(size);
        }
    }

}
