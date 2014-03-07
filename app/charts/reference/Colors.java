package charts.reference;

import java.awt.Color;

public class Colors {
  private final String[] colors;

  public Colors(Color color) {
    colors = new String[] {charts.graphics.Colors.toHex(color)};
  }

  public Colors(Color[] colors) {
    this.colors = new String[colors.length];
    for(int i=0;i<colors.length;i++) {
      this.colors[i] = charts.graphics.Colors.toHex(colors[i]);
    }
  }

  public String[] getColors() {
    return colors;
  }

}