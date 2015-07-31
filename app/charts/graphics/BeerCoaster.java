package charts.graphics;

import static com.google.common.base.Preconditions.checkNotNull;
import graphics.GraphUtils;

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
import java.util.Collections;
import java.util.Map;

import org.jfree.text.TextUtilities;
import org.jfree.ui.TextAnchor;

import charts.Drawable;

public class BeerCoaster implements Drawable {

    public static enum Condition {

      NOT_EVALUATED("Not evaluated", Colors.NOT_EVALUATED),
      VERY_GOOD("Very good", Colors.VERY_GOOD),
      GOOD("Good", Colors.GOOD),
      MODERATE("Moderate", Colors.MODERATE),
      POOR("Poor", Colors.POOR),
      VERY_POOR("Very poor", Colors.VERY_POOR);

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
      CHLOROPHYLL_A("Chlorophyll a", Category.WATER_QUALITY, 90, -60, Rotation.CLOCKWISE, 30),
      TOTAL_SUSPENDED_SOLIDS("Total suspended solids", Category.WATER_QUALITY, 30, -60, Rotation.COUNTER_CLOCKWISE, 90),
      SETTLEMENT("Change", Category.CORAL, -30, -30, Rotation.COUNTER_CLOCKWISE, 135),
      JUVENILE("Juvenile", Category.CORAL, -60, -30, Rotation.COUNTER_CLOCKWISE, 165),
      ALGAE("Macroalgae", Category.CORAL, -90, -30, Rotation.COUNTER_CLOCKWISE, 195),
      COVER("Cover", Category.CORAL, -120, -30, Rotation.COUNTER_CLOCKWISE, 225),
      ABUNDANCE("Abundance", Category.SEAGRASS, -150, -40, Rotation.CLOCKWISE, 260),
      REPRODUCTION("Reproduction", Category.SEAGRASS, -190, -40, Rotation.CLOCKWISE, 300),
      NUTRIENT_STATUS("Nutrient status", Category.SEAGRASS, -230, -40, Rotation.CLOCKWISE, 340);

      private final String name;
      private final Category category;
      private final int startAngle;
      private final int arcAngle;
      private final Rotation textDirection;
      private final int textAngle;

      Indicator(String name, Category category, int startAngle, int arcAngle,
          Rotation textDirection, int textAngle) {
        this.name = name;
        this.category = category;
        this.startAngle = startAngle;
        this.arcAngle = arcAngle;
        this.textDirection = textDirection;
        this.textAngle = textAngle;
      }

      public String getName() {
        return name;
      }

      public Category getCategory() {
        return category;
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

      Category(String name, int startAngle, int arcAngle, Rotation textDirection,
          int textAngle, String imageName) {
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

    private final Dimension dimension = new Dimension(510,380);
    private int bcx;
    private int bcy;
    private float bcr;
    // indicator condition - NOT_EVALUATED by default
    private final Condition[] iCondition = Collections.nCopies(
        Indicator.values().length,
        Condition.NOT_EVALUATED).toArray(new Condition[0]);
    // category condition - NOT_EVALUATED by default
    private final Condition[] cCondition = Collections.nCopies(
        Category.values().length,
        Condition.NOT_EVALUATED).toArray(new Condition[0]);

    private Condition overall = Condition.NOT_EVALUATED;

    private Map<Condition, Color> colors;

    public BeerCoaster() {}

    public BeerCoaster(Map<Condition, Color> colors) {
      this.colors = colors;
    }

    public Condition getCondition(Indicator indicator) {
      return iCondition[indicator.ordinal()];
    }

    public void setCondition(Indicator indicator, Condition condition) {
      iCondition[indicator.ordinal()] = checkNotNull(condition);
    }

    public Condition getCondition(Category category) {
      return cCondition[category.ordinal()];
    }

    public void setCondition(Category category, Condition condition) {
      cCondition[category.ordinal()] = checkNotNull(condition);
    }

    public Condition getOverallCondition() {
      return overall;
    }

    public void setOverallCondition(Condition overall) {
      this.overall = overall;
    }

    private void drawArc(GraphUtils g, float radius, int startAngle, int arcAngle, Condition condition,
            String label, float labelRadius, int labelAngle, Rotation labelDirection,
            Color fc, GraphUtils.TextAnchor anchor) {
      Stroke stroke = g.getGraphics().getStroke();
      g.getGraphics().setColor(getColor(condition));
      g.fillArc(bcx, bcy, radius, startAngle, arcAngle);
      g.getGraphics().setColor(Color.WHITE);
      g.getGraphics().setStroke(new BasicStroke(BORDER_WIDTH));
      g.drawArc(bcx, bcy, radius, startAngle, arcAngle);
      g.getGraphics().setColor(fc);
      g.drawCircleText(label, bcx, bcy, labelRadius,
        GraphUtils.toRadians(labelAngle),
        labelDirection == Rotation.CLOCKWISE, anchor);
      g.getGraphics().setStroke(stroke);
    }

    private void drawIndicator(GraphUtils g, Indicator indicator) {
      g.getGraphics().setFont(INDICATOR_FONT);
      FontMetrics fm = g.getGraphics().getFontMetrics();
      drawArc(g, bcr, indicator.getStartAngle(), indicator.getArcAngle(),
              getCondition(indicator), indicator.getName(), bcr - (fm.getDescent() + fm.getAscent()),
              indicator.getTextAngle(), indicator.getTextDirection(),
              INDICATOR_FONT_COLOR, GraphUtils.TextAnchor.BASELINE);
    }

    private void drawCategory(GraphUtils g, Category category) {
      g.getGraphics().setFont(CATEGORY_FONT);
      drawArc(g, bcr/3*2, category.getStartAngle(), category.getArcAngle(),
              getCondition(category), category.getName(), bcr/2,
              category.getTextAngle(), category.getTextDirection(),
              CATEGORY_FONT_COLOR, GraphUtils.TextAnchor.CENTER);
    }

    private void drawCategoryBorder(GraphUtils g) {
      Graphics2D g2d = g.getGraphics();
      Stroke stroke = g2d.getStroke();
      AffineTransform transform = g2d.getTransform();
      g2d.setColor(Color.WHITE);
      g2d.setStroke(new BasicStroke(CATEGORY_BORDER_WIDTH));
      g2d.drawLine(bcx, bcy, bcx, (int)(bcy-bcr));
      g2d.setTransform(AffineTransform.getRotateInstance(GraphUtils.toRadians(120), bcx, bcy));
      g2d.drawLine(bcx, bcy, bcx, (int)(bcy-bcr));
      g2d.setTransform(AffineTransform.getRotateInstance(GraphUtils.toRadians(240), bcx, bcy));
      g2d.drawLine(bcx, bcy, bcx, (int)(bcy-bcr));
      g2d.setStroke(stroke);
      g2d.setTransform(transform);
    }

    private void drawLegend(GraphUtils g) {
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
        // We don't show "Not Evaluated" in the legend
        if (condition == Condition.NOT_EVALUATED)
          continue;
        char ch[] = ("X "+condition.getLabel()).toCharArray();
        GlyphVector gv = LEGEND_FONT.createGlyphVector(frc, ch);
        for(int i=0;i<gv.getNumGlyphs();i++) {
            Shape glyph;
            if(i==0) {
                glyph = gv.getGlyphVisualBounds(i);
                g.setColor(getColor(condition));
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

    private void drawMarineCondition(GraphUtils g) {
      g.setColor(getColor(getOverallCondition()));
      g.fillCircle(bcx, bcy, bcr/3);
      g.setStroke(CATEGORY_BORDER_WIDTH);
      g.setColor(Color.white);
      g.drawCircle(bcx, bcy, bcr/3);
      g.setColor(CATEGORY_FONT_COLOR);
      g.getGraphics().setFont(CATEGORY_FONT);
      TextUtilities.drawAlignedString("Marine", g.getGraphics(), bcx, bcy, TextAnchor.BOTTOM_CENTER);
      TextUtilities.drawAlignedString("condition", g.getGraphics(), bcx, bcy, TextAnchor.TOP_CENTER);
    }

    private void drawCoralNotEvaluated(GraphUtils g) {
      if (getCondition(Category.CORAL) == Condition.NOT_EVALUATED) {
        g.setColor(Color.black);
        g.getGraphics().setFont(NOT_EVALUATED_FONT);
        TextUtilities.drawAlignedString("Not evaluated",
                g.getGraphics(), bcx, bcy+(bcr*2/3), TextAnchor.BASELINE_CENTER);
      }
    }

    @Override
    public void draw(Graphics2D graphics) {
      Graphics2D g2d = (Graphics2D) graphics.create();
      g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      GraphUtils g = new GraphUtils(g2d);
      g2d.setColor(Color.WHITE);
      g2d.fillRect(0, 0, dimension.width, dimension.height);

      g2d.setFont(CATEGORY_FONT);
      FontMetrics fm = g.getGraphics().getFontMetrics();
      // measured from original screenshoot
      int radius = 188;
      this.bcr = radius - fm.getHeight();
      this.bcx = radius;
      this.bcy = radius;

      for(Indicator indicator : Indicator.values()) {
          drawIndicator(g, indicator);
      }
      for(Category category : Category.values()) {
          drawCategory(g, category);
      }
      drawCategoryBorder(g);
      drawMarineCondition(g);
      drawCoralNotEvaluated(g);
      drawLegend(g);
      g2d.dispose();
    }

    @Override
    public Dimension getDimension(Graphics2D graphics) {
        return new Dimension(dimension);
    }

    private Color getColor(Condition c) {
      if((colors == null) || (colors.get(c) == null)) {
        return c.getColor();
      } else {
        return colors.get(c);
      }
    }
}
