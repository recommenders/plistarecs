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
import net.recommenders.plista.recommender.Recommender;

/**
 *
 * @author alejandr
 */
public class LogEvaluator {

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
            if (toks[0].equals(LogGenerator.UPDATE)) {
                final Message msg = new ChallengeMessage().parseItemUpdate(toks[1], false);
                for (int i = 0; i < recommenders.length; i++) {
                    recommenders[i].update(msg);
                }
            } else if (toks[0].equals(LogGenerator.IMPRESSION)) {
                final Message msg = new ChallengeMessage().parseEventNotification(toks[1], false);
                for (int i = 0; i < recommenders.length; i++) {
                    recommenders[i].impression(msg);
                }
            } else if (toks[0].equals(LogGenerator.REQUEST)) {
                if (toks.length > 2) {
                    final Message req = new ChallengeMessage().parseRecommendationRequest(toks[1], false);
                    final Message feedback = new ChallengeMessage().parseEventNotification(toks[2], false);
                    final Long clickedItem = feedback.getItemID();
                    for (int i = 0; i < recommenders.length; i++) {
                        switch (type) {
                            case FREE: {
                                final Integer limit = req.getNumberOfRequestedResults();
                                final List<Long> recommendedItems = recommenders[i].recommend(req, limit);
                                boolean clicked = new HashSet(recommendedItems).contains(clickedItem);
                                // print information: timestamp domain clicked?
                                outputs[i].println(req.getTimeStamp() + "\t" + req.getDomainID() + "\t" + (clicked ? "1" : "0"));
                            }
                            break;
                            case CONSTRAINED: {
                                final Set<Long> itemsToBeRecommended = new HashSet<Long>(req.getRecommendedResults());
                                final List<Long> recommendedItems = ((ConstrainedRecommender) recommenders[i]).recommend(req, itemsToBeRecommended);
                                int rank = -1;
                                for (int j = 0; j < recommendedItems.size(); j++) {
                                    if (clickedItem.equals(recommendedItems.get(j))) {
                                        rank = j;
                                        break;
                                    }
                                }
                                // print information: timestamp domain rank
                                outputs[i].println(req.getTimeStamp() + "\t" + req.getDomainID() + "\t" + rank);
                            }
                            break;
                        }
                        recommenders[i].click(feedback);
                    }
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
    
    public static void main(String[] args) {
        // TODO
    }
}
