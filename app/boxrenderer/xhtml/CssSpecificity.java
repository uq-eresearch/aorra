package boxrenderer.xhtml;

import com.osbcp.cssparser.Rule;
import com.osbcp.cssparser.Selector;

//http://www.webteacher.ws/2008/05/19/tip-calculate-the-specificity-of-css-selectors/
public class CssSpecificity implements Comparable<CssSpecificity> {

    private int inline;

    private int id;

    private int cssClass;

    private int element;
    

    public CssSpecificity(Rule rule) {
        if(rule.getSelectors().size() == 0) {
            inline++;
        } else {
            for(Selector selector : rule.getSelectors()) {
                if(isIdSelector(selector)) {
                    id++;
                } else if(isClassSelector(selector)) {
                    cssClass++;
                } else {
                    element++;
                }
            }
        }
    }

    private boolean isIdSelector(Selector selector) {
        return selector.toString().startsWith("#");
    }

    private boolean isClassSelector(Selector selector) {
        return selector.toString().startsWith(".");
    }

    @Override
    public int compareTo(CssSpecificity o) {
        int result = inline - o.inline;
        if(result == 0) {
            result = id - o.id;
        }
        if(result == 0) {
            result = cssClass - o.cssClass;
        }
        if(result == 0) {
            result = element - o.element;
        }
        return result;
    }

}
