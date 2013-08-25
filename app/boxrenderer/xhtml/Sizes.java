package boxrenderer.xhtml;

import org.apache.commons.lang3.StringUtils;

public class Sizes {

    public static int getPixelSize(String definition) {
        if(isPixel(definition)) {
            return Integer.parseInt(StringUtils.removeEnd(StringUtils.strip(definition), "px"));
        } else {
            throw new RuntimeException(String.format("'%s' not in pixel", definition));
        }
        
    }

    public static boolean isPixel(String definition) {
        return StringUtils.endsWith(StringUtils.strip(definition), "px");
    }

    public static boolean isPercentage(String definition) {
        return StringUtils.endsWith(StringUtils.strip(definition), "%");
    }

    public static int getPercentage(String definition) {
        if(isPercentage(definition)) {
            return Integer.parseInt(StringUtils.removeEnd(StringUtils.strip(definition), "%"));
        } else {
            throw new RuntimeException(String.format("'%s' not in %", definition));
        }
    }

}
