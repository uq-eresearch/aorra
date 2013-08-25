package boxrenderer;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.jfree.ui.RefineryUtilities;

public class BoxRendererUI {
    
    public static class MainPanel extends JPanel {

        public MainPanel() {
            setBorder(BorderFactory.createLineBorder(Color.black));
        }

        public Dimension getPreferredSize() {
            return new Dimension(500, 500);
        }

        public void paintComponent(Graphics g) {
            try {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D)g;
                g2.setColor(Color.black);
//                g2.fillRect(0, 0, 1000, 1000);
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                tableTest(g2);
//                Font font = new Font("Arial", Font.PLAIN, 20);
//                {
//                    TextBox box = new TextBox("test J p\nline2 J", font);
//                    Dimension d = box.getDimension(g2);
//                    Graphics2D g0 = (Graphics2D) g2.create(10, 10, d.width+1, d.height+1);
//                    g2.setColor(Color.red);
//                    g0.dispose();
//                }
//                {
//                    TextBox box = new TextBox("12345", font);
//                    RotationBox rbox = new RotationBox(box);
//                    Graphics2D g0 = (Graphics2D) g2.create(10, 10, 200, 200);
//                    //rbox.getDimension(g0);
//                    rbox.render(g0);
//                    g0.dispose();
//                }

                {
                    
//                    TextBlock block = TextUtilities.createTextBlock("12345", font, Color.black);
//                    block.setLineAlignment(HorizontalAlignment.LEFT);
//                    block.draw(g2, 100, 100, TextBlockAnchor.TOP_LEFT, 100, 100, GraphUtils.toRadians(0));
//                    block.draw(g2, 100, 100, TextBlockAnchor.TOP_LEFT, 100, 100+20, GraphUtils.toRadians(-90));
//                    new GraphUtils(g2).drawCircle(100, 100, 50);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static void tableTest(Graphics2D g2) throws Exception {
        Font font = new Font("Arial", Font.PLAIN, 20);
        TableBox table = new TableBox();
//        table.setBorder(new Border(1));
        TableRowBox row1 = new TableRowBox();
        TableRowBox row2 = new TableRowBox();
        TableRowBox row3 = new TableRowBox();
        TableRowBox row4 = new TableRowBox();
        TableRowBox row5 = new TableRowBox();
        TextBox box1=new TextBox("r1c1", font);
//        box1.setRotation(-90);
        TableCellBox tcb1 = new TableCellBox(box1);
//        tcb1.setBorder(new Border(1,1,0,0));
        row1.addCell(tcb1);
        row1.addCell(new TableCellBox(new TextBox("r1c2\nl2", font)));
        row1.addCell(new TableCellBox(new TextBox("r1c3\nl2", font), 1, 3));
        row2.addCell(new TableCellBox(new TextBox("r2c1", font)));
        row2.addCell(new TableCellBox(new TextBox("r2c2", font)));
        row3.addCell(new TableCellBox(new TextBox("r3c1c2(colspan=2)", font), 2, 1));
        row4.addCell(new TableCellBox(new TextBox("r4c1(colspan=3)", font), 3, 1));
        table.addRow(row1);
        table.addRow(row2);
        table.addRow(row3);
        table.addRow(row4);
        table.addRow(row5);
        
        {
            TableBox table2 = new TableBox();
            TableRowBox t2row1 = new TableRowBox();
            TableRowBox t2row2 = new TableRowBox();
            table2.addRow(t2row1);
            table2.addRow(t2row2);
            t2row1.addCell(new TableCellBox(new TextBox("t2r1c1", font)));
            t2row1.addCell(new TableCellBox(new TextBox("t2r1c2\nl2", font)));
            t2row2.addCell(new TableCellBox(new TextBox("t2r2c1", font)));
            t2row2.addCell(new TableCellBox(new TextBox("t2r2c2", font)));
            row5.addCell(new TableCellBox(new TextBox("r5c1", font)));
            row5.addCell(new TableCellBox(table2));
            row5.addCell(new TableCellBox(new TextBox("r5c3", font)));
        }
        Dimension d = table.getDimension(g2);
        System.out.println(d);
        Graphics2D g0 = (Graphics2D) g2.create(10, 10, d.width, d.height);
        table.render(g0);
        g0.dispose();
    }
    
    private static void createAndShowGUI() {
        //Create and set up the window.
        JFrame frame = new JFrame("Box renderer test");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(new MainPanel());
        //Display the window.
        frame.pack();
        RefineryUtilities.centerFrameOnScreen(frame);
        frame.setVisible(true);
    }
 
    public static void main(String[] args) {
        //Schedule a job for the event-dispatching thread:
        //creating and showing this application's GUI.
        javax.swing.SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        });
    }
}
