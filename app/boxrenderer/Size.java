package boxrenderer;

import org.apache.commons.lang3.tuple.Pair;

public interface Size {

    public Pair<Double, Double> getScale(double contentWidth, double contentHeight, double width, double height);

}
