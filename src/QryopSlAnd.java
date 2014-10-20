/**
 *  This class implements the AND operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

public class QryopSlAnd extends QryopSl {

    /**
     *  It is convenient for the constructor to accept a variable number
     *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
     *  @param q A query argument (a query operator).
     */
    public QryopSlAnd(Qryop... q) {
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
    	if (r instanceof RetrievalModelIndri) return (evaluateIndri(r));
    	else return (evaluateBoolean (r));
    }

    /**
     *  Evaluates the query operator for boolean retrieval models,
     *  including any child operators and returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluateIndri (RetrievalModel r) throws IOException {
    	allocDaaTPtrs (r);
        QryResult result = new QryResult ();
    	int num_of_lists = this.daatPtrs.size();
        //initialize an array of DaaTPtr objects.
        DaaTPtr ptr[] = new DaaTPtr[num_of_lists];
        for (int i = 0; i < num_of_lists; ++i) {
            ptr[i] = this.daatPtrs.get(i);
            ptr[i].nextDoc = 0;
        }

        EVALUATEDOCUMENTS:
        while (true) {
            int num_finishedlists = 0;//count the number of depleted lists
            int temp_min_docid = -1;//stores the min docid in one loop
            /* 
             * initialize the temp_min_docid. Since the pointers in different
             * lists will move, we need to search through all the list and find
             * the first valid docid as the initial value of temp_min_docid
             */
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
                if (curr_docid < temp_min_docid) {
                    temp_min_docid = curr_docid;
                }
            }
            //now we have the min_docid
            double temp_scores[] = new double[num_of_lists];
            double score = 1.0;
            for (int i = 0; i < num_of_lists; ++i) {
            	if (ptr[i].nextDoc < ptr[i].scoreList.scores.size()
            			&& ptr[i].scoreList.getDocid(ptr[i].nextDoc) == temp_min_docid) {
            		temp_scores[i] = ptr[i].scoreList.getDocidScore(ptr[i].nextDoc);
            		ptr[i].nextDoc++;
            	}
            	else {
            		temp_scores[i] = ((QryopSl)this.args.get(i)).getDefaultScore(r, temp_min_docid);
            	}
            	score *= Math.pow(temp_scores[i], (1.0/num_of_lists));
            }
            result.docScores.add(temp_min_docid, score);
        }
        freeDaaTPtrs ();
        return result;
    }
    
    public QryResult evaluateBoolean (RetrievalModel r) throws IOException {

        //  Initialization

        allocDaaTPtrs (r);
        QryResult result = new QryResult ();

        //  Sort the arguments so that the shortest lists are first.  This
        //  improves the efficiency of exact-match AND without changing
        //  the result.

        for (int i=0; i<(this.daatPtrs.size()-1); i++) {
            for (int j=i+1; j<this.daatPtrs.size(); j++) {
                if (this.daatPtrs.get(i).scoreList.scores.size() >
                        this.daatPtrs.get(j).scoreList.scores.size()) {
                    ScoreList tmpScoreList = this.daatPtrs.get(i).scoreList;
                    this.daatPtrs.get(i).scoreList = this.daatPtrs.get(j).scoreList;
                    this.daatPtrs.get(j).scoreList = tmpScoreList;
                }
            }
        }

        //  Exact-match AND requires that ALL scoreLists contain a
        //  document id.  Use the first (shortest) list to control the
        //  search for matches.

        //  Named loops are a little ugly.  However, they make it easy
        //  to terminate an outer loop from within an inner loop.
        //  Otherwise it is necessary to use flags, which is also ugly.

        DaaTPtr ptr0 = this.daatPtrs.get(0);

        EVALUATEDOCUMENTS:
        for ( ; ptr0.nextDoc < ptr0.scoreList.scores.size(); ptr0.nextDoc ++) {

            int ptr0Docid = ptr0.scoreList.getDocid (ptr0.nextDoc);
            double docScore = 1.0;

            //  Do the other query arguments have the ptr0Docid?

            for (int j=1; j<this.daatPtrs.size(); j++) {

                DaaTPtr ptrj = this.daatPtrs.get(j);

                while (true) {
                    if (ptrj.nextDoc >= ptrj.scoreList.scores.size())
                        break EVALUATEDOCUMENTS;		// No more docs can match
                    else if (ptrj.scoreList.getDocid (ptrj.nextDoc) > ptr0Docid)
                        continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
                    else if (ptrj.scoreList.getDocid (ptrj.nextDoc) < ptr0Docid)
                        ptrj.nextDoc ++;			// Not yet at the right doc.
                    else
                        break;				// ptrj matches ptr0Docid
                }
            }
            //  The ptr0Docid matched all query arguments, so save it.

            if (r instanceof RetrievalModelUnrankedBoolean) {
                result.docScores.add (ptr0Docid, docScore);
            }
            else if (r instanceof RetrievalModelRankedBoolean) {
                double minscore = ptr0.scoreList.getDocidScore(ptr0.nextDoc);
                for (int i = 0; i < this.daatPtrs.size(); ++i) {
                    DaaTPtr ptri = this.daatPtrs.get(i);
                    double curr_score = ptri.scoreList.getDocidScore(ptri.nextDoc);
                    if (curr_score < minscore) minscore = curr_score;
                }
                result.docScores.add (ptr0Docid, minscore);
            }
        }
        freeDaaTPtrs ();
        return result;
    }

    /*
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
    	if (r instanceof RetrievalModelIndri) {
    		double score = 1.0;
    		int num_of_lists = this.args.size();
    		for (int i = 0; i < num_of_lists; ++i) {
    			score *= ((QryopSl)this.args.get(i)).getDefaultScore(r, docid);
    		}
    		//System.out.println(Math.pow(score, (1.0/(double)num_of_lists)));
    		return Math.pow(score, (1.0/(double)num_of_lists));
    	}
    	return 0.0;
    }

    /*
     *  Return a string version of this query operator.
     *  @return The string version of this query operator.
     */
    public String toString() {

        String result = new String ();

        for (int i=0; i<this.args.size(); i++)
            result += this.args.get(i).toString() + " ";

        return ("#AND( " + result + ")");
    }
}
