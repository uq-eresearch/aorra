package boxrenderer.xhtml;

import boxrenderer.Box;
import boxrenderer.TableBox;

public class BorderCollapseCssStyle extends AbstractCssStyle implements
        CssStyle {

    @Override
    public void style(Box box) {
        if("collapse".equals(getProperty().getValue()) && (box instanceof TableBox)) {
            ((TableBox)box).setBorderCollapse(true);
        } else if("separate".equals(getProperty().getValue()) && (box instanceof TableBox)) {
            ((TableBox)box).setBorderCollapse(false);
        }
    }

}
