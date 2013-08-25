package boxrenderer.xhtml;

import java.awt.Paint;

import boxrenderer.Box;


public class BackgroundColorCssStyle extends AbstractCssStyle {

    @Override
    public void style(Box box) {
        Paint paint = Colors.getPaint(this.getProperty().getValue());
        box.setBackground(paint);
    }

}
