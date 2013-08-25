package boxrenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.osbcp.cssparser.Rule;


public abstract class AbstractBox implements Box {

    private static final Logger logger = LoggerFactory.getLogger(AbstractBox.class);

    private Margin margin = new Margin();

    private Border border = new Border();

    private Padding padding = new Padding();

    private Paint background;

    private int width;

    private int height;

    private Box parent;

    private String id;

    private String tag;

    private Set<String> cssClasses = Sets.newHashSet();

    private Map<String, String> attributes;

    private List<Rule> cssRules = Lists.newArrayList();

    private String fontFamily;

    private int fontSize;

    private Paint color;

    private boolean bold;

    private boolean inline = false;

    private double rotate;

    private String rotationPoint = "left top";

    private ImageRenderer backgroundImage;

    // TODO also support vertical align of background
    private Align backgroundPosition = Align.LEFT;

    private Size backgroundSize;

    private Paint backgroundTexture;

    private Color linearGradientFrom;

    private Color linearGradientTo;

    public AbstractBox() {
    }

    @Override
    public Dimension getDimension(Graphics2D g2) throws Exception {
        Dimension d = getRotationBoxDimension(g2);
        int w = d.width;
        int h = d.height;
        if(rotate!=0) {
            Dimension dim = getRotatedDimension(new Dimension(w,h));
            w = dim.width;
            h = dim.height;
        }
        if(margin.isApplicable()) {
            w+=margin.getLeft()+margin.getRight();
            h+=margin.getTop()+margin.getBottom();
        }
        return new Dimension(w,h);
    }

    private Dimension getRotatedDimension(Dimension d) {
        Rectangle2D r2 = getRotatedBounds(d);
        int w = (int)Math.round(r2.getMaxX() - r2.getMinX());
        int h = (int)Math.round(r2.getMaxY() - r2.getMinY());
        logger.debug(String.format("rotated (%s deg) dimensions are w: %s h: %s",
                GraphUtils.toDegrees(rotate), w, h));
        return new Dimension(w,h);
    }

    private Rectangle2D getRotatedBounds(Dimension d) {
        logger.debug(String.format("dimensions are w: %s h: %s", d.width, d.height));
        Rectangle r = new Rectangle(0,0,d.width,d.height);
        AffineTransform rot = AffineTransform.getRotateInstance(rotate);
        Shape rotated = rot.createTransformedShape(r);
        Rectangle2D r2 = rotated.getBounds2D();
        logger.debug(String.format("maxX %s", r2.getMaxX()));
        logger.debug(String.format("minX %s", r2.getMinX()));
        logger.debug(String.format("maxY %s", r2.getMaxY()));
        logger.debug(String.format("minY %s", r2.getMinY()));
        return r2;
    }

    private void setRotation(Graphics2D g, int width, int height, Rectangle2D rotatedBounds) {
        Point rotationPoint = getDecodedRotationPoint(width, height);
        AffineTransform transform = g.getTransform();
        transform.concatenate(AffineTransform.getTranslateInstance(-rotatedBounds.getMinX(), -rotatedBounds.getMinY()));
        transform.concatenate(AffineTransform.getRotateInstance(rotate, rotationPoint.x, rotationPoint.y));
        g.setTransform(transform);
    }

    private Point getDecodedRotationPoint(int width, int height) {
        int px;
        int py;
        String[] s = StringUtils.split(rotationPoint);
        String s0 = s[0];
        if(StringUtils.endsWith(s0, "%")) {
            px = Math.round((float)width/100*Float.parseFloat(StringUtils.removeEnd(s0, "%")));
        } else if("left".equals(s0)) {
            px = 0;
        } else if("right".equals(s0)) {
            px = width;
        } else if("center".equals(s0)) {
            px = width/2;
        } else {
            throw new RuntimeException("unknown rotation point "+s0);
        }

        String s1; 
        if(s.length>=2) {
            s1 = s[1];
        } else {
            s1 = "center";
        }
        if(StringUtils.endsWith(s1, "%")) {
            py = Math.round((float)height/100*Float.parseFloat(StringUtils.removeEnd(s1, "%")));
        } else if("top".equals(s1)) {
            py = 0;
        } else if("bottom".equals(s1)) {
            py = height;
        } else if("center".equals(s1)) {
            py = height/2;
        } else {
            throw new RuntimeException("unknown rotation point "+s1);
        }
        return new Point(px,py);
    }

    private Dimension getRotationBoxDimension(Graphics2D g2) throws Exception {
        Dimension d = getContentDimension(g2);
        int w = Math.max(d.width, getWidth());
        int h = Math.max(d.height, getHeight());
        if(padding.isApplicable()) {
            w+=padding.getLeft()+padding.getRight();
            h+=padding.getTop()+padding.getBottom();
        }
        if(border.isApplicable()) {
            w+=border.getLeft()+border.getRight();
            h+=border.getTop()+border.getBottom();
        }
        return new Dimension(w,h);
    }

    @Override
    public void render(Graphics2D g2) throws Exception {
        Graphics2D mInner = margin.render(g2);
        if(rotate!=0) {
            Dimension d = getRotationBoxDimension(g2);
            Rectangle2D rotatedBounds = getRotatedBounds(d);
            setRotation(mInner, d.width, d.height, rotatedBounds);
        }
        Graphics2D bInner = border.render(mInner);
//        padding.setPaint(getBackground());
        padding.setRender(false);
        Paint background = getBackground();
        if(background!=null) {
            bInner.setPaint(background);
            bInner.fill(bInner.getClipBounds());
        }
        if(backgroundTexture!=null) {
            bInner.setPaint(backgroundTexture);
            bInner.fill(bInner.getClipBounds());
        }
        if(linearGradientFrom!=null && linearGradientTo!=null) {
            float x1 = bInner.getClipBounds().x;
            float x2 = bInner.getClipBounds().width;
            float y = 0;
            GradientPaint gradient = new GradientPaint(
                    x1,y,linearGradientFrom, x2, y, linearGradientTo);
            bInner.setPaint(gradient);
            bInner.fill(bInner.getClip());
        }
        Graphics2D pInner = padding.render(bInner);
        if(border.getRadius() > 0) {
            Shape clipShape = border.makeInnerShape(
                    mInner.getClipBounds().width, mInner.getClipBounds().height);
            AffineTransform t = AffineTransform.getTranslateInstance(-border.getLeft(), -border.getTop());
            pInner.setClip(t.createTransformedShape(clipShape));
        }
        drawBackgroundImage(pInner);
        renderContent(pInner);
        pInner.dispose();
        bInner.dispose();
        mInner.dispose();
    }

    private void drawBackgroundImage(Graphics2D g2) {
        Graphics2D g0 = null;
        try {
            g0 = (Graphics2D)g2.create();
            if(backgroundImage!=null) {
                AffineTransform t = g0.getTransform();
                Dimension d = backgroundImage.getDimension(g0);
                double cw = g0.getClipBounds().getWidth();
                double ch = g0.getClipBounds().getHeight();
                if(backgroundSize != null) {
                    Pair<Double, Double> scale = backgroundSize.getScale(
                            cw, ch, d.getWidth(), d.getHeight());
                    t.concatenate(AffineTransform.getTranslateInstance((cw/2.0), (ch/2.0)));
                    t.concatenate(AffineTransform.getScaleInstance(scale.getLeft(), scale.getRight()));
                    t.concatenate(AffineTransform.getTranslateInstance(-(cw/2.0), -(ch/2.0)));
                }
                if(backgroundPosition == Align.CENTER) {
                    t.concatenate(AffineTransform.getTranslateInstance(
                            (cw - d.getWidth()) / 2.0, (ch - d.getHeight()) / 2.0));
                }
                //TODO also support align right
                g0.setTransform(t);
                backgroundImage.render(g0);
            }
        } catch(Exception e) {
            throw new RuntimeException("failed to render background image", e);
        } finally {
            if(g0 != null) {
                g0.dispose();
            }
        }
    }

    public void setBackground(Paint background) {
        this.background = background;
    }

    public abstract void renderContent(Graphics2D g2) throws Exception;

    public Paint getBackground() {
        return background;
    }

    public void setMargin(Margin margin) {
        this.margin = margin;
    }

    public void setPadding(Padding padding) {
        this.padding = padding;
    }

    public void setBorder(Border border) {
        this.border = border;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public Margin getMargin() {
        return margin;
    }

    public Border getBorder() {
        return border;
    }

    public Padding getPadding() {
        return padding;
    }

    public void addCssClass(String cssClass) {
        cssClasses.add(cssClass);
    }

    public boolean hasCssClass(String cssClass) {
        return cssClasses.contains(cssClass);
    }

    public Box getParent() {
        return parent;
    }

    public void setParent(Box parent) {
        this.parent = parent;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void addRule(Rule cssrule) {
        cssRules.add(cssrule);
    }

    public List<Rule> getCssRules() {
        return cssRules;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public int getFontSize() {
        return fontSize;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public Paint getColor() {
        return color;
    }

    public void setColor(Paint color) {
        this.color = color;
    }

    public boolean isBold() {
        return bold;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isInline() {
        return this.inline;
    }

    public void setInline(boolean inline) {
        this.inline = inline;
    }

    public double getRotate() {
        return rotate;
    }

    public void setRotate(double rotate) {
        this.rotate = rotate;
    }

    public String getRotationPoint() {
        return rotationPoint;
    }

    public void setRotationPoint(String rotationPoint) {
        this.rotationPoint = rotationPoint;
    }

    public ImageRenderer getBackgroundImage() {
        return backgroundImage;
    }

    public void setBackgroundImage(ImageRenderer backgroundImage) {
        this.backgroundImage = backgroundImage;
    }

    public Paint getBackgroundTexture() {
        return backgroundTexture;
    }

    public void setBackgroundTexture(Paint backgroundTexture) {
        this.backgroundTexture = backgroundTexture;
    }

    public void setLinearGradient(Color from, Color to) {
        linearGradientFrom = from;
        linearGradientTo = to;
    }

    public void setBackgroundPosition(Align backgroundPosition) {
        this.backgroundPosition = backgroundPosition;
    }

    public void setBackgroundSize(Size backgroundSize) {
        this.backgroundSize = backgroundSize;
    }

}
