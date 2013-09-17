package net.recommenders.plista.rec.combination;

import net.recommenders.plista.recommender.RecentRecommender;
import net.recommenders.plista.recommender.Recommender;
import net.recommenders.plista.rec.LuceneRecommender;
import net.recommenders.plista.rec.filter.FilteredRecommender;
import net.recommenders.plista.rec.filter.UserFilter;

/**
 * An index for news articles
 *
 * @author alan
 */
public class CombinedLuceneRecentWithUserFilterRecommender extends AbstractCombinationRecommender implements Recommender {

    public CombinedLuceneRecentWithUserFilterRecommender() {
        super(new FilteredRecommender(new UserFilter(), new LuceneRecommender()), new RecentRecommender());
    }
}
