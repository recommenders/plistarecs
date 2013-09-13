package net.recommenders.plista.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Comparator;
import net.recommenders.plista.client.ChallengeMessage;
import net.recommenders.plista.client.Message;
import org.apache.commons.io.filefilter.WildcardFileFilter;

/**
 *
 * @author alejandr
 */
public class LogGenerator {

    public static final int UPDATE = 2;
    public static final int IMPRESSION = 0;
    public static final int REQUEST = 1;
    public static final int FEEDBACK = 3;
    //
    private static final String DB_TABLE_DATA = "data";
    private static final String DB_TABLE_MATCHING = "matching";
    //
    private static final String TABLE_DATA_FIELD_TIMESTAMP = "timestamp";
    private static final String TABLE_DATA_FIELD_TYPE = "type";
    private static final String TABLE_DATA_FIELD_DOMAIN = "domain";
    private static final String TABLE_DATA_FIELD_ITEM = "item";
    private static final String TABLE_DATA_FIELD_USER = "user";
    private static final String TABLE_DATA_FIELD_TEAM = "team";
    private static final String TABLE_DATA_FIELD_RECS = "recs";
    private static final String TABLE_DATA_FIELD_MESSAGE = "json";
    private static final String TABLE_MATCHING_FIELD_REQ_TIMESTAMP = "rt";
    private static final String TABLE_MATCHING_FIELD_FEEDBACK_TIMESTAMP = "ft";
    private static final String TABLE_MATCHING_FIELD_DOMAIN = "d";
    private static final String TABLE_MATCHING_FIELD_ITEM = "i";
    private static final String TABLE_MATCHING_FIELD_USER = "u";

    public static void generateDbFromChallengeData(String dbFile) {
        try {
            Connection con = getDBConnection(dbFile);
            createTables(con);
            // TODO
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void generateDbFromORPSelfLogs(String[] logFiles, String dbFile, String messageIdentifier, boolean processFiles, boolean processFeedback) {
        try {
            Connection con = getDBConnection(dbFile);
            createTables(con);
            if (processFiles) {
                try {
                    for (String logFile : logFiles) {
                        // get all the previous logs
                        final FileFilter fileFilter = new WildcardFileFilter(logFile + ".*");
                        final File[] files = new File(".").listFiles(fileFilter);
                        // Sort these files: the newer the file, the later it should be processed
                        Arrays.sort(files, new Comparator<File>() {
                            public int compare(File t, File t1) {
                                return (int) (t.lastModified() - t1.lastModified());
                            }
                        });
                        for (File file : files) {
                            System.out.println("Processing " + file);
                            populateDbFromORPSelfLog(file, con, messageIdentifier);
                        }
                        // get the last log
                        System.out.println("Processing " + logFile);
                        populateDbFromORPSelfLog(new File(logFile), con, messageIdentifier);
                    }
                    System.out.println("Finished populating DB");
                } catch (IOException e) {
                }
            }
            if (processFeedback) {
                processFeedback(con);
            }
            System.out.println("Finished processing feedback");
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void populateDbFromORPSelfLog(File file, Connection con, String messageIdentifier) throws IOException, SQLException {
        ChallengeMessage parser = new ChallengeMessage();
        BufferedReader in = new BufferedReader(new FileReader(file));
        String line = null;
        while ((line = in.readLine()) != null) {
            String[] toks = line.split("\t");
            if (toks[0].equals(messageIdentifier)) {
                String type = toks[1];
                String msg = toks[2];
                Message message = null;
                int messageType = -1;
                if (ChallengeMessage.MSG_UPDATE.equalsIgnoreCase(type)) {
                    message = parser.parseItemUpdate(msg, false);
                    messageType = UPDATE;
                } else if (ChallengeMessage.MSG_REC_REQUEST.equalsIgnoreCase(type)) {
                    message = parser.parseRecommendationRequest(msg, false);
                    messageType = REQUEST;
                } else if (ChallengeMessage.MSG_EVENT_NOTIFICATION.equalsIgnoreCase(type)) {
                    // parse the type of the event
                    final Message item = parser.parseEventNotification(msg, false);
                    final String eventNotificationType = item.getNotificationType();
                    // impression refers to articles read by the user
                    if (ChallengeMessage.MSG_NOTIFICATION_IMPRESSION.equalsIgnoreCase(eventNotificationType)) {
                        message = item;
                        messageType = IMPRESSION;
                    } else if (ChallengeMessage.MSG_NOTIFICATION_IMPRESSION_EMPTY.equalsIgnoreCase(eventNotificationType)) {
                        message = item;
                        messageType = IMPRESSION;
                    } else if (ChallengeMessage.MSG_NOTIFICATION_CLICK.equalsIgnoreCase(eventNotificationType)) {
                        message = item;
                        messageType = FEEDBACK;
                    }
                }
                if (message != null) {
                    // update DB
                    populateDataDb(con, messageType, message, msg);
                }
            }
        }
        in.close();
    }

    private static void populateDataDb(Connection con, int messageType, Message msg, String json) throws SQLException {
        Long timestamp = msg.getTimeStamp();
        Long domain = msg.getDomainID();
        Long item = msg.getItemSourceID() == null ? msg.getItemID() : msg.getItemSourceID();
        Long user = msg.getUserID();
        Long team = msg.getContestTeamID();
        String recs = msg.getRecommendedResults() == null ? "" : msg.getRecommendedResults().toString().replace("[", "").replace("]", "");
        String message = json;
        // timestamp, type, domain, item, user, team, recs, message
        PreparedStatement prep = con.prepareStatement("INSERT INTO " + DB_TABLE_DATA + " VALUES(?,?,?,?,?,?,?,?);");
        prep.setLong(1, timestamp == null ? -1L : timestamp);
        prep.setInt(2, messageType);
        prep.setLong(3, domain == null ? -1L : domain);
        prep.setLong(4, item == null ? -1L : item);
        prep.setLong(5, user == null ? -1L : user);
        prep.setLong(6, team == null ? -1L : team);
        prep.setString(7, recs);
        prep.setString(8, message);
        prep.execute();
        prep.close();
    }

    public static void generateDbFromContestSelfLogs(String dbFile) {
        try {
            Connection con = getDBConnection(dbFile);
            createTables(con);
            // TODO
            con.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void processFeedback(Connection con) throws SQLException {
        String allFeedback = "SELECT * FROM " + DB_TABLE_DATA + " WHERE " + TABLE_DATA_FIELD_TYPE + "=" + FEEDBACK;
        Statement statFeedback = con.createStatement();
        ResultSet resFeedback = statFeedback.executeQuery(allFeedback);
        while (resFeedback.next()) {
            Long timestamp = resFeedback.getLong(TABLE_DATA_FIELD_TIMESTAMP);
            Long domain = resFeedback.getLong(TABLE_DATA_FIELD_DOMAIN);
            Long item = resFeedback.getLong(TABLE_DATA_FIELD_ITEM);
            Long user = resFeedback.getLong(TABLE_DATA_FIELD_USER);
            // there should be only one item in the recs field
            Long clickedItem = Long.parseLong(resFeedback.getString(TABLE_DATA_FIELD_RECS));
            // find the corresponding impression/request
            String correspondingMessage = "SELECT * FROM " + DB_TABLE_DATA + " WHERE " + TABLE_DATA_FIELD_TYPE + "!=" + FEEDBACK
                    + (timestamp == -1L ? "" : " AND " + TABLE_DATA_FIELD_TIMESTAMP + "<" + timestamp)
                    + (domain == -1L ? "" : " AND " + TABLE_DATA_FIELD_DOMAIN + "=" + domain)
                    + (item == -1L ? "" : " AND " + TABLE_DATA_FIELD_ITEM + "=" + item)
                    + (user == -1L ? "" : " AND " + TABLE_DATA_FIELD_USER + "=" + user)
                    + " ORDER BY " + TABLE_DATA_FIELD_TIMESTAMP + " DESC";
            Statement statMessage = con.createStatement();
            ResultSet resMessage = statMessage.executeQuery(correspondingMessage);
            while (resMessage.next()) {
                String recs = resMessage.getString(TABLE_DATA_FIELD_RECS);
                String[] recItems = recs.split(",");
                for (String recItem : recItems) {
                    if (recItem.trim().equals(clickedItem + "")) {
                        Long otherTimestamp = resMessage.getLong(TABLE_DATA_FIELD_TIMESTAMP);
                        PreparedStatement prep = con.prepareStatement("INSERT INTO " + DB_TABLE_MATCHING + " VALUES(?,?,?,?,?);");
                        prep.setLong(1, otherTimestamp == null ? -1L : otherTimestamp);
                        prep.setLong(2, timestamp == null ? -1L : timestamp);
                        prep.setLong(3, domain == null ? -1L : domain);
                        prep.setLong(4, item == null ? -1L : item);
                        prep.setLong(5, user == null ? -1L : user);
                        prep.execute();
                        prep.close();
                        break;
                    }
                }
            }
            resMessage.close();
            statMessage.close();
        }
        resFeedback.close();
        statFeedback.close();
    }

    public static void generateLogFromDb(String dbFile, String logFile) throws FileNotFoundException {
        PrintStream log = new PrintStream(logFile);
        try {
            Connection con = getDBConnection(dbFile);
            String queryAll = "SELECT * FROM " + DB_TABLE_DATA + " ORDER BY " + TABLE_DATA_FIELD_TIMESTAMP + " ASC;";
            Statement statAll = con.createStatement();
            ResultSet resAll = statAll.executeQuery(queryAll);
            while (resAll.next()) {
                int type = resAll.getInt(TABLE_DATA_FIELD_TYPE);
                String msg = resAll.getString(TABLE_DATA_FIELD_MESSAGE);
                if ((type == REQUEST) || (type == IMPRESSION)) {
                    // check the other table
                    Long t = resAll.getLong(TABLE_DATA_FIELD_TIMESTAMP);
                    Long domain = resAll.getLong(TABLE_DATA_FIELD_DOMAIN);
                    Long item = resAll.getLong(TABLE_DATA_FIELD_ITEM);
                    Long user = resAll.getLong(TABLE_DATA_FIELD_USER);
                    String query = "SELECT * FROM " + DB_TABLE_MATCHING + " WHERE "
                            + TABLE_MATCHING_FIELD_REQ_TIMESTAMP + "=" + t
                            + (domain == -1L ? "" : " AND " + TABLE_MATCHING_FIELD_DOMAIN + "=" + domain)
                            + (item == -1L ? "" : " AND " + TABLE_MATCHING_FIELD_ITEM + "=" + item)
                            + (user == -1L ? "" : " AND " + TABLE_MATCHING_FIELD_USER + "=" + user);
                    Statement stat = con.createStatement();
                    ResultSet res = stat.executeQuery(query);
                    boolean notFound = true;
                    while (res.next()) {
                        // get feedback data
                        Long feedbackTime = res.getLong(TABLE_MATCHING_FIELD_FEEDBACK_TIMESTAMP);
                        String queryFeedback = "SELECT * FROM " + DB_TABLE_DATA + " WHERE "
                                + TABLE_DATA_FIELD_TIMESTAMP + "=" + feedbackTime
                                + (domain == -1L ? "" : " AND " + TABLE_DATA_FIELD_DOMAIN + "=" + domain)
                                + (item == -1L ? "" : " AND " + TABLE_DATA_FIELD_ITEM + "=" + item)
                                + (user == -1L ? "" : " AND " + TABLE_DATA_FIELD_USER + "=" + user);
                        Statement statFeedback = con.createStatement();
                        ResultSet resFeedback = statFeedback.executeQuery(queryFeedback);
                        while (resFeedback.next()) {
                            String msgFeedback = resFeedback.getString(TABLE_DATA_FIELD_MESSAGE);
                            log.println(REQUEST + "\t" + msg + "\t" + msgFeedback);
                            notFound = false;
                        }
                        resFeedback.close();
                        statFeedback.close();
                    }
                    res.close();
                    stat.close();
                    if (notFound && (type == IMPRESSION)) {
                        log.println(type + "\t" + msg);
                    }
                } else if ((type == FEEDBACK) || (type == UPDATE)) {
                    // print the row
                    log.println(type + "\t" + msg);
                }
            }
            resAll.close();
            statAll.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        log.close();
    }

    private static Connection getDBConnection(String file) throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC");

            File dbFile = new File(file);
            boolean dbExists = dbFile.exists();
            Connection con = DriverManager.getConnection("jdbc:sqlite:" + file);
            if (dbExists) {
                con.prepareStatement("ATTACH DATABASE '" + file + "' AS 'content'").execute();
            }
            return con;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static void createTables(Connection con) throws SQLException {
        // DB structure:
        //    data: timestamp, message_type, domain, item, user, team, recs, json
        //    feedback_matching: request.timestamp, feedback.timestamp
        Statement stat = con.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS " + DB_TABLE_DATA + "("
                + TABLE_DATA_FIELD_TIMESTAMP + " INTEGER,"
                + TABLE_DATA_FIELD_TYPE + " INTEGER,"
                + TABLE_DATA_FIELD_DOMAIN + " INTEGER,"
                + TABLE_DATA_FIELD_ITEM + " INTEGER,"
                + TABLE_DATA_FIELD_USER + " INTEGER,"
                + TABLE_DATA_FIELD_TEAM + " INTEGER,"
                + TABLE_DATA_FIELD_RECS + " TEXT,"
                + TABLE_DATA_FIELD_MESSAGE + " TEXT"
                //                + ", PRIMARY KEY (" + TABLE_DATA_FIELD_TIMESTAMP + ")"
                + ");");
        stat.close();
        stat = con.createStatement();
        stat.executeUpdate("CREATE TABLE IF NOT EXISTS " + DB_TABLE_MATCHING + "("
                + TABLE_MATCHING_FIELD_REQ_TIMESTAMP + " INTEGER,"
                + TABLE_MATCHING_FIELD_FEEDBACK_TIMESTAMP + " INTEGER,"
                + TABLE_MATCHING_FIELD_DOMAIN + " INTEGER,"
                + TABLE_MATCHING_FIELD_ITEM + " INTEGER,"
                + TABLE_MATCHING_FIELD_USER + " INTEGER"
                + ");");
        stat.close();
    }

    public static void main(String[] args) throws Exception {
//        args = new String[]{"1", "temp_data-small.log", "MESSAGE", "log_test-small.db"};
//        args = new String[]{"2", "log_test-small.db", "log_from_db-small.log"};

//        args = new String[]{"1", "temp_data.log", "MESSAGE", "log_test.db"};
//        args = new String[]{"2", "log_test.db", "log_from_db.log"};

        int step = -1;
        try {
            step = Integer.parseInt(args[0]);
        } catch (Exception e) {
        }

        switch (step) {
            case 1: {
//                generateDbFromORPSelfLogs(new String[]{args[1]}, args[3], args[2], true, true);
//                generateDbFromORPSelfLogs(new String[]{args[1]}, args[3], args[2], true, false);
//                generateDbFromORPSelfLogs(new String[]{args[1]}, args[3], args[2], false, true);
            }
            break;

            case 2: {
                generateLogFromDb(args[1], args[2]);
            }
            break;

            default:
                throw new AssertionError();
        }
    }
}
