package net.recommenders.plista.rec;

import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import java.util.List;

/**
 *
 * Interface to allow recommenders to be wrapped by other classes, like filters
 * 
 * @author alejandr
 */
public interface WrappableRecommenderIF extends ContestRecommender {
    
    public List<ContestItem> recommendAll(final String _client, final String _item, final String _domain,
                    final String _description, final String _limit);
}
