
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// set of common functions for web crawling

public class CrawlerUtils {
   
   // wraps a URL with more statistics to be tracked
   
   public static class URLInfo implements Serializable, Comparable<URLInfo> {

      private static final long serialVersionUID = 4811447443757578144L;

      public URL url;
      public Integer httpCode = -1;
      public Integer pageSize = -1;
      public Integer childCount = 0;
      public Integer pageRank = -1;
      public String error = "";

      URLInfo(URL url) {
         this.url = url;
      }

      URLInfo(String url) throws MalformedURLException {
         this.url = new URL(url);
      }

      @Override
      public int compareTo(URLInfo otherVal) {
         return this.url.toString().compareTo(otherVal.url.toString());
      }

      @Override
      public String toString() {
         // return CrawlerUtils.getShortURLString(url);

         if (url == null) {
            return "null";
         }

         return url.toString();
      }

      @Override
      public boolean equals(Object otherVal) {
         if (otherVal instanceof URLInfo)
            return url.equals(((URLInfo) otherVal).url);
         return false;
      }

      @Override
      public int hashCode() {
         return url.hashCode();
      }

   }
   
   // tracks the relation of two URLs
   
   public static class URLContext implements Serializable,
         Comparable<URLContext> {
      private static final long serialVersionUID = 3663064198792923094L;

      public URLInfo parent;
      public URLInfo node;
      public Integer depth;
      private Long queueTime;
      
      public static Boolean ORDER_BY_BEST = false;
      
      URLContext(URL parent, URL url, int depth) {
         this.parent = new URLInfo(parent);
         this.node = new URLInfo(url);
         this.depth = depth;
         this.queueTime = System.currentTimeMillis();
      }

      @Override
      public String toString() {
         if (parent != null)
            return "{Depth: " + depth + " Parent: " + parent + " Child:" + node
                  + "} ";
         return "{Depth: " + depth + " Parent: [None] Child:" + node + "} ";

      }

      @Override
      public int compareTo(URLContext otherVal) {
         int compare = 0;
         if (ORDER_BY_BEST == true)
         {
            if (node != null && otherVal.node != null ){
               compare = this.node.pageRank.compareTo(otherVal.node.pageRank);
            }
            if (compare == 0) {
               compare = this.depth.compareTo(otherVal.depth);
            }
         }
         else{
            compare = this.depth.compareTo(otherVal.depth);
         }
         if (compare == 0) {
            compare = this.queueTime.compareTo(otherVal.queueTime);
         }
         return compare;
      }

   }

   // Cache of robot disallow lists.
   private static HashMap disallowListCache = new HashMap();
   
   ////////////////////////////
   // download a URL
   
   public static void fetchURL(URLInfo node) {
      fetchURL(node.url);
   }

   public static void fetchURL(URL url) {
      try {

         BufferedReader br = new BufferedReader(new InputStreamReader(
               url.openStream()));
         String strTemp = "";
         while (null != (strTemp = br.readLine())) {
            System.out.println(strTemp);
         }
      } catch (Exception ex) {
         ex.printStackTrace();
      }
   }

   ////////////////////////
   // Get a URL object from a string
   
   public static URL getUrl(String urlStr) {
      URL urlObj = null;
      try {
         if (urlStr.toLowerCase().startsWith("http://"))
            urlObj = new URL(removeWwwFromUrl(urlStr));
         else
            throw new Exception("Not HTTP request");
      } catch (Exception e) {
      }

      return urlObj;
   }

   //////////////////////////////
   // Check robots.txt
   
   public static boolean isURLVisitAllowed(URLInfo urlNodeToCheck) {
      return isURLVisitAllowed(urlNodeToCheck.url);
   }

   public static boolean isURLVisitAllowed(URL urlToCheck) {
      String host = urlToCheck.getHost().toLowerCase();

      // Retrieve host's disallow list from cache.
      ArrayList disallowList = (ArrayList) disallowListCache.get(host);

      // If list is not in the cache, download and cache it.
      if (disallowList == null) {
         disallowList = new ArrayList();

         try {
            URL robotsUrl = new URL("http://" + host + "/robots.txt");

            // Open connection to robot file URL for reading.
            String robotContents = downloadURL(robotsUrl);

            // Read robot file, creating list of disallowed paths.
            for (String line : robotContents.split("\n")) {
               if (line.indexOf("Disallow:") == 0) {
                  String disallowPath = line.substring("Disallow:".length());

                  // Check disallow path for comments and
                  // remove if present.
                  int commentIndex = disallowPath.indexOf("#");
                  if (commentIndex != -1) {
                     disallowPath = disallowPath.substring(0, commentIndex);
                  }

                  // Add disallow path to list.
                  disallowList.add(disallowPath.trim());
               }
            }

            // Add new disallow list to cache.
            disallowListCache.put(host, disallowList);
         } catch (Exception e) {
            /*
             * Assume robot is allowed since an exception is thrown if the robot
             * file doesn't exist.
             */
            return true;
         }
      }

      /*
       * Loop through disallow list to see if the crawling is allowed for the
       * given URL.
       */
      String file = urlToCheck.getFile();
      for (int i = 0; i < disallowList.size(); i++) {
         String disallow = (String) disallowList.get(i);
         if (file.startsWith(disallow)) {
            return false;
         }
      }

      return true;
   }

   /////////////////////////////////////////
   // download the given URL
   
   public static String downloadURL(URLInfo pageUrlInfo) {
      return downloadURL(pageUrlInfo.url);
   }

   // Download page at given URL.
   public static String downloadURL(URL pageUrl) {
      try {
         // Open connection to URL for reading.
         BufferedReader reader = new BufferedReader(new InputStreamReader(
               pageUrl.openStream()));

         // Read page into buffer.
         String line;
         StringBuffer pageBuffer = new StringBuffer();
         while ((line = reader.readLine()) != null) {
            pageBuffer.append(line + "\n");
         }

         return pageBuffer.toString();
      } catch (Exception e) {
      }

      return null;
   }

   ////////////////////////////////
   // Remove leading "www" from a URL's host if present.
   
   public static String removeWwwFromUrl(String url) {
      int index = url.indexOf("://www.");
      if (index != -1) {
         return url.substring(0, index + 3) + url.substring(index + 7);
      }

      return (url);
   }

   /////////////////////////////////////////////////////
   // Parse through page contents and retrieve links.
   
   public static List<URL> retrieveLinks(URLInfo pageUrlInfo,
         String pageContents, HashSet crawledList) {
      return retrieveLinks(pageUrlInfo.url, pageContents, crawledList, false);
   }

   public static List<URL> retrieveLinks(URL pageUrl, String pageContents,
         HashSet crawledList) {
      return retrieveLinks(pageUrl, pageContents, crawledList, false);
   }

   public static List<URL> retrieveLinks(URLInfo pageUrlInfo,
         String pageContents) {
      return retrieveLinks(pageUrlInfo.url, pageContents, new HashSet(), false);
   }

   public static List<URL> retrieveLinks(URL pageUrl, String pageContents) {
      return retrieveLinks(pageUrl, pageContents, new HashSet(), false);
   }

   public static List<URL> retrieveLinks(URLInfo pageUrlInfo,
         String pageContents, HashSet crawledList, boolean limitHost) {
      return retrieveLinks(pageUrlInfo.url, pageContents, new HashSet(),
            limitHost);
   }

   public static List<URL> retrieveLinks(URL pageUrl, String pageContents,
         HashSet crawledList, boolean limitHost) {
      // Compile link matching pattern.
      Pattern p = Pattern.compile("<a\\s+href\\s*=\\s*\"?(.*?)[\"|>]",
            Pattern.CASE_INSENSITIVE);
      Matcher m = p.matcher(pageContents);

      // Create list of link matches.
      List linkList = new ArrayList();
      while (m.find()) {
         String link = m.group(1).trim();

         // Skip empty links.
         if (link.length() < 1) {
            continue;
         }

         // Skip links that are just page anchors.
         if (link.charAt(0) == '#') {
            continue;
         }

         // Skip mailto links.
         if (link.indexOf("mailto:") != -1) {
            continue;
         }

         // Skip JavaScript links.
         if (link.toLowerCase().indexOf("javascript") != -1) {
            continue;
         }

         // Prefix absolute and relative URLs if necessary.
         if (link.indexOf("://") == -1) {
            // Handle absolute URLs.
            if (link.charAt(0) == '/') {
               link = "http://" + pageUrl.getHost() + link;
               // Handle relative URLs.
            } else {
               String file = pageUrl.getFile();
               if (file.indexOf('/') == -1) {
                  link = "http://" + pageUrl.getHost() + "/" + link;
               } else {
                  String path = file.substring(0, file.lastIndexOf('/') + 1);
                  link = "http://" + pageUrl.getHost() + path + link;
               }
            }
         }

         // Remove anchors from link.
         int index = link.indexOf('#');
         if (index != -1) {
            link = link.substring(0, index);
         }

         // Remove leading "www" from URL's host if present.
         link = removeWwwFromUrl(link);

         // Verify link and skip if invalid.
         URL verifiedLink = getUrl(link);
         if (verifiedLink == null) {
            continue;
         }

         /*
          * If specified, limit links to those having the same host as the start
          * URL.
          */
         if (limitHost
               && !pageUrl.getHost().toLowerCase()
                     .equals(verifiedLink.getHost().toLowerCase())) {
            continue;
         }

         // Skip link if it has already been crawled.
         if (crawledList.contains(link)) {
            continue;
         }

         // Add link to list.
         linkList.add(verifiedLink);
      }

      return (linkList);
   }

   /////////////////////////////////////////////////////////////
   // Determine whether or not search string is matched in the given page contents.
   
   public static boolean searchStringMatches(String pageContents,
         String searchString, boolean caseSensitive) {
      String searchContents = pageContents;

      /*
       * If case sensitive search, lowercase page contents for comparison.
       */
      if (!caseSensitive) {
         searchContents = pageContents.toLowerCase();
      }

      // Split search string into individual terms.
      Pattern p = Pattern.compile("[\\s]+");
      String[] terms = p.split(searchString);

      // Check to see if each term matches.
      for (int i = 0; i < terms.length; i++) {
         if (caseSensitive) {
            if (searchContents.indexOf(terms[i]) == -1) {
               return false;
            }
         } else {
            if (searchContents.indexOf(terms[i].toLowerCase()) == -1) {
               return false;
            }
         }
      }

      return true;
   }

   
   //// Just some tests
   public static void main(String[] args) throws MalformedURLException {

      URLInfo test = new URLInfo("http://www.space.com/robots.txt");
      URLInfo test1 = new URLInfo("http://www.space.com/robots.txt");

      System.out.println(String.valueOf(test == test1));
      System.out.println(String.valueOf(test.equals(test1)));

      /*
       * // URL url = new // URL(
       * "http://viralpatel.net/blogs/windows-7-set-environment-variable-without-admin-access/"
       * ); // fetchURL(url);
       * 
       * 
       * String robots = "http://www.space.com/robots.txt"; String badRobot =
       * "http://www.space.com/search.php"; String typicalLink =
       * "http://www.space.com/16149-night-sky.html";
       * 
       * URL url = getUrl(badRobot);
       * System.out.println("Bad Robot Verify URL: "); System.out.println(url);
       * 
       * boolean allowed = isURLVisitAllowed(url);
       * System.out.println("Can robots search here? ");
       * System.out.println(allowed);
       * 
       * System.out.println("-----------------------------");
       * 
       * url = getUrl(typicalLink);
       * System.out.println("Typical Link Verify URL: ");
       * System.out.println(url);
       * 
       * allowed = isURLVisitAllowed(url);
       * System.out.println("Can robots search here? ");
       * System.out.println(allowed);
       * 
       * System.out.println("-----------------------------");
       * 
       * String pageContents = downloadURL(url);
       * System.out.println(pageContents); List links = retrieveLinks(url,
       * pageContents, new HashSet(), false);
       * System.out.println("Contained Links"); for (Object obj : links) {
       * System.out.println("   " + obj); }
       */
   }

}
