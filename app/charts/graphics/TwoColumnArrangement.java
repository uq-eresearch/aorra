package charts.graphics;

import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;
import java.util.List;

import org.jfree.chart.block.Arrangement;
import org.jfree.chart.block.Block;
import org.jfree.chart.block.BlockContainer;
import org.jfree.chart.block.RectangleConstraint;
import org.jfree.ui.Size2D;

public class TwoColumnArrangement implements Arrangement {

  @Override
  public void add(Block block, Object key) {}

  @SuppressWarnings("unchecked")
  @Override
  public Size2D arrange(BlockContainer container, Graphics2D g2,
      RectangleConstraint constraint) {
    List<Block> blocks = container.getBlocks();
    double cellHeight = maxHeight(blocks, g2);
    double cellWidth = maxWidth(blocks, g2);
    int rows = Math.round(blocks.size() / 2.0f);
    for (int i = 0; i < blocks.size(); i++) {
      Block block = blocks.get(i);
      int col = (i<rows?0:1);
      int row = i-col*rows;
      block.setBounds(new Rectangle2D.Double(
          col*cellWidth, row*cellHeight, cellWidth, cellHeight));
    }
    return new Size2D(blocks.size()==1?cellWidth:cellWidth*2, cellHeight*rows);
  }

  private double maxWidth(List<Block> blocks, Graphics2D g2) {
    double max = 0.0;
    for(Block b : blocks) {
      max = Math.max(max, b.arrange(g2, RectangleConstraint.NONE).width);
    }
    return max;
  }

  private double maxHeight(List<Block> blocks, Graphics2D g2) {
    double max = 0.0;
    for(Block b : blocks) {
      max = Math.max(max, b.arrange(g2, RectangleConstraint.NONE).height);
    }
    return max;
  }


  @Override
  public void clear() {}

}
