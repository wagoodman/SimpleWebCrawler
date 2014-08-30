import javax.swing.Box;
import javax.swing.JPanel;

// defines the general strategies and contracts for crawling 

public class Crawler {

   public static void apply(Crawler.Function method, CrawlerApp app,
         String seedURL, int searchDepthLimit, int searchURLCountLimit,
         int workers) {
      if (method == Function.breadthFirstSearch) {
         CrawlerUtils.URLContext.ORDER_BY_BEST = false;
         app.setWorker(new CrawlerBFSEngine(app, seedURL, searchDepthLimit,
               searchURLCountLimit, workers, app.VIS));
      }

      else if (method == Function.bestFirstSearch) {
         CrawlerUtils.URLContext.ORDER_BY_BEST = true;
         app.setWorker(new CrawlerBFSEngine(app, seedURL, searchDepthLimit,
               searchURLCountLimit, workers, app.VIS));
      }
      else if (method == Function.limitedDepthFirstSearch) { 
         CrawlerUtils.URLContext.ORDER_BY_BEST = false;
         app.setWorker(new CrawlerLDFSEngine(app, seedURL, searchDepthLimit,
               searchURLCountLimit, workers, app.VIS));
      }
      else {
         System.err.println("Oops. Invalid Random Function Given!\n"
               + String.valueOf(method));
      }
   }

   public static enum Function {
      breadthFirstSearch, bestFirstSearch, limitedDepthFirstSearch;
   }

   public interface CrawlerEngineInterface {

      // return the visual graph
      public JPanel getTreePanel();

      public JPanel getUrlTable();

      public Box getInfoLine();

      // set the initial URL to start from
      public void setSeedURL(String seedUrl);

      // set the max depth to search
      public void setSearchDepthLimit(int depth);

      // the max number of nodes to discover before quitting
      public void setSearchURLCountLimit(int count);

      public void setWorkerCount(int count);

      // attempts to cancel the background worker if it is working
      public void cancelCrawl();

      // create a new background worker and begin execution
      public void crawl();

   }
}