import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.apache.lucene.analysis.tokenattributes.FlagsAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

public class FeatureVector {
	/*
	
	public double spam_score;
	public double url_depth;
	public double wiki_score;
	public double pagerank_score;
	public double BM25_body;
	public double Indri_body;
	public double term_overlap_body;
	public double BM25_title;
	public double Indri_title;
	public double term_overlap_title;
	public double BM25_url;
	public double Indri_url;
	public double term_overlap_url;
	public double BM25_inlink;
	public double Indri_inlink;
	public double term_overlap_inlink;
	// custom features
	public double custom_1;
	public double custom_2;
	*/
	private int FEATURES_NUM = 18;
	boolean mask[];
	ArrayList<Double> feature;
	HashMap<String, Double> pagerank_map;
	String[] tokens;
	/**
	 * @brief: construction function
	 */
	public FeatureVector()
	{
		/*
		spam_score = 0.0;
		url_depth = 0;
		wiki_score = 0.0;
		pagerank_score = 0.0;
		BM25_body = 0.0;
		Indri_body = 0.0;
		term_overlap_body = 0.0;
		BM25_title = 0.0;
		Indri_title = 0.0;
		term_overlap_title = 0.0;
		BM25_url = 0.0;
		Indri_url = 0.0;
		term_overlap_url = 0.0;
		BM25_inlink = 0.0;
		Indri_inlink = 0.0;
		term_overlap_inlink = 0.0;
		*/
		mask = new boolean[FEATURES_NUM];
		for (int i = 0; i < FEATURES_NUM; ++i)
			mask[i] = true;
		feature = new ArrayList<Double>();
		pagerank_map = new HashMap<String, Double>();
	}
	
	/**
	 * @param docid
	 * @throws Exception
	 */
	public ArrayList<Double> generateFeatureVector(int docid) throws Exception
	{
		ArrayList<Double> res = new ArrayList<Double>();
		// document reader
		Document doc = QryEval.READER.document(docid);
		double default_score = Double.NaN;
		// 0, spam score
		if (mask[0])
		{
			double tmp = Integer.parseInt(doc.get("score"));
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 1, url depth
		if (mask[1])
		{
			res.add(getUrlDepth(doc.get("rawUrl")));
		}
		else
		{
			res.add(default_score);
		}
		
		// 2, wiki score
		if (mask[2])
		{
			res.add(getWikiScore(doc.get("rawUrl")));
		}
		else
		{
			res.add(default_score);
		}
		// 3, pagerank
		if (mask[3])
		{
			// the pagerank file doesn't necessarily contain a score
			if (pagerank_map.containsKey(QryEval.getExternalDocid(docid)))
			{
				res.add(pagerank_map.get(QryEval.getExternalDocid(docid)));
			}
			else
			{
				res.add(default_score);//TODO normalize
			}
		}
		else
		{
			res.add(default_score);
		}
		
		boolean flag = true;
		TermVector tv = null;
		try {
			tv = new TermVector(docid, "body");
		}
		catch (Exception e) {
			flag = false;
		}
		
		//4, BM25, body
		
		if (mask[4] && flag)
		{
			double tmp = getBM25Score(docid, "body", tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 5, indri, body
		if (mask[5] && flag)
		{
			double tmp = getIndriScore(docid, "body", tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 6, term overlap body
		if (mask[6] && flag)
		{
			double tmp = getOverlapScore(tokens, tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		tv = null;
		flag = true;
		try {
			tv = new TermVector(docid, "title");
		}
		catch (Exception e) {
			flag = false;
		}
		
		//7, BM25, title
		
		if (mask[7] && flag)
		{
			double tmp = getBM25Score(docid, "title", tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 8, indri, title
		if (mask[8] && flag)
		{
			double tmp = getIndriScore(docid, "title", tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 9, term overlap title
		if (mask[9] && flag)
		{
			double tmp = getOverlapScore(tokens, tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		tv = null;
		flag = true;
		try {
			tv = new TermVector(docid, "url");
		}
		catch (Exception e) {
			flag = false;
		}
		// 10, BM25, url
		
		if (mask[10] && flag)
		{
			double tmp = getBM25Score(docid, "url", tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 11, indri, url
		if (mask[11] && flag)
		{
			double tmp = getIndriScore(docid, "url", tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 12, term overlap url
		if (mask[12] && flag)
		{
			double tmp = getOverlapScore(tokens, tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		tv = null;
		flag = true;
		try {
			tv = new TermVector(docid, "inlink");
		}
		catch (Exception e) {
			flag = false;
		}		
		
		// 13, BM25, inlink
		
		if (mask[13] && flag)
		{
			double tmp = getBM25Score(docid, "inlink", tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 14, indri, inlink
		if (mask[14] && flag)
		{
			double tmp = getIndriScore(docid, "inlink", tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		
		// 15, term overlap inlink
		if (mask[15] && flag)
		{
			double tmp = getOverlapScore(tokens, tv);
			res.add(tmp);
		}
		else
		{
			res.add(default_score);
		}
		// custom feature 1
		if (mask[16])
		{
			res.add(1.0);
		}
		else
		{
			res.add(default_score);
		}
		// custom feature 2
		if (mask[17])
		{
			res.add(1.0);
		}
		else
		{
			res.add(default_score);
		}
		if (res.size() != FEATURES_NUM)
		{
			throw new Exception("feature number mismatch");
		}
		return res;
	}
	
	public void normalizeFeatureVector(ArrayList<ArrayList<Double>> ls, int idx)
	{
		if (mask[idx]) // if the feature is not disabled
		{
			double max = Double.MIN_VALUE;
			double min = Double.MAX_VALUE;
			
			for (int i = 0; i < ls.size(); ++i)
			{
				if (!Double.isNaN(ls.get(i).get(idx)))
				{
					max = Math.max(max, ls.get(i).get(idx));
					min = Math.min(min, ls.get(i).get(idx));
				}
			}
			// now we have the max and min
			if (max == min)//TODO for double, equals is not a good idea
			{
				for (int i = 0; i < ls.size(); ++i)
					ls.get(i).set(idx, 0.0d);
			}
			else 
			{
				for (int i = 0; i < ls.size(); ++i)
				{
					double tmp = ls.get(i).get(idx);
					if (!Double.isNaN(tmp))
					{
						double tmp2 = (tmp - min) / (max - min);
						ls.get(i).set(idx, tmp2);
					}
					else
					{
						ls.get(i).set(idx, 0.0d);
					}
				}
			}
		}
	}
	
	public ArrayList<String> generateNormalizedFeatureVector(ArrayList<ArrayList<Double>> ls) throws Exception
	{
		ArrayList<String> res = new ArrayList<String>();
		
		for (int i = 0; i < FEATURES_NUM; ++i)
		{
			normalizeFeatureVector(ls, i);//TODO
		}
		
		for (int i = 0; i < ls.size(); ++i)
		{
			StringBuilder tmp = new StringBuilder();
			for (int j = 0; j < FEATURES_NUM; ++j)
			{
				tmp.append(j+1);
				tmp.append(":");
				tmp.append(ls.get(i).get(j));
				tmp.append(" ");
			}
			res.add(tmp.toString());
		}
		return res;
	}

	
	/**
	 * 
	 * @param url
	 * @return
	 */
	public double getUrlDepth(String url)
	{
		if (url == null)
		{
			return 0.0;
		}
		else
		{
			double res = 0.0;
			for (int i = 0; i < url.length(); ++i)
			{
				if (url.charAt(i) == '/')
					res += 1.0;
			}
			return res;
		}
	}
	
	/**
	 * @param docid
	 * @param field
	 * @param vec
	 * @return
	 * @throws IOException
	 */
	
	public double getBM25Score(int docid, String field, TermVector tv) throws IOException
	{
		double score = 0.0d;
		double avg_doclen = (double)QryEval.READER.getSumTotalTermFreq(field) / (double)QryEval.READER.getDocCount(field); 
		long doclen = QryEval.dls.getDocLength(field, docid);
		int N = QryEval.READER.numDocs();
		
		HashMap<String, Integer> token_hm = new HashMap<String, Integer>();
		for (int i = 0; i < tokens.length; ++i)
		{
			token_hm.put(tokens[i], 0);
		}
		
		for (int i = 1; i < tv.stemsLength(); ++i)
		{
			if (token_hm.containsKey(tv.stemString(i)))
			{
				int df = tv.stemDf(i);
				int tf = tv.stemFreq(i);
				
				double RSJ_weight = Math.log((double)(N - df + 0.5) / (double)(df + 0.5));
				double tf_weight = tf / ((double)tf + QryEval.letor_k_1 * ((1 - QryEval.letor_b) + 
	            		QryEval.letor_b * doclen / avg_doclen));
				score += RSJ_weight * tf_weight;//user weight is 1, so we omit it
			}
		}
		return score;
	}
	
	
	/**
	 * 
	 * @param docid
	 * @param field
	 * @param tv
	 * @return
	 * @throws IOException
	 */
	public double getIndriScore(int docid, String field, TermVector tv) throws IOException
	{
		double score = 1.0d;
		
		double length_C = QryEval.READER.getSumTotalTermFreq(field);
		double length_d = QryEval.dls.getDocLength(field, docid);
		
		// build a reverse lookup table, which will be used to retrieve the index of a stem
		HashMap<String, Integer> reverseLookup = new HashMap<String, Integer>();
		for (int i = 1; i < tv.stemsLength(); ++i)
		{
			if (!reverseLookup.containsKey(tv.stemString(i)))
				reverseLookup.put(tv.stemString(i), i);
		}
		
		// calculate the score
		int contain_flag = 0;
		for (int i = 0; i < tokens.length; ++i)
		{
			String curr = tokens[i];
			// get ctf
			long ctf = QryEval.READER.totalTermFreq (new Term(field, new BytesRef(curr)));
			// get tf
			int tf = 0;
			if (reverseLookup.containsKey(curr))
			{
				
				int index = reverseLookup.get(curr);
				tf = tv.stemFreq(index);
				contain_flag = 1;
			}
			// smoothing factor
			double p_MLE = ((double)ctf) / ((double)length_C);
			// calculate the score
			double tmp = QryEval.letor_lambda * ((double)tf + QryEval.letor_mu * p_MLE) /
            		((double)length_d + QryEval.letor_mu) + (1 - QryEval.letor_lambda) * p_MLE;
			score *= Math.pow(tmp, 1 / (double)(tokens.length));
		}
		// if the document doesn't contain any token, the score is 0 instead of default score
		if (contain_flag == 0)
			return 0.0;
		else
			return score;
	}
	/**
	 * @param tokens
	 * @param tv
	 * @return
	 */
	public double getOverlapScore(String[] tokens, TermVector tv)
	{
		int cnt = 0;
		// create a hashmap which contains all the stems in tv
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		for (int i = 1; i < tv.stemsLength(); ++i)
		{
			if (!map.containsKey(tv.stemString(i)))
				map.put(tv.stemString(i), 0);
		}
		// find overlap
		for (int i = 0; i < tokens.length; ++i)
		{
			if (map.containsKey(tokens[i]))
				++cnt;
		}
		return (double)cnt / (double)tokens.length;
	}
	
	/**
	 * 
	 * @param url
	 * @return
	 */
	public double getWikiScore(String url)
	{
		if (url == null) return 0.0;
		if (url.toLowerCase().contains("wikipedia.org"))
			return 1.0;
		else
			return 0.0;
	}
}
