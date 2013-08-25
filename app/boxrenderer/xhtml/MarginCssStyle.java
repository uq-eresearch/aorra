package boxrenderer.xhtml;

import org.apache.commons.lang3.StringUtils;

import boxrenderer.Box;


public class MarginCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) {
        int size = Integer.parseInt(StringUtils.removeEnd(getProperty().getValue(), "px"));
        String prop = getProperty().getProperty();
        if("margin".equals(prop)) {
            box.getMargin().setSize(size);
        } else if("margin-top".equals(prop)) {
            box.getMargin().setTop(size);
        } else if("margin-bottom".equals(prop)) {
            box.getMargin().setBottom(size);
        } else if("margin-left".equals(prop)) {
            box.getMargin().setLeft(size);
        } else if("margin-right".equals(prop)) {
            box.getMargin().setRight(size);
        }
    }

}
