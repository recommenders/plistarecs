package net.recommenders.plista.rec;

import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class ContestLuceneRecommenderWithUserFilter extends AbstractRecommender implements Recommender {

    public ContestLuceneRecommenderWithUserFilter() {
        super(new UserFilterWrapper(new WrappedRecommender(new LuceneRecommender())));
    }
}
