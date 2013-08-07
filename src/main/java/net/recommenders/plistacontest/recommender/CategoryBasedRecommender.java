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
    private Set<Long> allItems;
    private Map<Integer, Map<Integer, Set<Long>>> mapDomainCategoryItems;
    private Map<Integer, Set<Long>> mapDomainItems;

    public CategoryBasedRecommender() {
        mapDomainCategoryItems = new ConcurrentHashMap<Integer, Map<Integer, Set<Long>>>();
        mapDomainItems = new ConcurrentHashMap<Integer, Set<Long>>();
        forbiddenItems = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
        allItems = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
    }

    public static void main(String[] args) {
        String json = "{\"msg\":\"impression\",\"id\":97847,\"client\":{\"id\":6433},\"domain\":{\"id\":107},\"item\":{\"id\":57537,\"title\":\"Nothing but a test\",\"url\":\"http:\\/\\/www.example.com\\/articles\\/57537\",\"created\":1375710320,\"text\":\"Still nothing but a <strong>test<\\/strong>.\",\"img\":null,\"recommendable\":true},\"context\":{\"category\":{\"id\":60}},\"config\":{\"timeout\":1,\"recommend\":true,\"limit\":4},\"version\":\"1.0\"},2013-08-05 16:45:20,700";
        Integer category = JsonUtils.getContextCategoryIdFromImpression(json);
        System.out.println(category);

        category = JsonUtils.getContextCategoryIdFromImpression(json.substring(0, json.length() - 24));
        System.out.println(category);

        CategoryBasedRecommender cbr = new CategoryBasedRecommender();
        cbr.init();
        json = "{\"msg\":\"impression\",\"id\":22429,\"client\":{\"id\":7856},\"domain\":{\"id\":795},\"item\":{\"id\":92364,\"title\":\"Nothing but a test\",\"url\":\"http:\\/\\/www.example.com\\/articles\\/92364\",\"created\":1375711387,\"text\":\"Still nothing but a <strong>test<\\/strong>.\",\"img\":null,\"recommendable\":true},\"context\":{\"category\":{\"id\":33}},\"config\":{\"timeout\":1,\"recommend\":true,\"limit\":4},\"version\":\"1.0\"},2013-08-05 17:03:06,975";
        json = "{\"msg\":\"impression\",\"id\":69465,\"client\":{\"id\":3777},\"domain\":{\"id\":140},\"item\":{\"id\":90450,\"title\":\"Nothing but a test\",\"url\":\"http:\\/\\/www.example.com\\/articles\\/90450\",\"created\":1375713174,\"text\":\"Still nothing but a <strong>test<\\/strong>.\",\"img\":null,\"recommendable\":true},\"context\":{\"category\":{\"id\":99}},\"config\":{\"timeout\":1,\"recommend\":true,\"limit\":4},\"version\":\"1.0\"}";
        System.out.println(cbr.recommend("" + JsonUtils.getClientId(json), JsonUtils.getItemIdFromImpression(json), "" + JsonUtils.getDomainId(json), json, "" + JsonUtils.getConfigLimitFromImpression(json)));
    }

    public List<ContestItem> recommend(String _client, String _item, String _domain, String _description, String _limit) {
//        logger.debug("recommend:" + _description);
        final List<ContestItem> recList = new ArrayList<ContestItem>();

        int limit = Integer.parseInt(_limit);
        int domain = Integer.parseInt(_domain);
        Integer category = JsonUtils.getContextCategoryIdFromImpression(_description);

        final Set<Long> recItems = new HashSet<Long>();
        recItems.add(Long.parseLong(_item));
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
                        if (forbiddenItems.contains(candidate) || _item.equals(candidate.toString())) {
                            continue; // ignore this item
                        }
                        recList.add(new ContestItem(candidate));
                        recItems.add(candidate);
                        n++;
                    }
                }
            }
        }

//        logger.debug("recommend: first size=" + recList.size());
        completeList(recList, recItems, mapDomainItems.get(domain), limit - recList.size(), forbiddenItems);
//        logger.debug("recommend: second size=" + recList.size());
        // just in case no information is found... (useful for the test case)
        completeList(recList, recItems, allItems, limit - recList.size(), forbiddenItems);
//        logger.debug("recommend: third size=" + recList.size());

        return recList;
    }

    private static void completeList(List<ContestItem> recList, Set<Long> itemsAlreadyRecommended, Set<Long> domainItems, int howMany, Set<Long> forbiddenItems) {
        int n = 0;
        if (domainItems != null) {
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
        logger.debug("init finished");
    }

    public void impression(String _impression) {
        Integer domainId = JsonUtils.getDomainIdFromImpression(_impression);
        String item = JsonUtils.getItemIdFromImpression(_impression);
        Integer category = JsonUtils.getContextCategoryIdFromImpression(_impression);
        Boolean recommendable = JsonUtils.getItemRecommendableFromImpression(_impression);

        if ((domainId != null) && (item != null)) {
            update(domainId, item, category, recommendable);
        }
    }

    private void update(int domainId, String item, Integer category, Boolean recommendable) {
        Long itemId = Long.parseLong(item);
        if (category != null) {
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
            items.add(itemId);
        }
        // without category constraint
        Set<Long> items = mapDomainItems.get(domainId);
        if (items == null) {
            items = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
            mapDomainItems.put(domainId, items);
        }
        items.add(itemId);
        // all items
        allItems.add(itemId);
        if (recommendable != null && !recommendable.booleanValue()) {
            forbiddenItems.add(itemId);
        }
    }

    public void feedback(String _feedback) {
        Integer domainId = JsonUtils.getDomainIdFromFeedback(_feedback);
        String source = JsonUtils.getSourceIdFromFeedback(_feedback);
        String target = JsonUtils.getTargetIdFromFeedback(_feedback);
        Integer category = JsonUtils.getContextCategoryIdFromFeedback(_feedback);

        if ((domainId != null) && (source != null)) {
            update(domainId, source, category, null);
        }
        if ((domainId != null) && (target != null) && (category != null)) {
            update(domainId, target, category, null);
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
