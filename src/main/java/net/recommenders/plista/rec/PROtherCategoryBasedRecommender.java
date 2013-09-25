package net.recommenders.plista.rec;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.log.DataLogger;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * CategoryBased recommender with popularity and recency information. It
 * recommends items in a different category as the target item.
 *
 * @author alejandr
 */
public class PROtherCategoryBasedRecommender extends PRCategoryBasedRecommender implements Recommender {

    private static DataLogger logger = DataLogger.getLogger(PROtherCategoryBasedRecommender.class);

    public PROtherCategoryBasedRecommender() {
        super();
    }

    @Override
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
                        // create a list with all the items not in this category
                        PathRecommender.WeightedItemList candidateItems = new PathRecommender.WeightedItemList();
                        for (Long cat : categoryItems.keySet()) {
                            if (cat.equals(category)) {
                                continue;
                            }
                            for (PathRecommender.WeightedItem wi : categoryItems.get(cat)) {
                                candidateItems.add(wi);
                            }
                        }
                        if (!candidateItems.isEmpty()) {
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
}
