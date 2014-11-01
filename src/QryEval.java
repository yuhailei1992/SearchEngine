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
    
    public static float BM25_k_1 = 0.0f;
    public static float BM25_b = 0.0f;
    public static float BM25_k_3 = 0.0f;
    public static float Indri_mu = 0.0f;
    public static float Indri_lambda = 0.0f;
    public static DocLengthStore dls;
    /**
     *  @param args The only argument is the path to the parameter file.
     *  @throws Exception
     */
    public static void main(String[] args) throws Exception {

        /*
         * check if the parameters are valid
         */
        if (args.length < 1) {
            System.err.println(usage);
            System.exit(1);
        }

        /*
         *  read in the parameter file; one parameter per line in format of key=value
         */
        Map<String, String> params = new HashMap<String, String>();
        Scanner scan = new Scanner(new File(args[0]));
        String line = null;
        System.out.println("Parameters are: ");
        do {
            line = scan.nextLine();
            System.out.println(line);
            String[] pair = line.split("=");
            params.put(pair[0].trim(), pair[1].trim());
        } while (scan.hasNext());
        scan.close();

        /*
         *  parameters required for this example to run
         */
        if (!params.containsKey("indexPath")) {
            System.err.println("Error: Parameters were missing.");
            System.exit(1);
        }

        /*
         *  open the index
         */
        READER = DirectoryReader.open(FSDirectory.open(new File(params.get("indexPath"))));

        if (READER == null) {
            System.err.println(usage);
            System.exit(1);
        }

        /* 
         * initialize the appropriate retrieval model
         */
        RetrievalModel model = new RetrievalModelRankedBoolean();
        if (params.get("retrievalAlgorithm").equals("UnrankedBoolean")) {
            model = new RetrievalModelUnrankedBoolean();
        }
        else if (params.get("retrievalAlgorithm").equals("RankedBoolean")) {
            model = new RetrievalModelRankedBoolean();
        }
        else if (params.get("retrievalAlgorithm").equals("BM25")) {
            model = new RetrievalModelBM25();
            dls = new DocLengthStore(READER);
            if (!params.containsKey("BM25:k_1") || !params.containsKey("BM25:b") || !params.containsKey("BM25:k_3")) {
            	System.err.println("Error: Parameters were missing for BM25.");
                System.exit(1);
            }
            ///set the parameters of BM25
            QryEval.BM25_k_1 = Float.parseFloat(params.get("BM25:k_1"));
            QryEval.BM25_b = Float.parseFloat(params.get("BM25:b"));
            QryEval.BM25_k_3 = Float.parseFloat(params.get("BM25:k_3"));
        }
        else if (params.get("retrievalAlgorithm").equals("Indri")) {
        	model = new RetrievalModelIndri();
        	dls = new DocLengthStore(READER);
        	if (!params.containsKey("Indri:mu") || !params.containsKey("Indri:lambda")) {
            	System.err.println("Error: Parameters were missing for Indri.");
                System.exit(1);
            }
        	QryEval.Indri_mu = Float.parseFloat(params.get("Indri:mu"));
        	QryEval.Indri_lambda = Float.parseFloat(params.get("Indri:lambda"));
        }
        else {
            System.err.println("Error: Unknown retrieval model.");
            System.exit(1);
        }

        /*
         * scan the query file
         */
        Scanner queryScanner = new Scanner(new File(params.get("queryFilePath")));
        String queryLine = null;

        BufferedWriter writer = null;
        writer = new BufferedWriter(new FileWriter(new File(
                                        params.get("trecEvalOutputPath"))));
        /*
         * iteratively process all the queries
         */
        do {
            queryLine = queryScanner.nextLine();
            System.out.println("query is " + queryLine);
            String[] parts = queryLine.split(":");
            int queryID = 0;
            //if the query line doesn't contain a queryID
            if (parts.length == 1) {
                queryLine = parts[0];
            }
            //the query line contains a queryID
            else {
                queryID = Integer.parseInt(parts[0]);
                queryLine = parts[1];
            }
            /*
             * hw4
             */
            // no query expansion
            if (!params.containsKey("fb") || !Boolean.parseBoolean(params.get("fb"))) {
            	// use the query to retrieve documents
            	System.out.println("directly retrieve");
            	Qryop qTree;
                qTree = parseQuery (queryLine, model);
                QryResult result = qTree.evaluate (model);
                
                System.out.println("rank begin");
                if (result != null) {
                	rank(result);
                }
                System.out.println("rank end");
                printResults (queryLine, result, queryID);
                writeResults (writer, queryLine, result, queryID);
            }
            else {//with query expansion
            	ArrayList<Integer> top_docid = new ArrayList<Integer>();//stores the top N document ids
            	ArrayList<Double> top_scores = new ArrayList<Double>();//stores the top N Indri scores
            	//if there is initialranking file, fetch the ranking from that file
            	if (params.containsKey("fbInitialRankingFile")) {
            		/*
            		 * read initialranking file
            		 */
            		System.out.println("read file");
            		Scanner scan_ranking = new Scanner(new File(params.get("fbInitialRankingFile")));
            		String line_ranking = null;
            		int j = 0;
            		/*
            		 * get the top fbDocs documents
            		 */
                    do {
                    	line_ranking = scan_ranking.nextLine();
                        String[] pair = line_ranking.split(" ");
                        
                        top_docid.add(getInternalDocid(pair[2]));
                        top_scores.add(Double.parseDouble(pair[4]));
                        //System.out.println("The extid is " + pair[2] + "score" + pair[4]);
                        j++;
                    } while (scan_ranking.hasNext() && j < Integer.parseInt(params.get("fbDocs")));
                    scan_ranking.close();
            	}
            	else {
            		System.out.println("query expansion");
            		Qryop qTree;
                    qTree = parseQuery (queryLine, model);
                    QryResult result = qTree.evaluate (model);
                    
                    System.out.println("rank begin");
                    if (result != null) {
                    	rank(result);
                    }
                    System.out.println("rank end");
                    //query expansion
                    for (int i = 0; i < Math.min(Integer.parseInt(params.get("fbDocs")), result.docScores.scores.size()); ++i) {
                    	top_docid.add(result.docScores.getDocid(i));
                    	top_scores.add(result.docScores.getDocidScore(i));
                    }
            	}
            	
            	/*
            	 * HashMaps:
            	 * hm stores all the terms along with their ctf
            	 * hm2 is for later use. It contains terms with their score
            	 * doc_hm stores terms in each document
            	 */
                HashMap<String, Double> hm = new HashMap<String, Double>();
                HashMap<String, Double> hm2 = new HashMap<String, Double>();
                ArrayList<HashMap<String, Double>> doc_hm = new ArrayList<HashMap<String, Double>>();
                /*
                 * prepare some global variables that remains constant for any document, any term
                 */
                double fbMu = Double.parseDouble(params.get("fbMu"));
                double length_C = (double)QryEval.READER.getSumTotalTermFreq("body");
                
                /*
            	 * first loop: go through all the top docs, store terms and their ctf in a hashmap
            	 */
                for (int i = 0; i < top_docid.size(); ++i) {
                	TermVector tv = new TermVector(top_docid.get(i), "body");
                	/*
                	 * temp_hm is a temporary hashmap that stores terms with their tf. 
                	 * temp_hm will be added to doc_hm
                	 */
                	HashMap<String, Double> temp_hm = new HashMap<String, Double>();
                	/*
                	 * jump over the first element in tv
                	 */
                	for (int j = 1; j < tv.terms.length; ++j) {
                		
                		String curr_term = tv.stemString(j);
                		/*
                		 * store term with tf in temp_hm
                		 */
                		temp_hm.put(curr_term, (double)tv.stemFreq(j));
                		/*
                		 * then store term with ctf in hm
                		 */
                		double ctf = (double)tv.totalStemFreq(j);
                		if (hm.containsKey(curr_term)) {
                			//do nothing, cause ctf is constant for any document
                		}
                		else {
                			hm.put(curr_term, ctf);
                		}
                	}
                	/*
                	 * add the temp hm to the doc_hm
                	 */
                	doc_hm.add(temp_hm);
                }
                
                /*
                 * loop2: go throught the hashmap again and calculate the scores
                 */
                Iterator it = hm.entrySet().iterator();
                while (it.hasNext()) {
                    Map.Entry pairs = (Map.Entry)it.next();
                    double score = 0.0;
                    String curr_term = (String)pairs.getKey();
                    /*
                     * traverse all the documents, compute score for a term
                     * if the term appeared in a document, then simply calculate its score
                     * else, compute the default score
                     */
                    for (int i = 0; i < top_docid.size(); ++i) {
                    	
                    	double length_d = (double)QryEval.dls.getDocLength("body", top_docid.get(i));
                    	
                		double ctf = hm.get(curr_term);
                		// smoothing factor
                		double p_MLE = ctf / length_C;
                		/*
                		 * the default tf is 0. If the term appeared in a document, then the tf is not zero
                		 */
                		double curr_tf = 0.0;
                		if (doc_hm.get(i).containsKey(curr_term)) {
                			curr_tf = doc_hm.get(i).get(curr_term);
                		}
                		
                		double p_td = (curr_tf + fbMu * p_MLE) / (length_d + fbMu);
                		double p_Id = top_scores.get(i);
                		double p_tC = Math.log(length_C / ctf);
                		score += p_td * p_Id * p_tC;
                    	hm2.put(curr_term, score);
                    }
                }
                /*
                 * sort the hashmap by score
                 */
                Map<String, Double> map = sortByValues(hm2); 
                System.out.println("================After Sorting:");
                Set set2 = map.entrySet();
                /*
                 * configure the expanded query
                 */
                Iterator iterator2 = set2.iterator();
                int i = 0;
                String exp_qry = queryID + ": #WAND ( ";
                while(iterator2.hasNext() && i < Integer.parseInt(params.get("fbTerms"))) {
                     Map.Entry me2 = (Map.Entry)iterator2.next();
                     System.out.print(me2.getKey() + ": ");
                     System.out.println(me2.getValue());
                     
                     /*
                      * ignore terms that contain "." or ","
                      */
                     if (me2.getKey().toString().contains(".") || me2.getKey().toString().contains(",")) {
                    	 ++i;//haileiy
                     }
                     /*
                      * regular process. Add this term to the expanded query
                      */
                     else {
                    	 //add to the expanded query
                    	 exp_qry += String.format( "%.4f", me2.getValue());
                    	 exp_qry += " ";
                    	 exp_qry += me2.getKey();
                    	 exp_qry += " ";
                    	 ++i;
                     }
                }
                exp_qry += ")";
                System.out.println(exp_qry);
                
                /*
                 * write the expanded query to fbExpansionQueryFile
                 */
                BufferedWriter writer_expand = new BufferedWriter(new FileWriter(new File(
                                                params.get("fbExpansionQueryFile"))));
                writer_expand.write(exp_qry);
                try {
                	writer_expand.close();
                } catch (Exception e) {
                	
                }
                /*
                 * use the expanded query to retrieve
                 */
                Qryop qTree;
                qTree = parseQuery (exp_qry, model);
                QryResult result = qTree.evaluate (model);
                
                System.out.println("new rank begin");
                if (result != null) {
                	rank(result);
                }
                System.out.println("new rank end");
                printResults (exp_qry, result, queryID);
                BufferedWriter writer_expand_res = null;
                writer_expand_res = new BufferedWriter(new FileWriter(new File(
                                                params.get("trecEvalOutputPath"))));
                writeResults (writer_expand_res, queryLine, result, queryID);
                writer_expand_res.close();
            }
        } while (queryScanner.hasNext());
        try {
            writer.close();
            queryScanner.close();
        } catch (Exception e) {
        }
        
        /*
         *  Create the trec_eval output.  Your code should write to the
         *  file specified in the parameter file, and it should write the
         *  results that you retrieved above.  This code just allows the
         *  testing infrastructure to work on QryEval.
         */

        printMemoryUsage(false);
    }
    
    /**
     * this function is for sorting hashmap
     * @param map
     * @return
     */
    private static HashMap<String, Double> sortByValues(HashMap<String, Double> map) { 
        List list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
             public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue())
                   .compareTo(((Map.Entry) (o1)).getValue());
             }
        });
        HashMap sortedHashMap = new LinkedHashMap();
        for (Iterator it = list.iterator(); it.hasNext();) {
               Map.Entry entry = (Map.Entry) it.next();
               sortedHashMap.put(entry.getKey(), entry.getValue());
        } 
        return sortedHashMap;
    }
	
    /**
     * rank the results by score.
     * @param result
     * @throws IOException
     */
    static void rank (QryResult result) throws IOException {
        //first, set the externalId
        for (ScoreList.ScoreListEntry p : result.docScores.scores) {
            p.setExternalID(getExternalDocid(p.docid));
        }
        //then,  rank the results
        result.docScores.sortByScore();
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
    static Qryop parseQuery(String qString, RetrievalModel model) throws IOException {
        Qryop currentOp = null;
        Stack<Qryop> stack = new Stack<Qryop>();

        // Add a default query operator to an unstructured query. This
        // is a tiny bit easier if unnecessary whitespace is removed.

        qString = qString.trim();
        if (model instanceof RetrievalModelUnrankedBoolean || model instanceof RetrievalModelRankedBoolean) {
	        if (qString.charAt(0) != '#' || qString.toLowerCase().startsWith("#syn") || 
	        		qString.substring(0, 5).equalsIgnoreCase("#near") ||
	        		qString.substring(0, 7).equalsIgnoreCase("#window")) {
	            qString = "#or(" + qString + ")";
	        }
	        if (qString.charAt(qString.length()-1) != ')') {
	        	qString = "#or(" + qString + ")";
	        }
        }
        else  if (model instanceof RetrievalModelBM25) {
        	qString = "#sum(" + qString + ")";
        }
        else  if (model instanceof RetrievalModelIndri) {
        	qString = "#and(" + qString + ")";
        }
        // Tokenize the query.
        StringTokenizer tokens = new StringTokenizer(qString, "\t\n\r ,()", true);//del /
        String token = null;
        // Each pass of the loop processes one token. To improve
        // efficiency and clarity, the query operator on the top of the
        // stack is also stored in currentOp.
        int isWeight = 1;
        while (tokens.hasMoreTokens()) {
            token = tokens.nextToken();
            if (token.matches("[ ,(\t\n\r]")) {
            	if (token.matches(" ")) {
            		continue;
            	}
            	isWeight = 1;
            	//continue;
                // Ignore most delimiters.
            } else if (token.equalsIgnoreCase("#and")) {
                currentOp = new QryopSlAnd();
                stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#or")) {
                currentOp = new QryopSlOr();
                stack.push(currentOp);
            } else if (token.toLowerCase().startsWith("#near")) {
            	String temp[] = token.split("/");
            	currentOp = new QryopIlNear(Integer.parseInt(temp[1]));
            	stack.push(currentOp);
            } else if (token.toLowerCase().startsWith("#window")) {
            	String temp[] = token.split("/");
            	currentOp = new QryopIlWindow(Integer.parseInt(temp[1]));
            	stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#sum")) {
            	currentOp = new QryopSlSum();
            	stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#wand")) {
            	currentOp = new QryopSlWAnd();
            	stack.push(currentOp);
            } else if (token.equalsIgnoreCase("#wsum")) {
            	currentOp = new QryopSlWSum();
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
            	isWeight = 1;
                stack.pop();
                if (stack.empty())
                    break;
                Qryop arg = currentOp;
                currentOp = stack.peek();
                currentOp.add(arg);
            } else {
                // split the token into term and field
            	if (isWeight == 1 && (currentOp instanceof QryopSlWAnd || currentOp instanceof QryopSlWSum)) {
	            	if (isWeight == 1) {
	            		isWeight = 0;
            			if (currentOp instanceof QryopSlWAnd) {
            				((QryopSlWAnd)currentOp).weight.add(Float.parseFloat(token));
            			}
            			else if (currentOp instanceof QryopSlWSum) {
            				((QryopSlWSum)currentOp).weight.add(Double.parseDouble(token));
            			}
            		}
	            	else {
	            		isWeight = 1;
	            	}
             	}
            	else {
            		if (isWeight == 1) isWeight = 0;
            		else isWeight = 1;
            		//isWeight = 1;
	                String[] parts = token.split("\\.");
	                
	                // parts.length is 1: the query doesn't specify field
	                //System.out.println(parts[0]);
	                //if it is a stopword, then remove the weight in our qryop
	                if (tokenizeQuery(parts[0]).length == 0) {
	                	System.out.println("Found Stopwords");
	                	if (currentOp instanceof QryopSlWAnd || currentOp instanceof QryopSlWSum) {
	                		if (currentOp instanceof QryopSlWAnd) {
	                			int idx = ((QryopSlWAnd)currentOp).weight.size();
	            				((QryopSlWAnd)currentOp).weight.remove(idx-1);
	            			}
	            			else if (currentOp instanceof QryopSlWSum) {
	            				int idx = ((QryopSlWSum)currentOp).weight.size();
	            				((QryopSlWSum)currentOp).weight.remove(idx-1);
	            			}
	                		
	                	}
	                	continue;
	                }
	                if (parts.length == 1) {
	                    if (tokenizeQuery(token).length > 0) {
	                        token = tokenizeQuery(parts[0])[0];
	                        currentOp.add(new QryopIlTerm(token));
	                    }
	                }
	                // parts.length is 2: the query specifies field
	                else if (parts.length == 2) {
	                    if (tokenizeQuery(token).length > 0) {
	                    	if (tokenizeQuery(parts[0]).length != 0) {
		                        token = tokenizeQuery(parts[0])[0];
		                        currentOp.add(new QryopIlTerm(token, parts[1]));
	                    	}
	                    }
	                }
	                //isWeight = 1;
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
