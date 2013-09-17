package net.recommenders.plista.rec.filter;

import java.util.List;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.client.Model;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public interface Filter extends Model {

    public List<Long> filter(Message message, Integer limit, Recommender rec);
}
