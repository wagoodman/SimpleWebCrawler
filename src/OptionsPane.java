import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

class OptionsPane extends JPanel {
   private static final long serialVersionUID = 2682594237182950911L;
   private JButton goButton;
   private JComboBox solverComboBox;
   private JTextField seedURLField;
   private JComboBox depthLimitComboBox;
   private JComboBox countLimitComboBox;
   private JComboBox workerLimitComboBox;
   private JLabel solverLabel;
   private JLabel seedURLLabel;
   private JLabel searchDepthLabel;
   private JLabel searchCountLabel;
   private JLabel workerLimitLabel;

   public static final String SEED_URL = "http://www.endoftheinternet.com/";

   private static final String CRAWL_TEXT = "Start Crawling!";
   private static final String CANCEL_TEXT = "CANCEL";

   OptionsPane(final CrawlerApp crawlerApp) {

      setLayout(new GridBagLayout());
      GridBagConstraints constraints = new GridBagConstraints();

      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 0.1;
      constraints.weighty = 0.1;

      goButton = new JButton(CRAWL_TEXT);

      solverComboBox = new JComboBox(Crawler.Function.values());
      seedURLField = new JTextField();
      seedURLField.setText(SEED_URL);
      depthLimitComboBox = new JComboBox(new Integer[] { 2, 3, 4, 5, 6, 7, 8,
            9, 10, 50, 100, 500, 1000, 1500, 2000 });
      depthLimitComboBox.setSelectedIndex(3);
      countLimitComboBox = new JComboBox(new Integer[] { 10, 100, 500, 1000,
            1500, 2000 });
      countLimitComboBox.setSelectedIndex(2);
      workerLimitComboBox = new JComboBox(new Integer[] { 1, 2, 3, 4, 6, 8, 10});
      workerLimitComboBox.setSelectedIndex(3);

      solverLabel = new JLabel("Search Method:");
      seedURLLabel = new JLabel("Seed URL:");
      searchDepthLabel = new JLabel("Depth Limit:");
      searchCountLabel = new JLabel("URL Count Limit:");
      workerLimitLabel = new JLabel("Thread Limit:");

      solverLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      seedURLLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      searchDepthLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      searchCountLabel.setHorizontalAlignment(SwingConstants.RIGHT);
      workerLimitLabel.setHorizontalAlignment(SwingConstants.RIGHT);

      goButton.addActionListener(new ActionListener() {

         public void actionPerformed(ActionEvent e) {

            if (goButton.getText().equals(CRAWL_TEXT)) {

               Crawler.apply((Crawler.Function) solverComboBox
                     .getSelectedItem(), crawlerApp, ((String) seedURLField
                     .getText()), ((Integer) depthLimitComboBox
                     .getSelectedItem()).intValue(),
                     ((Integer) countLimitComboBox.getSelectedItem())
                           .intValue(), ((Integer) workerLimitComboBox
                           .getSelectedItem()).intValue());

               crawlerApp.getWorker().crawl();
            } else {
               if (crawlerApp.getWorker() != null) {
                  crawlerApp.getWorker().cancelCrawl();
               }
            }
         }
      });


      // SOLVER
      constraints.gridx = 0;
      constraints.gridy = 0;
      add(solverLabel, constraints);
      constraints.gridx = 1;
      constraints.gridy = 0;
      add(solverComboBox, constraints);

      // SEED URL
      constraints.gridx = 2;
      constraints.gridy = 0;
      add(seedURLLabel, constraints);
      constraints.gridx = 3;
      constraints.gridy = 0;
      add(seedURLField, constraints);

      // THREAD LIMIT
      constraints.gridx = 4;
      constraints.gridy = 0;
      add(workerLimitLabel, constraints);
      constraints.gridx = 5;
      constraints.gridy = 0;
      add(workerLimitComboBox, constraints);

      // DEPTH LIMIT
      constraints.gridx = 6;
      constraints.gridy = 0;
      add(searchDepthLabel, constraints);
      constraints.gridx = 7;
      constraints.gridy = 0;
      add(depthLimitComboBox, constraints);

      // URL COUNT LIMIT
      constraints.gridx = 8;
      constraints.gridy = 0;
      add(searchCountLabel, constraints);
      constraints.gridx = 9;
      constraints.gridy = 0;
      add(countLimitComboBox, constraints);

      // GLUE
      constraints = new GridBagConstraints();
      constraints.gridx = 10;
      constraints.gridy = 0;
      constraints.fill = GridBagConstraints.HORIZONTAL;
      constraints.weightx = 0.25;
      constraints.weighty = 0;
      add(Box.createGlue(), constraints);

      // RUN AGAIN
      constraints.gridx = 11;
      constraints.gridy = 0;
      constraints.ipadx = 20;
      constraints.ipady = 20;
      constraints.gridheight = 2;
      constraints.anchor = GridBagConstraints.EAST;
      add(goButton, constraints);

   }

   // all input options should be disabled while working and
   // enabled at all other times
   public void setAvailability(boolean isAvailable) {
      if (isAvailable) {

         goButton.setEnabled(false);
         goButton.setText("Close App to Crawl Again");
      } else {
         solverComboBox.setEnabled(false);
         seedURLField.setEnabled(false);
         // goButton.setEnabled(false);
         goButton.setText(CANCEL_TEXT);
         workerLimitComboBox.setEnabled(false);
         countLimitComboBox.setEnabled(false);
         depthLimitComboBox.setEnabled(false);
      }

   }

}