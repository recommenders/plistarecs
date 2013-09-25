package net.recommenders.plista.rec;


import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.log.DataLogger;
import net.recommenders.plista.recommender.Recommender;
import net.recommenders.plista.utils.JsonUtils;

/**
 *
 * @author alejandr
 */
public class CategoryBasedRecommender implements Recommender {

    private static DataLogger logger = DataLogger.getLogger(CategoryBasedRecommender.class);
    private Set<Long> forbiddenItems;
    private Set<Long> allItems;
    private Map<Long, Map<Long, Set<Long>>> mapDomainCategoryItems;
    private Map<Long, Set<Long>> mapDomainItems;

    public CategoryBasedRecommender() {
        mapDomainCategoryItems = new ConcurrentHashMap<Long, Map<Long, Set<Long>>>();
        mapDomainItems = new ConcurrentHashMap<Long, Set<Long>>();
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
//        System.out.println(cbr.recommend("" + JsonUtils.getClientId(json), JsonUtils.getItemIdFromImpression(json), "" + JsonUtils.getDomainId(json), json, "" + JsonUtils.getConfigLimitFromImpression(json)));
    }

    public List<Long> recommend(Message message, Integer limit) {
        Long domain = message.getDomainID();
        Long category = message.getItemCategory();
        Long item = message.getItemID();

        return recommend(null, item, domain, category, limit);
    }

    public List<Long> recommend(Long user, Long item, Long domain, Long category, Integer limit) {
        final List<Long> recList = new ArrayList<Long>();

        final Set<Long> recItems = new HashSet<Long>();
        recItems.add(item);
        if (category != null) {
            Map<Long, Set<Long>> categoryItems = mapDomainCategoryItems.get(domain);
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
                        if (forbiddenItems.contains(candidate) || item == candidate) {
                            continue; // ignore this item
                        }
                        recList.add(candidate);
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

    private static void completeList(List<Long> recList, Set<Long> itemsAlreadyRecommended, Set<Long> domainItems, int howMany, Set<Long> forbiddenItems) {
        int n = 0;
        if (domainItems != null) {
            // TODO: improve this iteration using some popularity information or recency
            for (Long item : domainItems) {
                if (n >= howMany) {
                    break;
                }
                if (!forbiddenItems.contains(item) && !itemsAlreadyRecommended.contains(item)) {
                    recList.add(item);
                    itemsAlreadyRecommended.add(item);
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
            update(domainId, item, category, recommendable);
        }
    }

    private void update(Long domainId, Long itemId, Long category, Boolean recommendable) {
        if (category != null) {
            Map<Long, Set<Long>> categoryItems = mapDomainCategoryItems.get(domainId);
            if (categoryItems == null) {
                categoryItems = new ConcurrentHashMap<Long, Set<Long>>();
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

    public void click(Message _click) {
        Long domainId = _click.getDomainID();
        Long source = _click.getItemSourceID();
        Long target = _click.getItemID();
        Long category = _click.getItemCategory();

        if ((domainId != null) && (source != null)) {
            update(domainId, source, category, null);
        }
        if ((domainId != null) && (target != null) && (category != null)) {
            update(domainId, target, category, null);
        }
    }

    public void setProperties(Properties properties) {
    }

    public void impression(Message _impression) {
        update(_impression);
    }
}
