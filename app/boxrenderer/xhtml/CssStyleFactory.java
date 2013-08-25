package boxrenderer.xhtml;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import boxrenderer.Resolver;

import com.google.common.collect.Maps;
import com.osbcp.cssparser.PropertyValue;


public class CssStyleFactory {

    private static final Logger logger = LoggerFactory.getLogger(CssStyleFactory.class);

    public Map<String, Class<?>> styleClassMap = Maps.newHashMap();

    private Resolver resolver;

    public CssStyleFactory() {
        styleClassMap.put("background-color", BackgroundColorCssStyle.class);
        styleClassMap.put("font-family", FontFamilyCssStyle.class);
        styleClassMap.put("font-size", FontSizeCssStyle.class);
        styleClassMap.put("border-color", BorderColorCssStyle.class);
        styleClassMap.put("border-width", BorderWidthCssStyle.class);
        styleClassMap.put("border-bottom-width", BorderWidthCssStyle.class);
        styleClassMap.put("border-left-width", BorderWidthCssStyle.class);
        styleClassMap.put("border-right-width", BorderWidthCssStyle.class);
        styleClassMap.put("border-top-width", BorderWidthCssStyle.class);
        styleClassMap.put("border-collapse", BorderCollapseCssStyle.class);
        // css shorthands are not properly implemented and collide with e.g. border-color and boder-width
        //styleClassMap.put("border", BorderCssStyle.class);
        styleClassMap.put("color", ColorCssStyle.class);
        styleClassMap.put("padding", PaddingCssStyle.class);
        styleClassMap.put("padding-bottom", PaddingCssStyle.class);
        styleClassMap.put("padding-left", PaddingCssStyle.class);
        styleClassMap.put("padding-right", PaddingCssStyle.class);
        styleClassMap.put("padding-top", PaddingCssStyle.class);
        styleClassMap.put("font-weight", FontWeightCssStyle.class);
        styleClassMap.put("width", WidthCssStyle.class);
        styleClassMap.put("height", HeightCssStyle.class);
        styleClassMap.put("text-align", TextAlignCssStyle.class);
        styleClassMap.put("vertical-align", VerticalAlignCssStyle.class);
        styleClassMap.put("margin", MarginCssStyle.class);
        styleClassMap.put("margin-bottom", MarginCssStyle.class);
        styleClassMap.put("margin-left", MarginCssStyle.class);
        styleClassMap.put("margin-right", MarginCssStyle.class);
        styleClassMap.put("margin-top", MarginCssStyle.class);
        styleClassMap.put("rotation", RotationCssStyle.class);
        styleClassMap.put("rotation-point", RotationPointCssStyle.class);
        styleClassMap.put("background-image", BackgroundImageCssStyle.class);
        styleClassMap.put("background-texture", BackgroundTextureCssStyle.class);
        styleClassMap.put("border-radius", BorderRadiusCssStyle.class);
        styleClassMap.put("background-position", BackgroundPosition.class);
        styleClassMap.put("background-size", BackgroundSize.class);
    }

    public CssStyleFactory(Resolver resolver) {
        this();
        this.resolver = resolver;
    }

    public CssStyle getCssStyle(PropertyValue p) throws Exception {
        Class<?> styleClass = styleClassMap.get(p.getProperty());
        AbstractCssStyle cssStyle = null;
        if(styleClass != null) {
            cssStyle = (AbstractCssStyle)styleClass.newInstance();
            cssStyle.setProperty(p);
            cssStyle.setResolver(resolver);
        } else {
            logger.debug(String.format(
                    "css style %s not implemented, ignoring...",p.getProperty()));
        }
        return cssStyle;
    }

}
