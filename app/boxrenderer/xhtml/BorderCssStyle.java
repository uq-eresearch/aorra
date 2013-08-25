package boxrenderer.xhtml;

import java.awt.Paint;

import org.apache.commons.lang3.StringUtils;

import boxrenderer.Box;


public class BorderCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) throws Exception {
        String[] values = StringUtils.split(getProperty().getValue());
        int size = Sizes.getPixelSize(values[0]);
        box.getBorder().setSize(size);
        Paint paint = Colors.getPaint(values[2]);
        box.getBorder().setPaint(paint);
    }

}
