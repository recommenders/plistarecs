package net.recommenders.plista.utils;

import net.recommenders.plista.client.ChallengeMessage;
import net.recommenders.plista.client.Message;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;
import net.recommenders.plista.log.DataLogger;

/**
 * Created with IntelliJ IDEA. User: alan Date: 2013-08-30 Time: 14:28 To change
 * this template use File | Settings | File Templates.
 */
public class ContentDB {

    private final static DataLogger logger = DataLogger.getLogger(ContentDB.class);
    private static Connection con;
    private String dbFileName = "content.db";
    private Set<Long> items;

    public ContentDB() {
        items = new HashSet<Long>();
    }

    public static void main(String[] args) {
        ContentDB db = new ContentDB();
        db.init();
    }

    public void init() {
        try {
            Class.forName("org.sqlite.JDBC");

            File dbFile = new File(dbFileName);
            boolean dbExists = dbFile.exists();
            con = DriverManager.getConnection("jdbc:sqlite:" + dbFileName);
            if (dbExists) {
                con.prepareStatement("ATTACH DATABASE '" + dbFileName + "' AS 'content'").execute();
            }

            Statement stat = con.createStatement();
            stat.executeUpdate("CREATE TABLE IF NOT EXISTS messages("
                    + "id INTEGER,"
                    + "domain NUMERIC,"
                    + "title TEXT,"
                    + "text TEXT,"
                    + "content TEXT,"
                    + "PRIMARY KEY (id));"
                    + "CREATE INDEX IF NOT EXISTS 'domain' ON 'messages' ('domain' ASC);");
            stat.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addMessage(Message message, String content) {
        boolean result = false;
        Long itemID = message.getItemID();
        Long domainID = message.getDomainID();
        String title = message.getItemTitle();
        String text = message.getItemText();
        if (items.contains(itemID)) {
            return true;
        } else {
            items.add(itemID);
        }
        try {
            PreparedStatement prep = con
                    .prepareStatement("INSERT INTO messages VALUES(?,?,?,?,?);");
            prep.setString(1, itemID.toString());
            prep.setString(2, domainID.toString());
            prep.setString(3, title);
            prep.setString(4, text);
            prep.setString(5, content);
            prep.execute();
            prep.close();
            result = true;
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return result;
    }

    public String getContent(Long itemID, Long domainID) {
        String result = null;
        try {
            Statement stat = con.createStatement();
            ResultSet res = stat.executeQuery("SELECT * FROM messages where domain = '" + domainID
                    + "' and id = '" + itemID + "'");
            while (res.next()) {
                result = res.getString("content");
            }
            stat.close();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return result;
    }

    public Message getMessage(Long itemID, Long domainID) {
        ChallengeMessage message = null;
        try {
            Statement stat = con.createStatement();
            ResultSet res = stat.executeQuery("SELECT * FROM messages where domain = '" + domainID
                    + "' and id = '" + itemID + "'");
            while (res.next()) {
                message = new ChallengeMessage();
                message.setItemID(itemID);
                message.setDomainID(domainID);
                message.setItemText(res.getString("text"));
                message.setItemTitle(res.getString("title"));
            }
            stat.close();
        } catch (SQLException e) {
            logger.error(e.getMessage());
        }
        return message;
    }

    public void disconnect() throws SQLException {
        con.close();
    }

    public boolean itemExists(Long itemID) {
        return items.contains(itemID);
    }
}
