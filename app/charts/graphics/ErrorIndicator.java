package charts.graphics;

import java.awt.Color;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;

public class ErrorIndicator extends Path2D.Double {

  public static final Color ERROR_INDICATOR_COLOR = new Color(147,112,45);

  public ErrorIndicator(Rectangle2D bounds) {
    this.moveTo(bounds.getMinX(), bounds.getMinY());
    this.lineTo(bounds.getMaxX(), bounds.getMinY());
    this.lineTo(bounds.getCenterX(), bounds.getMinY());
    this.lineTo(bounds.getCenterX(), bounds.getMaxY());
    this.lineTo(bounds.getMaxX(), bounds.getMaxY());
    this.lineTo(bounds.getMinX(), bounds.getMaxY());
    this.lineTo(bounds.getCenterX(), bounds.getMaxY());
    this.lineTo(bounds.getCenterX(), bounds.getMinY());
    this.closePath();
  }
}
