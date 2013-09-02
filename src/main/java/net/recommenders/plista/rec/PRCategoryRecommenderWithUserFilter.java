package net.recommenders.plista.rec;

import java.util.List;
import java.util.Properties;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class PRCategoryRecommenderWithUserFilter implements Recommender {

    private Recommender wrapped;

    public PRCategoryRecommenderWithUserFilter() {
        this.wrapped = new UserFilterWrapper(new WrappableRecommender(new PRCategoryBasedRecommender()));
    }

    public List<Long> recommend(Message input, Integer limit) {
        return wrapped.recommend(input, limit);
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
