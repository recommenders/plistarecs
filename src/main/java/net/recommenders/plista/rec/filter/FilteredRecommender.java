package net.recommenders.plista.rec.filter;

import java.util.List;
import java.util.Properties;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class FilteredRecommender implements Recommender {

    private Filter filter;
    private Recommender recommender;
    private int factor;

    public FilteredRecommender(Filter filter, Recommender rec) {
        this(filter, rec, 10);
    }

    public FilteredRecommender(Filter filter, Recommender rec, int factor) {
        this.filter = filter;
        this.recommender = rec;
        this.factor = factor;
    }

    public List<Long> recommend(Message _input, Integer limit) {
        return filter.filter(_input, factor * limit, recommender);
    }

    public void init() {
        recommender.init();
    }

    public void impression(Message _impression) {
        recommender.impression(_impression);
    }

    public void click(Message _feedback) {
        recommender.click(_feedback);
    }

    public void update(Message _update) {
        recommender.update(_update);
    }

    public void setProperties(Properties properties) {
        recommender.setProperties(properties);
    }
}
