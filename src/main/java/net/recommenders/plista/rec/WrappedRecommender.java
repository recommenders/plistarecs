package net.recommenders.plista.rec;

import java.util.List;
import java.util.Properties;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandro
 */
public class WrappedRecommender extends AbstractRecommender implements Recommender, WrappableRecommender {

    private Recommender wrapped;
    private int factor;

    public WrappedRecommender(Recommender wrapped) {
        this(wrapped, 10);
    }

    public WrappedRecommender(Recommender wrapped, int factor) {
        super(wrapped);
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
}
