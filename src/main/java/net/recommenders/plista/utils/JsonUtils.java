package net.recommenders.plista.utils;

import net.recommenders.plista.log.DataLogger;
import org.json.JSONObject;
import org.json.JSONException;

/**
 *
 * @author alejandr
 */
public class JsonUtils {

    private static final DataLogger logger = DataLogger.getLogger(JsonUtils.class);

    // general methods
    public static JSONObject getObject(String json) throws JSONException {
//        final JSONObject jObj = (JSONObject) JSONValue.parse(json); // json-simple
        final JSONObject jObj = new JSONObject(json);
        return jObj;
    }

    public static Integer getDomainId(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("domain").getInt("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getClientId(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("client").getInt("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getContextCategoryId(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("context").getJSONObject("category").getInt("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getConfigTeamId(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("config").getJSONObject("team").getInt("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Boolean isImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
//        if (jObj.containsKey("msg")) {
            if (jObj.has("msg")) {
                if (jObj.getString("msg").equals("impression")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Boolean isFeedback(String json) {
        try {
            final JSONObject jObj = getObject(json);
//        if (jObj.containsKey("msg")) {
            if (jObj.has("msg")) {
                if (jObj.getString("msg").equals("feedback")) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    // Impression messages
    public static Integer getImpressionIdFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getInt("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getClientIdFromImpression(String json) {
        return getClientId(json);
    }

    public static Integer getDomainIdFromImpression(String json) {
        return getDomainId(json);
    }

    public static String getItemIdFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("item").getString("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static String getItemTitleFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("item").getString("title").replaceAll("\u00ad", "");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static String getItemTextFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("item").getString("text").replaceAll("\u00ad", "");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static String getItemUrlFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("item").getString("url");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static String getItemImgFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("item").getString("img");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getItemCreatedFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("item").getInt("created");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Boolean getItemRecommendableFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("item").getBoolean("recommendable");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getContextCategoryIdFromImpression(String json) {
        return getContextCategoryId(json);
    }

    public static Integer getConfigTeamIdFromImpression(String json) {
        return getConfigTeamId(json);
    }

    public static Double getConfigTimeoutFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("config").getDouble("timeout");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Boolean getConfigRecommendFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("config").getBoolean("recommend");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getConfigLimitFromImpression(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("config").getInt("limit");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    // Feedback messages
    public static Integer getClientIdFromFeedback(String json) {
        return getClientId(json);
    }

    public static Integer getDomainIdFromFeedback(String json) {
        return getDomainId(json);
    }

    public static String getSourceIdFromFeedback(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("source").getString("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static String getTargetIdFromFeedback(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("target").getString("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getContextCategoryIdFromFeedback(String json) {
        return getContextCategoryId(json);
    }

    public static Integer getConfigTeamIdFromFeedback(String json) {
        return getConfigTeamId(json);
    }

    // Error messages
    public static String[] getInvalidItemsFromError(String json) {
        try {
            String[] errorItems = null;
            final JSONObject jErrorObj = getObject(json);
//        if (jErrorObj.containsKey("error")) {
            if (jErrorObj.has("error")) {
                String error = jErrorObj.getString("error");
                if (error.contains("invalid items returned:")) {
                    String tmpError = error.replace("invalid items returned:", "");
                    errorItems = tmpError.split(",");
                }
            }
            return errorItems;
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }

    public static Integer getDomainIdFromError(String json) {
        return getDomainId(json);
    }

    public static Integer getTeamId(String json) {
        try {
            final JSONObject jObj = getObject(json);
            return jObj.getJSONObject("team").getInt("id");
        } catch (Exception e) {
            logger.debug(e.getMessage());
            return null;
        }
    }
}
