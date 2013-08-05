package net.recommenders.plistacontest.recommender;

import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import java.util.*;

/**
 *
 * @author alejandro
 */
public class WrappableRecommender implements ContestRecommender, WrappableRecommenderIF {

    private ContestRecommender wrapped;
    private int factor;

    public WrappableRecommender(ContestRecommender wrapped) {
        this(wrapped, 10);
    }

    public WrappableRecommender(ContestRecommender wrapped, int factor) {
        this.wrapped = wrapped;
        this.factor = factor;
    }

    @Override
    public List<ContestItem> recommendAll(String _client, String _item,
            String _domain, String _description, String _limit) {
        return wrapped.recommend(_client, _item, _domain, _description, (factor * Integer.parseInt(_limit)) + "");
    }

    public List<ContestItem> recommend(String _client, String _item, String _domain, String _description, String _limit) {
        return wrapped.recommend(_client, _item, _domain, _description, _limit);
    }

    public void init() {
        wrapped.init();
    }

    public void impression(String _impression) {
        wrapped.impression(_impression);
    }

    public void feedback(String _feedback) {
        wrapped.feedback(_feedback);
    }

    public void error(String _error) {
        wrapped.error(_error);
    }

    public void setProperties(Properties properties) {
        wrapped.setProperties(properties);
    }
}
