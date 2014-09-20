/**
 *  This class implements the document score list data structure
 *  and provides methods for accessing and manipulating them.
 *
 *  Copyright (c) 2014, Carnegie Mellon University.  All Rights Reserved.
 */

import java.io.IOException;
import java.util.*;

import org.apache.lucene.document.Document;

public class ScoreList {

    //  A little utilty class to create a <docid, score> object.

    public static class ScoreListEntry implements Comparable<ScoreListEntry> {
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
        public int compareTo(ScoreListEntry o)
        {
            return(this.externalID.compareTo(o.externalID));
        }
        /*
        final Comparator<ScoreListEntry> BY_SCORE = new Comparator<ScoreListEntry>() {
        	public int compare(ScoreListEntry o1, ScoreListEntry o2) {
                return (o1.score > o2.score) ? 1 : 0; // salary is also positive integer
            }
        };*/
        public static final Comparator<ScoreListEntry> byScore = new Comparator<ScoreListEntry>() {
            @Override
            public int compare(ScoreListEntry o1, ScoreListEntry o2) {
                return o1.score > o2.score ? 0 : 1;
            }
        };
    }

    List<ScoreListEntry> scores = new ArrayList<ScoreListEntry>();

    public void sortScoreListByExternalId () {
        Collections.sort(scores, Collections.reverseOrder());
    }
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

    public void sortScoreListByScore () {
        Collections.sort(scores, ScoreListEntry.byScore);
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
