package net.recommenders.plista.rec.combination;

import net.recommenders.plista.rec.LuceneRecommender;
import net.recommenders.plista.rec.filter.UserFilter;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class LuceneRecommenderWithUserFilter extends AbstractCombinationRecommender implements Recommender {

    public LuceneRecommenderWithUserFilter() {
        super(new LuceneRecommender(), new UserFilter());
    }
}
