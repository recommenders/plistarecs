package net.recommenders.plista.rec;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import net.recommenders.plista.utils.ContentDB;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.de.GermanAnalyzer;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.recommender.Recommender;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.FieldInfo;
import org.apache.lucene.index.IndexReader;
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
public class ContestLuceneRecommender implements Recommender {

    private static final Logger logger = Logger.getLogger(ContestLuceneRecommender.class);
    private final Map<Long, IndexWriter> domainWriter = new HashMap<Long, IndexWriter>();
    private final Map<Long, IndexReader> domainReader = new HashMap<Long, IndexReader>();
    private Map<Long, HashSet<Long>> indexedDocs = new HashMap<Long, HashSet<Long>>();
    private Map<Long, Document> cachedDocs = new HashMap<Long, Document>();
    private static final int NUM_THREADS = 5;
    private ExecutorService pool = Executors.newFixedThreadPool(NUM_THREADS);
    public static final Analyzer ANALYZER = new GermanAnalyzer(Version.LUCENE_43);
    private static final int days = 3;
    ContentDB contendDB = new ContentDB();

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

        contendDB.init();


    }

    private void addDocument(final Message _doc) {
        Long domain = _doc.getDomainID();
        Long itemID = _doc.getItemID();
        String title = _doc.getItemTitle();
        String text = _doc.getItemText();
        String url = _doc.getItemURL();
        Boolean recommendable = _doc.getItemRecommendable();
        Long created = _doc.getItemCreated();

        if(!contendDB.itemExists(itemID))
            contendDB.addMessage(_doc, "");
        Document doc = new Document();
        doc.add(new LongField(StatusField.DOMAIN.name, domain, Field.Store.YES));
        doc.add(new Field(StatusField.ID.name, "" + itemID, numOptions));
        doc.add(new StringField(StatusField.TITLE.name, title, Field.Store.YES));
        doc.add(new StringField(StatusField.TEXT.name, text, Field.Store.YES));
        doc.add(new Field(StatusField.TEXTTITLE.name, title + " " + text, textOptions));
        doc.add(new StringField(StatusField.URL.name, url, Field.Store.YES));
        doc.add(new LongField(StatusField.CREATED.name, created.longValue(), Field.Store.YES));
        doc.add(new Field(StatusField.RECOMMENDABLE.name, "" + recommendable, recOptions));

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
                    iw.updateDocument(new Term(StatusField.ID.name, "" + itemID), doc);
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
    public List<Long> recommend(Message input, Integer limit) {
        final List<Long> recList = new ArrayList<Long>();

        Long itemID = input.getItemID();
        Long domain = input.getDomainID();
        String title = "";
        String text = "";
        Message message = contendDB.getMessage(itemID, domain);
        if (message == null)
            return null;
        else
            System.out.println(message.getItemID());
        ////////////////////////////////////////
        // TODO: now the item information is not included in the recommendation request!!!
        ////////////////////////////////////////
        title = message.getItemTitle();
        text = message.getItemText();

        Query idQuery = new TermQuery(new Term(StatusField.ID.name, "" + itemID));
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
                ir = DirectoryReader.open(domainWriter.get(domain), true);
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
                    ir = DirectoryReader.open(domainWriter.get(domain), true);
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
//        new StatsWriter(this.statFile, "rec", _item, _limit);
        return recList;
    }

    @Override
    public void impression(final Message _impression) {
        update(_impression);
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
        final Long client = _feedback.getUserID();
        final Long item = _feedback.getItemID();
        // write stats
//            new StatsWriter(this.statFile, "feedback", "-1", item);
    }

    @Override
    public void setProperties(Properties properties) {
        // TODO Auto-generated method stub
    }
}
