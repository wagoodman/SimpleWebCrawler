
Simple Web Crawler
==================

Given a web page this just finds all links in that page and visits them. Be careful though! This 
blindly visits as many pages as possible to any link found.

```
Compiler Compliance Level: 	1.6 (Oracle JDK 1.6)
IDE Used: 			Eclipse Kepler for Java Developers (Build 20130614-0229)
```

Import the Project
------------------
	1) Open Eclipse
	2) File > New > Java Project
	3) Unslelect 'Use Default Location' and input the path to the SimpleWebCrawler directory
	4) Select all projects from the detected project list
	5) Click Finish


Usage
-----
	1) In Eclipse, right click "CrawlerApp.java" > RunAs... > Java Application
	2) Input the following items:
		- Search Mechanism: BreadthFirst, BestFirst, DepthFirst
		- Seed URL
		- Threads to use
		- Maximum depth to search to
		- Maximum URL count to search to
	3) Click the button to start

A tree will form in the first tab showing all URL relations; you can drag and zoom in/out.

A table and chart will be shown in the second tab with the details about each URL.

Progress is shown on the bottom bar with the search status in the bottom right corner.

To start another search, close the program and reopen it.

Note: 	when using multiple threads the group will need to discover existing nodes, give the
	program about 15 seconds to get all threads in sync.

