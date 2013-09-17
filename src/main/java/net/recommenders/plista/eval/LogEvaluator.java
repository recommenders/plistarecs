package net.recommenders.plista.eval;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.recommenders.plista.client.ChallengeMessage;
import net.recommenders.plista.client.Message;
import net.recommenders.plista.rec.ConstrainedRecommender;
import net.recommenders.plista.rec.combination.CombinedLuceneRecentRecommender;
import net.recommenders.plista.rec.LuceneRecommender;
import net.recommenders.plista.rec.PRCategoryBasedRecommender;
import net.recommenders.plista.rec.combination.PRCategoryRecommenderWithUserFilter;
import net.recommenders.plista.rec.PROtherCategoryBasedRecommender;
import net.recommenders.plista.rec.combination.PROtherCategoryRecommenderWithUserFilter;
import net.recommenders.plista.recommender.RecentRecommender;
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class LogEvaluator {

    public static int[] CUT_OFFS = new int[]{1, 2, 3, 4, 5, 10, 20, 50};

    private static enum EVALUATION_TYPE {

        FREE,
        CONSTRAINED;
    }

    private static void evaluate(EVALUATION_TYPE type, File inputFile, Recommender[] recommenders, File[] outputFiles) throws IOException {
        if (recommenders.length != outputFiles.length) {
            throw new IllegalArgumentException();
        }
        // init
        BufferedReader input = new BufferedReader(new FileReader(inputFile));
        PrintStream[] outputs = new PrintStream[outputFiles.length];
        for (int i = 0; i < outputFiles.length; i++) {
            outputs[i] = new PrintStream(outputFiles[i]);
            recommenders[i].init();
        }
        // read the file
        //   assume file format as follows:
        //     type_message \t original_message
        //     if message is a recommendation request, the third column gives feedback (click) information
        String line = null;
        while ((line = input.readLine()) != null) {
            String[] toks = line.split("\t");
            if (toks[0].equals("" + LogGenerator.UPDATE)) {
                final Message msg = new ChallengeMessage().parseItemUpdate(toks[1], false);
                for (int i = 0; i < recommenders.length; i++) {
                    recommenders[i].update(msg);
                }
            } else if (toks[0].equals("" + LogGenerator.IMPRESSION)) {
                final Message msg = new ChallengeMessage().parseEventNotification(toks[1], false);
                for (int i = 0; i < recommenders.length; i++) {
                    recommenders[i].impression(msg);
                }
            } else if (toks[0].equals("" + LogGenerator.REQUEST)) {
                if (toks.length > 2) {
                    final Message req = toks[1].contains("\"type\":\"impression\"") ? new ChallengeMessage().parseEventNotification(toks[1], false) : new ChallengeMessage().parseRecommendationRequest(toks[1], false);
                    final Message feedback = new ChallengeMessage().parseEventNotification(toks[2], false);
                    final Long clickedItem = feedback.getItemID();
                    for (int i = 0; i < recommenders.length; i++) {
                        switch (type) {
                            case FREE: {
                                final Integer limit = req.getNumberOfRequestedResults();
                                int N = CUT_OFFS[CUT_OFFS.length - 1];
                                final List<Long> recommendedItems = recommenders[i].recommend(req, N);
                                boolean clicked = recommendedItems == null ? false : new HashSet(recommendedItems.subList(0, Math.min(limit, recommendedItems.size()))).contains(clickedItem);
                                // print information: timestamp domain clicked?
                                outputs[i].print(req.getTimeStamp() + "\t" + req.getDomainID() + "\t" + (clicked ? "1" : "0") + "\t");
                                for (int n : CUT_OFFS) {
                                    clicked = recommendedItems == null ? false : new HashSet(recommendedItems.subList(0, Math.min(n, recommendedItems.size()))).contains(clickedItem);
                                    outputs[i].print("|" + n + ":" + (clicked ? "1" : "0") + "|");
                                }
                                outputs[i].println();
                            }
                            break;
                            case CONSTRAINED: {
                                // only valid for impressions, where we have the list of items being recommended
                                if (req.getRecommendedResults() != null) {
                                    final Set<Long> itemsToBeRecommended = new HashSet<Long>(req.getRecommendedResults());
                                    final List<Long> recommendedItems = ((ConstrainedRecommender) recommenders[i]).recommend(req, itemsToBeRecommended);
                                    int rank = itemsToBeRecommended.size() + 1;
                                    if (recommendedItems != null) {
                                        for (int j = 0; j < recommendedItems.size(); j++) {
                                            if (clickedItem.equals(recommendedItems.get(j))) {
                                                rank = j;
                                                break;
                                            }
                                        }
                                    }
                                    // print information: timestamp domain rank
                                    outputs[i].println(req.getTimeStamp() + "\t" + req.getDomainID() + "\t" + rank);
                                }
                            }
                            break;
                        }
                    }
                }
            } else if (toks[0].equals("" + LogGenerator.FEEDBACK)) {
                // feedback has to be processed at the given timestamp, not before
                final Message msg = new ChallengeMessage().parseEventNotification(toks[1], false);
                for (int i = 0; i < recommenders.length; i++) {
                    recommenders[i].click(msg);
                }
            }
        }
        // finish
        for (int i = 0; i < outputFiles.length; i++) {
            outputs[i].close();
        }
        input.close();
    }

    public static void doFreeEvaluation(File inputFile, Recommender[] recommenders, File[] outputFiles) throws IOException {
        evaluate(EVALUATION_TYPE.FREE, inputFile, recommenders, outputFiles);
    }

    public static void doConstrainedEvaluation(File inputFile, ConstrainedRecommender[] recommenders, File[] outputFiles) throws IOException {
        evaluate(EVALUATION_TYPE.CONSTRAINED, inputFile, recommenders, outputFiles);
    }

    public static void main(String[] args) throws Exception {
//        args = new String[]{"1", "log_from_db-small.log", "./"};

//        args = new String[]{"1", "log_from_db.log", "./"};

        int step = -1;
        try {
            step = Integer.parseInt(args[0]);
        } catch (Exception e) {
        }

        switch (step) {
            case 1: {
//                doFreeEvaluation(new File(args[1]), new Recommender[]{new RecentRecommender()}, new File[]{new File(args[2] + "recent.free.out")});
//                doFreeEvaluation(new File(args[1]), new Recommender[]{new LuceneRecommender()}, new File[]{new File(args[2] + "lucene.free.out")});
//                doFreeEvaluation(new File(args[1]), new Recommender[]{new LRwRecentRecommender()}, new File[]{new File(args[2] + "lucenerecent.free.out")});
//                doFreeEvaluation(new File(args[1]), new Recommender[]{new PRCategoryBasedRecommender()}, new File[]{new File(args[2] + "prc.free.out")});
//                doFreeEvaluation(new File(args[1]), new Recommender[]{new PROtherCategoryBasedRecommender()}, new File[]{new File(args[2] + "proc.free.out")});
//                doFreeEvaluation(new File(args[1]), new Recommender[]{new PRCategoryRecommenderWithUserFilter()}, new File[]{new File(args[2] + "prcwuf.free.out")});
//                doFreeEvaluation(new File(args[1]), new Recommender[]{new PROtherCategoryRecommenderWithUserFilter()}, new File[]{new File(args[2] + "procwuf.free.out")});
            }
            case 2: {
//                doConstrainedEvaluation(new File(args[1]), new ConstrainedRecommender[]{new RecentRecommender()}, new File[]{new File(args[2] + "recent.cons.out")});
            }
            break;
        }
    }
}
