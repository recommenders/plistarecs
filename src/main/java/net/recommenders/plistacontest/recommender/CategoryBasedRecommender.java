package net.recommenders.plistacontest.recommender;

import de.dailab.plistacontest.helper.DateHelper;
import de.dailab.plistacontest.helper.FalseItems;
import de.dailab.plistacontest.helper.StatsWriter;
import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 *
 * @author alejandr
 */
public class CategoryBasedRecommender implements ContestRecommender {

    private static Logger logger = Logger.getLogger(CategoryBasedRecommender.class);
    private FalseItems falseItems;
    private Map<String, Map<String, Set<Long>>> mapDomainCategoryItems;

    public CategoryBasedRecommender() {
        falseItems = new FalseItems();
        mapDomainCategoryItems = new HashMap<String, Map<String, Set<Long>>>();
    }

    public List<ContestItem> recommend(String _client, String _item, String _domain, String _description, String _limit) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void init() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void impression(String _impression) {
        final JSONObject jObj = (JSONObject) JSONValue.parse(_impression);
        final String domain = ((JSONObject) jObj.get("domain")).get("id").toString();

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void feedback(String _feedback) {
        final JSONObject jObj = (JSONObject) JSONValue.parse(_feedback);
        final String client = ((JSONObject) jObj.get("client")).get("id").toString();
        final String item = ((JSONObject) jObj.get("target")).get("id").toString();

        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    public void error(String _error) {
        logger.error(_error);
        // {"error":"invalid items returned:89194287","team":{"id":"65"},"code":null,"version":"1.0"}
        try {
            final JSONObject jErrorObj = (JSONObject) JSONValue.parse(_error);
            if (jErrorObj.containsKey("error")) {
                String error = jErrorObj.get("error").toString();
                if (error.contains("invalid items returned:")) {
                    String tmpError = error.replace("invalid items returned:", "");
                    String[] errorItems = tmpError.split(",");
                    for (String errorItem : errorItems) {
                        this.falseItems.addItem(Long.parseLong(errorItem));
                    }
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
        serialize(this.falseItems);
    }

    private void serialize(final FalseItems _falseItemse) {
        try {
            final FileOutputStream fileOut = new FileOutputStream("falseitems_cbr.ser");
            final ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(_falseItemse);
            out.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void deserialize() {
        try {
            final FileInputStream fileIn = new FileInputStream("falseitems_cbr.ser");
            final ObjectInputStream in = new ObjectInputStream(fileIn);
            this.falseItems = (FalseItems) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        } catch (ClassNotFoundException e1) {
            logger.error(e1.getMessage());
        }
    }

    public void setProperties(Properties properties) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
