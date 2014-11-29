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
    /* bm25 parameters */
    public static float BM25_k_1 = 0.0f;
    public static float BM25_b = 0.0f;
    public static float BM25_k_3 = 0.0f;
    /* indri parameters */
    public static float Indri_mu = 0.0f;
    public static float Indri_lambda = 0.0f;
    /* letor parameters */
    public static float letor_k_1 = 0.0f;
    public static float letor_k_3 = 0.0f;
    public static float letor_b = 0.0f;
    public static float letor_mu = 0.0f;
    public static float letor_lambda = 0.0f;
    public static DocLengthStore dls;
    /**
     *  @param args The only argument is the path to the parameter file.
     *  @throws Exception
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
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
        do {
            line = scan.nextLine();
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
        else if (params.get("retrievalAlgorithm").equals("letor")){
        	model = new RetrievalModelLearningtoRank();
        	dls = new DocLengthStore(READER);
        	QryEval.letor_mu = Float.parseFloat(params.get("Indri:mu"));
        	QryEval.letor_lambda = Float.parseFloat(params.get("Indri:lambda"));
        	QryEval.letor_k_1 = Float.parseFloat(params.get("BM25:k_1"));
        	QryEval.letor_k_3 = Float.parseFloat(params.get("BM25:k_3"));
        	QryEval.letor_b = Float.parseFloat(params.get("BM25:b"));
        }
        else
        {
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
        if (params.containsKey("fbExpansionQueryFile")) {
        		File file = new File(params.get("fbExpansionQueryFile"));
        		if(file.delete()){
        			System.out.println(">> Original expansion file, " + file.getName() + ", is deleted!");
        		}else{
        			System.out.println(">> Delete operation failed.");
        		}
        }
        
        /*
         * hw5
         */
        
        if (model instanceof RetrievalModelLearningtoRank)
        {
        	// read the relevance file into a hashmap
        	Map<String, ArrayList<String>> relevance_map = new HashMap<String, ArrayList<String>>();
        	Scanner rel_scan = new Scanner(new File(params.get("letor:trainingQrelsFile")));
        	ArrayList<Integer> rel_docid = new ArrayList<Integer>();
        	ArrayList<String> rel_docid_str = new ArrayList<String>();
        	do {
        		String curr = rel_scan.nextLine();
        		String curr_arr[] = curr.split("\\s+");
        		//System.out.println(curr_arr[0]);
        		
        		if (relevance_map.containsKey(curr_arr[0]))// the query already exists
        		{
        			ArrayList<String> rel_al = relevance_map.get(curr_arr[0]);
        			rel_al.add(curr);
        			relevance_map.put(curr_arr[0], rel_al);
        		}
        		else
        		{
        			ArrayList<String> rel_al = new ArrayList<String>();
        			rel_al.add(curr);
        			relevance_map.put(curr_arr[0], rel_al);
        		}
        		rel_docid.add(Integer.parseInt(curr_arr[0]));
        		rel_docid_str.add(curr_arr[0]);
        		
        	}while (rel_scan.hasNext());
        	rel_scan.close();
        	
        	// fetch pagerank from index
        	System.out.println("Now we fetch pagerank scores from index file");
        	HashMap<String, Double> pagerank_map = new HashMap<String, Double>();
        	Scanner pagerank_scan = new Scanner(new File(params.get("letor:pageRankFile")));
        	do {
        		String curr = pagerank_scan.nextLine();
        		String curr_arr[] = curr.split("\\s+");
        		pagerank_map.put(curr_arr[0], Double.parseDouble(curr_arr[1]));
        	}while(pagerank_scan.hasNext());
        	pagerank_scan.close();
        	
        	// set disable 
        	boolean mask[] = new boolean[18];
        	for (int i = 0; i < 18; ++i)
        		mask[i] = true;
        	String disable = params.get("letor:featureDisable");
        	String disable_arr[] = disable.split(",");
        	int disable_arr_int[] = new int[disable_arr.length];
        	for (int i = 0; i < disable_arr.length; ++i)
        	{
        		disable_arr_int[i] = Integer.parseInt(disable_arr[i].trim()); 
        		mask[disable_arr_int[i]-1] = false; 
        	}
        	
        	Scanner training_scan = new Scanner(new File(params.get("letor:trainingQueryFile")));
        	do {
        		String curr_query = training_scan.nextLine();
        		String curr_query_arr[] = curr_query.split(":");
        		String tokens[] = tokenizeQuery(curr_query_arr[1]);
        		System.out.println("$$Current training query is : " + curr_query);
        		
        		ArrayList<String> rel = relevance_map.get(curr_query_arr[0]);
        		// foreach document d in the relevance udgements for training query q
        		
        		ArrayList<ArrayList<Double>> ls = new ArrayList<ArrayList<Double>>();
        		FeatureVector fv = new FeatureVector();
        		fv.tokens = tokens;
    			fv.mask = mask;
    			fv.pagerank_map = pagerank_map;
        		for (int i = 0; i < rel.size(); ++i)
        		{
        			String curr_ext_id = rel.get(i).split("\\s+")[2];
        			int curr_int_id = getInternalDocid(curr_ext_id);
        			ls.add(fv.generateFeatureVector(curr_int_id));
        			//System.out.println(fv.generateFeatureVector(curr_int_id));
        		}
        		ArrayList<String> s = fv.generateNormalizedFeatureVector(ls);
        		// write to file
        		BufferedWriter FeatureVectorWriter = null;
        		FeatureVectorWriter = new BufferedWriter(new FileWriter(new File(
                        params.get("letor:trainingFeatureVectorsFile"))));
        		//System.out.println("s size is " + s.size());
        		for (int i = 0; i < rel.size(); ++i)
        		{
        			String arr[] = rel.get(i).split("\\s+");
    				StringBuilder tmp_str = new StringBuilder();
    				tmp_str.append(arr[3]);
    				tmp_str.append(" qid:");
    				tmp_str.append(curr_query_arr[0]);
    				tmp_str.append(" ");
    				tmp_str.append(s.get(i));
    				tmp_str.append("# ");
    				tmp_str.append(arr[2]);
    				FeatureVectorWriter.write(tmp_str.toString());
    				FeatureVectorWriter.newLine();
        		}
        		//Thread.sleep(10000);
        		try {
        			FeatureVectorWriter.close();
        		}
        		catch (Exception e) {
        			throw new Exception("featurevectorwriter close error");
        		}
        	}while(training_scan.hasNext());
        	training_scan.close();
        	System.out.println("$$now we call SVM to train a model");
        	// call letor to generate the feature vector for training queries
            // runs svm_rank_learn from within Java to train the model
            // execPath is the location of the svm_rank_learn utility, 
            // which is specified by letor:svmRankLearnPath in the parameter file.
            // FEAT_GEN.c is the value of the letor:c parameter.
        	String execPath = params.get("letor:svmRankLearnPath");
        	String FEAT_GEN = params.get("letor:svmRankParamC");
        	String qrelsFeatureOutputFile = params.get("letor:trainingFeatureVectorsFile");
        	String modelOutputFile = params.get("letor:svmRankModelFile");
            Process cmdProc = Runtime.getRuntime().exec(
                new String[] { execPath, "-c", String.valueOf(FEAT_GEN), qrelsFeatureOutputFile,
                    modelOutputFile });

            // The stdout/stderr consuming code MUST be included.
            // It prevents the OS from running out of output buffer space and stalling.

            // consume stdout and print it out for debugging purposes
            BufferedReader stdoutReader = new BufferedReader(
                new InputStreamReader(cmdProc.getInputStream()));
            String SVM_line;
            while ((SVM_line = stdoutReader.readLine()) != null) {
              System.out.println(SVM_line);
            }
            // consume stderr and print it for debugging purposes
            BufferedReader stderrReader = new BufferedReader(
                new InputStreamReader(cmdProc.getErrorStream()));
            while ((SVM_line = stderrReader.readLine()) != null) {
              System.out.println(SVM_line);
            }

            // get the return value from the executable. 0 means success, non-zero 
            // indicates a problem
            int retValue = cmdProc.waitFor();
            if (retValue != 0) {
              throw new Exception("SVM Rank crashed.");
            }
        	
        	// read test queries from input file
            
            Scanner queryScanner2 = new Scanner(new File(params.get("queryFilePath")));
            String queryLine2 = null;
        	do {
            	// use BM25 to get initial ranking for test queries
        		queryLine2 = queryScanner2.nextLine();
                System.out.println(">> Original query is " + queryLine);
                String[] parts = queryLine2.split(":");
        		String tokens[];
                int queryID = 0;
                //if the query line doesn't contain a queryID
                if (parts.length == 1) {
                	queryLine2 = parts[0];
                	tokens = tokenizeQuery(parts[0]);
                }
                //the query line contains a queryID
                else {
                    queryID = Integer.parseInt(parts[0]);
                    queryLine2 = parts[1];
                    tokens = tokenizeQuery(parts[1]);
                }

        		System.out.println("$$ Use BM25 to get initial ranking");
            	Qryop qTree;
            	RetrievalModel model2 = new RetrievalModelBM25();
                qTree = parseQuery (queryLine2, model2);
                QryResult result = qTree.evaluate (model2);
                
                System.out.println(">> BM25 Rank begin");
                if (result != null) {
                	rank(result);
                }
                System.out.println(">> BM25 Rank end");
                printResults (queryLine2, result, queryID);
                writeResults (writer, queryLine2, result, queryID);
                // store the top 100 results
                ArrayList<Integer> svm_top_docid = new ArrayList<Integer>();
                for (int i = 0; i < Math.min(100, result.docScores.scores.size()); ++i) {
                	svm_top_docid.add(result.docScores.getDocid(i));
                }
                //System.out.println(svm_top_docid);
                System.out.println("calculate feature vectors for top 100 ranked documents(for each query)");
            	// calculate feature vectors for top 100 ranked documents(for each query)
                ArrayList<ArrayList<Double>> ls2 = new ArrayList<ArrayList<Double>>();
        		FeatureVector fv2 = new FeatureVector();
        		fv2.tokens = tokens;
    			fv2.mask = mask;
    			fv2.pagerank_map = pagerank_map;
        		for (int i = 0; i < svm_top_docid.size(); ++i)
        		{
        			int curr_int_id = svm_top_docid.get(i);
        			ls2.add(fv2.generateFeatureVector(curr_int_id));
        		}
        		ArrayList<String> s2 = fv2.generateNormalizedFeatureVector(ls2);
        		// write to file
        		
        		BufferedWriter FeatureVectorWriter2 = null;
        		FeatureVectorWriter2 = new BufferedWriter(new FileWriter(new File(
                        params.get("letor:testingFeatureVectorsFile"))));
        		for (int i = 0; i < svm_top_docid.size(); ++i)
        		{
    				StringBuilder tmp_str = new StringBuilder();
    				tmp_str.append(0);
    				tmp_str.append(" qid:");
    				tmp_str.append(queryID);
    				tmp_str.append(" ");
    				tmp_str.append(s2.get(i));
    				tmp_str.append("# ");
    				tmp_str.append(getExternalDocid(queryID));
    				FeatureVectorWriter2.write(tmp_str.toString());
    				FeatureVectorWriter2.newLine();
        		}
        		try {
        			FeatureVectorWriter2.close();
        		}
        		catch (Exception e) {
        			throw new Exception("featurevectorwriter2 close error");
        		}
        		System.out.println("$$Initial ranking has been wrote to file!");
        	} while(queryScanner2.hasNext());
        	
        	System.out.println("$$now we call SVM to train a model");
        	// call letor to generate the feature vector for training queries
            // runs svm_rank_learn from within Java to train the model
            // execPath is the location of the svm_rank_learn utility, 
            // which is specified by letor:svmRankLearnPath in the parameter file.
            // FEAT_GEN.c is the value of the letor:c parameter.
        	String execPath2 = params.get("letor:svmRankClassifyPath");
        	String FEAT_GEN2 = params.get("letor:svmRankParamC");
        	String qrelsFeatureOutputFile2 = params.get("letor:testingFeatureVectorsFile");
        	String modelOutputFile2 = params.get("letor:testingDocumentScore");
            Process cmdProc2 = Runtime.getRuntime().exec(
            		new String[] { params.get("letor:svmRankClassifyPath").trim(), 
            				params.get("letor:testingFeatureVectorsFile").trim(), 
            				params.get("letor:svmRankModelFile").trim(),
            				params.get("letor:testingDocumentScores").trim()});
            
            // The stdout/stderr consuming code MUST be included.
            // It prevents the OS from running out of output buffer space and stalling.

            // consume stdout and print it out for debugging purposes
            BufferedReader stdoutReader2 = new BufferedReader(
                new InputStreamReader(cmdProc2.getInputStream()));
            String SVM_line2;
            while ((SVM_line2 = stdoutReader2.readLine()) != null) {
              System.out.println(SVM_line2);
            }
            // consume stderr and print it for debugging purposes
            BufferedReader stderrReader2 = new BufferedReader(
                new InputStreamReader(cmdProc2.getErrorStream()));
            while ((SVM_line2 = stderrReader2.readLine()) != null) {
              System.out.println(SVM_line2);
            }

            // get the return value from the executable. 0 means success, non-zero 
            // indicates a problem
            int retValue2 = cmdProc2.waitFor();
            if (retValue2 != 0) {
              throw new Exception("SVM Rank crashed.");
            }
        	
        	
        	// write the feature vectors to a file
        	
        	// read the score produced by SVM
        	
        	// write the final ranking in trec_eval input format
        	
        }
   	 
        /*
         * iteratively process all the queries
         */
        do {
            queryLine = queryScanner.nextLine();
            System.out.println(">> Original query is " + queryLine);
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
            	System.out.println(">> No expansion, directly retrieve");
            	Qryop qTree;
                qTree = parseQuery (queryLine, model);
                QryResult result = qTree.evaluate (model);
                
                System.out.println(">> Rank begin");
                if (result != null) {
                	rank(result);
                }
                System.out.println(">> Rank end");
                printResults (queryLine, result, queryID);
                writeResults (writer, queryLine, result, queryID);
            }
            else {//with query expansion
            	/*
            	 * first, delete the old expand query file
            	 */
            	ArrayList<Integer> top_docid = new ArrayList<Integer>();//stores the top N document ids
            	ArrayList<Double> top_scores = new ArrayList<Double>();//stores the top N Indri scores
            	//if there is initialranking file, fetch the ranking from that file
            	if (params.containsKey("fbInitialRankingFile")) {
            		/*
            		 * read initialranking file
            		 */
            		System.out.println(">> Reading file from reference system");
            		Scanner scan_ranking = new Scanner(new File(params.get("fbInitialRankingFile")));
            		String line_ranking = null;
            		int j = 0;
            		/*
            		 * get the top fbDocs documents
            		 */
                    do {
                    	line_ranking = scan_ranking.nextLine();
                    	String[] pair = line_ranking.split(" ");
                    	if (Integer.parseInt(pair[0]) != queryID) {
                    		continue;
                    	}
                        top_docid.add(getInternalDocid(pair[2]));
                        top_scores.add(Double.parseDouble(pair[4]));
                        System.out.println("The extid is " + pair[2] + "score" + pair[4]);
                        j++;
                    } while (scan_ranking.hasNext() && j < Integer.parseInt(params.get("fbDocs")));
                    scan_ranking.close();
            	}
            	else {
            		System.out.println(">> Query expansion without reference system");
            		Qryop qTree;
                    qTree = parseQuery (queryLine, model);
                    QryResult result = qTree.evaluate (model);
                    
                    System.out.println(">> Rank begin");
                    if (result != null) {
                    	rank(result);
                    }
                    System.out.println(">> Rank end");
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
					Map.Entry<String, Double> pairs = (Map.Entry)it.next();
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
                		
                    }
                    hm2.put(curr_term, score);
                }
                /*
                 * sort the hashmap by score
                 */
                Map<String, Double> map = sortByValues(hm2); 
                
                Set set2 = map.entrySet();
                /*
                 * configure the expanded query
                 */
                Iterator iterator2 = set2.iterator();
                int i = 0;
                String exp_qry = "#WAND ( ";
                while(iterator2.hasNext() && i < Integer.parseInt(params.get("fbTerms"))) {
                     Map.Entry me2 = (Map.Entry)iterator2.next();
                     /*
                      * ignore terms that contain "." or ","
                      */
                     if (me2.getKey().toString().contains(".") || me2.getKey().toString().contains(",")) {
                     }
                     /*
                      * regular process. Add this term to the expanded query
                      */
                     else {
                    	 //add to the expanded query
                    	 exp_qry += me2.getValue();
                    	 exp_qry += " ";
                    	 exp_qry += me2.getKey();
                    	 exp_qry += " ";
                    	 ++i;
                     }
                }
                exp_qry += ")";
                String exp_qry_with_qryid = queryID + ":" + exp_qry;
                /*
                 * write the expanded query to fbExpansionQueryFile
                 */
                BufferedWriter writer_expand = new BufferedWriter(new FileWriter(new File(
                                                params.get("fbExpansionQueryFile")), true));
                writer_expand.write(exp_qry_with_qryid);
                writer_expand.write("\n");
                try {
                	writer_expand.close();
                } catch (Exception e) {
                	//
                }
                /*
                 * use the expanded query to retrieve
                 */
                double orig_weight = Double.parseDouble(params.get("fbOrigWeight"));
                double exp_weight = 1.0 - orig_weight;
                String combined_query = "#WAND ( " + orig_weight + " #AND (" + queryLine + " ) " + exp_weight + " " + exp_qry + ")";
                System.out.println(">> RETRIEVE WITH  " + combined_query);
                Qryop qTree;
                qTree = parseQuery (combined_query, model);
                QryResult result = qTree.evaluate (model);
                
                System.out.println(">> New rank begin");
                if (result != null) {
                	rank(result);
                }
                System.out.println(">> New rank end");
                printResults (combined_query, result, queryID);
                writeResults (writer, queryLine, result, queryID);
            }
        } while (queryScanner.hasNext());
        try {
            writer.close();
            queryScanner.close();
        } catch (Exception e) {
        	
        }
        
        printMemoryUsage(true);
    }
    
    /**
     * this function is for sorting hashmap
     * @param map
     * @return
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static HashMap<String, Double> sortByValues(HashMap<String, Double> map) { 
        List<Double> list = new LinkedList(map.entrySet());
        // Defined Custom Comparator here
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
               return ((Comparable) ((Map.Entry) (o2)).getValue())
                  .compareTo(((Map.Entry) (o1)).getValue());
            }
        });
        HashMap<String, Double> sortedHashMap = new LinkedHashMap<String, Double>();
        for (Iterator it = list.iterator(); it.hasNext();) {
               Map.Entry entry = (Map.Entry) it.next();
               sortedHashMap.put((String)entry.getKey(), (Double)entry.getValue());
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
        //then,  rank the results by score
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
                if(arg.args.size() != 0)
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
	                String[] parts = token.split("\\.");
	                
	                // parts.length is 1: the query doesn't specify field
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
