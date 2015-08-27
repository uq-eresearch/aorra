package boxrenderer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

import com.google.common.collect.Lists;

public class TableRowBox extends AbstractBox implements Box {

    private List<TableCellBox> cells = Lists.newArrayList();

    @Override
    public Dimension getContentDimension(Graphics2D g2) throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void renderContent(Graphics2D g2) throws Exception {
        // TODO Auto-generated method stub

    }

    public TableRowBox() {}

    public TableRowBox(TableCellBox... cells) {
      for(TableCellBox cell :cells) {
        addCell(cell);
      }
    }

    public void addCell(TableCellBox cell) {
        cells.add(cell);
    }

    List<TableCellBox> getCells() {
        return cells;
    }

}
