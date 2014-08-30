import java.awt.*;
import java.awt.event.KeyEvent;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import prefuse.Visualization;

////////////////////////////////////////////////////////////////////////////////////////////////////////////
// This applet hosts the panels/classes necessary to web crawl

public class CrawlerApp extends JApplet {
   
   // politeness policy
   public static final int CRAWL_SLEEP_BETWEEN_FETCH = 100; // milliseconds
   
   public static int WIDTH = 1200;
   public static int HEIGHT = 800;

   public static final Visualization VIS = new Visualization();

   private static final long serialVersionUID = 8854554420244001L;
   private Crawler.CrawlerEngineInterface crawlerWorker = null;
   private OptionsPane optionsPanel;

   JTabbedPane tabbedPane;
   JComponent panel1;
   JComponent panel2;

   public CrawlerApp() {

      optionsPanel = new OptionsPane(this);
      add(optionsPanel, BorderLayout.PAGE_START);

   }

   protected JComponent makeTextPanel(String text) {
      JPanel panel = new JPanel(false);
      JLabel filler = new JLabel(text);
      filler.setHorizontalAlignment(JLabel.CENTER);
      panel.setLayout(new GridLayout(1, 1));
      panel.add(filler);
      return panel;
   }

   public Crawler.CrawlerEngineInterface getWorker() {
      return crawlerWorker;
   }

   public void setWorker(Crawler.CrawlerEngineInterface crawler) {
      if (crawlerWorker != null) {
         remove(crawlerWorker.getTreePanel());
      }
      crawlerWorker = crawler;

      tabbedPane = new JTabbedPane();

      panel1 = crawlerWorker.getTreePanel();
      tabbedPane.addTab("Graph", panel1);
      tabbedPane.setMnemonicAt(0, KeyEvent.VK_1);

      panel2 = makeTextPanel("Panel #2");
      tabbedPane.addTab("Detailed URL Info", crawlerWorker.getUrlTable());
      tabbedPane.setMnemonicAt(1, KeyEvent.VK_2);

      // Add the tabbed pane to this panel.
      add(tabbedPane, BorderLayout.CENTER);

      add(crawlerWorker.getInfoLine(), BorderLayout.PAGE_END);
   }

   // return the options panel (to disable all buttons while working)
   public OptionsPane getOptions() {
      return optionsPanel;
   }

   // Entry Point
   public static void main(String[] args) {

      SwingUtilities.invokeLater(new Runnable() {
         @Override
         public void run() {
            final JFrame frame = new JFrame("Web Crawler");
            frame.setPreferredSize(new Dimension(WIDTH, HEIGHT));
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
            WIDTH = Toolkit.getDefaultToolkit().getScreenSize().width;
            HEIGHT = Toolkit.getDefaultToolkit().getScreenSize().height;

            try {
               for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                  if ("Nimbus".equals(info.getName())) {
                     UIManager.setLookAndFeel(info.getClassName());
                     break;
                  }
               }
            } catch (Exception e) {
               e.printStackTrace();
            }
            SwingUtilities.updateComponentTreeUI(frame);
            frame.pack();

            CrawlerApp applet = new CrawlerApp();

            frame.getContentPane().add(applet, BorderLayout.CENTER);
            applet.init();
            applet.start();

            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null); // Center the frame
            frame.setVisible(true);
         }
      });

   }

}