import java.io.*;
import java.util.*;
import java.io.IOException;

public class ParseResult {

    public static void main(String[] args) {

        try {
            String writer_filename = "/Users/Caesar/Documents/workspace/SearchEngineLab1/src/results/fbMu" + args[0];
            BufferedWriter writer = null;
            writer = new BufferedWriter(new FileWriter(new File(writer_filename)));
            Scanner queryScanner = new Scanner(new File("/Users/Caesar/Documents/workspace/SearchEngineLab1/src/results/result.txt"));
            String queryLine = null;
            do {
                queryLine = queryScanner.nextLine();
                String[] parts = queryLine.split("\\s+");
                if (parts[0].equals("map") || parts[0].equals("P10") || 
                    parts[0].equals("P20") || parts[0].equals("P30")) {
                    writer.write(parts[0]);
                    writer.write("\t");
                    writer.write(parts[2]);
                    writer.write("\n");
                }
            } while (queryScanner.hasNext());
        
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
