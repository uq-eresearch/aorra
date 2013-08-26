package boxrenderer.xhtml;

import graphics.GraphUtils;

import org.apache.commons.lang3.StringUtils;

import boxrenderer.Box;


public class RotationCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) {
        double rotation = 0;
        String val = getProperty().getValue();
        if(StringUtils.endsWith(val, "deg")) {
            rotation = GraphUtils.toRadians(Double.parseDouble(StringUtils.removeEnd(val, "deg")));
        } else {
            rotation = Double.parseDouble(val);
        }
        box.setRotate(rotation);
    }
}
