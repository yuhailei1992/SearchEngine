import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class WriteParam {

    public static void main(String[] args) {
        FileWriter fileWriter = null;

        try {
            fileWriter = new FileWriter(new File("/Users/Caesar/Documents/workspace/SearchEngineLab1/parameterFile"));

            fileWriter.append("indexPath=/Users/Caesar/Downloads/index" + "\n" + 
                "retrievalAlgorithm=Indri" + "\n" +
                "queryFilePath=/Users/Caesar/Documents/workspace/SearchEngineLab1/queries.txt" + "\n" +
                "trecEvalOutputPath=/Users/Caesar/Documents/workspace/SearchEngineLab1/HW1-queries-UB.teIn" + "\n" +
                "BM25:k_1=1.2" + "\n" +
                "BM25:b=0.75" + "\n" +
                "BM25:k_3=0" + "\n" +
                "Indri:mu=1000" + "\n" +
                "Indri:lambda=0.7" + "\n" +
                "Indri:smoothing=ctf" + "\n" +
                "fb=true " + "\n" +
                "fbDocs=10 " + "\n" +
                "fbTerms=10 " + "\n" +
                "fbMu=" + args[0] + "\n" +
                "fbOrigWeight=0.5" + "\n" +
                "fbExpansionQueryFile=/Users/Caesar/Documents/workspace/SearchEngineLab1/queryExpansion.txt" + "\n"
                //"fbInitialRankingFile=/Users/Caesar/Documents/workspace/SearchEngineLab1/HW4-train-16.fbRank" + "\n"
            );
            
            fileWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
