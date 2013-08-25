package boxrenderer.xhtml;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import boxrenderer.Box;
import boxrenderer.TableCellBox;
import boxrenderer.TableCellBox.VAlign;


public class VerticalAlignCssStyle extends AbstractCssStyle implements CssStyle {

    private static final Logger logger = LoggerFactory.getLogger(VerticalAlignCssStyle.class);

    @Override
    public void style(Box box) {
        String align = getProperty().getValue();
        // currently only works with table cells
        if((box instanceof TableCellBox) && !StringUtils.isBlank(align)) {
            TableCellBox tbox = (TableCellBox)box;
            VAlign a = VAlign.valueOf(align.toUpperCase());
            if(a != null) {
                tbox.setValign(a);
            } else {
                logger.warn("unknown vertical align "+align);
            }
        }
    }

}
