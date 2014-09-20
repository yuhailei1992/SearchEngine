/**
 *  QryEval illustrates the architecture for the portion of a search
 *  engine that evaluates queries.  It is a template for class
 *  homework assignments, so it emphasizes simplicity over efficiency.
 *  It implements an unranked Boolean retrieval model, however it is
 *  easily extended to other retrieval models.  For more information,
 *  see the ReadMe.txt file.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.*;
import java.util.*;

import org.apache.lucene.analysis.Analyzer.TokenStreamComponents;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

public class QryEval {

    static String usage = "Usage:  java " + System.getProperty("sun.java.command")
                          + " paramFile\n\n";

    //  The index file reader is accessible via a global variable. This
    //  isn't great programming style, but the alternative is for every
    //  query operator to store or pass this value, which creates its
    //  own headaches.

    public static IndexReader READER;

    //  Create and configure an English analyzer that will be used for
    //  query parsing.

    public static EnglishAnalyzerConfigurable analyzer =
        new EnglishAnalyzerConfigurable (Version.LUCENE_43);
    static {
        analyzer.setLowercase(true);
        analyzer.setStopwordRemoval(true);
        analyzer.setStemmer(EnglishAnalyzerConfigurable.StemmerType.KSTEM);
    }

    /**
     *  @param args The only argument is the path to the parameter file.
     *  @throws Exception
     */
    public static void main(String[] args) throws Exception {

        // must supply parameter file
        if (args.length < 1) {
            System.err.println(usage);
            System.exit(1);
        }

        // read in the parameter file; one parameter per line in format of key=value
        Map<String, String> params = new HashMap<String, String>();
        Scanner scan = new Scanner(new File(args[0]));
        String line = null;
        do {
            line = scan.nextLine();
            String[] pair = line.split("=");
            params.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());
        scan.close();

        // parameters required for this example to run
        if (!params.containsKey("indexPath")) {
            System.err.println("Error: Parameters were missing.");
            System.exit(1);
        }

        // open the index
        READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

        if (READER == null) {
            System.err.println(usage);
            System.exit(1);
        }

        DocLengthStore s = new DocLengthStore(READER);

        //initialize the appropriate retrieval model
        RetrievalModel model = new RetrievalModelRankedBoolean();
        if (params.get("retrievalAlgorithm").equals("UnrankedBoolean")) {
            model = new RetrievalModelUnrankedBoolean();
        }
        else if (params.get("retrievalAlgorithm").equals("RankedBoolean")) {
            model = new RetrievalModelRankedBoolean();
        }
        else {
            System.err.println("Error: Unknown retrieval model.");
            System.exit(1);
        }

        /*
         *  The code below is an unorganized set of examples that show
         *  you different ways of accessing the index.  Some of these
         *  are only useful in HW2 or HW3.
         */

        // Lookup the document length of the body field of doc 0.
        // How to use the term vector.
        /**
         *  The index is open. Start evaluating queries. The examples
         *  below show query trees for two simple queries.  These are
         *  meant to illustrate how query nodes are created and connected.
         *  However your software will not create queries like this.  Your
         *  software will use a query parser.  See parseQuery.
         *
         *  The general pattern is to tokenize the query term (so that it
         *  gets converted to lowercase, stopped, stemmed, etc), create a
         *  Term node to fetch the inverted list, create a Score node to
         *  convert an inverted list to a score list, evaluate the query,
         *  and print results.
         *
         *  Modify the software so that you read a query from a file,
         *  parse it, and form the query tree automatically.
         */

        //  A different way to create the previous query.  This doesn't use
        //  a stack, but it may make it easier to see how you would parse a
        //  query with a stack-based architecture.

        //  Using the example query parser.  Notice that this does no
        //  lexical processing of query terms.  Add that to the query
        //  parser.

        /**
         * Used isHardCodeTest as a flag to indicate if this test should read
         * queries from file or just use the hardcoded query. Very useful for
         * testing.
         */
        int isHardCodeTest = 0;
        if (isHardCodeTest == 0) {
            Scanner queryScanner = new Scanner(new File(params.get("queryFilePath")));
            String queryLine = null;

            BufferedWriter writer = null;
            writer = new BufferedWriter(new FileWriter(new File(
                                            params.get("trecEvalOutputPath"))));

            do {
                queryLine = queryScanner.nextLine();
                //System.out.println("query is " + queryLine);
                String[] parts = queryLine.split(":");
                int queryID = 0;
                //if the query line contains a queryID
                if (parts.length == 1) {
                    queryLine = parts[0];
                }
                //the query line doesn't contain a queryID
                else {
                    queryID = Integer.parseInt(parts[0]);
                    queryLine = parts[1];
                }
                Qryop qTree;
                qTree = parseQuery (queryLine);
                QryResult result = qTree.evaluate (model);
                if (result != null) {
                    // score is the first ranking keyword, and externalId is
                    // the second.
                    rankByExternalId(result);
                    rankByScore(result);
                }
                printResults (queryLine, result, queryID);
                writeResults (writer, queryLine, result, queryID);
            } while (queryScanner.hasNext());
            try {
                writer.close();
                queryScanner.close();
            } catch (Exception e) {
            }
        }
        else {
            //hardcoded tests are only used for testing a single query.
            System.out.println("Hardcoded test");
            Qryop qTree;
            String query = new String ("#or(apple banana)");
            qTree = parseQuery (query);
            QryResult result = qTree.evaluate (model);
            rankByExternalId(result);
            rankByScore(result);
            printResults (query, result, 1);
        }
        /*
         *  Create the trec_eval output.  Your code should write to the
         *  file specified in the parameter file, and it should write the
         *  results that you retrieved above.  This code just allows the
         *  testing infrastructure to work on QryEval.
         */

        // Later HW assignments will use more RAM, so you want to be aware
        // of how much memory your program uses.

        printMemoryUsage(false);
    }

    /**
     * rank the results by externalId.
     * @param result
     * @throws IOException
     */
    static void rankByExternalId (QryResult result) throws IOException {
        //first, set the externalId
        for (ScoreList.ScoreListEntry p : result.docScores.scores) {
            p.setExternalID(getExternalDocid(p.docid));
        }
        //then,  rank the results by externalId
        result.docScores.sortScoreListByExternalId();
    }

    /**
     * rank by score
     * @param result
     * @throws IOException
     */
    static void rankByScore (QryResult result) throws IOException {
        result.docScores.sortScoreListByScore();
    }

    /**
     *  Write an error message and exit.  This can be done in other
     *  ways, but I wanted something that takes just one statement so
     *  that it is easy to insert checks without cluttering the code.
     *  @param message The error message to write before exiting.
     *  @return void
     */
    static void fatalError (String message) {
        System.err.println (message);
        System.exit(1);
    }

    /**
     *  Get the external document id for a document specified by an
     *  internal document id. If the internal id doesn't exists, returns null.
     *
     * @param iid The internal document id of the document.
     * @throws IOException
     */
    static String getExternalDocid (int iid) throws IOException {
        Document d = QryEval.READER.document (iid);
        String eid = d.get ("externalId");
        return eid;
    }

    /**
     *  Finds the internal document id for a document specified by its
     *  external id, e.g. clueweb09-enwp00-88-09710.  If no such
     *  document exists, it throws an exception.
     *
     * @param externalId The external document id of a document.s
     * @return An internal doc id suitable for finding document vectors etc.
     * @throws Exception
     */
    static int getInternalDocid (String externalId) throws Exception {
        Query q = new TermQuery(new Term("externalId", externalId));

        IndexSearcher searcher = new IndexSearcher(QryEval.READER);
        TopScoreDocCollector collector = TopScoreDocCollector.create(1,false);
        searcher.search(q, collector);
        ScoreDoc[] hits = collector.topDocs().scoreDocs;

        if (hits.length < 1) {
            throw new Exception("External id not found.");
        } else {
            return hits[0].doc;
        }
    }

    /**
     * parseQuery converts a query string into a query tree.
     *
     * @param qString
     *          A string containing a query.
     * @param qTree
     *          A query tree
     * @throws IOException
     */
    static Qryop parseQuery(String qString) throws IOException {

        Qryop currentOp = null;
        Stack<Qryop> stack = new Stack<Qryop>();

        // Add a default query operator to an unstructured query. This
        // is a tiny bit easier if unnecessary whitespace is removed.

        qString = qString.trim();
        //default operator is OR
        if (qString.charAt(0) != '#') {
            qString = "#or(" + qString + ")";
        }
        // if the query begins with #near, wrap it with #SCORE so that it returns
        // scorelist instead of invertedlist. I wrapped it with #OR instead,
        // since #OR does the same thing as #SCORE when there is only one term.
        if (qString.substring(0, 5).equalsIgnoreCase("#near")) {
            qString = "#or(" + qString + ")";
        }
        // Tokenize the query.
        StringTokenizer tokens = new StringTokenizer(qString, "/\t\n\r ,()", true);
        String token = null;
        // Each pass of the loop processes one token. To improve
        // efficiency and clarity, the query operator on the top of the
        // stack is also stored in currentOp.

        while (tokens.hasMoreTokens()) {
            token = tokens.nextToken();
            if (token.matches("[ ,(\t\n\r]")) {
                // Ignore most delimiters.
            } else if (token.equalsIgnoreCase("#and")) {
                currentOp = new QryopSlAnd();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#or")) {
                currentOp = new QryopSlOr();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#near")) {
                String t = tokens.nextToken();//ignore the "/"
                t = tokens.nextToken();
                currentOp = new QryopIlNear(Integer.parseInt(t));
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#syn")) {
                currentOp = new QryopIlSyn();
                stack.push(currentOp);
            } else if (token.startsWith(")")) {
                // Finish current query operator.
                // If the current query operator is not an argument to
                // another query operator (i.e., the stack is empty when it
                // is removed), we're done (assuming correct syntax - see
                // below). Otherwise, add the current operator as an
                // argument to the higher-level operator, and shift
                // processing back to the higher-level operator.
                stack.pop();
                if (stack.empty())
                    break;
                Qryop arg = currentOp;
                currentOp = stack.peek();
                currentOp.add(arg);
            } else {

                // split the token into term and field
                String[] parts = token.split("\\.");
                // if parts.length is 1, we know the query doesn't specify field
                if (parts.length == 1) {
                    if (tokenizeQuery(token).length > 0) {
                        token = tokenizeQuery(parts[0])[0];
                        currentOp.add(new QryopIlTerm(token));
                    }
                }
                // parts.length is 2, we know the query specifies field
                else if (parts.length == 2) {
                    if (tokenizeQuery(token).length > 0) {
                        token = tokenizeQuery(parts[1])[0];
                        currentOp.add(new QryopIlTerm(token, parts[0]));
                    }
                }
            }
        }

        // A broken structured query can leave unprocessed tokens on the
        // stack, so check for that.

        if (tokens.hasMoreTokens()) {
            System.err.println("Error:  Query syntax is incorrect.  " + qString);
            return null;
        }

        return currentOp;
    }

    /**
     *  Print a message indicating the amount of memory used.  The
     *  caller can indicate whether garbage collection should be
     *  performed, which slows the program but reduces memory usage.
     *  @param gc If true, run the garbage collector before reporting.
     *  @return void
     */
    public static void printMemoryUsage (boolean gc) {

        Runtime runtime = Runtime.getRuntime();

        if (gc) {
            runtime.gc();
        }

        System.out.println ("Memory used:  " +
                            ((runtime.totalMemory() - runtime.freeMemory()) /
                             (1024L * 1024L)) + " MB");
    }

    /**
     * Print the query results.
     * Format:
     * QueryID Q0 DocID Rank Score RunID
     *
     * @param queryName Original query.
     * @param result Result object generated by {@link Qryop#evaluate()}.
     * @throws IOException
     */
    static void printResults(String queryName, QryResult result, int queryID) throws IOException {

        if (result.docScores.scores.size() < 1) {
            System.out.println(queryID + "\t" + "Q0" + "\t" + "dummy" + "\t" +
                               "1" + "\t" + "0" + "\t" + "run-1");
        } else {
            for (int i = 0; i < result.docScores.scores.size(); i++) {
                if (i >= 100) break;
                System.out.println(queryID + "\t" + "Q0"
                                   + "\t" + getExternalDocid (result.docScores.getDocid(i))
                                   + "\t" + Integer.toString(i+1)
                                   + "\t" + result.docScores.getDocidScore(i)
                                   + "\t" + "run-1");
            }
        }
    }
    /**
     * writes results to the file specified in parameterFile
     * @param writer
     * @param queryName
     * @param result
     * @param queryID
     * @throws IOException
     */
    static void writeResults(BufferedWriter writer, String queryName,
                             QryResult result, int queryID) throws IOException {

        if (result.docScores.scores.size() < 1) {
            String towrite = queryID + "\t" + "Q0" + "\t" + "dummy" + "\t" +
                             "1" + "\t" + "0" + "\t" + "run-1" + "\n";
            writer.write(towrite);
        } else {
            for (int i = 0; i < result.docScores.scores.size(); i++) {
                if (i >= 100) break;
                String towrite = queryID + "\t" + "Q0"
                                 + "\t" + getExternalDocid (result.docScores.getDocid(i))
                                 + "\t" + Integer.toString(i+1)
                                 + "\t" + result.docScores.getDocidScore(i)
                                 + "\t" + "run-1" + "\n";
                writer.write(towrite);
            }
        }
    }

    /**
     *  Given a query string, returns the terms one at a time with stopwords
     *  removed and the terms stemmed using the Krovetz stemmer.
     *
     *  Use this method to process raw query terms.
     *
     *  @param query String containing query
     *  @return Array of query tokens
     *  @throws IOException
     */
    static String[] tokenizeQuery(String query) throws IOException {

        TokenStreamComponents comp = analyzer.createComponents("dummy", new StringReader(query));
        TokenStream tokenStream = comp.getTokenStream();

        CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
        tokenStream.reset();

        List<String> tokens = new ArrayList<String>();
        while (tokenStream.incrementToken()) {
            String term = charTermAttribute.toString();
            tokens.add(term);
        }
        return tokens.toArray(new String[tokens.size()]);
    }
}
