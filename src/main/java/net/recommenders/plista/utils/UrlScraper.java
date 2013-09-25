package net.recommenders.plista.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import net.recommenders.plista.client.ChallengeMessage;
import net.recommenders.plista.client.Message;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import net.recommenders.plista.log.DataLogger;

/**
 * Created with IntelliJ IDEA. User: alan Date: 2013-08-30 Time: 11:10 To change
 * this template use File | Settings | File Templates.
 */
public class UrlScraper {

    private final static DataLogger logger = DataLogger.getLogger(ContentDB.class);
    private static final String FILEPATH = "items/";
    private static ContentDB db = null;
    private Map<Long, String> identifiers;

    public UrlScraper(ContentDB _db) {
        this.db = _db;

    }

    public UrlScraper() {
        identifiers = new HashMap<Long, String>();
        identifiers.put(13554L, "div#maincolumn"); //motor-talk.de
        identifiers.put(418L, "span.ArticleTitle, div.article_text"); //ksta.de
        identifiers.put(596L, "div.headline, p.absatz, "); //sport1.de
        identifiers.put(2522L, "article.idgArticleDetail"); //computerwoche.de
        identifiers.put(1677L, "div.hcf-article"); //tagesspiegel.de
        identifiers.put(3336L, "article.article_content"); //tecchannel.de
        identifiers.put(2524L, "article.MidContDet"); //cio.de
        identifiers.put(12935L, "div.headline_teaser, div.smalltext, div.teaser_text, div.headline_teaser"); //wohnen-und-garten.de
        identifiers.put(694L, "h1.Clear, h2.newsTeaser, div#_contentLeft p"); //gulli.com
        // identifiers.put("div.panel-pane pane-node-content"); //cfoworld.de

    }

    public static void main(String args[]) throws Exception {
        UrlScraper scraper = new UrlScraper();

        db = new ContentDB();
        db.init();
        scraper.readLog();

//        System.out.println(db.getContent(134664595L, 13554L));
//        db.disconnect();
    }

    public void readLog() {
        int num = 0;
        Long itemID = null;
        Long domainID = null;
        String content = null;
        final File dir = new File(".");
        FileFilter logFilter = new WildcardFileFilter("data.log*");
        File[] logs = dir.listFiles(logFilter);
        for (File file : logs) {
            Scanner scnr = null;
            try {
                scnr = new Scanner(file, "US-ASCII");
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            }
            while (scnr.hasNextLine()) {
                num++;
                String line = scnr.nextLine();
                if (line.contains("item_update")) {
                    line = line.split("\t")[2];
                    final Message message = new ChallengeMessage().parseItemUpdate(line, false);
//                    new Thread(){
//                        public void run(){
                    try {
                        scrapeURL(message);
                    } catch (InterruptedException e) {
                        logger.error(e.getMessage());
                        e.printStackTrace();
                    }

//                        }
//                    }.start();
                    System.out.println(num);
                }
            }
        }
    }

    public void scrapeURL(Message message) throws InterruptedException {
        Long itemID = message.getItemID();
        Long domainID = message.getDomainID();
        String content = null;
        String url = message.getItemURL();
        Document doc = null;
        if (db.itemExists(itemID)) {
            return;
        }
        try {
            Thread.sleep(1000);
            doc = Jsoup.connect(url).timeout(10000).get();
        } catch (IOException e) {
            logger.error(e.getMessage());
            e.printStackTrace();
        }
        try {
            content = doc.select(identifiers.get(domainID)).first().text();
        } catch (NullPointerException e) {
            logger.error(e.getMessage());
            System.out.println("ERROR: domain identifier not available or wrong. DOMAIN: " + domainID);
        }

        if (db.addMessage(message, content)) {
            try {
                File file = new File(FILEPATH + domainID.toString() + "/" + itemID.toString() + ".html");
                file.getParentFile().mkdirs();
                file.createNewFile();
                BufferedWriter output = new BufferedWriter(new FileWriter(file));
                output.write(doc.html());
                output.close();
            } catch (IOException e) {
                logger.error(e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
