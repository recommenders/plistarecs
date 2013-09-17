package net.recommenders.plista.rec.combination;

import net.recommenders.plista.rec.PROtherCategoryBasedRecommender;
import net.recommenders.plista.rec.filter.UserFilter;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class PROtherCategoryRecommenderWithUserFilter extends AbstractCombinationRecommender implements Recommender {

    public PROtherCategoryRecommenderWithUserFilter() {
        super(new PROtherCategoryBasedRecommender(), new UserFilter());
    }
}
