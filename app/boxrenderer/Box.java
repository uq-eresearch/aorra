package boxrenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.util.List;
import java.util.Map;

import com.osbcp.cssparser.Rule;

public interface Box {

    /**
     * minimum content dimension
     */
    public Dimension getContentDimension(Graphics2D g2) throws Exception;

    /**
     * total minimum dimension, including margin, border, padding and content
     */
    public Dimension getDimension(Graphics2D g2) throws Exception;

    /**
     * render the box at position 0,0 (top left corner)
     */
    public void render(Graphics2D g2) throws Exception;

    public void setMargin(Margin margin);

    public Margin getMargin();

    public void setPadding(Padding padding);

    public Padding getPadding();

    public void setBorder(Border border);

    public Border getBorder();

    public void setWidth(int width);

    public void setHeight(int height);

    public void setParent(Box parent);

    public Box getParent();

    public void setId(String id);

    public String getId();

    public void addCssClass(String cssClass);

    public boolean hasCssClass(String cssClass);

    public void setTag(String tagname);

    public String getTag();

    public void setAttributes(Map<String, String> attributes);

    public void setBackground(Paint background);

    public Paint getBackground();

    public void addRule(Rule cssrule);

    public List<Rule> getCssRules();

    public String getFontFamily();

    public void setFontFamily(String fontFamily);

    public int getFontSize();

    public void setFontSize(int fontSize);

    public Paint getColor();

    public void setColor(Paint color);

    public void setBold(boolean bold);

    public boolean isBold();

    public boolean isInline();

    public double getRotate();

    public void setRotate(double rotate);

    public String getRotationPoint();

    public void setRotationPoint(String rotationPoint);

    public ImageRenderer getBackgroundImage();

    public void setBackgroundImage(ImageRenderer backgroundImage);

    public Paint getBackgroundTexture();

    public void setBackgroundTexture(Paint backgroundTexture);

    public void setLinearGradient(Color from, Color to);

    public void setBackgroundPosition(Align backgroundPosition);

    public void setBackgroundSize(Size backgroundSize);
}
