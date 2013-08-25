package boxrenderer.xhtml;

import boxrenderer.Box;
import boxrenderer.Resolver;

import com.osbcp.cssparser.PropertyValue;


public abstract class AbstractCssStyle implements CssStyle {

    private PropertyValue property;

    private Resolver resolver;

    @Override
    public abstract void style(Box box) throws Exception;

    public PropertyValue getProperty() {
        return property;
    }

    public void setProperty(PropertyValue property) {
        this.property = property;
    }

    public Resolver getResolver() {
        return resolver;
    }

    public void setResolver(Resolver resolver) {
        this.resolver = resolver;
    }

}
