import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URL;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.jfree.chart.ChartPanel;


// just a table with URL information
public class UrlTable extends JPanel {

   private static final long serialVersionUID = -2521599961889839610L;

   private final JLabel score;
   private final JLabel nodesDiscovered;
   private final JLabel averagePagerank;
   private final JLabel validNodesDiscovered;
   private final JLabel status;

   private final DynamicDataChart pageRankChart;
   private final DynamicDataChart depthChart;

   private final JTable table;
   private final DefaultTableModel tableModel;

   UrlTable(JLabel score, JLabel nodesDiscovered, JLabel averagePagerank,
         JLabel validNodesDiscovered, JLabel status) {
      super(new GridLayout(2, 1));

      this.score = score;
      this.nodesDiscovered = nodesDiscovered;
      this.averagePagerank = averagePagerank;
      this.validNodesDiscovered = validNodesDiscovered;
      this.status = status;

      String[] columnNames = { "Overall Order Discovered",
            "Worker-Worker Order Discovered", "Discovered Depth", "URL",
            "Page Size", "Child URL Count", "Google Page Rank", "Error Message" };

      Object[][] data = {};

      tableModel = new DefaultTableModel(data, columnNames);

      table = new JTable(tableModel);
      table.setPreferredScrollableViewportSize(new Dimension(500, 70));
      table.setFillsViewportHeight(true);
      table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

      // Create the scroll pane and add the table to it.
      JScrollPane scrollPane = new JScrollPane(table);

      // chart
      pageRankChart = new DynamicDataChart("Page Rank vs URL Discover Order","URL Discovered (Chronological)", "PageRank");
      pageRankChart.removeLegend();

      depthChart = new DynamicDataChart("Graph Depth vs URL Discover Order", "URL Discovered (Chronological)", "Graph Depth");
      depthChart.removeLegend();
      
      add(scrollPane);
      new ChartPanel(pageRankChart);
      add(new ChartPanel(depthChart));

   }

   // return score
   private Integer rowCount = 0;

   public void addNode(String workerNodeNum, Integer depth, URL url,
         Integer pageSize, Integer childUrlCount, Integer pageRankResults,
         String errorMessage) {
      rowCount += 1;
      tableModel.addRow(new Object[] { rowCount, workerNodeNum, depth, url,
            pageSize, childUrlCount, pageRankResults, errorMessage });

      pageRankChart.addPoint(rowCount, pageRankResults);
      depthChart.addPoint(rowCount, depth);
      
      updateScore();
   }

   private void updateScore() {

      Double curScore = 0.0;
      Integer rowOrder = 0;
      Integer pageRank = 0;

      Integer validPageRanks = 0;
      Double avgPageRank = 0.0;
      for (int row = 0; row < tableModel.getRowCount(); row++) {

         rowOrder = (Integer) tableModel.getValueAt(row, 0);
         pageRank = (Integer) tableModel.getValueAt(row, 6);

         if (pageRank > 0) {
            validPageRanks += 1;
            curScore += (((rowCount - (rowOrder - 1)) * pageRank) * 1.0)
                  / rowCount;
            avgPageRank += pageRank;
         }

      }
      score.setText("Overall Crawl Score: "
            + String.format("%.2f", curScore / validPageRanks));

      nodesDiscovered.setText("Nodes Discovered: " + String.valueOf(rowCount));

      validNodesDiscovered.setText("Valid Nodes Discovered: "
            + String.valueOf(validPageRanks));

      avgPageRank = avgPageRank / validPageRanks;
      averagePagerank.setText("Average PageRank: "
            + String.format("%.2f", avgPageRank));

   }

   // haven't used this yet...
   public void writeCSVfile(JTable table) throws IOException, ClassNotFoundException{
      Writer writer = null;
      DefaultTableModel dtm = (DefaultTableModel) table.getModel();
      int nRow = dtm.getRowCount();
      int nCol = dtm.getColumnCount();
      try {
          writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("crawlResults.txt"), "utf-8"));

          //write the header information
          StringBuffer bufferHeader = new StringBuffer();
          for (int j = 0; j < nCol; j++) {
              bufferHeader.append(dtm.getColumnName(j));
              if (j!=nCol) bufferHeader.append(", ");
          }
          writer.write(bufferHeader.toString() + "\r\n");

         //write row information
          for (int i = 0 ; i < nRow ; i++){
               StringBuffer buffer = new StringBuffer();
              for (int j = 0 ; j < nCol ; j++){
                  buffer.append(dtm.getValueAt(i,j));
                  if (j!=nCol) buffer.append(", ");
              }
              writer.write(buffer.toString() + "\r\n");
          }
      } finally {
            writer.close();
      }
  }
   
}
