package net.recommenders.plista.rec;

import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author alejandr
 */
public class ContestLuceneRecommenderWithUserFilter implements ContestRecommender {

    private ContestRecommender wrapped;

    public ContestLuceneRecommenderWithUserFilter() {
        this.wrapped = new UserFilterWrapper(new WrappableRecommender(new ContestLuceneRecommender()));
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
