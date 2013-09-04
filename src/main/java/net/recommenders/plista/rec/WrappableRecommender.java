package net.recommenders.plista.rec;

import java.util.List;
import java.util.Properties;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandro
 */
public class WrappableRecommender implements Recommender, WrappableRecommenderIF {

    private Recommender wrapped;
    private int factor;

    public WrappableRecommender(Recommender wrapped) {
        this(wrapped, 10);
    }

    public WrappableRecommender(Recommender wrapped, int factor) {
        this.wrapped = wrapped;
        this.factor = factor;
    }

    @Override
    public List<Long> recommendAll(Message message, Integer limit) {
        return wrapped.recommend(message, (factor * limit));
    }

    public List<Long> recommend(Message message, Integer limit) {
        return wrapped.recommend(message, limit);
    }

    public void init() {
        wrapped.init();
    }

    public void impression(Message _impression) {
        wrapped.impression(_impression);
    }

    public void click(Message _feedback) {
        wrapped.click(_feedback);
    }

    public void update(Message _update) {
        wrapped.update(_update);
    }

    public void setProperties(Properties properties) {
        wrapped.setProperties(properties);
    }
}
