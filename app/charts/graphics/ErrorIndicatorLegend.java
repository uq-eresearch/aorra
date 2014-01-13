package charts.graphics;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.Rectangle2D;

import org.jfree.chart.LegendItem;
import org.jfree.chart.LegendItemCollection;
import org.jfree.chart.LegendItemSource;
import org.jfree.chart.title.LegendTitle;
import org.jfree.ui.HorizontalAlignment;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

public class ErrorIndicatorLegend {

  public static LegendTitle createLegend() {
    final LegendItemCollection legendItems = new LegendItemCollection();
    FontRenderContext frc = new FontRenderContext(null, true, true);
    Font legenfont = new Font(Font.SANS_SERIF, Font.PLAIN, 12);
    GlyphVector gv = legenfont.createGlyphVector(frc, new char[] {'I', 'I'});
    Shape shape = gv.getVisualBounds();
    Rectangle2D bounds = shape.getBounds2D();
    {
      LegendItem li = new LegendItem("Standard error", null, null, null,
          new ErrorIndicator(bounds), new BasicStroke(), ErrorIndicator.ERROR_INDICATOR_COLOR);
      li.setLabelFont(legenfont);
      legendItems.add(li);
    }
    LegendTitle legend = new LegendTitle(new LegendItemSource() {
      @Override
      public LegendItemCollection getLegendItems() {
        return legendItems;
      }});
    legend.setPosition(RectangleEdge.BOTTOM);
    legend.setMargin(new RectangleInsets(0,30,0,0));
    legend.setPadding(RectangleInsets.ZERO_INSETS);
    legend.setLegendItemGraphicPadding(new RectangleInsets(0,20,0,0));
    legend.setHorizontalAlignment(HorizontalAlignment.LEFT);
    return legend;
  }

}
