/**
 *  This class implements the NEAR operator for all retrieval models.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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
        //  Initialization
        allocDaaTPtrs (r);
        QryResult result = new QryResult ();
        result.invertedList.field = new String (this.daatPtrs.get(0).invList.field);

        //  Exact-match NEAR requires that ALL scoreLists contain a
        //  document id.  Use the first (shortest) list to control the
        //  search for matches.

        //  Named loops are a little ugly.  However, they make it easy
        //  to terminate an outer loop from within an inner loop.
        //  Otherwise it is necessary to use flags, which is also ugly.

        DaaTPtr ptr0 = this.daatPtrs.get(0);

        int num_of_lists = this.daatPtrs.size();
        DaaTPtr ptr[] = new DaaTPtr[num_of_lists];
        for (int i = 0; i < num_of_lists; ++i) {
            ptr[i] = this.daatPtrs.get(i);
            ptr[i].nextDoc = 0;

        }

        EVALUATEDOCUMENTS:

        for ( ; ptr0.nextDoc < ptr0.invList.postings.size(); ptr0.nextDoc ++) {

            int ptr0Docid = ptr0.invList.getDocid(ptr0.nextDoc);

            //  Do the other query arguments have the ptr0Docid?
            for (int j=1; j<this.daatPtrs.size(); j++) {
                DaaTPtr ptrj = this.daatPtrs.get(j);
                while (true) {
                    if (ptrj.nextDoc >= ptrj.invList.postings.size())
                        break EVALUATEDOCUMENTS;		// No more docs can match
                    else if (ptrj.invList.getDocid (ptrj.nextDoc) > ptr0Docid)
                        continue EVALUATEDOCUMENTS;	// The ptr0docid can't match.
                    else if (ptrj.invList.getDocid (ptrj.nextDoc) < ptr0Docid)
                        ptrj.nextDoc ++;			// Not yet at the right doc.
                    else
                        break;				// ptrj matches ptr0Docid
                }
            }

            // cnt counts the number of passed compares. For example, if there are
            // 3 list, in order to satisfy the requirement of NEAR, we need to
            // compare list 0 and 1, 1 and 2. So there are two tests. After
            // passing 2 compares, we regard it as satisfying the requirement of
            // NEAR

            //the score is not the official score. It just counts the number of
            //matches in a document
            int score = 0;

            List<Integer> pos = new ArrayList<Integer>();
            COMPARE:
            for (int i = 0; i < ptr0.invList.postings.get(ptr0.nextDoc).positions.size(); ++i) {
                int cnt = 0;
                int last_pos = i;
                TRAVERSE:
                for (int j = 0; j < this.daatPtrs.size()-1; ++j) {
                    for (int n = 0; n < ptr[j+1].invList.postings.get(ptr[j+1].nextDoc).positions.size(); ++n) {
                        int m = last_pos;
                        int diff = ptr[j+1].invList.postings.get(ptr[j+1].nextDoc).positions.get(n)
                                   - ptr[j].invList.postings.get(ptr[j].nextDoc).positions.get(m);
                        if (diff > 0 && diff <= this.adjacency) {
                            ++cnt;
                            //if we have compared enough lists, we know this docid
                            //contains a match
                            if(cnt == this.daatPtrs.size()-1) {
                                //score is the count of occurances
                                score++;
                                pos.add(i);
                                //continue to find more matches in this document
                                continue COMPARE;
                            }
                            last_pos = n;//stores the position of last term
                            continue TRAVERSE;//continue to check ptr[j+1] and ptr[j+2]
                        }
                    }
                    break;
                }
            }
            if (score > 0) {
                result.invertedList.appendPosting(ptr0Docid, pos);
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
