package boxrenderer.xhtml;

import boxrenderer.Align;
import boxrenderer.Box;

public class BackgroundPosition extends AbstractCssStyle {

    @Override
    public void style(Box box) throws Exception {
        String value = getProperty().getValue();
        Align align = Align.valueOf(value.toUpperCase());
        if(align != null) {
            box.setBackgroundPosition(align);
        }
    }

}
