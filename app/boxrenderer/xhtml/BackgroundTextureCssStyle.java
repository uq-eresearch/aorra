package boxrenderer.xhtml;

import java.awt.TexturePaint;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;

import org.apache.commons.lang3.StringUtils;

import boxrenderer.Box;
import boxrenderer.GraphUtils;


public class BackgroundTextureCssStyle extends AbstractCssStyle implements CssStyle {

    @Override
    public void style(Box box) throws Exception {
        String value = getProperty().getValue();
        if(value.startsWith("hatching")) {
            Sizes sizes = new Sizes();
            String[] params = StringUtils.split(value);
            String color = params[1];
            String dash1 = params[2];
            String dash2 = params[3];
            BufferedImage hachtingImg = GraphUtils.createHatchingTexture(
                    Colors.getPaint(color), sizes.getPixelSize(dash1), sizes.getPixelSize(dash2));
            Rectangle2D anchor = new Rectangle2D.Double(0, 0, hachtingImg.getWidth(), hachtingImg.getHeight());
            box.setBackgroundTexture(new TexturePaint(hachtingImg, anchor));
        }
    }

}
