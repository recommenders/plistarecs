package net.recommenders.plista.rec;

import org.apache.log4j.Logger;

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
 * CategoryBased recommender with popularity and recency information
 *
 * @author alejandr
 */
public class PRCategoryBasedRecommender implements Recommender {

    private static Logger logger = Logger.getLogger(PRCategoryBasedRecommender.class);
    protected Set<Long> forbiddenItems;
    protected PathRecommender.WeightedItemList allItems;
    protected Map<Long, Map<Long, PathRecommender.WeightedItemList>> mapDomainCategoryItems;
    protected Map<Long, PathRecommender.WeightedItemList> mapDomainItems;

    public PRCategoryBasedRecommender() {
        mapDomainCategoryItems = new ConcurrentHashMap<Long, Map<Long, PathRecommender.WeightedItemList>>();
        mapDomainItems = new ConcurrentHashMap<Long, PathRecommender.WeightedItemList>();
        forbiddenItems = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
        allItems = new PathRecommender.WeightedItemList();
    }

    public List<Long> recommend(Message input, Integer limit) {
        final List<Long> recList = new ArrayList<Long>();

        Long domain = input.getDomainID();
        Long category = input.getItemCategory();

        final Set<Long> recItems = new HashSet<Long>();
        if (domain != null) {
            Long item = input.getItemID();
            if (item != null) {
                recItems.add(item);
                if (category != null) {
                    Map<Long, PathRecommender.WeightedItemList> categoryItems = mapDomainCategoryItems.get(domain);
                    if (categoryItems != null) {
                        PathRecommender.WeightedItemList candidateItems = categoryItems.get(category);
                        if (candidateItems != null) {
                            // sort it
                            Collections.sort(candidateItems);
                            //
                            int n = 0;
                            int size = Math.min(limit, candidateItems.size());
                            for (PathRecommender.WeightedItem candidate : candidateItems) {
                                if (n >= size) {
                                    break;
                                }
                                if (forbiddenItems.contains(candidate.getItemId()) || item == candidate.getItemId()) {
                                    continue; // ignore this item
                                }
                                recList.add(candidate.getItemId());
                                recItems.add(candidate.getItemId());
                                n++;
                            }
                        }
                    }
                }
            }
            completeList(recList, recItems, mapDomainItems.get(domain), limit - recList.size(), forbiddenItems);
        }
        // just in case no information is found... (useful for the test case)
        completeList(recList, recItems, allItems, limit - recList.size(), forbiddenItems);

        return recList;
    }

    protected static void completeList(List<Long> recList, Set<Long> itemsAlreadyRecommended, PathRecommender.WeightedItemList domainItems, int howMany, Set<Long> forbiddenItems) {
        int n = 0;
        if (domainItems != null) {
            for (PathRecommender.WeightedItem item : domainItems) {
                if (n >= howMany) {
                    break;
                }
                if (!forbiddenItems.contains(item.getItemId()) && !itemsAlreadyRecommended.contains(item.getItemId())) {
                    recList.add(item.getItemId());
                    itemsAlreadyRecommended.add(item.getItemId());
                    n++;
                }
            }
        }
    }

    public void init() {
    }

    public void update(Message _update) {
        Long domainId = _update.getDomainID();
        Long item = _update.getItemID();
        Long category = _update.getItemCategory();
        Boolean recommendable = _update.getItemRecommendable();

        if ((domainId != null) && (item != null)) {
            update(domainId, item, category, recommendable, 1);
        }
    }

    public void impression(Message _impression) {
        update(_impression);
    }

    private void update(Long domainId, Long itemId, Long category, Boolean recommendable, int confidence) {
        long curTime = System.currentTimeMillis();
        if (category != null) {
            Map<Long, PathRecommender.WeightedItemList> categoryItems = mapDomainCategoryItems.get(domainId);
            if (categoryItems == null) {
                categoryItems = new ConcurrentHashMap<Long, PathRecommender.WeightedItemList>();
                mapDomainCategoryItems.put(domainId, categoryItems);
            }
            PathRecommender.WeightedItemList items = categoryItems.get(category);
            if (items == null) {
                items = new PathRecommender.WeightedItemList();
                categoryItems.put(category, items);
            }
            items.add(new PathRecommender.WeightedItem(itemId, curTime), confidence);
        }
        // without category constraint
        PathRecommender.WeightedItemList items = mapDomainItems.get(domainId);
        if (items == null) {
            items = new PathRecommender.WeightedItemList();
            mapDomainItems.put(domainId, items);
        }
        items.add(new PathRecommender.WeightedItem(itemId, curTime), confidence);
        // all items
        allItems.add(new PathRecommender.WeightedItem(itemId, curTime), confidence);
        if (recommendable != null && !recommendable.booleanValue()) {
            forbiddenItems.add(itemId);
        }
    }

    public void click(Message _feedback) {
        Long domainId = _feedback.getDomainID();
        Long source = _feedback.getItemSourceID();
        Long target = _feedback.getItemID();
        Long category = _feedback.getItemCategory();

        if ((domainId != null) && (source != null)) {
            update(domainId, source, category, null, 3);
        }
        if ((domainId != null) && (target != null)) {
            update(domainId, target, category, null, 5);
        }
    }

    public void setProperties(Properties properties) {
    }
}
