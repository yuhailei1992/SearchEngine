/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.util.*;

import org.apache.lucene.document.Document;

public class ScoreList {

	/**
	 * ScoreList contains three fields: docid, score, externalID
	 * @author haileiy
	 *
	 */
    public static class ScoreListEntry {
        public int docid;//changed to public by haileiy
        public double score;
        public String externalID;
        private ScoreListEntry(int docid, double score) {
            this.docid = docid;
            this.score = score;
        }
        public void setExternalID (String extID) {
            this.externalID = extID;
        }
    }
    
    Comparator<ScoreListEntry> BY_SCORE = new Comparator<ScoreListEntry> () {
		public int compare(ScoreListEntry s1, ScoreListEntry s2) {
			if (s1.score == s2.score) {//rank by id
				return s1.externalID.compareTo(s2.externalID);
			}
			else if (s1.score > s2.score) return -1;
			else return 1;
		}
	};

    List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();
    //public int ctf;
    //public String field;
    
    /**
     *  Append a document score to a score list.
     *  @param docid An internal document id.
     *  @param score The document's score.
     *  @return void
     */
    public void add(int docid, double score) {
        scores.add(new ScoreListEntry(docid, score));
    }

    public void removeLast() {
        ScoreListEntry toRemove = scores.get(scores.size() - 1);
        scores.remove(toRemove);
    }

    public ScoreListEntry getLast() {
        return scores.get(scores.size() - 1);
    }

    /**
     *  Get the n'th document id.
     *  @param n The index of the requested document.
     *  @return The internal document id.
     */
    public int getDocid(int n) {
        return this.scores.get(n).docid;
    }
    
    /** Sort the results by score
     * When there is a tie, sort by externalID
     */
    public void sortByScore() {
    	Collections.sort(this.scores, this.BY_SCORE);
    }
    /**
     *  Get the score of the n'th document.
     *  @param n The index of the requested document score.
     *  @return The document's score.
     */
    public double getDocidScore(int n) {
        return this.scores.get(n).score;
    }
    /**
     * print the scorelist
     */
    public void printScoreList () {
        System.out.println("======This is list =====");
        for (ScoreListEntry p : scores) {
            System.out.println(p.docid + p.externalID);
        }
    }
}
