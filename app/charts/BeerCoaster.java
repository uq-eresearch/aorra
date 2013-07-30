package charts;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

import org.jfree.text.TextUtilities;
import org.jfree.ui.TextAnchor;

public class BeerCoaster implements Dimensions {

    private static final Color NOT_EVALUATED = new Color(229,229,229);

    public static enum Condition {

        VERY_GOOD("Very good", new Color(0,118,70)),
        GOOD("Good", new Color(168,198,162)),
        MODERATE("Moderate", new Color(252,203,38)),
        POOR("Poor", new Color(244,141,64)),
        VERY_POOR("Very poor", new Color(233,44,48));

        private final String label;
        private final Color color;

        Condition(String label, Color color) {
            this.label = label;
            this.color = color;
        }

        public String getLabel() {
            return label;
        }

        public Color getColor() {
            return color;
        }

    }

    public static enum Rotation {
        CLOCKWISE, COUNTER_CLOCKWISE;
    }

    public static enum Indicator {
        CHLOROPHYLL_A("Chlorophyll \u03b1", 90, -60, Rotation.CLOCKWISE, 30),
        TOTAL_SUSPENDED_SOLIDS("Total suspended solids", 30, -60, Rotation.COUNTER_CLOCKWISE, 90),
        SETTLEMENT("Change", -30, -30, Rotation.COUNTER_CLOCKWISE, 135),
        JUVENILE("Juvenile", -60, -30, Rotation.COUNTER_CLOCKWISE, 165),
        ALGAE("Macroalgae", -90, -30, Rotation.COUNTER_CLOCKWISE, 195),
        COVER("Cover", -120, -30, Rotation.COUNTER_CLOCKWISE, 225),
        ABUNDANCE("Abundance", -150, -40, Rotation.CLOCKWISE, 260),
        REPRODUCTION("Reproduction", -190, -40, Rotation.CLOCKWISE, 300),
        NUTRIENT_STATUS("Nutrient status", -230, -40, Rotation.CLOCKWISE, 340);

        private final String name;
        private final int startAngle;
        private final int arcAngle;
        private final Rotation textDirection;
        private final int textAngle;

        Indicator(String name, int startAngle, int arcAngle, Rotation textDirection, int textAngle) {
            this.name = name;
            this.startAngle = startAngle;
            this.arcAngle = arcAngle;
            this.textDirection = textDirection;
            this.textAngle = textAngle;
        }

        public String getName() {
            return name;
        }

        public int getStartAngle() {
            return startAngle;
        }

        public int getArcAngle() {
            return arcAngle;
        }

        public Rotation getTextDirection() {
            return textDirection;
        }

        public int getTextAngle() {
            return textAngle;
        }
    }

    public static enum Category {

        WATER_QUALITY("Water quality", 90, -120, Rotation.CLOCKWISE, 60, "waterquality.png"),
        CORAL("Coral", -30, -120, Rotation.COUNTER_CLOCKWISE, 180, "coral.png"),
        SEAGRASS("Seagrass", -150, -120, Rotation.CLOCKWISE, 300, "seagrass.png");

        private String name;
        private final int startAngle;
        private final int arcAngle;
        private final Rotation textDirection;
        private final int textAngle;
        private final String imageName;

        Category(String name, int startAngle, int arcAngle, Rotation textDirection,
                int textAngle, String imageName) {
            this.name = name;
            this.startAngle = startAngle;
            this.arcAngle = arcAngle;
            this.textDirection = textDirection;
            this.textAngle = textAngle;
            this.imageName = imageName;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getStartAngle() {
            return startAngle;
        }

        public int getArcAngle() {
            return arcAngle;
        }

        public Rotation getTextDirection() {
            return textDirection;
        }

        public int getTextAngle() {
            return textAngle;
        }

        public BufferedImage getImage() {
            try {
                return GraphUtils.getImage("images/"+imageName);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private static final float BORDER_WIDTH = 1.5f;
    private static final float CATEGORY_BORDER_WIDTH = 4.0f;
    private static final Color CATEGORY_FONT_COLOR = Color.BLACK;
    private static final Color INDICATOR_FONT_COLOR = Color.BLACK;
    private static final Color LEGEND_FONT_COLOR = Color.BLACK;
    private static final Font CATEGORY_FONT = new Font(Font.SANS_SERIF, Font.BOLD, 18);
    private static final Font INDICATOR_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    private static final Font LEGEND_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 18);
    private static final Font NOT_EVALUATED_FONT = new Font(Font.SANS_SERIF, Font.PLAIN, 17);
    // The legend will be placed at the LEGEND_ANGLE measured from the center of the beer coaster
    private static final int LEGEND_ANGLE = 79;

//    private int width;
//    private int height;
    private Dimension dimension = new Dimension(510,380);
    private int bcx;
    private int bcy;
    private float bcr;
    private GraphUtils g;
    // indicator condition
    private Condition[] iCondition = new Condition[Indicator.values().length];
    // category condition
    private Condition[] cCondition = new Condition[Category.values().length];

    private Condition overall;

//    public BeerCoaster(int width, int height) {
//        this.width = width;
//        this.height = height;
//    }

    public Condition getCondition(Indicator indicator) {
        return iCondition[indicator.ordinal()];
    }

    public void setCondition(Indicator indicator, Condition condition) {
        iCondition[indicator.ordinal()] = condition;
    }

    public Condition getCondition(Category category) {
        return cCondition[category.ordinal()];
    }

    public void setCondition(Category category, Condition condition) {
        cCondition[category.ordinal()] = condition;
    }

    public Condition getOverallCondition() {
        return overall;
    }

    public void setOverallCondition(Condition overall) {
        this.overall = overall;
    }

    private void drawArc(float radius, int startAngle, int arcAngle, Condition condition,
            String label, float labelRadius, int labelAngle, Rotation labelDirection,
            Color fc, GraphUtils.TextAnchor anchor) {
        Stroke stroke = g.getGraphics().getStroke();
        try {
            if(condition!=null) {
                g.getGraphics().setColor(condition.getColor());
            } else {
                g.getGraphics().setColor(NOT_EVALUATED);
            }
            g.fillArc(bcx, bcy, radius, startAngle, arcAngle);
            g.getGraphics().setColor(Color.WHITE);
            g.getGraphics().setStroke(new BasicStroke(BORDER_WIDTH));
            g.drawArc(bcx, bcy, radius, startAngle, arcAngle);
            g.getGraphics().setColor(fc);
            g.drawCircleText(label, bcx, bcy, labelRadius,
                    GraphUtils.toRadians(labelAngle), labelDirection == Rotation.CLOCKWISE, anchor);
//             draw following circle to check if the anchor is working
//            g.drawCircle(bcx, bcy, labelRadius);
        } finally {
            g.getGraphics().setStroke(stroke);
        }
    }

    private void drawIndicator(Indicator indicator) {
        g.getGraphics().setFont(INDICATOR_FONT);
        FontMetrics fm = g.getGraphics().getFontMetrics();
        drawArc(bcr, indicator.getStartAngle(), indicator.getArcAngle(),
                getCondition(indicator), indicator.getName(), bcr - (fm.getDescent() + fm.getAscent()),
                indicator.getTextAngle(), indicator.getTextDirection(),
                INDICATOR_FONT_COLOR, GraphUtils.TextAnchor.BASELINE);
    }

    private void drawCategory(Category category) {
        g.getGraphics().setFont(CATEGORY_FONT);
//        FontMetrics fm = g.getGraphics().getFontMetrics();
        drawArc(bcr/3*2, category.getStartAngle(), category.getArcAngle(),
                getCondition(category), category.getName(), bcr/2,
                category.getTextAngle(), category.getTextDirection(),
                CATEGORY_FONT_COLOR, GraphUtils.TextAnchor.CENTER);
    }

    private void drawCategoryBorder() {
        Graphics2D g2d = g.getGraphics();
        Stroke stroke = g2d.getStroke();
        AffineTransform transform = g2d.getTransform();
        try {
            g2d.setColor(Color.WHITE);
            g2d.setStroke(new BasicStroke(CATEGORY_BORDER_WIDTH));
            g2d.drawLine(bcx, bcy, bcx, (int)(bcy-bcr));
            g2d.setTransform(AffineTransform.getRotateInstance(GraphUtils.toRadians(120), bcx, bcy));
            g2d.drawLine(bcx, bcy, bcx, (int)(bcy-bcr));
            g2d.setTransform(AffineTransform.getRotateInstance(GraphUtils.toRadians(240), bcx, bcy));
            g2d.drawLine(bcx, bcy, bcx, (int)(bcy-bcr));
        } finally {
            g2d.setStroke(stroke);
            g2d.setTransform(transform);
        }
    }

    private void drawLegend() {
        g.getGraphics().setFont(LEGEND_FONT);
        g.setColor(LEGEND_FONT_COLOR);
        FontMetrics fm = g.getGraphics().getFontMetrics();
        FontRenderContext frc = g.getGraphics().getFontRenderContext();
        Point2D.Double ptSrc = new Point2D.Double(0,-(bcr+fm.getHeight()));
        Point2D.Double ptDst = new Point2D.Double();
        AffineTransform at = new AffineTransform();
        at.concatenate(AffineTransform.getTranslateInstance(bcx, bcy));
        at.concatenate(AffineTransform.getRotateInstance(
                GraphUtils.toRadians(LEGEND_ANGLE)));
        at.transform(ptSrc, ptDst);
        float x = (float)ptDst.getX();
        float y = (float)ptDst.getY();
        for(Condition condition : Condition.values()) {
            char ch[] = ("X "+condition.getLabel()).toCharArray();
            GlyphVector gv = LEGEND_FONT.createGlyphVector(frc, ch);
            for(int i=0;i<gv.getNumGlyphs();i++) {
                Shape glyph;
                if(i==0) {
                    glyph = gv.getGlyphVisualBounds(i);
                    g.setColor(condition.getColor());
                } else {
                    glyph = gv.getGlyphOutline(i);
                    g.setColor(LEGEND_FONT_COLOR);
                }
                AffineTransform transform = new AffineTransform();
                transform.concatenate(AffineTransform.getTranslateInstance(x, y));
                glyph = transform.createTransformedShape(glyph);
                g.getGraphics().fill(glyph);
            }
            y += fm.getHeight();
        }

    }

    private void drawMarineCondition() {
        if(getOverallCondition() != null) {
            g.setColor(getOverallCondition().getColor());
        } else {
            g.setColor(Color.lightGray);
        }
        g.fillCircle(bcx, bcy, bcr/3);
        g.setStroke(CATEGORY_BORDER_WIDTH);
        g.setColor(Color.white);
        g.drawCircle(bcx, bcy, bcr/3);
        g.setColor(CATEGORY_FONT_COLOR);
        g.getGraphics().setFont(CATEGORY_FONT);
        TextUtilities.drawAlignedString("Marine", g.getGraphics(), bcx, bcy, TextAnchor.BOTTOM_CENTER);
        TextUtilities.drawAlignedString("condition", g.getGraphics(), bcx, bcy, TextAnchor.TOP_CENTER);
    }

    private void drawCoralNotEvaluated() {
        if(getCondition(Category.CORAL) == null) {
            g.setColor(Color.black);
            g.getGraphics().setFont(NOT_EVALUATED_FONT);
            TextUtilities.drawAlignedString("Not evaluated",
                    g.getGraphics(), bcx, bcy+(bcr*2/3), TextAnchor.BASELINE_CENTER);
        }
    }

    private void draw(Graphics2D graphics) {
        Graphics2D g2d = (Graphics2D) graphics.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g = new GraphUtils(g2d);
            g2d.setColor(Color.WHITE);
            g2d.fillRect(0, 0, dimension.width, dimension.height);

            g2d.setFont(CATEGORY_FONT);
            FontMetrics fm = g.getGraphics().getFontMetrics();
//            int radius = Math.min(dimension.width, dimension.height)/2;
            // measured from original screenshoot
            int radius = 188;
            this.bcr = radius - fm.getHeight();
            this.bcx = radius;
            this.bcy = radius;

            for(Indicator indicator : Indicator.values()) {
                drawIndicator(indicator);
            }
            for(Category category : Category.values()) {
                drawCategory(category);
            }
            drawCategoryBorder();
            drawMarineCondition();
            drawCoralNotEvaluated();
            drawLegend();
        } finally {
            g2d.dispose();
            g=null;
        }
    }

    @Override
    public void draw(Graphics2D graphics, Rectangle2D area) {
        if(dimension == null){
            dimension = new Dimension((int)area.getWidth(), (int)area.getHeight());
        }
        draw(graphics);
    }

    @Override
    public Dimension getDimension() {
        return new Dimension(dimension);
    }

    @Override
    public void setDimension(Dimension dimension) {
        throw new RuntimeException("can't render with other dimensions, try scaling the result image");
    }
}
