package net.recommenders.plistacontest.recommender;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import de.dailab.plistacontest.helper.DataModelHelper;
import de.dailab.plistacontest.helper.DateHelper;
import de.dailab.plistacontest.helper.MahoutWriter;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;

/**
 *
 * Filter
 *
 * @author alejandr
 */
public class UserFilterWrapper implements ContestRecommender {

    private static final Logger logger = Logger.getLogger(UserFilterWrapper.class);
    private static final long UPDATE_TIME = 10L * 60 * 1000; // 10 minutes
    private static final int NUM_THREADS = 5;
    private WrappableRecommenderIF rec;
    // number of days taken into account for the data model
    private int numberOfDays;
    private Map<String, DataModel> domainModels;
    private long lastUpdate;
    private ExecutorService pool;

    public UserFilterWrapper(WrappableRecommenderIF rec) {
        this.rec = rec;

        this.pool = Executors.newFixedThreadPool(NUM_THREADS);

        this.numberOfDays = Integer.parseInt(System.getProperty("plista.numOfDays", "5"));

        this.domainModels = new HashMap<String, DataModel>();
        this.lastUpdate = System.currentTimeMillis();

        // get all data files for the different domains
        final File dir = new File(".");
        final FileFilter fileFilter = new WildcardFileFilter("*_m_data*.txt");
        final File[] files = dir.listFiles(fileFilter);

        // get domains
        List<String> domains = new ArrayList<String>();
        for (int i = 0; i < files.length; i++) {
            final String domainFile = files[i].getName();
            final String domain = domainFile.substring(0, domainFile.indexOf("_"));
            if (!domains.contains(domain)) {
                domains.add(domain);
            }
        }

        // create domain MP Recommender
        for (String d : domains) {
            try {
                getDataModel(d);
            } catch (Exception e) {
                this.logger.warn(e.toString());
            }
        }

        FileFilter logFilter = new WildcardFileFilter("contest.log*");
        File[] logs = dir.listFiles(logFilter);
        for (File file : logs) {
            Scanner scnr = null;
            try {
                scnr = new Scanner(file, "US-ASCII");
            } catch (FileNotFoundException e) {
                logger.error(e.getMessage());
            }
            while (scnr.hasNextLine()) {
                String line = scnr.nextLine();
                line = line.substring(0, line.length() - 24);
                JSONObject jObj = (JSONObject) JSONValue.parse(line);
                if (jObj.containsKey("msg")) {
                    if (jObj.get("msg").toString().equals("impression")) {
                        impression(line, false);
                    }
                    if (jObj.get("msg").toString().equals("feedback")) {
                        feedback(line, false);
                    }
                }
            }
        }
        pool.shutdown();
        try {
            pool.awaitTermination(10L, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            this.logger.error(e.toString());
        }
        pool = Executors.newFixedThreadPool(NUM_THREADS);
        logger.error("this is not an error: init finished");
    }

    private DataModel getDataModel(String _domain) throws IOException {
        long curTime = System.currentTimeMillis();
        DataModel model = domainModels.get(_domain);
        if ((model == null) || (curTime - lastUpdate > UPDATE_TIME)) {
            model = DataModelHelper.getDataModel(this.numberOfDays, _domain);
            domainModels.put(_domain, model);
            lastUpdate = curTime;
        }
        return model;
    }

    public List<ContestItem> recommend(String _client, String _item, String _domain, String _description, String _limit) {
        List<ContestItem> allItems = rec.recommendAll(_client, _item, _domain, _description, _limit);
        List<ContestItem> filteredItems = new ArrayList<ContestItem>();
        // DataModelHelper.getDataModel(this.numberOfDays, d)
        long userID = Long.parseLong(_client);
        FastIDSet idSet = new FastIDSet();
        try {
            idSet = getDataModel(_domain).getItemIDsFromUser(userID);
        } catch (org.apache.mahout.cf.taste.common.NoSuchUserException e1) {
            this.logger.debug(e1.toString());
        } catch (Exception e2) {
            this.logger.warn(e2.toString());
        }

        int limit = Integer.parseInt(_limit);
        int n = 0;
        for (ContestItem i : allItems) {
            // check items by the user
            if (!idSet.contains(i.getId())) {
                filteredItems.add(i);
                n++;
                if (n >= limit) {
                    break;
                }
            }
        }
        return filteredItems;
    }

    public void init() {
        rec.init();
    }

    public void impression(String _impression) {
        impression(_impression, true);
    }

    public void impression(String _impression, boolean pass) {
        final JSONObject jObj = (JSONObject) JSONValue.parse(_impression);
        final String domain = ((JSONObject) jObj.get("domain")).get("id").toString();
        // write info directly in MAHOUT format
        pool.submit(new Thread(new MahoutWriter(domain + "_m_data_" + DateHelper.getDate() + ".txt", _impression, 3)));

        if (pass) {
            rec.impression(_impression);
        }
    }

    public void feedback(String _feedback) {
        feedback(_feedback, true);
    }

    public void feedback(String _feedback, boolean pass) {
        final JSONObject jObj = (JSONObject) JSONValue.parse(_feedback);
        final String client = ((JSONObject) jObj.get("client")).get("id").toString();
        final String item = ((JSONObject) jObj.get("target")).get("id").toString();
        // write info directly in MAHOUT format -> with pref 5
        pool.submit(new Thread(new MahoutWriter("m_data_" + DateHelper.getDate() + ".txt", client + "," + item, 5)));

        if (pass) {
            rec.feedback(_feedback);
        }
    }

    public void error(String _error) {
        rec.error(_error);
    }

    public void setProperties(Properties properties) {
        rec.setProperties(properties);
    }
}
