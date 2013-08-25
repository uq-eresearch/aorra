package boxrenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Shape;
import java.awt.geom.Rectangle2D;

import org.apache.commons.lang3.StringUtils;
import org.jfree.text.TextBlock;
import org.jfree.text.TextBlockAnchor;
import org.jfree.text.TextUtilities;
import org.jfree.ui.HorizontalAlignment;

public class TextBox extends AbstractBox {

    private String text;

    private Font font;

    private FontManager fontManager;

    public TextBox(String text) {
        this.text = text;
        getBorder().setApplicable(false);
        getPadding().setApplicable(false);
        getMargin().setApplicable(false);
        setInline(true);
    }

    public TextBox(String text, FontManager fontManager) {
        this(text);
        this.fontManager = fontManager;
    }

    public TextBox(String text, Font font) {
        this(text);
        this.font = font;
    }

    @Override
    public Dimension getContentDimension(Graphics2D g2) throws Exception {
        setupFont();
        TextBlock block = TextUtilities.createTextBlock(text, font, getTextColor());
        Shape shape = block.calculateBounds(g2, 0, 0, TextBlockAnchor.TOP_LEFT, 0, 0, 0);
        Rectangle2D rect = shape.getBounds2D();
        return new Dimension((int)rect.getMaxX()+1, (int)rect.getMaxY()+1);
    }

    private Paint getTextColor() {
        Paint paint = getColor();
        return paint!=null?paint:Color.black;
    }

    @Override
    public void renderContent(Graphics2D g2) throws Exception {
        setupFont();
        g2.setPaint(getTextColor());
        TextBlock block = TextUtilities.createTextBlock(text, font, getTextColor());
        block.setLineAlignment(HorizontalAlignment.LEFT);
        block.draw(g2, 0, 0, TextBlockAnchor.TOP_LEFT, 0, 0, 0);
    }

    private void setupFont() {
        if(font == null) {
            if(fontManager != null) {
                font = fontManager.getFont(getFFamily(), getFStyle(), getFSize());
            } else {
                font = new Font(getFFamily(), getFStyle(), getFSize());
            }
        }
    }

    private String getFFamily() {
        String ff = getFontFamily();
        if(StringUtils.isBlank(ff)) {
            ff = "Serif";
        }
        return ff;
    }

    private int getFSize() {
        int size = getFontSize();
        if(size <=0) {
            size = 12;
        }
        return size;
    }

    private int getFStyle() {
        int style = Font.PLAIN;
        if(isBold()) {
            style |= Font.BOLD;
        }
        return style;
    }

}
