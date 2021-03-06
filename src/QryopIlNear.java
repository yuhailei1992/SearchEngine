/**
 *  This class implements the NEAR operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

public class QryopIlNear extends QryopIl {
    int adjacency;//near/n
    /**
     *  It is convenient for the constructor to accept a variable number
     *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
     *  @param q A query argument (a query operator).
     */
    public QryopIlNear(int n, Qryop... q) {
        for (int i = 0; i < q.length; i++)
            this.args.add(q[i]);
        this.adjacency = n;
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
        // Initialization
		allocDaaTPtrs (r);
		
		
		QryResult result = new QryResult();
		if(this.daatPtrs.size()==0){
			return result;
		}
		else if(this.daatPtrs.size()==1){
			result.invertedList = this.daatPtrs.get(0).invList;
			return result;
		}
		/*
		 * Store daatPtrs.get(0).invList in tempList
		 */
		InvList tempList = this.daatPtrs.get(0).invList;
		/* 
		 * There are three levels of loops
		 * The first level will traverse all the invlists, comparing adjacent
		 * invlists and get a new invlist
		 * The second level will traverse two adjacent invlists and find docids
		 * that match. 
		 * When the docid match, there is a third level loop, which compares the 
		 * positions and check if the position meet the demand of #NEAR/n
		 * 
		 */
		for (int i = 1; i < args.size(); ++i) {
			InvList currList = this.daatPtrs.get(i).invList;
			InvList resList = new InvList();
			//result.invertedList.ctf = currList.ctf;
			for (int temp_doc = 0, curr_doc = 0; temp_doc < tempList.df && curr_doc < currList.df; ){
				int temp_docid = tempList.postings.get(temp_doc).docid;
				int curr_docid = currList.postings.get(curr_doc).docid;
				// There is a match in docids
				if (temp_docid == curr_docid) {
					int ifAppend = 0;
					InvList.DocPosting to_append_posting = new InvList.DocPosting(tempList.postings.get(temp_doc).docid);
					int temp_pos = 0;
					int curr_pos = 0;
					
					while (temp_pos < tempList.postings.get(temp_doc).tf && curr_pos < currList.postings.get(curr_doc).tf) {
						// similar to the second level loop. When the positions
						// don't match the requirement of #near, we move pointers
						if (tempList.postings.get(temp_doc).positions.get(temp_pos) + this.adjacency < currList.postings.get(curr_doc).positions.get(curr_pos)) {
							++temp_pos;
						}
						else if (tempList.postings.get(temp_doc).positions.get(temp_pos) > currList.postings.get(curr_doc).positions.get(curr_pos)) {
							++curr_pos;
						}
						/*
						 * If the positions match, append the pos in to_append_posting
						 * , which will be appended in resList
						 */
						else {
							int pos = currList.postings.get(curr_doc).positions.get(curr_pos);
							to_append_posting.positions.add(pos);
							// flag. If the ifAppend is 1, we know the positions is
							// valid and should be appended. A little ugly. 
							ifAppend = 1;
							++curr_pos;
							++temp_pos;
						}
					}
					++temp_doc;
					++curr_doc;
					/*
					 * After one loop, append the posting in resList, which, after
					 * traversing two positions, will be stored in tempList. 
					 */
					if (ifAppend == 1) {
						resList.appendPosting(curr_docid, to_append_posting.positions);
					}
				}
				// if the docids don't match, move pointers. 
				else if (temp_docid > curr_docid) {
					++curr_doc;
				}
				else if (temp_docid < curr_docid) {
					++temp_doc;
				}
			}
			/*
			 * Store the last resList in tempList, which is to be used in the 
			 * next comparison
			 */
			tempList = resList;//store the last resList in tempList, to be used 
			//in the next comparison
			//store the result
			if (i == args.size()-1) {
				result.invertedList = resList;
				result.invertedList.field = currList.field;
			}
		}
		freeDaaTPtrs();
		System.out.println(result.invertedList.ctf);
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

        return ("#NEAR/" + this.adjacency + result + ")");
    }
}
