package net.recommenders.plista.rec.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.log.DataLogger;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * Filter
 *
 * @author alejandr
 */
public class UserFilter implements Filter {

    private static final DataLogger logger = DataLogger.getLogger(UserFilter.class);
    private Map<Long, InteractionModel> domainModels;

    public UserFilter() {
        this.domainModels = new ConcurrentHashMap<Long, InteractionModel>();
    }

    private InteractionModel getModel(Long domain) {
        if (domain == null) {
            logger.warn("EXCEPTION\tdomain null");
            return null;
        }
        InteractionModel model = domainModels.get(domain);
        if (model == null) {
            model = new SimpleInteractionModel();
            domainModels.put(domain, model);
        }
        return model;
    }

    public List<Long> filter(Message message, Integer limit, Recommender rec) {
        List<Long> allItems = rec.recommend(message, limit);
        List<Long> filteredItems = new ArrayList<Long>();
        // DataModelHelper.getDataModel(this.numberOfDays, d)
        Long userID = message.getUserID();
        Long domain = message.getDomainID();

        if (domain != null) {
            InteractionModel model = getModel(domain);
            if (userID != null) {
                Set<Long> alreadyObservedItems = model.getInteractions(userID);
                int n = 0;
                for (Long i : allItems) {
                    // check items by the user
                    if (!alreadyObservedItems.contains(i)) {
                        filteredItems.add(i);
                        n++;
                        if (n >= limit) {
                            break;
                        }
                    }
                }
            }
        }
        return filteredItems;
    }

    public void impression(Message _impression) {
        final Long domain = _impression.getDomainID();
        final Long client = _impression.getUserID();
        final Long item = _impression.getItemID();

        if (domain != null && client != null && item != null) {
            getModel(domain).addImpression(client, item);
        }
    }

    public void click(Message _feedback) {
        final Long client = _feedback.getUserID();
        final Long item = _feedback.getItemID();
        final Long domain = _feedback.getDomainID();

        if (domain != null && client != null && item != null) {
            getModel(domain).addClick(client, item);
        }
    }

    public void update(Message _update) {
    }

    public static interface InteractionModel {

        public void addClick(Long user, Long item);

        public void addImpression(Long user, Long item);

        public Set<Long> getInteractions(Long user);
    }

    public static class SimpleInteractionModel implements InteractionModel {

        private Map<Long, Set<Long>> model;

        public SimpleInteractionModel() {
            model = new ConcurrentHashMap<Long, Set<Long>>();
        }

        private void add(Long user, Long item) {
            if (user == null || item == null) {
                return;
            }
            Set<Long> items = model.get(user);
            if (items == null) {
                items = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
                model.put(user, items);
            }
            items.add(item);
        }

        public void addClick(Long user, Long item) {
            add(user, item);
        }

        public void addImpression(Long user, Long item) {
            add(user, item);
        }

        public Set<Long> getInteractions(Long user) {
            if (user == null || !model.containsKey(user)) {
                return new TreeSet<Long>();
            }
            return model.get(user);
        }
    }
}
