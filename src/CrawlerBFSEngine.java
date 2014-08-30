import googlePageRank.GoogleSeoHelper;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Queue;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.jgroups.Message;
import org.jgroups.ReceiverAdapter;

import prefuse.Visualization;
import prefuse.util.FontLib;

// this is the best first and breadth first crawler (based on the parameters provided)

public class CrawlerBFSEngine implements Crawler.CrawlerEngineInterface {

   private final Box infoBox;
   private final JLabel score;
   private final JLabel nodesDiscovered;
   private final JLabel averagePagerank;
   private final JLabel validNodesDiscovered;
   private final JLabel status;

   private TreeCommon.TreeStructure<URL> searchPane;
   private UrlTable urlTable;
   private List<CrawlerWorker> currentWorkers;
   private CrawlerApp app;

   private URL seedURL;
   private int maxDepth;
   private int maxURLCount;
   private int workerCount;

   // used by all workers
   private HashSet<URL> seenURLs;

   private static GoogleSeoHelper googlePRInterface = new GoogleSeoHelper();

   public CrawlerBFSEngine(CrawlerApp app, String seedURL,
         int searchDepthLimit, int searchURLCountLimit, int count,
         Visualization vis) {

      // necessary for jgroup
      System.setProperty("java.net.preferIPv4Stack", "true");

      this.app = app;
      setSeedURL(seedURL);
      setSearchDepthLimit(searchDepthLimit);
      setSearchURLCountLimit(searchURLCountLimit);
      setWorkerCount(count);
      searchPane = new PrefuseRadialTreePanel(app.WIDTH, app.HEIGHT,
            CrawlerUtils.removeWwwFromUrl("http://127.0.0.1"), vis);

      infoBox = new Box(BoxLayout.X_AXIS);

      score = new JLabel("Overall Crawl Score: ---");
      score.setFont(FontLib.getFont("Consolas", 12));

      nodesDiscovered = new JLabel("Nodes Discovered: ---");
      nodesDiscovered.setFont(FontLib.getFont("Consolas", 12));
      
      validNodesDiscovered = new JLabel("Valid Nodes Discovered: ---");
      validNodesDiscovered.setFont(FontLib.getFont("Consolas", 12));
      
      averagePagerank = new JLabel("Average PageRank: ---");
      averagePagerank.setFont(FontLib.getFont("Consolas", 12));

      status = new JLabel("Status: Stopped");
      status.setFont(FontLib.getFont("Consolas", 12));

      infoBox.add(score);
      infoBox.add(Box.createHorizontalGlue());
      infoBox.add(nodesDiscovered);
      infoBox.add(Box.createHorizontalGlue());
      infoBox.add(validNodesDiscovered);
      infoBox.add(Box.createHorizontalGlue());
      infoBox.add(averagePagerank);
      infoBox.add(Box.createHorizontalGlue());
      infoBox.add(status);
      
      
      urlTable = new UrlTable(score, nodesDiscovered, validNodesDiscovered, averagePagerank, status);
   }

   @Override
   public JPanel getTreePanel() {
      return (JPanel) searchPane;
   }

   public JPanel getUrlTable() {
      return (JPanel) urlTable;
   }

   public Box getInfoLine() {
      return infoBox;
   }

   @Override
   public void setSeedURL(String seedUrl) {
      this.seedURL = CrawlerUtils.getUrl(seedUrl);
   }

   @Override
   public void setSearchDepthLimit(int depth) {
      this.maxDepth = depth;
   }

   @Override
   public void setSearchURLCountLimit(int count) {
      this.maxURLCount = count;

   }

   @Override
   public void setWorkerCount(int count) {
      this.workerCount = count;
   }

   @Override
   public void cancelCrawl() {
      if (currentWorkers != null) {
         for (CrawlerWorker worker : currentWorkers) {
            worker.cancel(true);
         }

         currentWorkers.clear();
      }

   }

   @Override
   public void crawl() {
      cancelCrawl();
      try {
         currentWorkers = new ArrayList<CrawlerWorker>();
         for (int i = 0; i < workerCount; i++) {
            Debug.out("Starting Worker " + i + " of " + workerCount + "...");
            currentWorkers.add(new CrawlerWorker());

         }
         status.setText("Status: Working...");
      } catch (Exception e) {
         System.err.println("When Starting a worker: ");
         e.printStackTrace();
      }
   }

   private boolean reachedMaxURLCount() {
      Debug.out("Is Tree (" + searchPane.getTreeSize() + ") > maxURLCount ("
            + maxURLCount + ") ? "
            + String.valueOf(searchPane.getTreeSize() > maxURLCount));
      if (searchPane.getTreeSize() > maxURLCount)
         return true;
      return false;
   }

   // //////////////////////////////////////////////////////////////////////////////////////////////////////////
   // This is the class responsible for actually crawling urls

   private class CrawlerWorker extends
         SwingWorker<Integer, CrawlerUtils.URLContext>
         implements
         TreeCommon.TreeCrawler<TreeCommon.TreeStructure<CrawlerUtils.URLContext>, CrawlerUtils.URLContext> {
      
      
      // reference to the jgroup coordinator
      private CoordinatedCluster cluster;
      
      // URLs from other crawlers 
      private PriorityQueue<CrawlerUtils.URLContext> remoteQueue;
      
      
      // what to do when a mesage has been recieved from another crawler process
      ReceiverAdapter recvr = new ReceiverAdapter() {
         public void receive(Message msg) {

            if (msg.getObject() instanceof CrawlerUtils.URLContext) {
               Debug.out(cluster.getHashIndex() + ":   CRAWLER recvd msg from "
                     + msg.getSrc() + " : " + msg.getObject());
               CrawlerUtils.URLContext url = (CrawlerUtils.URLContext) msg
                     .getObject();
               remoteQueue.add(url);
            }

         }
      };

      CrawlerWorker() throws Exception {

         // perform GUI setup tasks...
         app.getOptions().setAvailability(false);

         // start!
         execute();

      }

      @Override
      protected Integer doInBackground() throws Exception {

         try {

            Debug.out("Background Crawler Initializing Comms...");

            // setup comms...
            remoteQueue = new PriorityQueue<CrawlerUtils.URLContext>();
            cluster = new CoordinatedCluster("BFSCrawler", recvr);
            seenURLs = new HashSet<URL>();

            // wait for everyone to join...
            try {
            } catch (Exception e) {
            }
            while (cluster.getMemberCount() != workerCount) {
               Debug.out("Have " + cluster.getMemberCount() + " of "
                     + workerCount + ". Waiting...");
               Thread.sleep(1000);
               try {
               } catch (Exception e) {
               }
            }
            Debug.out("Have " + cluster.getMemberCount() + " of " + workerCount
                  + ". Let's Go! (...Hokies!)");

            // crawl the seed url
            Debug.out(cluster.getHashIndex() + ":Background Crawler Started!");

            Queue<CrawlerUtils.URLContext> fifoQueue = new PriorityQueue<CrawlerUtils.URLContext>();
            fifoQueue.add(new CrawlerUtils.URLContext(null, seedURL, 0));

            Debug.out(cluster.getHashIndex() + ":Starting Queue: " + fifoQueue);

            crawl(fifoQueue);

            Debug.out(cluster.getHashIndex()
                  + ":Background Worker Done! but I'll check my remote queue just in case");
            
            // keep crawling until every crawler process has completed their work
            while (!cluster.everyoneIsExhaused(workerCount)
                  || remoteQueue.size() != 0) {
               Thread.sleep(1000);

               if (remoteQueue.size() > 0) {
                  Debug.out(cluster.getHashIndex()
                        + ":Found more work to to after I was exhausted...");
                  // unset exhausted
                  cluster.setExhausted(false);

                  // crawl all found nodes (this will remove all nodes from the
                  // remote queue)
                  fifoQueue.add(remoteQueue.poll());
                  crawl(fifoQueue);
                  fifoQueue.clear();

                  Debug.out(cluster.getHashIndex() + ":I'm exhausted again...");
               } else if (!cluster.isExhausted()) {
                  cluster.setExhausted(true);
               } else {
                  Debug.out(cluster.getHashIndex()
                        + ":Waiting for someone to finish... (Everyone Exhausted? "
                        + cluster.everyoneIsExhaused(workerCount) + ")");
               }

            }

            Debug.out(cluster.getHashIndex() + ":Everyone is Exhausted");

         } catch (Exception e) {
            System.err.println("WORKER: " + cluster.getHashIndex());
            e.printStackTrace();
         }

         return null;
      }

      @Override
      public void crawl(Queue<CrawlerUtils.URLContext> queue) {
         // just bail.
         if (isCancelled()) {
            return;
         }

         // have we searched across too many documents? return if so...
         if (reachedMaxURLCount()) {
            // no need to keep track of whats left...
            queue.clear();
            remoteQueue.clear();

            return;
         }

         // is there anything in the remote queue? add it
         while (remoteQueue.size() > 0) {
            queue.add(remoteQueue.poll());
         }

         // if there is nothing left to search.... return
         if (queue.size() == 0)
            return;

         // is current depth allowed? return if not...
         if (queue.peek().depth > maxDepth)
            return;

         CrawlerUtils.URLContext input = queue.poll();

         // visit this url, get a list of children to visit next
         List<CrawlerUtils.URLContext> children = visit(input);

         Debug.out(cluster.getHashIndex() + ":   Adding Found children: "
               + children);
         // add all found children to the queue
         for (CrawlerUtils.URLContext child : children)
            queue.add(child);

         Debug.out(cluster.getHashIndex() + ":   continuing the crawl...");
         crawl(queue);

         return;
      }

      public List<CrawlerUtils.URLContext> visit(CrawlerUtils.URLContext input) {
         List<CrawlerUtils.URLContext> children = new ArrayList<CrawlerUtils.URLContext>();

         Debug.out(cluster.getHashIndex() + ":      Visiting: " + input);

         // already seen this url, skip it.
         if (seenURLs.contains(input.node.url)) {
            Debug.out(cluster.getHashIndex()
                  + ":         ALREADY SEEN URL, skipping: " + input.node);
            return children;
         }

         // Will the Robot Overloads let us continue? return if not...
         if (!CrawlerUtils.isURLVisitAllowed(input.node)){
            publish(input);   //show that the node was at least visited
            return children;
         }

         // Does this URL hash belong to me? then visit it. Otherwise send off
         // the URL for processing by the worker that should be doing this.
         Integer objHashIndex = cluster.getHashIndexOfObj(input.node);

         if (objHashIndex == cluster.getHashIndex()) {
            // visit it!
            try {

               Debug.out(cluster.getHashIndex()
                     + ":         Woot, mine to take!");

               // download the URL contents
               String urlContents = CrawlerUtils.downloadURL(input.node);

               input.node.pageSize = urlContents.length();

               // get more links from the page contents
               List<URL> discoveredChildren = CrawlerUtils.retrieveLinks(
                     input.node, urlContents, seenURLs);

               // make new nodes to discover, add them to the list to return!
               for (URL child : discoveredChildren) {
                  CrawlerUtils.URLContext newNode = new CrawlerUtils.URLContext(
                        input.node.url, child, input.depth + 1);
                  children.add(newNode);
               }

               input.node.childCount = discoveredChildren.size();

               // count this input URL as seen
               seenURLs.add(input.node.url);

               input.node.pageRank = googlePRInterface.getPR(input.node.url
                     .toString());

               // add this link to the tree...
               publish(input);
               
               // be polite...
               Thread.sleep(CrawlerApp.CRAWL_SLEEP_BETWEEN_FETCH);

            } catch (Exception e) {
               System.err.println("WORKER: " + cluster.getHashIndex());
               e.printStackTrace();

               if (input != null) {
                  input.node.error = e.toString();
                  // add this link to the tree anyway...
                  publish(input);
               }
            }
         } else {
            // send this URL to the worker that will process it
            try {

               Debug.out(cluster.getHashIndex()
                     + ":         ...awww, giving it away to " + objHashIndex);

               cluster.send(objHashIndex, input);

            } catch (Exception e) {
               System.err.println("WORKER: " + cluster.getHashIndex());
               e.printStackTrace();
            }

         }

         return children;
      }

      // any publish() calls invoke this in the GUI dispatcher thread. Let the
      // user know how many trials
      // you've done as well as point the latest trial for fun.

      private Integer numNodes = 0;

      @Override
      protected void process(List<CrawlerUtils.URLContext> discoveredNodes) {
         for (CrawlerUtils.URLContext input : discoveredNodes) {

            searchPane.addNode(input.parent.url, input.node.url);
            numNodes += 1;
            urlTable.addNode(
                  new String("Worker " + String.valueOf(cluster.getHashIndex())
                        + " : URL " + numNodes), input.depth, input.node.url,
                  input.node.pageSize, input.node.childCount,
                  input.node.pageRank, input.node.error);

         }
      }

      @Override
      // the worker thread is done, show the user the result
      protected void done() {
         // perform progress GUI teardown tasks...
         // present result to the GUI...

         try {
            cluster.endCluster();
         } catch (Exception e) {
            e.printStackTrace();
         }
         app.getOptions().setAvailability(true);
         status.setText("Status: Completed");

      }

   }

}