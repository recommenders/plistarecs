package net.recommenders.plista.rec.combination;

import java.util.List;
import java.util.Properties;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.rec.filter.Filter;
import net.recommenders.plista.rec.filter.FilteredRecommender;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public abstract class AbstractCombinationRecommender implements Recommender {

    private static final int BACKUP_LENGTH = 2;
    private Recommender[] recommenders;

    public AbstractCombinationRecommender(Recommender recommender, Filter filter) {
        this.recommenders = new Recommender[]{new FilteredRecommender(filter, recommender)};
    }

    public AbstractCombinationRecommender(Recommender mainRecommender, Recommender backupRecommender) {
        this(new Recommender[]{mainRecommender, backupRecommender});
    }

    public AbstractCombinationRecommender(Recommender[] recommenders) {
        if (recommenders.length < 2) {
            throw new IllegalArgumentException("Not enough recommenders given: " + recommenders.length);
        }
        this.recommenders = recommenders;
    }

    public List<Long> recommend(Message input, Integer limit) {
        if (recommenders.length > 2) {
            throw new IllegalArgumentException("Too many recommenders ('recommend' method needs to be overridden): " + recommenders.length);
        }
        Recommender recommender = recommenders[0];
        Recommender backupRecommender = null;
        if (recommenders.length == 2) {
            backupRecommender = recommenders[1];
        }
        final List<Long> recList = recommender.recommend(input, limit);
        // we complete the recommendation list if a backup recommender was provided
        if (backupRecommender != null && recList.size() < limit) {
            List<Long> backupList = backupRecommender.recommend(input, BACKUP_LENGTH * limit);
            for (Long item : backupList) {
                if (recList.size() < limit && !recList.contains(item)) {
                    recList.add(item);
                }
            }

        }
        return recList;
    }

    public void init() {
        for (Recommender recommender : recommenders) {
            recommender.init();
        }
    }

    public void impression(Message _impression) {
        for (Recommender recommender : recommenders) {
            recommender.impression(_impression);
        }
    }

    public void click(Message _feedback) {
        for (Recommender recommender : recommenders) {
            recommender.click(_feedback);
        }
    }

    public void update(Message _update) {
        for (Recommender recommender : recommenders) {
            recommender.update(_update);
        }
    }

    public void setProperties(Properties properties) {
        for (Recommender recommender : recommenders) {
            recommender.setProperties(properties);
        }
    }
}
