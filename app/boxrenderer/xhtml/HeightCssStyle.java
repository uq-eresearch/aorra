package boxrenderer.xhtml;

import org.apache.commons.lang3.StringUtils;

import boxrenderer.Box;


public class HeightCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) {
        box.setHeight(Integer.parseInt(StringUtils.removeEnd(getProperty().getValue(), "px")));
    }

}
