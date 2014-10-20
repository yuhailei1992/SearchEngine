/**
 *  This class implements the Window operator.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;

//import Qryop.DaaTPtr;

public class QryopIlWindow extends QryopIl {
    int adjacency;//window/n
    /**
     *  It is convenient for the constructor to accept a variable number
     *  of arguments. Thus new qryopAnd (arg1, arg2, arg3, ...).
     *  @param q A query argument (a query operator).
     */
    public QryopIlWindow(int n, Qryop... q) {
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
		int num_of_lists = this.daatPtrs.size();
		DaaTPtr ptr[] = new DaaTPtr[num_of_lists];
		for (int i = 0; i < num_of_lists; ++i) {
            ptr[i] = this.daatPtrs.get(i);
            ptr[i].nextDoc = 0;
        }
		
		EVALUATE2:
			for (; ptr[0].nextDoc < ptr[0].invList.df; ++ptr[0].nextDoc) {
				int docid0 = ptr[0].invList.getDocid(ptr[0].nextDoc);
				//System.out.println("docid 0 is " + docid0);
				//find matching documents
				for (int j = 1; j < num_of_lists; ++j) {
					
					while (ptr[j].nextDoc < ptr[j].invList.df && ptr[j].invList.getDocid(ptr[j].nextDoc) < docid0) {
						ptr[j].nextDoc++;
					}
					if (ptr[j].nextDoc == ptr[j].invList.df) continue EVALUATE2;
					if (ptr[j].invList.getDocid(ptr[j].nextDoc) != docid0) {
						continue EVALUATE2;
					}
				}
				//sanity check
				for (int j = 1; j < num_of_lists; ++j) {
					if (ptr[j].invList.getDocid(ptr[j].nextDoc) != docid0) {
						//System.out.println("docid mismatch in QryopIlWindow");
						continue EVALUATE2;
					}
				}
				
				//now, the nextDoc pointers point to the same docid
				//we need to iterate through the position vector now.
				
				InvList.DocPosting to_append_posting = new InvList.DocPosting(docid0);
				int curr_pos[] = new int[num_of_lists];
				int flag = 0;
				EVALUATE3:
				while (true) {
					int temp_max = -1;
					int temp_min = Integer.MAX_VALUE / 2;
					int temp_min_pos = 0;
					for (int j = 0; j < num_of_lists; ++j) {
						if (curr_pos[j] == ptr[j].invList.postings.get(ptr[j].nextDoc).positions.size()) {
							//reached the end
							break EVALUATE3;
						}
						//update the max and min
						int curr = ptr[j].invList.postings.get(ptr[j].nextDoc).positions.get(curr_pos[j]);
						if (curr > temp_max) {
							temp_max = curr;
						}
						if (curr < temp_min) {
							temp_min = curr;
							temp_min_pos = j;
						}
					}
					if (temp_max + 1 - temp_min > this.adjacency) {
						curr_pos[temp_min_pos]++;
						continue EVALUATE3;
					}
					//match
					//advance all these pointers and store the position into result.
					//to_append_posting.tf++;
					
					to_append_posting.positions.add(temp_min);
					flag = 1;
					for (int j = 0; j < num_of_lists; ++j) {
						curr_pos[j]++;
					}
				}
				if (flag == 1) result.invertedList.appendPosting(docid0, to_append_posting.positions);
			}
		freeDaaTPtrs();
		result.invertedList.field = ptr[0].invList.field;
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
