package boxrenderer;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.util.List;

import com.google.common.collect.Lists;

public class TableBox extends AbstractBox implements Box {

    private List<TableRowBox> rows = Lists.newArrayList();

    // if set to true then the border and padding is not applied on tablebox render and 
    // and the margin of the nested cells is not applied too.
    // Also the cell borders are modified so that they to not overlap 
    private boolean borderCollapse = false;

    private static class RenderInfo {
        public TableCellBox content;
//        public Character debug;
        public boolean rendered = false;
        public boolean collapsed = false;
        public boolean processed = false;
    }

    public TableBox() {}

    public TableBox(TableRowBox... rows) {
      for(TableRowBox row : rows) {
        addRow(row);
      }
    }

    @Override
    public Dimension getContentDimension(Graphics2D g2) throws Exception {
        if(borderCollapse) {
//            printDebug();
            collapseBorder(g2);
        }
        int[] columnWidths = getColumnWidths(g2);
        int[] rowHeights = getRowHeight(g2);
        int width = getSum(columnWidths, 0, columnWidths.length);
        int height = getSum(rowHeights, 0, rowHeights.length);
        Dimension result = new Dimension(width, height);
        return result;
    }

    @Override
    public void renderContent(Graphics2D g2) throws Exception {
        if(borderCollapse) {
            collapseBorder(g2);
        }
        int[] columnWidths = getColumnWidths(g2);
        int[] rowHeights = getRowHeight(g2);
        RenderInfo[][] renderInfo = getRenderInfo();
        int columnCount = columnWidths.length;
        int rowCount = rowHeights.length;
        // render the cells
        for(int row = 0;row<renderInfo.length;row++) {
            for(int column = 0;column<renderInfo[row].length;column++) {
                RenderInfo ri = renderInfo[row][column];
                if((ri != null) && !ri.rendered) {
                    ri.rendered=true;
                    int width = getSum(columnWidths, column, getColspan(ri.content, column, columnCount));
                    int height = getSum(rowHeights, row, getRowspan(ri.content, row, rowCount));
                    int x = getSum(columnWidths, 0, column);
                    int y = getSum(rowHeights, 0, row);
                    Graphics2D g0 = (Graphics2D)g2.create(x, y, width, height);
                    try {
                        ri.content.render(g0);
                    } finally {
                        g0.dispose();
                    }
                }
            }
        }
    }

    public void addRow(TableRowBox row) {
        rows.add(row);
    }

    private RenderInfo[][] getRenderInfo() {
        int columnCount = getColumnCount();
        int rowCount = getRowCount();
        RenderInfo[][] renderInfo = new RenderInfo[rowCount][columnCount];
        int rowIndex = 0;
        for(TableRowBox row : rows) {
            int columnIndex = 0;
            for(TableCellBox cell : row.getCells()) {
                while(renderInfo[rowIndex][columnIndex] != null) {
                    columnIndex++;
                }
                RenderInfo ri = new RenderInfo();
                ri.content = cell;
                for(int i = 0;i<getRowspan(cell, rowIndex, rowCount);i++) {
                    for(int j = 0;j<getColspan(cell, columnIndex, columnCount);j++) {
                        renderInfo[rowIndex+i][columnIndex+j] = ri;
                    }
                }
            }
            rowIndex++;
        }
        return renderInfo;
    }

    /**
     * returns the colspan of the current cell. if the colspan is set to 0 this mean span to the end
     * of the table 
     * @param cell
     * @param currentColumn - count from 0
     * @param maxColumns
     */
    private int getColspan(TableCellBox cell, int currentColumn, int maxColumns) {
        int colspan = cell.getColspan();
        if(colspan == 0) {
            return maxColumns - currentColumn;
        } else {
            return colspan;
        }
    }

    private int getRowspan(TableCellBox cell, int currentRow, int maxRows) {
        int rowspan = cell.getRowspan();
        if(rowspan == 0) {
            return maxRows - currentRow;
        } else {
            return rowspan;
        }
    }

    private int getColumnCount() {
        int columns = 0;
        int rowIndex = 0;
        int rowCount = getRowCount();
        int[] rowspans = new int[rowCount];
        for(TableRowBox row : rows) {
            int i = 0;
            for(TableCellBox cell : row.getCells()) {
                int cols = cell.getColspan();
                if(cols == 0) {
                    cols = 1;
                }
                i+=cols;
                for(int rs = 1; rs<getRowspan(cell, rowIndex, rowCount); rs++) {
                    rowspans[rowIndex+rs]+=cols;
                }
            }
            columns = Math.max(columns, i+rowspans[rowIndex]);
            rowIndex++;
        }
        return columns;
    }

    private int getRowCount() {
        int rowIndex = 0;
        int rowsTotal = rows.size();
        for(TableRowBox row : rows) {
            for(TableCellBox cell : row.getCells()) {
                rowsTotal = Math.max(rowsTotal, cell.getRowspan()+rowIndex);
            }
            rowIndex++;
        }
        return rowsTotal;
    }

    private void collapseBorder(Graphics2D g2) throws Exception {
        setBorder(new Border());
        setPadding(new Padding());
        RenderInfo[][] renderInfo = getRenderInfo();
        for(int row = 0;row<renderInfo.length;row++) {
            for(int column = 0;column<renderInfo[row].length;column++) {
                RenderInfo ri = renderInfo[row][column];
                if((ri != null) && !ri.collapsed) {
                    ri.collapsed=true;
                    ri.content.setMargin(new Margin());
                    if(row > 0) {
                        ri.content.getBorder().setTop(0);
                    }
                    if(column > 0) {
                        ri.content.getBorder().setLeft(0);
                    }
                }
            }
        }
    }

    private int[] getColumnWidths(Graphics2D g2) throws Exception {
        int columnCount = getColumnCount();
        int[] result = new int[columnCount];
        RenderInfo[][] renderInfo = getRenderInfo();
        // set the largest column width for each columm, ignoring colspan columns
        for(int r = 0;r<renderInfo.length;r++) {
            for(int c=0;c<renderInfo[r].length;c++) {
                RenderInfo ri = renderInfo[r][c];
                if(ri!= null) {
                    TableCellBox cell = ri.content;
                    if(cell.getColspan()==1) {
                        result[c] = Math.max(cell.getDimension(g2).width, result[c]);
                    }
                }
            }
        }
        // make sure that the colspan cells fit into the space determined by the previous loop
        // if not then the needed extra space it distributed over the spaned columns
        for(int r = 0;r<renderInfo.length;r++) {
            for(int c=0;c<renderInfo[r].length;c++) {
                RenderInfo ri = renderInfo[r][c];
                if(ri == null) {
                    continue;
                }
                TableCellBox cell = ri.content;
                int span = getColspan(cell, c, columnCount);
                if(span>1 && !ri.processed) {
                    ri.processed = true;
                    int width = getSum(result, c, span);
                    int cellWidth = cell.getDimension(g2).width;
                    if(cellWidth > width) {
//                        this code widens just the last column of the span:
//                        int width2 = getSum(result, c, span-1);
//                        int width3 = cellWidth-width2;
//                        result[c+span-1] = width3;
                        widen(result, c, span, cellWidth);
                    }
                }
            }
        }
        return result;
    }

    private int[] getRowHeight(Graphics2D g2) throws Exception {
        int rowCount = getRowCount();
        int[] result = new int[rowCount];
        RenderInfo[][] renderInfo = getRenderInfo();
        // set the largest row heigth for each row, ignoring rowspan cells
        for(int r = 0;r<renderInfo.length;r++) {
            for(int c=0;c<renderInfo[r].length;c++) {
                RenderInfo ri = renderInfo[r][c];
                if(ri != null) {
                    TableCellBox cell = ri.content;
                    if(cell.getRowspan()==1) {
                        result[r] = Math.max(cell.getDimension(g2).height, result[r]);
                    }
                }
            }
        }
        // make sure that the rowspan cells fit into the space determined by previous loop
        // if not then the needed space is distributed over the spaned rows
        for(int r = 0;r<renderInfo.length;r++) {
            for(int c=0;c<renderInfo[r].length;c++) {
                RenderInfo ri = renderInfo[r][c];
                if(ri == null) {
                    continue;
                }
                TableCellBox cell = ri.content;
                int span = getRowspan(cell, r, rowCount);
                if((span>1) && (!ri.processed)) {
                    ri.processed=true;
                    int height = getSum(result, r, span);
                    int cellHeight = cell.getDimension(g2).height;
                    if(cellHeight > height) {
//                        int height2 = getSum(result, r, span-1);
//                        int height3 = cellHeight-height2;
//                        result[r+span-1] = height3;
                        widen(result, r, span, cellHeight);
                    }
                }
            }
        }
        return result;
    }

    private void widen(int[] result, int fromIndex, int count, int spaceNeeded) {
        int measure = getSum(result, fromIndex, count);
        int spaceNeededRemaining = spaceNeeded - measure;
        for(int i=fromIndex;i<fromIndex+count;i++) {
            if(spaceNeededRemaining <= 0) {
                break;
            }
            int remaining = count-(i-fromIndex);
            int thisWidened = spaceNeededRemaining / remaining;
            result[i] += thisWidened;
            spaceNeededRemaining -= thisWidened;
        }
    }

    private int getSum(int[] iarray, int index, int span) {
        int result = 0;
        for(int i = index; i < index+span;i++) {
            result+=iarray[i];
        }
        return result;
    }

    public boolean isBorderCollapse() {
        return borderCollapse;
    }

    public void setBorderCollapse(boolean borderCollapse) {
        this.borderCollapse = borderCollapse;
    }

//    private void printDebug() {
//        String chars = "0123456789abcdefghijklmnopqrstuvwxyz";
//        int i = 0;
//        StringBuilder builder = new StringBuilder();
//        RenderInfo[][] renderInfo = getRenderInfo();
//        for(int row = 0;row<renderInfo.length;row++) {
//            for(int column = 0;column<renderInfo[row].length;column++) {
//                RenderInfo ri = renderInfo[row][column];
//                if(ri == null) {
//                    builder.append('-');
//                } else if(ri.debug != null){ 
//                    builder.append(ri.debug);
//                } else {
//                    if(i >= chars.length()) {
//                        i = 0;
//                    }
//                    char next = chars.charAt(i++);
//                    ri.debug = next;
//                    builder.append(ri.debug);
//                }
//            }
//            builder.append('\n');
//        }
//        System.out.println(builder.toString());
//    }

}
