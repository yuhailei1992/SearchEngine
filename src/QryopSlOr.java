/**
 *  This class implements the OR operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

public class QryopSlOr extends QryopSl {

    /**
     *  It is convenient for the constructor to accept a variable number
     *  of arguments. Thus new qryopOr (arg1, arg2, arg3, ...).
     *  @param q A query argument (a query operator).
     */
    public QryopSlOr(Qryop... q) {
        for (int i = 0; i < q.length; i++)
            this.args.add(q[i]);
    }

    /**
     *  Appends an argument to the list of query operator arguments.  This
     *  simplifies the design of some query parsing architectures.
     *  @param {q} q The query argument (query operator) to append.
     *  @return void
     *  @throws IOException
     */
    public void add (Qryop a) {
        this.args.add(a);
    }

    /**
     *  Evaluates the query operator, including any child operators and
     *  returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluate(RetrievalModel r) throws IOException {
        return (evaluateBoolean (r));
    }

    /**
     *  Evaluates the query operator for boolean retrieval models,
     *  including any child operators and returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluateBoolean (RetrievalModel r) throws IOException {

        allocDaaTPtrs (r);
        QryResult result = new QryResult ();

        int num_of_lists = this.daatPtrs.size();
        //initialize an array of DaaTPtr objects.
        DaaTPtr ptr[] = new DaaTPtr[num_of_lists];
        for (int i = 0; i < num_of_lists; ++i) {
            ptr[i] = this.daatPtrs.get(i);
            ptr[i].nextDoc = 0;
        }

        int curr_min_docid = -1;//stores the current minimum docid
        double docScore = 1.0;//for unranked boolean, default score is 1.0

        EVALUATEDOCUMENTS:
        while (true) {
            int num_finishedlists = 0;//count the number of depleted lists
            int temp_min_docid_pos = 0;//stores the index of the min docid
            int temp_min_docid = -1;//stores the min docid in one loop
            //initialize the temp_min_docid. Since the pointers in different
            //lists will move, we need to search through all the list and find
            //the first valid docid as the initial value of temp_min_docid
            for (int i = 0; i < num_of_lists; ++i) {
                if (ptr[i].nextDoc < ptr[i].scoreList.scores.size()) {
                    temp_min_docid = ptr[i].scoreList.getDocid(ptr[i].nextDoc);
                    break;
                }
            }
            // failed to initialize, meaning that all the lists are depleted.
            // So we break the EVALUATEDOCUMENTS loop
            if (temp_min_docid == -1) {
                break EVALUATEDOCUMENTS;
            }

            //search through the lists, find the min docid
            for (int i = 0; i < num_of_lists; ++i) {
                //if the list is finished, then judge if all the lists are finished
                if (ptr[i].nextDoc >= ptr[i].scoreList.scores.size()) {
                    num_finishedlists++;//count the number of finished lists
                    //if all lists are depleted, we break EVALUATEDOCUMENTS
                    if (num_finishedlists == num_of_lists) {
                        break EVALUATEDOCUMENTS;
                    }
                    continue;
                }

                int curr_docid = ptr[i].scoreList.getDocid(ptr[i].nextDoc);

                //update temp_min_docid
                //and remember the position of the minimum value;
                if (curr_docid <= temp_min_docid) {
                    temp_min_docid = curr_docid;
                    temp_min_docid_pos = i;
                }
            }

            //duplicate entry. Need to find the one with the max score
            //
            //The docids are inserted into the result in order.
            //So, if the temp_min_docid is equal to curr_min_docid, we know that
            //there are duplicates.
            if (temp_min_docid == curr_min_docid) {
                double curr_score = ptr[temp_min_docid_pos].scoreList.
                                    getDocidScore(ptr[temp_min_docid_pos].nextDoc);
                double prev_score = result.docScores.getLast().score;
                if (curr_score > prev_score) {
                    // replace the last entry with one that has same docid and
                    // smaller score.
                    result.docScores.removeLast();
                    result.docScores.add(temp_min_docid, curr_score);
                }
                ptr[temp_min_docid_pos].nextDoc++;
            }

            // if temp_min_docid is greater than curr_min_docid, insert it to
            // the result.
            if (temp_min_docid > curr_min_docid) {
                if (r instanceof RetrievalModelUnrankedBoolean) {
                    result.docScores.add (temp_min_docid, docScore);
                }
                else if (r instanceof RetrievalModelRankedBoolean) {
                    double toInsert = ptr[temp_min_docid_pos].scoreList.
                                      getDocidScore(ptr[temp_min_docid_pos].nextDoc);
                    result.docScores.add(temp_min_docid, toInsert);
                }
                ptr[temp_min_docid_pos].nextDoc++;
                curr_min_docid = temp_min_docid;
            }
        }
        freeDaaTPtrs ();
        return result;
    }

    /**
     *  Calculate the default score for the specified document if it
     *  does not match the query operator.  This score is 0 for many
     *  retrieval models, but not all retrieval models.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @param docid The internal id of the document that needs a default score.
     *  @return The default score.
     */
    public double getDefaultScore (RetrievalModel r, long docid) throws IOException {

        if (r instanceof RetrievalModelUnrankedBoolean)
            return (0.0);

        return 0.0;
    }

    /**
     *  Return a string version of this query operator.
     *  @return The string version of this query operator.
     */
    public String toString() {

        String result = new String ();

        for (int i=0; i<this.args.size(); i++)
            result += this.args.get(i).toString() + " ";

        return ("#OR( " + result + ")");
    }
}
