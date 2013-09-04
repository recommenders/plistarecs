package net.recommenders.plista.rec;

import org.apache.log4j.Logger;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class PathRecommender implements Recommender {

    private static final Logger logger = Logger.getLogger(PathRecommender.class);
    private Map<Long, Long> domainLastItem;
    private Map<Long, Map<Long, WeightedItemList>> domainItemPath;
    private Set<Long> forbiddenItems;
    private Map<Long, WeightedItemList> allItems;

    public PathRecommender() {
        domainLastItem = new ConcurrentHashMap<Long, Long>();
        domainItemPath = new ConcurrentHashMap<Long, Map<Long, WeightedItemList>>();
        forbiddenItems = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
        allItems = new ConcurrentHashMap<Long, WeightedItemList>();
    }

    public static void main(String[] args) {
        PathRecommender pr = new PathRecommender();
        pr.init();

        WeightedItemList wil = new WeightedItemList();

        wil.add(new WeightedItem(1L, 2L));
        wil.add(new WeightedItem(1L, 6L));
        wil.add(new WeightedItem(2L, 4L));
        wil.add(new WeightedItem(2L, 5L));

        Collections.sort(wil);

        System.out.println(wil);
    }

    public List<Long> recommend(Message input, Integer limit) {
        final List<Long> recList = new ArrayList<Long>();

        Long domain = input.getDomainID();
        Long itemId = input.getItemID();

        final Set<Long> recItems = new HashSet<Long>();
        if (domain != null) {
            if (itemId != null) {
                if (domainItemPath.containsKey(domain)) {
                    final WeightedItemList path = domainItemPath.get(domain).get(itemId);
                    recItems.add(itemId);
                    if (path != null && !path.isEmpty()) {
                        // sort the weighted list
                        Collections.sort(path);
                        // get the first N items (i.e., limit)
                        int n = 0; // recList index
                        int size = Math.min(limit, path.size());
                        int i = 0; // path index
                        while (n < size) {
                            WeightedItem wi = path.get(i);
                            long id = wi.getItemId();
                            i++;
                            if (forbiddenItems.contains(id) || itemId == id) {
                                continue; // ignore this item
                            }
                            recList.add(id);
                            recItems.add(id);
                            n++;
                        }
                    }
                }
            }
            completeList(recList, recItems, domainItemPath.get(domain), limit - recList.size(), forbiddenItems);
        }
        completeList(recList, recItems, allItems, limit - recList.size(), forbiddenItems);

        return recList;
    }

    private static void completeList(List<Long> recList, Set<Long> itemsAlreadyRecommended, Map<Long, WeightedItemList> domainItems, int howMany, Set<Long> forbiddenItems) {
        int n = 0;
        if (domainItems != null) {
            for (WeightedItemList wil : domainItems.values()) {
                for (WeightedItem wi : wil) {
                    if (n >= howMany) {
                        break;
                    }
                    long id = wi.getItemId();
                    if (!forbiddenItems.contains(id) && !itemsAlreadyRecommended.contains(id)) {
                        recList.add(id);
                        itemsAlreadyRecommended.add(id);
                        n++;
                    }
                }
            }
        }
    }

    public void init() {
    }

    public void impression(Message _impression) {
        update(_impression);
    }

    public void update(Message _update) {
        Long domainId = _update.getDomainID();
        Long item = _update.getItemID();
        Boolean recommendable = _update.getItemRecommendable();

        if ((domainId != null) && (item != null)) {
            update(domainId, item, recommendable, 1);
        }
    }

    private void update(Long domainId, Long item, Boolean recommendable, int confidence) {
        long curTime = System.currentTimeMillis();

        Long lastItem = null;
        synchronized (this) {
            lastItem = domainLastItem.get(domainId);
            domainLastItem.put(domainId, item);
        }
        WeightedItemList toUpdate = null;
        if (lastItem == null) {
            toUpdate = new WeightedItemList();
            Map<Long, WeightedItemList> m = new ConcurrentHashMap<Long, WeightedItemList>();
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
            toUpdate.add(new WeightedItem(item, curTime), confidence);
        }
        // all items
        WeightedItemList all = allItems.get(1L);
        if (all == null) {
            all = new WeightedItemList();
            allItems.put(1L, all);
        }
        all.add(new WeightedItem(item, curTime), confidence);
        if (recommendable != null && !recommendable.booleanValue()) {
            forbiddenItems.add(item);
        }
    }

    public void click(Message input) {
        Long domainId = input.getDomainID();
        Long source = input.getItemSourceID();
        Long target = input.getItemID();

        if ((domainId != null) && (source != null)) {
            update(domainId, source, null, 3);
        }
        if ((domainId != null) && (target != null)) {
            update(domainId, target, null, 5);
        }
    }

    public void setProperties(Properties properties) {
    }

    public static class WeightedItem implements Serializable, Comparable<WeightedItem> {

        private Long itemId;
        private long time;
        private int freq;

        public WeightedItem(Long itemId, long time) {
            this.itemId = itemId;
            this.time = time;
            this.freq = 0;
        }

        public Long getItemId() {
            return itemId;
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
            int c = -getFreq() + t.getFreq();
            if (c == 0) {
                long diff = -getTime() + t.getTime();
                c = (diff == 0L ? getItemId().compareTo(t.getItemId()) : (diff < 0L ? -1 : 1));
            }
            return c;
        }

        @Override
        public String toString() {
            return "[" + itemId + "," + time + "," + freq + "]";
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
            return add(e, 1);
        }

        public boolean add(WeightedItem e, int w) {
            synchronized (this) {
                if (!positions.containsKey(e.getItemId())) {
                    positions.put(e.getItemId(), curPos);
                    e.setFreq(w + e.getFreq());
                    curPos++;
                    return super.add(e);
                } else {
                    WeightedItem ee = get(positions.get(e.getItemId()));
                    ee.setFreq(w + ee.getFreq());
                    ee.setTime(e.getTime());
                    return false;
                }
            }
        }
    }
}
