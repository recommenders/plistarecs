package net.recommenders.plista.rec;

import java.util.List;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * Interface to allow recommenders to be wrapped by other classes, like filters
 *
 * @author alejandr
 */
public interface WrappableRecommenderIF extends Recommender {

    public List<Long> recommendAll(final Message input, final Integer limit);
}
