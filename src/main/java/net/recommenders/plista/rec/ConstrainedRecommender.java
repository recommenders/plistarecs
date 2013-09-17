package net.recommenders.plista.rec;

import java.util.List;
import java.util.Set;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public interface ConstrainedRecommender extends Recommender {

    public List<Long> recommend(final Message _input, Set<Long> constraints);
}
