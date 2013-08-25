package boxrenderer.xhtml;

import org.apache.commons.lang3.tuple.Pair;

import boxrenderer.Box;
import boxrenderer.Size;


public class BackgroundSize extends AbstractCssStyle {

    @Override
    public void style(Box box) throws Exception {
        String value = getProperty().getValue();
        if(Sizes.isPercentage(value)) {
            final int percent = Sizes.getPercentage(value);
            box.setBackgroundSize(new Size() {
                @Override
                public Pair<Double, Double> getScale(double contentWidth,
                        double contentHeight, double width, double height) {
                    double sx = (contentWidth / 100.0 * percent) / width;
                    return Pair.of(sx, sx);
                }});
        } else if(Sizes.isPixel(value)) {
            final int px = Sizes.getPixelSize(value);
            box.setBackgroundSize(new Size() {
                @Override
                public Pair<Double, Double> getScale(double contentWidth,
                        double contentHeight, double width, double height) {
                    double sx = (double)px / width;
                    return Pair.of(sx, sx);
                }});
        } else {
            throw new RuntimeException("background-size not supported in this format: "+value);
        }
    }

}
