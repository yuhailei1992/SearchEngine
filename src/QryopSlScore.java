/**
 *  This class implements the SCORE operator for all retrieval models.
 *  The single argument to a score operator is a query operator that
 *  produces an inverted list.  The SCORE operator uses this
 *  information to produce a score list that contains document ids and
 *  scores.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

public class QryopSlScore extends QryopSl {
	public String field;
	public int ctf;
    /**
     *  Construct a new SCORE operator.  The SCORE operator accepts just
     *  one argument.
     *  @param q The query operator argument.
     *  @return @link{QryopSlScore}
     */
    public QryopSlScore(Qryop q) {
        this.args.add(q);
    }

    /**
     *  Construct a new SCORE operator.  Allow a SCORE operator to be
     *  created with no arguments.  This simplifies the design of some
     *  query parsing architectures.
     *  @return @link{QryopSlScore}
     */
    public QryopSlScore() {
    }

    /**
     *  Appends an argument to the list of query operator arguments.  This
     *  simplifies the design of some query parsing architectures.
     *  @param q The query argument to append.
     */
    public void add (Qryop a) {
        this.args.add(a);
    }

    /**
     *  Evaluate the query operator.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @return The result of evaluating the query.
     *  @throws IOException
     */
    public QryResult evaluate(RetrievalModel r) throws IOException {
        if (r instanceof RetrievalModelUnrankedBoolean || 
        		r instanceof RetrievalModelRankedBoolean) {
        	return (evaluateBoolean(r));
        }
        else if (r instanceof RetrievalModelBM25) {
        	return evaluateBM25(r);
        }
        else if (r instanceof RetrievalModelIndri) {
        	return evaluateIndri(r);
        }
    	return (evaluateBoolean (r));
    }
    
    public QryResult evaluateIndri(RetrievalModel r) throws IOException {
    	
    	QryResult result = args.get(0).evaluate(r);
    	
    	// get constants from invlist and index
    	String invfield = result.invertedList.field;
        int df = result.invertedList.df;
        int ctf = result.invertedList.ctf;
        long length_C = QryEval.READER.getSumTotalTermFreq(invfield);
        // smoothing
        double p_MLE = ((double)ctf) / ((double)length_C);
        
        // compute scores
        for (int j = 0; j < df; ++j) {
        	
            int docid = result.invertedList.postings.get(j).docid;
            int tf = result.invertedList.postings.get(j).tf;
            long length_d = QryEval.dls.getDocLength(invfield, docid);
            //calculate scores
            double score = ((RetrievalModelIndri)r).Indri_lambda * ((double)tf + ((RetrievalModelIndri)r).Indri_mu * p_MLE) /
            		((double)length_d + ((RetrievalModelIndri)r).Indri_mu) + (1 - ((RetrievalModelIndri)r).Indri_lambda) * p_MLE;
            
            result.docScores.add(docid, score);
        }
        
        //prepare the result, store the invlist's ctf and field in score object
        this.ctf = ctf;//haileiy1019
        this.field = invfield;
        return result;
    }
    
    public QryResult evaluateBM25(RetrievalModel r) throws IOException {
    	
    	QryResult result = args.get(0).evaluate(r);
    	System.out.println("parameters " + ((RetrievalModelBM25)r).BM25_k_1 + '\t' + 
    			((RetrievalModelBM25)r).BM25_b + '\t' + ((RetrievalModelBM25)r).BM25_k_3);
    	//get the contants from index.
        String invfield = result.invertedList.field;
        int N = QryEval.READER.numDocs();
        double avg_doclen = (double)QryEval.READER.getSumTotalTermFreq(invfield) / QryEval.READER.getDocCount(invfield);//haileiy 1011
        int df = result.invertedList.df;

        //calculate the weights
        double RSJ_weight = Math.log((double)(N - df + 0.5) / (double)(df + 0.5));
        
        //calculate scores
        for (int j = 0; j < df; ++j) {//calculates scores for every document
            int docid = result.invertedList.getDocid(j);
        	int tf = result.invertedList.getTf(j);

        	long doclen = QryEval.dls.getDocLength(invfield, docid);
            
            //calculate the tf_weight
            double tf_weight = tf / ((double)tf + ((RetrievalModelBM25)r).BM25_k_1 * ((1 - ((RetrievalModelBM25)r).BM25_b) + 
            		((RetrievalModelBM25)r).BM25_b * doclen / avg_doclen));
            
            double score = RSJ_weight * tf_weight;//didn't use user_weight
            result.docScores.add(docid, score);
        }
        return result;
    }
    

    /**
      *  Evaluate the query operator for boolean retrieval models.
      *  @param r A retrieval model that controls how the operator behaves.
      *  @return The result of evaluating the query.
      *  @throws IOException
      */
    public QryResult evaluateBoolean(RetrievalModel r) throws IOException {
        // Evaluate the query argument.

        QryResult result = args.get(0).evaluate(r);

        // Each pass of the loop computes a score for one document. Note:
        // If the evaluate operation above returned a score list (which is
        // very possible), this loop gets skipped.
        for (int i = 0; i < result.invertedList.df; i++) {

            // DIFFERENT RETRIEVAL MODELS IMPLEMENT THIS DIFFERENTLY.
            if (r instanceof RetrievalModelUnrankedBoolean) {
                result.docScores.add(result.invertedList.postings.get(i).docid,
                                     (float) 1.0);
            }
            else if (r instanceof RetrievalModelRankedBoolean) {
                result.docScores.add(result.invertedList.postings.get(i).docid,
                                     (double)result.invertedList.postings.get(i).tf);
            }
        }

        // The SCORE operator should not return a populated inverted list.
        // If there is one, replace it with an empty inverted list.

        if (result.invertedList.df > 0)
            result.invertedList = new InvList();

        return result;
    }

    /*
     *  Calculate the default score for a document that does not match
     *  the query argument.  This score is 0 for many retrieval models,
     *  but not all retrieval models.
     *  @param r A retrieval model that controls how the operator behaves.
     *  @param docid The internal id of the document that needs a default score.
     *  @return The default score.
     */
    public double getDefaultScore (RetrievalModel r, long docid) throws IOException {
        if (r instanceof RetrievalModelIndri){
        	long length_d = QryEval.dls.getDocLength(this.field, (int)docid);//should be long? haileiy
        	long length_C = QryEval.READER.getSumTotalTermFreq(this.field);
            double p_MLE = ((double)this.ctf) / ((double)length_C);
        	double score = ((RetrievalModelIndri)r).Indri_lambda * (((RetrievalModelIndri)r).Indri_mu * p_MLE) / 
        			(length_d + ((RetrievalModelIndri)r).Indri_mu) + (1 - ((RetrievalModelIndri)r).Indri_lambda) * p_MLE;
        	return score;
        }
        return 0.0;
    }

    /**
     *  Return a string version of this query operator.
     *  @return The string version of this query operator.
     */
    public String toString() {
        String result = new String ();
        for (Iterator<Qryop> i = this.args.iterator(); i.hasNext(); )
            result += (i.next().toString() + " ");
        return ("#SCORE( " + result + ")");
    }
}
