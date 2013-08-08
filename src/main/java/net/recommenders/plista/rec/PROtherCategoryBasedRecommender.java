package net.recommenders.plista.rec;

import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.recommenders.plista.utils.JsonUtils;

/**
 *
 * CategoryBased recommender with popularity and recency information.
 * It recommends items in a different category as the target item.
 *
 * @author alejandr
 */
public class PROtherCategoryBasedRecommender extends PRCategoryBasedRecommender implements ContestRecommender {

    private static Logger logger = Logger.getLogger(PROtherCategoryBasedRecommender.class);

    public PROtherCategoryBasedRecommender() {
        super();
    }

    @Override
    public List<ContestItem> recommend(String _client, String _item, String _domain, String _description, String _limit) {
        final List<ContestItem> recList = new ArrayList<ContestItem>();

        int limit = Integer.parseInt(_limit);
        int domain = Integer.parseInt(_domain);
        Integer category = JsonUtils.getContextCategoryIdFromImpression(_description);

        final Set<Long> recItems = new HashSet<Long>();
        recItems.add(Long.parseLong(_item));
        if (category != null) {
            Map<Integer, PathRecommender.WeightedItemList> categoryItems = mapDomainCategoryItems.get(domain);
            if (categoryItems != null) {
                // create a list with all the items not in this category
                PathRecommender.WeightedItemList candidateItems = new PathRecommender.WeightedItemList();
                for (Integer cat : categoryItems.keySet()) {
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
                        if (forbiddenItems.contains(candidate.getItemId()) || _item.equals(candidate.getItem().toString())) {
                            continue; // ignore this item
                        }
                        recList.add(new ContestItem(candidate.getItemId()));
                        recItems.add(candidate.getItemId());
                        n++;
                    }
                }
            }
        }

        completeList(recList, recItems, mapDomainItems.get(domain), limit - recList.size(), forbiddenItems);
        // just in case no information is found... (useful for the test case)
        completeList(recList, recItems, allItems, limit - recList.size(), forbiddenItems);

        return recList;
    }
}
