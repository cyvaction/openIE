package id.ac.itb.openie.crawler;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;
import id.ac.itb.openie.config.Config;
import id.ac.itb.openie.relations.Relations;
import id.ac.itb.openie.utils.Utilities;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Created by elvanowen on 2/22/17.
 */
public class Crawler extends WebCrawler {

    private static Crawler currentlyRunningCrawler = null;
    private static ICrawlerHandler currentlyRunningCrawlerHandler = null;

    private ICrawlerHandler crawlerHandler = null;
    private int totalDocumentCrawled = 0;
    private CrawlerConfig crawlerConfig = new CrawlerConfig();

    public Crawler setCrawlerhandler(ICrawlerHandler crawlerhandler) {
        crawlerHandler = crawlerhandler;
        return this;
    }

    public ICrawlerHandler getCrawlerhandler() {
        return crawlerHandler;
    }

    public Crawler setCrawlerConfig(CrawlerConfig crawlerConfig) {
        this.crawlerConfig = crawlerConfig;
        return this;
    }

    public CrawlerConfig getCrawlerConfig() {
        return this.crawlerConfig;
    }

    public synchronized void setTotalDocumentCrawled(int totalDocumentCrawled) {
        this.totalDocumentCrawled = totalDocumentCrawled;
    }

    public synchronized int getTotalDocumentCrawled() {
        return totalDocumentCrawled;
    }

    @Override
    public void onStart() {
        super.onStart();
        currentlyRunningCrawlerHandler.crawlerWillRun();
    }

    @Override
    public void onBeforeExit() {
        super.onBeforeExit();
        currentlyRunningCrawlerHandler.crawlerDidRun();
    }

    @Override
    public boolean shouldVisit(Page referringPage, WebURL url) {
        String targetHref = url.getURL().toLowerCase();
        String referringHref = referringPage.getWebURL().getURL().toLowerCase();

//        System.out.println(referringHref + " -> " + targetHref);

        // If url is seed then allow
        for (String seedURL: currentlyRunningCrawlerHandler.getCrawlerStartingUrls()) {
            if (targetHref.equalsIgnoreCase(seedURL)) {
                return true;
            }
        }

        if (currentlyRunningCrawler.getCrawlerConfig().getFilterRegexPattern().matcher(targetHref).matches()) {
            return false;
        }

        return currentlyRunningCrawlerHandler.shouldCrawlerFollowLink(targetHref);
    }

    @Override
    public void visit(Page page) {
        String url = page.getWebURL().getURL();
        System.out.println(url);

        if (page.getParseData() instanceof HtmlParseData) {
            HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
            String html = htmlParseData.getHtml();

            HashMap<String, String> fileContentMappings = currentlyRunningCrawlerHandler.extractContentFromHTML(url, html);
            Iterator<Map.Entry<String, String>> it = fileContentMappings.entrySet().iterator();

            currentlyRunningCrawler.setTotalDocumentCrawled(currentlyRunningCrawler.getTotalDocumentCrawled() + 1);

            while (it.hasNext()) {
                Map.Entry<String, String> pair = it.next();

                writeToFile(pair.getKey(), pair.getValue());

                it.remove(); // avoids a ConcurrentModificationException
            }
        }
    }

    protected void writeToFile(String url, String content) {
        Utilities.writeToFile(currentlyRunningCrawler.getCrawlerConfig().getCrawlStorageDirectoryPath(), url, content);
    }

    public void execute() throws Exception {
        if (crawlerHandler == null) {
            throw new Exception("No Crawler Handler specified");
        }

        currentlyRunningCrawlerHandler = crawlerHandler;
        currentlyRunningCrawler = this;

        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(crawlerConfig.getInternalCrawlerStorageDirectory());
        config.setMaxDepthOfCrawling(crawlerConfig.getMaxDepthOfCrawling());
        config.setMaxPagesToFetch(crawlerConfig.getMaxPagesToFetch());
        config.setUserAgentString(crawlerConfig.getUserAgentString());

        /*
         * Instantiate the controller for this crawl.
         */
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = null;

        try {
            controller = new CrawlController(config, pageFetcher, robotstxtServer);

            /*
             * For each crawl, you need to add some seed urls. These are the first
             * URLs that are fetched and then the crawler starts following links
             * which are found in these pages
             */
            for (String seed: currentlyRunningCrawlerHandler.getCrawlerStartingUrls()) {
                controller.addSeed(seed);
            }

            /*
             * Start the crawl. This is a blocking operation, meaning that your code
             * will reach the line after this only when crawling is finished.
             */
            controller.start(this.getClass(), 1);
            controller.waitUntilFinish();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String toString() {
        return this.getCrawlerhandler().getPluginName();
    }
}
