package net.recommenders.plistacontest.recommender;

import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import java.io.File;
import java.io.FileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;

import java.io.FileNotFoundException;
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
public class CategoryBasedRecommender implements ContestRecommender {

    private static Logger logger = Logger.getLogger(CategoryBasedRecommender.class);
    private Set<Long> forbiddenItems;
    private Map<Integer, Map<Integer, Set<Long>>> mapDomainCategoryItems;
    private Map<Integer, Set<Long>> mapDomainItems;

    public CategoryBasedRecommender() {
        mapDomainCategoryItems = new ConcurrentHashMap<Integer, Map<Integer, Set<Long>>>();
        mapDomainItems = new ConcurrentHashMap<Integer, Set<Long>>();
        forbiddenItems = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
    }

    public List<ContestItem> recommend(String _client, String _item, String _domain, String _description, String _limit) {
        final List<ContestItem> recList = new ArrayList<ContestItem>();

        int limit = Integer.parseInt(_limit);
        int domain = Integer.parseInt(_domain);
        Integer category = JsonUtils.getContextCategoryIdFromImpression(_description);

        final Set<Long> recItems = new HashSet<Long>();
        if (category != null) {
            Map<Integer, Set<Long>> categoryItems = mapDomainCategoryItems.get(domain);
            if (categoryItems != null) {
                Set<Long> candidateItems = categoryItems.get(category);
                if (candidateItems != null) {
                    int n = 0;
                    int size = Math.min(limit, candidateItems.size());
                    // TODO: improve this iteration using some popularity information or recency
                    for (Long candidate : candidateItems) {
                        if (n >= size) {
                            break;
                        }
                        if (forbiddenItems.contains(candidate)) {
                            continue; // ignore this item
                        }
                        recList.add(new ContestItem(candidate));
                        recItems.add(candidate);
                        n++;
                    }
                }
            }
        }

        completeList(recList, recItems, mapDomainItems.get(domain), limit - recList.size(), forbiddenItems);

        return recList;
    }

    private static void completeList(List<ContestItem> recList, Set<Long> itemsAlreadyRecommended, Set<Long> domainItems, int howMany, Set<Long> forbiddenItems) {
        int n = 0;
        // TODO: improve this iteration using some popularity information or recency
        for (Long item : domainItems) {
            if (n >= howMany) {
                break;
            }
            if (!forbiddenItems.contains(item) && !itemsAlreadyRecommended.contains(item)) {
                recList.add(new ContestItem(item));
                itemsAlreadyRecommended.add(item);
                n++;
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
        Integer category = JsonUtils.getContextCategoryIdFromImpression(_impression);

        if ((domainId != null) && (item != null) && (category != null)) {
            update(domainId, item, category);
        }
    }

    private void update(int domainId, String item, int category) {
        Map<Integer, Set<Long>> categoryItems = mapDomainCategoryItems.get(domainId);
        if (categoryItems == null) {
            categoryItems = new ConcurrentHashMap<Integer, Set<Long>>();
            mapDomainCategoryItems.put(domainId, categoryItems);
        }
        Set<Long> items = categoryItems.get(category);
        if (items == null) {
            items = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
            categoryItems.put(category, items);
        }
        items.add(Long.parseLong(item));
    }

    public void feedback(String _feedback) {
        Integer domainId = JsonUtils.getDomainIdFromFeedback(_feedback);
        String source = JsonUtils.getSourceIdFromFeedback(_feedback);
        String target = JsonUtils.getTargetIdFromFeedback(_feedback);
        Integer category = JsonUtils.getContextCategoryIdFromImpression(_feedback);

        if ((domainId != null) && (source != null) && (category != null)) {
            update(domainId, source, category);
        }
        if ((domainId != null) && (target != null) && (category != null)) {
            update(domainId, target, category);
        }
    }

    public void error(String _error) {
        logger.error(_error);
        String[] invalidItems = JsonUtils.getInvalidItemsFromError(_error);
        if (invalidItems != null) {
            // since domain is optional, we cannot store the forbidden items per domain
            for (String item : invalidItems) {
                forbiddenItems.add(Long.parseLong(item));
            }
        }
    }

    public void setProperties(Properties properties) {
    }
}
