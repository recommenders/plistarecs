package net.recommenders.plistacontest.recommender;

import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.recommenders.plistacontest.utils.JsonUtils;

/**
 *
 * @author alejandr
 */
public class PathRecommender implements ContestRecommender {

    private static final Logger logger = Logger.getLogger(PathRecommender.class);
    private Map<Integer, String> domainLastItem;
    private Map<Integer, Map<String, WeightedItemList>> domainItemPath;
    private Set<String> forbiddenItems;

    public PathRecommender() {
        domainLastItem = new ConcurrentHashMap<Integer, String>();
        domainItemPath = new ConcurrentHashMap<Integer, Map<String, WeightedItemList>>();
        forbiddenItems = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());
    }

    public static void main(String[] args) {
        new PathRecommender().init();
    }

    public List<ContestItem> recommend(String _client, String _item, String _domain, String _description, String _limit) {
        final List<ContestItem> recList = new ArrayList<ContestItem>();

        int limit = Integer.parseInt(_limit);
        int domain = Integer.parseInt(_domain);

        final WeightedItemList path = domainItemPath.get(domain).get(_item);
        final Set<Long> recItems = new HashSet<Long>();
        if (!path.isEmpty()) {
            // sort the weighted list
            Collections.sort(path);
            // get the first N items (i.e., limit)
            int n = 0; // recList index
            int size = Math.min(limit, path.size());
            int i = 0; // path index
            while (n < size) {
                WeightedItem wi = path.get(i);
                i++;
                if (forbiddenItems.contains(wi.getItem()) || _item.equals(wi.getItem())) {
                    continue; // ignore this item
                }
                long id = wi.getItemId();
                recList.add(new ContestItem(id));
                recItems.add(id);
                n++;
            }
        }
        completeList(recList, recItems, domainItemPath.get(domain), limit - recList.size(), forbiddenItems);

        return recList;
    }

    private static void completeList(List<ContestItem> recList, Set<Long> itemsAlreadyRecommended, Map<String, WeightedItemList> domainItems, int howMany, Set<String> forbiddenItems) {
        int n = 0;
        for (WeightedItemList wil : domainItems.values()) {
            for (WeightedItem wi : wil) {
                if (n >= howMany) {
                    break;
                }
                long id = wi.getItemId();
                if (!forbiddenItems.contains(wi.getItem()) && !itemsAlreadyRecommended.contains(id)) {
                    recList.add(new ContestItem(id));
                    itemsAlreadyRecommended.add(id);
                    n++;
                }
            }
        }
    }

    public void init() {
        FileFilter logFilter = new WildcardFileFilter("contest.log*");
        final File dir = new File(".");
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
                if (JsonUtils.isImpression(line)) {
                    impression(line);
                }
                if (JsonUtils.isFeedback(line)) {
                    feedback(line);
                }
            }
        }
    }

    public void impression(String _impression) {
        Integer domainId = JsonUtils.getDomainIdFromImpression(_impression);
        String item = JsonUtils.getItemIdFromImpression(_impression);

        if ((domainId != null) && (item != null)) {
            update(domainId, null, item);
        }
    }

    private void update(int domainId, String source, String target) {
        String item = target;

        if (source != null) {
            update(domainId, null, source);
            synchronized (this) {
                domainLastItem.put(domainId, source);
            }
        }

        String lastItem = null;
        synchronized (this) {
            lastItem = domainLastItem.get(domainId);
            domainLastItem.put(domainId, item);
        }
        WeightedItemList toUpdate = null;
        if (lastItem == null) {
            toUpdate = new WeightedItemList();
            Map<String, WeightedItemList> m = new ConcurrentHashMap<String, WeightedItemList>();
            m.put(item, toUpdate);
            synchronized (this) {
                domainItemPath.put(domainId, m);
            }
        } else {
            synchronized (this) {
                toUpdate = domainItemPath.get(domainId).get(lastItem);
            }
            if (toUpdate == null) {
                toUpdate = new WeightedItemList();
                domainItemPath.get(domainId).put(lastItem, toUpdate);
            }
            toUpdate.add(new WeightedItem(item, System.currentTimeMillis()));
        }
    }

    public void feedback(String _feedback) {
        Integer domainId = JsonUtils.getDomainIdFromFeedback(_feedback);
        String source = JsonUtils.getSourceIdFromFeedback(_feedback);
        String target = JsonUtils.getTargetIdFromFeedback(_feedback);

        if ((domainId != null) && (source != null) && (target != null)) {
            update(domainId, source, target);
        }
    }

    public void error(String _error) {
        logger.error(_error);
        String[] invalidItems = JsonUtils.getInvalidItemsFromError(_error);
        if (invalidItems != null) {
            // since domain is optional, we cannot store the forbidden items per domain
            for (String item : invalidItems) {
                forbiddenItems.add(item);
            }
        }
    }

    public void setProperties(Properties properties) {
    }

    public static class WeightedItem implements Serializable, Comparable<WeightedItem> {

        private String item;
        private long time;
        private int freq;

        public WeightedItem(String item, long time) {
            this.item = item;
            this.time = time;
            this.freq = 1;
        }

        public String getItem() {
            return item;
        }

        public Long getItemId() {
            return Long.parseLong(item);
        }

        public int getFreq() {
            return freq;
        }

        public void setTime(long time) {
            this.time = time;
        }

        public void setFreq(int freq) {
            this.freq = freq;
        }

        public long getTime() {
            return time;
        }

        public int compareTo(WeightedItem t) {
            int c = getFreq() - t.getFreq();
            if (c == 0) {
                long diff = getTime() - t.getTime();
                c = (diff == 0L ? getItemId().compareTo(t.getItemId()) : (diff < 0L ? -1 : 1));
            }
            return c;
        }

        @Override
        public String toString() {
            return "[" + item + "," + time + "," + freq + "]";
        }
    }

    public static class WeightedItemList extends ArrayList<WeightedItem> implements Serializable {

        private Map<Long, Integer> positions;
        private int curPos;

        public WeightedItemList() {
            super();

            positions = new ConcurrentHashMap<Long, Integer>();
            curPos = 0;
        }

        @Override
        public boolean add(WeightedItem e) {
            if (!positions.containsKey(e.getItemId())) {
                positions.put(e.getItemId(), curPos);
                return super.add(e);
            } else {
                WeightedItem ee = get(positions.get(e.getItemId()));
                ee.setFreq(1 + ee.getFreq());
                ee.setTime(e.getTime());
                return false;
            }
        }
    }
}
