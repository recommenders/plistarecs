package net.recommenders.plista.rec;

import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;
import net.recommenders.plista.utils.ContentDB;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.recommenders.plista.log.DataLogger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Filter;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.NumericRangeFilter;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;

/**
 * An index for news articles
 *
 * @author alan
 */
public class SingleIndexLuceneRecommender implements Recommender {

    private static final DataLogger logger = DataLogger.getLogger(SingleIndexLuceneRecommender.class);
    private  IndexWriter domainWriter;// = new HashMap<Long, IndexWriter>();
    private Map<Long, HashSet<Long>> indexedDocs = new HashMap<Long, HashSet<Long>>();
    private Map<Long, Message> cachedMessages = new HashMap<Long, Message>();
    private static final int NUM_THREADS = 5;
    private ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
    public static final Analyzer ANALYZER = new GermanAnalyzer(Version.LUCENE_43);
    private static final int days = 3;
    ContentDB contentDB = null;// = new ContentDB();

    static enum StatusField {

        ID("id"),
        DOMAIN("domain"),
        TITLE("title"),
        TEXT("text"),
        TEXTTITLE("texttitle"),
        CONTENT("content"),
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
     * Constructor
     * @param db    whether or not to use ContentDB (i.e. whether or not url's are crawled)
     */
    public SingleIndexLuceneRecommender(boolean db){
        if(db)
            contentDB = new ContentDB();
        try {
            Analyzer analyzer = new GermanAnalyzer(Version.LUCENE_43);
            IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_43, analyzer);
            File indexDir = new File("./index");
            Directory index = FSDirectory.open(indexDir);
            domainWriter = new IndexWriter(index, config);
        } catch (IOException e) {
            logger.error(e.getMessage());
        }
    }

    /**
     * Default constructor
     */
    public SingleIndexLuceneRecommender(){
        this(false);
    }

    /**
     * main only used for debugging
     *
     * @param args
     */
    public static void main(String[] args) {
        SingleIndexLuceneRecommender ia = new SingleIndexLuceneRecommender();
        ia.init();
        String queryString = "Genau hinschauen lohnt sich: Das Plakat ist voller Typen, Themen und Thesen - und wer noch genauer hinschaut, entdeckt";
        String domain = "1677";

        QueryParser p = new QueryParser(Version.LUCENE_43, StatusField.TEXTTITLE.name, ANALYZER);
        Query query = null;
        DirectoryReader ir = null;
        IndexSearcher is = null;
        BooleanQuery cq = new BooleanQuery();
        Query rq = new TermQuery(new Term(StatusField.RECOMMENDABLE.name, "true"));
        Query dq = new TermQuery(new Term(StatusField.CREATED.name, domain));

        Filter filter = NumericRangeFilter.newLongRange(StatusField.CREATED.name, ((System.currentTimeMillis() / 1000) - (10 * 86400)), Long.MAX_VALUE, true, true);

        try {
            query = p.parse(QueryParser.escape(queryString));
            cq.add(rq, BooleanClause.Occur.MUST);
            cq.add(query, BooleanClause.Occur.MUST);
            cq.add(dq, BooleanClause.Occur.MUST);
            System.out.println("q: " + query.toString());
            System.out.println("rq: " + rq.toString());
            System.out.println("cq: " + cq.toString());
        } catch (org.apache.lucene.queryparser.classic.ParseException e1) {
            logger.error(e1.getMessage());
        }

        try {
            ir = DirectoryReader.open(ia.domainWriter, true);
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

        if(contentDB != null)
            contentDB.init();


    }

    private void addDocument(final Message message) {
        Long domain = message.getDomainID();
        Long itemID = message.getItemID();
        String title = message.getItemTitle();
        String text = message.getItemText();
        String url = message.getItemURL();
        Boolean recommendable = message.getItemRecommendable();
        Long created = message.getItemCreated();

        if(title == null)
            return;

        if(!cachedMessages.containsKey(itemID))
            cachedMessages.put(itemID, message);

        if(contentDB != null)
            if(!contentDB.itemExists(itemID))
                contentDB.addMessage(message, "");
        Document doc = new Document();
        doc.add(new LongField(StatusField.DOMAIN.name, domain, numOptions));
        doc.add(new Field(StatusField.ID.name, "" + itemID, numOptions));
        doc.add(new StringField(StatusField.TITLE.name, title, Field.Store.YES));
        doc.add(new StringField(StatusField.TEXT.name, text, Field.Store.YES));
        doc.add(new Field(StatusField.TEXTTITLE.name, title + " " + text, textOptions));
        doc.add(new StringField(StatusField.URL.name, url, Field.Store.YES));
        doc.add(new LongField(StatusField.CREATED.name, created.longValue(), Field.Store.YES));
        doc.add(new Field(StatusField.RECOMMENDABLE.name, "" + recommendable, recOptions));

        synchronized (this) {
            if (indexedDocs.containsKey(itemID) && indexedDocs.get(domain).contains(itemID)) {
                try {
                    domainWriter.updateDocument(new Term(StatusField.ID.name, "" + itemID), doc);
                } catch (IOException e) {
                    logger.error(e.getMessage());
                }
                return;
            } else if (indexedDocs.containsKey(domain) && !indexedDocs.get(domain).contains(itemID)) {
                indexedDocs.get(domain).add(itemID);
            } else {
                HashSet<Long> tmpSet = new HashSet<Long>();
                tmpSet.add(itemID);
                indexedDocs.put(domain, tmpSet);
            }
        }
        synchronized (this) {
            try {
                domainWriter.addDocument(doc);
            } catch (IOException e) {
                logger.error(e.getMessage());
            } catch (NullPointerException e) {
                logger.error(e.getMessage());
            }
        }
    }

    @Override
    public List<Long> recommend(Message input, Integer limit) {
        final List<Long> recList = new ArrayList<Long>();

        Long itemID = input.getItemID();
        Long domain = input.getDomainID();

        if(domainWriter == null)
            return null;

        String title = "";
        String text = "";
        Message message = null;
        if(contentDB != null)
            message = contentDB.getMessage(itemID, domain);
        else if(cachedMessages.containsKey(itemID))
            message = cachedMessages.get(itemID);
        else //if (message == null)
            return null;
        System.out.println("\n\n" + message.getItemID() + "\t" + domain);

        title = message.getItemTitle();
        text = message.getItemText();
        System.out.println("\n\n" + title + "  " + text + "\n");

        Query idQuery = new TermQuery(new Term(StatusField.ID.name, itemID.toString()));
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
                ir = DirectoryReader.open(domainWriter, true);
                is = new IndexSearcher(ir);
                rs = is.search(cq, filter, limit);
            }
            for (ScoreDoc sd : rs.scoreDocs) {
                Document hit = is.doc(sd.doc);
                long item = Long.parseLong(hit.get(StatusField.ID.name).toString());
//                if (!falseItems.containsItem(item)) {
                recList.add(item);
//                }
            }
            if (recList.size() < limit) {
                cq = new BooleanQuery();
                cq.add(rq, BooleanClause.Occur.MUST);
                cq.add(idQuery, BooleanClause.Occur.MUST_NOT);
                synchronized (this) {
                    ir = DirectoryReader.open(domainWriter, true);
                    is = new IndexSearcher(ir);
                    rs = is.search(cq, filter, limit - recList.size());
                }
                for (ScoreDoc sd : rs.scoreDocs) {
                    Document hit = is.doc(sd.doc);
                    recList.add(Long.parseLong(hit.get(StatusField.ID.name).toString()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.toString() + " DOMAIN: " + domain);
        }
        return recList;
    }

    @Override
    public void impression(final Message _impression) {
        //update(_impression);
    }

    public void update(final Message _update) {
        pool.submit(new Thread() {
            public void run() {

                addDocument(_update);

            }
        });
    }

    @Override
    public void click(Message _feedback) {
        //update(_feedback);
//        final Long client = _feedback.getUserID();
//        final Long item = _feedback.getItemID();
        // write stats
//            new StatsWriter(this.statFile, "feedback", "-1", item);
    }

    @Override
    public void setProperties(Properties properties) {
        // TODO Auto-generated method stub
    }
}
