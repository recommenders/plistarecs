package net.recommenders.plista.rec;

import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class PROtherCategoryRecommenderWithUserFilter extends AbstractRecommender implements Recommender {

    public PROtherCategoryRecommenderWithUserFilter() {
        super(new UserFilterWrapper(new WrappedRecommender(new PROtherCategoryBasedRecommender())));
    }
}
