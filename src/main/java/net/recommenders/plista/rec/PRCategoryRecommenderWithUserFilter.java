package net.recommenders.plista.rec;

import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class PRCategoryRecommenderWithUserFilter extends AbstractRecommender implements Recommender {

    public PRCategoryRecommenderWithUserFilter() {
        super(new UserFilterWrapper(new WrappedRecommender(new PRCategoryBasedRecommender())));
    }
}
