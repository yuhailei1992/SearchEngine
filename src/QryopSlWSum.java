/**
 *  This class implements the WSUM operator.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.ArrayList;

public class QryopSlWSum extends QryopSl {
	ArrayList<Double> weight = new ArrayList<Double>();

    /**
     *  It is convenient for the constructor to accept a variable number
     *  of arguments. Thus new qryopSum (arg1, arg2, arg3, ...).
     *  @param q A query argument (a query operator).
     */
    public QryopSlWSum(Qryop... q) {
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
        return (evaluateIndri (r));
    }

    /**
     *  Evaluates the query operator for boolean retrieval models,
     *  including any child operators and returns the result.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluateIndri (RetrievalModel r) throws IOException {
    	//pre processing. Remove weights for stopwords
    	/*String nums = "0123456789";
    	for (int i = 0; i < this.args.size()-1; ++i) {
    		//if (this.args.get(0) instanceof QryopIl) {
	    		if (nums.contains(((QryopIlTerm)this.args.get(i)).field) &&
	    				nums.contains(((QryopIlTerm)this.args.get(i+1)).field)) {
	    			System.out.println("stopword!");
	    			this.args.remove(i);
	    		}
    		//}
    	}
    	
    	ArrayList<Qryop> bakargs = new ArrayList<Qryop>();
    	for (int i = 0; i < this.args.size(); ++i) {
    		if (i % 2 == 0) {//weights
    			String temp = ((QryopIlTerm)this.args.get(i)).term + "." +
        				((QryopIlTerm)this.args.get(i)).field;
    			System.out.println(temp);
    			this.weight.add(Double.parseDouble(temp));
    		}
    		else {
    			bakargs.add(this.args.get(i));
    		}
    	}
    	this.args = bakargs;
    	*/
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
            float score = 0.0f;
            for (int i = 0; i < num_of_lists; ++i) {
            	if (ptr[i].nextDoc < ptr[i].scoreList.scores.size()
            			&& ptr[i].scoreList.getDocid(ptr[i].nextDoc) == temp_min_docid) {
            		temp_scores[i] = ptr[i].scoreList.getDocidScore(ptr[i].nextDoc);
            		ptr[i].nextDoc++;
            	}
            	else {
            		temp_scores[i] = ((QryopSl)this.args.get(i)).getDefaultScore(r, temp_min_docid);
            	}
            	score += this.weight.get(i) * temp_scores[i];
            }
            result.docScores.add(temp_min_docid, score);
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
    	if (r instanceof RetrievalModelIndri) {
    		double score = 1.0;
    		int num_of_lists = this.args.size();
    		for (int i = 0; i < num_of_lists; ++i) {
    			score *= ((QryopSl)this.args.get(i)).getDefaultScore(r, docid);
    		}
    		return Math.pow(score, (1.0/(double)num_of_lists));
    	}
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

        return ("#sum( " + result + ")");
    }
}
