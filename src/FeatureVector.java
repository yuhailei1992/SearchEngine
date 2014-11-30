import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.util.BytesRef;

public class FeatureVector {
	ArrayList<Double> feature;
	private int FEATURES_NUM = 18;
	boolean mask[];
	HashMap<String, Double> pagerank_map;
	String[] tokens;
	
	/**
	 * @brief: constructor
	 */
	public FeatureVector()
	{
		// initialize enable mask
		mask = new boolean[FEATURES_NUM];
		for (int i = 0; i < FEATURES_NUM; ++i)
			mask[i] = true;
		// initialize feature and pagerank_map
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
		// default_score is the score used for unavailable features
		double default_score = Double.NaN;
		// 0, spam score
		if (mask[0])
		{
			res.add((double)Integer.parseInt(doc.get("score")));
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
		// the pagerank file doesn't necessarily contain a score
		if (mask[3] && pagerank_map.containsKey(QryEval.getExternalDocid(docid)))
		{
			res.add(pagerank_map.get(QryEval.getExternalDocid(docid)));
		}
		else
		{
			res.add(default_score);
		}
		// flag is used to indicate whether the termvector is available
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
			res.add(getBM25Score(docid, "body", tv));
		}
		else
		{
			res.add(default_score);
		}
		
		// 5, indri, body
		if (mask[5] && flag)
		{
			res.add(getIndriScore(docid, "body", tv));
		}
		else
		{
			res.add(default_score);
		}
		
		// 6, term overlap body
		if (mask[6] && flag)
		{
			res.add(getOverlapScore(tokens, tv));
		}
		else
		{
			res.add(default_score);
		}
		// reset tv and flag
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
			res.add(getBM25Score(docid, "title", tv));
		}
		else
		{
			res.add(default_score);
		}
		
		// 8, indri, title
		if (mask[8] && flag)
		{
			res.add(getIndriScore(docid, "title", tv));
		}
		else
		{
			res.add(default_score);
		}
		
		// 9, term overlap title
		if (mask[9] && flag)
		{
			res.add(getOverlapScore(tokens, tv));
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
			res.add(getBM25Score(docid, "url", tv));
		}
		else
		{
			res.add(default_score);
		}
		
		// 11, indri, url
		if (mask[11] && flag)
		{
			res.add(getIndriScore(docid, "url", tv));
		}
		else
		{
			res.add(default_score);
		}
		
		// 12, term overlap url
		if (mask[12] && flag)
		{
			res.add(getOverlapScore(tokens, tv));
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
			res.add(getBM25Score(docid, "inlink", tv));
		}
		else
		{
			res.add(default_score);
		}
		
		// 14, indri, inlink
		if (mask[14] && flag)
		{
			res.add(getIndriScore(docid, "inlink", tv));
		}
		else
		{
			res.add(default_score);
		}
		
		// 15, term overlap inlink
		if (mask[15] && flag)
		{
			res.add(getOverlapScore(tokens, tv));
		}
		else
		{
			res.add(default_score);
		}
		// custom feature 1
		if (mask[16])//TODO
		{
			res.add(1.0);
		}
		else
		{
			res.add(0.0);
		}
		// custom feature 2
		if (mask[17])
		{
			res.add(1.0);
		}
		else
		{
			res.add(0.0);
		}
		// check the number of features
		if (res.size() != FEATURES_NUM)
		{
			throw new Exception("feature number mismatch");
		}
		return res;
	}
	/**
	 * Normalize features
	 * @param ls
	 * @param idx
	 */
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
			if (max == min)
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
			normalizeFeatureVector(ls, i);
		}
		// build the feature vector to be wrote in a file
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
