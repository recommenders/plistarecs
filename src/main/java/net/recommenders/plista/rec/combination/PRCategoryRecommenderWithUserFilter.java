package net.recommenders.plista.rec.combination;

import net.recommenders.plista.rec.PRCategoryBasedRecommender;
import net.recommenders.plista.rec.filter.UserFilter;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class PRCategoryRecommenderWithUserFilter extends AbstractCombinationRecommender implements Recommender {

    public PRCategoryRecommenderWithUserFilter() {
        super(new PRCategoryBasedRecommender(), new UserFilter());
    }
}
