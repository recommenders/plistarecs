package net.recommenders.plista.rec.combination;

import net.recommenders.plista.recommender.RecentRecommender;
import net.recommenders.plista.recommender.Recommender;
import net.recommenders.plista.rec.LuceneRecommender;

/**
 * An index for news articles
 *
 * @author alan
 */
public class CombinedLuceneRecentRecommender extends AbstractCombinationRecommender implements Recommender {

    public CombinedLuceneRecentRecommender() {
        super(new LuceneRecommender(), new RecentRecommender());
    }
}
