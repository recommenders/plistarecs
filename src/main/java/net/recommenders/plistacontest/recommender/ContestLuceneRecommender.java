package net.recommenders.plistacontest.recommender;

import de.dailab.plistacontest.helper.DateHelper;
import de.dailab.plistacontest.helper.FalseItems;
import de.dailab.plistacontest.helper.StatsWriter;
import de.dailab.plistacontest.recommender.ContestItem;
import de.dailab.plistacontest.recommender.ContestRecommender;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * An index for news articles
 *
 * @author alan
 */
public class ContestLuceneRecommender implements ContestRecommender {

    private static final Logger logger = Logger.getLogger(ContestLuceneRecommender.class);
    private final Map<String, IndexWriter> domainWriter = new HashMap<String, IndexWriter>();
    private final Map<String, IndexReader> domainReader = new HashMap<String, IndexReader>();
    private Map<String, HashSet<String>> indexedDocs = new HashMap<String, HashSet<String>>();
    private Map<String, Document> cachedDocs = new HashMap<String, Document>();
    private static final int NUM_THREADS = 5;
    private ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
    public static final Analyzer ANALYZER = new GermanAnalyzer(Version.LUCENE_43);
    //no of days old articles to recommend
    private static final int days = 3;
    final Map<String, Integer> counter = new HashMap<String, Integer>();
    private FalseItems falseItems = new FalseItems();
    private String statFile = "stats_" + DateHelper.getDate() + ".txt";

    static enum StatusField {

        ID("id"),
        DOMAIN("domain"),
        TITLE("title"),
        TEXT("text"),
        TEXTTITLE("texttitle"),
        URL("url"),
        RECOMMENDABLE("recommendable"),
        CREATED("created");
        public final String name;

        StatusField(String s) {
            name = s;
        }
    };
    FieldType textOptions;
    FieldType recOptions;
    FieldType numOptions;

    /**
     * main only used for debugging
     *
     * @param args
     */
    public static void main(String[] args) {
        ContestLuceneRecommender ia = new ContestLuceneRecommender();
        ia.init();
        String queryString = "Genau hinschauen lohnt sich: Das Plakat ist voller Typen, Themen und Thesen - und wer noch genauer hinschaut, entdeckt";
        String domain = "1677";

        QueryParser p = new QueryParser(Version.LUCENE_43, StatusField.TEXTTITLE.name, ANALYZER);
        Query query = null;
        DirectoryReader ir = null;
        IndexSearcher is = null;
        BooleanQuery cq = new BooleanQuery();
        Query rq = new TermQuery(new Term(StatusField.RECOMMENDABLE.name, "true"));
        Filter filter = NumericRangeFilter.newLongRange(StatusField.CREATED.name, ((System.currentTimeMillis() / 1000) - (10 * 86400)), Long.MAX_VALUE, true, true);

        try {
            query = p.parse(QueryParser.escape(queryString));
            cq.add(rq, BooleanClause.Occur.MUST);
            cq.add(query, BooleanClause.Occur.MUST);
            System.out.println("q: " + query.toString());
            System.out.println("rq: " + rq.toString());
            System.out.println("cq: " + cq.toString());
        } catch (org.apache.lucene.queryparser.classic.ParseException e1) {
            logger.error(e1.getMessage());
        }

        try {
            ir = DirectoryReader.open(ia.domainWriter.get(domain), true);
            is = new IndexSearcher(ir);
            TopDocs rs = is.search(cq, filter, 20);
            for (ScoreDoc sd : rs.scoreDocs) {
                Document hit = is.doc(sd.doc);
                System.out.println(sd.doc + " " + hit.getField(StatusField.ID.name) + " " + sd.score);
                System.out.println(hit.get(StatusField.ID.name));
            }
        } catch (IOException e) {
            logger.error(e.toString());
        }
        try {
            TermQuery idQuery = new TermQuery(new Term(StatusField.ID.name, "134791579"));
            TopDocs hits = is.search(idQuery, 10);
            for (ScoreDoc sd : hits.scoreDocs) {
                Document hit = is.doc(sd.doc);
                System.out.println(sd.doc + " " + hit.getField(StatusField.ID.name) + " " + sd.score);
                System.out.println(hit.toString());
            }

        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    public void init() {
        // get all data files for the different domains
        final File dir = new File(".");

        textOptions = new FieldType();
        textOptions.setIndexed(true);
        textOptions.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        textOptions.setStored(true);
        textOptions.setTokenized(true);


        recOptions = new FieldType();
        recOptions.setIndexed(true);
        recOptions.setIndexOptions(FieldInfo.IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        recOptions.setStored(true);
        recOptions.setTokenized(false);

        numOptions = new FieldType();
        numOptions.setIndexed(true);
        numOptions.setStored(true);

        // fill indexes
        FileFilter logFilter = new WildcardFileFilter("contest.log*");
        File[] logs = dir.listFiles(logFilter);
        for (File file : logs) {
            initializeIndexes(file);
        }
        deserialize();
        logger.error("this is not an error: init finished");
    }

    private void initializeIndexes(File file) {
        Scanner scnr = null;
        try {
            scnr = new Scanner(file, "US-ASCII");
        } catch (FileNotFoundException e) {
            logger.error(e.getMessage());
        }
        while (scnr.hasNextLine()) {
            String line = scnr.nextLine();
            line = line.substring(0, line.length() - 24);
            JSONObject jObj = (JSONObject) JSONValue.parse(line);
//            if (jObj.containsKey("msg") && !jObj.get("msg").toString().equals("impression") || !jObj.containsKey("item"))
//                continue;
//            addDocument(jObj);
            if (jObj.containsKey("msg")) {
                if (jObj.get("msg").toString().equals("impression")) {
                    impression(line);
                }
                if (jObj.get("msg").toString().equals("feedback")) {
                    feedback(line);
                }
            }
        }
        pool.shutdown();
        try {
            pool.awaitTermination(10L, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            this.logger.error(e.toString());
        }
        pool = Executors.newFixedThreadPool(NUM_THREADS);
        deserialize();
    }

    private void addDocument(JSONObject _jObj) {
        JSONObject jObj = _jObj;
        String domain = ((JSONObject) jObj.get("domain")).get("id").toString();
        String itemID = ((JSONObject) jObj.get("item")).get("id").toString();
        String title = ((JSONObject) jObj.get("item")).get("title").toString().replaceAll("\u00ad", "");
        String text = ((JSONObject) jObj.get("item")).get("text").toString().replaceAll("\u00ad", "");
        String url = ((JSONObject) jObj.get("item")).get("url").toString();
        String recommendable = ((JSONObject) jObj.get("item")).get("recommendable").toString();
        Long created = Long.parseLong(((JSONObject) jObj.get("item")).get("created").toString());

        Document doc = new Document();
        doc.add(new IntField(StatusField.DOMAIN.name, Integer.parseInt(domain), Field.Store.YES));
        doc.add(new Field(StatusField.ID.name, itemID, numOptions));
        doc.add(new StringField(StatusField.TITLE.name, title, Field.Store.YES));
        doc.add(new StringField(StatusField.TEXT.name, text, Field.Store.YES));
        doc.add(new Field(StatusField.TEXTTITLE.name, title + " " + text, textOptions));
        doc.add(new StringField(StatusField.URL.name, url, Field.Store.YES));
        doc.add(new LongField(StatusField.CREATED.name, created.longValue(), Field.Store.YES));
        doc.add(new Field(StatusField.RECOMMENDABLE.name, recommendable, recOptions));

        // the whole block must be sync'ed, otherwise more than one thread 
        // will try to create the index because they don't find the domain in the map
        synchronized (this) {
            if (!domainWriter.containsKey(domain)) {
                try {
                    Analyzer analyzer = new GermanAnalyzer(Version.LUCENE_43);
                    IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
                    File indexDir = new File("./index_" + domain);
                    Directory index = FSDirectory.open(indexDir);
                    IndexWriter iw = new IndexWriter(index, config);
                    this.domainWriter.put(domain, iw);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
            }
        }
        IndexWriter iw = null;
        synchronized (this) {
            iw = domainWriter.get(domain);
            if (indexedDocs.containsKey(domain) && indexedDocs.get(domain).contains(itemID)) {
                try {
                    iw.updateDocument(new Term(StatusField.ID.name, itemID), doc);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
                return;
            } else if (indexedDocs.containsKey(domain) && !indexedDocs.get(domain).contains(itemID)) {
                indexedDocs.get(domain).add(itemID);
            } else {
                HashSet<String> tmpSet = new HashSet<String>();
                tmpSet.add(itemID);
                indexedDocs.put(domain, tmpSet);
            }
        }
        synchronized (this) {
            try {
                iw.addDocument(doc);
                domainWriter.put(domain, iw);
            } catch (IOException e) {
                logger.error(e.getMessage());
            } catch (NullPointerException e) {
                logger.error(e.getMessage());
            }
        }
    }

    @Override
    public List<ContestItem> recommend(String _client, String _item,
            String _domain, String _description, String _limit) {
        final List<ContestItem> recList = new ArrayList<ContestItem>();

        JSONObject jObj = (JSONObject) JSONValue.parse(_description);
        String itemID = ((JSONObject) jObj.get("item")).get("id").toString();
        String title = ((JSONObject) jObj.get("item")).get("title").toString().replaceAll("\u00ad", "");
        String text = ((JSONObject) jObj.get("item")).get("text").toString().replaceAll("\u00ad", "");

        Query idQuery = new TermQuery(new Term(StatusField.ID.name, itemID));
        QueryParser p = new QueryParser(Version.LUCENE_43, StatusField.TEXTTITLE.name, ANALYZER);
        Query query = null;
        DirectoryReader ir = null;
        IndexSearcher is = null;
        BooleanQuery cq = new BooleanQuery();
        Query rq = new TermQuery(new Term(StatusField.RECOMMENDABLE.name, "true"));
        Filter filter = NumericRangeFilter.newLongRange(StatusField.CREATED.name, ((System.currentTimeMillis() / 1000) - (days * 86400)), Long.MAX_VALUE, true, true);

        try {
            query = p.parse(QueryParser.escape(title + text));
            cq.add(rq, BooleanClause.Occur.MUST);
            cq.add(query, BooleanClause.Occur.MUST);
            cq.add(idQuery, BooleanClause.Occur.MUST_NOT);
            TopDocs rs = null;
            synchronized (this) {
                ir = DirectoryReader.open(domainWriter.get(_domain), true);
                is = new IndexSearcher(ir);
                rs = is.search(cq, filter, Integer.parseInt(_limit));
            }
            for (ScoreDoc sd : rs.scoreDocs) {
                Document hit = is.doc(sd.doc);
                long item = Long.parseLong(hit.get(StatusField.ID.name).toString());
                if (!falseItems.containsItem(item)) {
                    recList.add(new ContestItem(item));
                }
            }
            if (recList.size() < Integer.parseInt(_limit)) {
                cq = new BooleanQuery();
                cq.add(rq, BooleanClause.Occur.MUST);
                cq.add(idQuery, BooleanClause.Occur.MUST_NOT);
                synchronized (this) {
                    ir = DirectoryReader.open(domainWriter.get(_domain), true);
                    is = new IndexSearcher(ir);
                    rs = is.search(cq, filter, Integer.parseInt(_limit) - recList.size());
                }
                for (ScoreDoc sd : rs.scoreDocs) {
                    Document hit = is.doc(sd.doc);
                    recList.add(new ContestItem(Integer.parseInt(hit.get(StatusField.ID.name).toString())));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.toString() + " DOMAIN: " + _domain);
        }
        new StatsWriter(this.statFile, "rec", _item, _limit);
        return recList;
    }

    @Override
    public void impression(String _impression) {

        final JSONObject jObj = (JSONObject) JSONValue.parse(_impression);
        final String domain = ((JSONObject) jObj.get("domain")).get("id").toString();
        final boolean answer = Boolean.valueOf(((JSONObject) jObj.get("config")).get("recommend").toString()) || !jObj.containsKey("item");

        // write info directly in MAHOUT format
//        new Thread(new MahoutWriter(domain + "_m_data_" + DateHelper.getDate() + ".txt", _impression, 3)).start();

        // update impression counter
        if (this.counter.containsKey(domain)) {
            this.counter.put(domain, this.counter.get(domain) + 1);
        } else {
            this.counter.put(domain, 1);
        }
        this.counter.put(domain, 0);
        if (jObj.containsKey("item")) {
            pool.submit(new Thread() {
                public void run() {
                    addDocument(jObj);
                }
            });
        }
    }

    @Override
    public void feedback(String _feedback) {
        try {
            final JSONObject jObj = (JSONObject) JSONValue.parse(_feedback);
            final String client = ((JSONObject) jObj.get("client")).get("id").toString();
            final String item = ((JSONObject) jObj.get("target")).get("id").toString();
            // write info directly in MAHOUT format -> with pref 5
//            new Thread(new MahoutWriter("m_data_" + DateHelper.getDate() + ".txt", client + "," + item, 5)).start();

            // write stats
            new StatsWriter(this.statFile, "feedback", "-1", item);
        } catch (Exception e) {
            logger.error(e.getMessage());
        }
    }

    @Override
    public void error(String _error) {
        logger.error(_error);
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
            final FileOutputStream fileOut = new FileOutputStream("falseitems.ser");
            final ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(_falseItemse);
            out.close();
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    private void deserialize() {
        try {
            final FileInputStream fileIn = new FileInputStream("falseitems.ser");
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

    @Override
    public void setProperties(Properties properties) {
        // TODO Auto-generated method stub
    }
}
