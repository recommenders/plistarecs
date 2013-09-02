package net.recommenders.plista.rec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;
import org.apache.log4j.Logger;

/**
 *
 * Filter
 *
 * @author alejandr
 */
public class UserFilterWrapper implements Recommender {

    private static final Logger logger = Logger.getLogger(UserFilterWrapper.class);
    private WrappableRecommenderIF rec;
    private Map<Long, InteractionModel> domainModels;

    public UserFilterWrapper(WrappableRecommenderIF rec) {
        this.rec = rec;

        this.domainModels = new HashMap<Long, InteractionModel>();
    }

    private InteractionModel getModel(Long domain) {
        InteractionModel model = domainModels.get(domain);
        if (model == null) {
            model = new SimpleInteractionModel();
            domainModels.put(domain, model);
        }
        return model;
    }

    public List<Long> recommend(Message message, Integer limit) {
        List<Long> allItems = rec.recommendAll(message, limit);
        List<Long> filteredItems = new ArrayList<Long>();
        // DataModelHelper.getDataModel(this.numberOfDays, d)
        Long userID = message.getUserID();
        Long domain = message.getDomainID();

        InteractionModel model = getModel(domain);
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
        return filteredItems;
    }

    public void init() {
        rec.init();
    }

    public void impression(Message _impression) {
        final Long domain = _impression.getDomainID();
        final Long client = _impression.getUserID();
        final Long item = _impression.getItemID();

        getModel(domain).addImpression(client, item);

        rec.impression(_impression);
    }

    public void click(Message _feedback) {
        final Long client = _feedback.getUserID();
        final Long item = _feedback.getItemID();
        final Long domain = _feedback.getDomainID();

        getModel(domain).addClick(client, item);

        rec.click(_feedback);
    }

    public void setProperties(Properties properties) {
        rec.setProperties(properties);
    }

    public void update(Message _update) {
        rec.update(_update);
    }

    public static interface InteractionModel {

        public void addClick(Long user, Long item);

        public void addImpression(Long user, Long item);

        public Set<Long> getInteractions(Long user);
    }

    public static class SimpleInteractionModel implements InteractionModel {

        private Map<Long, Set<Long>> model;

        public SimpleInteractionModel() {
            model = new HashMap<Long, Set<Long>>();
        }

        private void add(Long user, Long item) {
            if (user == null || item == null) {
                return;
            }
            Set<Long> items = model.get(user);
            if (items == null) {
                items = new TreeSet<Long>();
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
