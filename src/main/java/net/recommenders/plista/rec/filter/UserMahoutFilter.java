package net.recommenders.plista.rec.filter;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.impl.common.FastIDSet;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.log.DataLogger;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * Filter
 *
 * @author alejandr
 */
public class UserMahoutFilter implements Filter {

    private static final DataLogger logger = DataLogger.getLogger(UserMahoutFilter.class);
    private static final long UPDATE_TIME = 10L * 60 * 1000; // 10 minutes
    private static final int NUM_THREADS = 5;
    // number of days taken into account for the data model
    private int numberOfDays;
    private Map<Long, DataModel> domainModels;
    private long lastUpdate;
    private ExecutorService pool;

    public UserMahoutFilter() {
        this.pool = Executors.newFixedThreadPool(NUM_THREADS);

        this.numberOfDays = Integer.parseInt(System.getProperty("plista.numOfDays", "5"));

        this.domainModels = new HashMap<Long, DataModel>();
        this.lastUpdate = System.currentTimeMillis();

        // get all data files for the different domains
        final File dir = new File(".");
        final FileFilter fileFilter = new WildcardFileFilter("*_m_data*.txt");
        final File[] files = dir.listFiles(fileFilter);

        // get domains
        List<Long> domains = new ArrayList<Long>();
        for (int i = 0; i < files.length; i++) {
            final String domainFile = files[i].getName();
            final Long domain = Long.parseLong(domainFile.substring(0, domainFile.indexOf("_")));
            if (!domains.contains(domain)) {
                domains.add(domain);
            }
        }

        // create domain MP Recommender
        for (Long d : domains) {
            try {
                getDataModel(d);
            } catch (Exception e) {
                this.logger.warn(e.toString());
            }
        }
    }

    private DataModel getDataModel(Long _domain) throws IOException {
        long curTime = System.currentTimeMillis();
        DataModel model = domainModels.get(_domain);
        if ((model == null) || (curTime - lastUpdate > UPDATE_TIME)) {
            // TODO
//            model = DataModelHelper.getDataModel(this.numberOfDays, _domain);
            domainModels.put(_domain, model);
            lastUpdate = curTime;
        }
        return model;
    }

    public List<Long> filter(Message message, Integer limit, Recommender rec) {
        List<Long> allItems = rec.recommend(message, limit);
        List<Long> filteredItems = new ArrayList<Long>();
        // DataModelHelper.getDataModel(this.numberOfDays, d)
        long userID = message.getUserID();
        FastIDSet idSet = new FastIDSet();
        try {
            idSet = getDataModel(message.getDomainID()).getItemIDsFromUser(userID);
        } catch (org.apache.mahout.cf.taste.common.NoSuchUserException e1) {
            this.logger.debug(e1.toString());
        } catch (Exception e2) {
            this.logger.warn(e2.toString());
        }

        int n = 0;
        for (Long i : allItems) {
            // check items by the user
            if (!idSet.contains(i)) {
                filteredItems.add(i);
                n++;
                if (n >= limit) {
                    break;
                }
            }
        }
        return filteredItems;
    }

    public void impression(Message _impression) {
        final Long domain = _impression.getDomainID();
        // write info directly in MAHOUT format
        // TODO
//        pool.submit(new Thread(new MahoutWriter(domain + "_m_data_" + DateHelper.getDate() + ".txt", _impression, 3)));
    }

    public void click(Message _feedback) {
        final Long client = _feedback.getUserID();
        final Long item = _feedback.getItemID();
        // write info directly in MAHOUT format -> with pref 5
        // TODO
//        pool.submit(new Thread(new MahoutWriter("m_data_" + DateHelper.getDate() + ".txt", client + "," + item, 5)));
    }

    public void update(Message _update) {
    }
}
