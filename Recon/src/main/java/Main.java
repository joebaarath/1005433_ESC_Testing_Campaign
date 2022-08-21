import java.io.IOException;
import java.io.InvalidObjectException;

public class Main {

    public static void main(String[] args) throws IOException {
        try {
            System.out.println("Reconciliation System starting...");
            Reconciliation recon = new Reconciliation();
            System.out.println("Verifying if arguments are valid...");
            try {
                if(recon.isArgumentsValid(args) != true){
                    throw new Exception("Reconciliation System exiting due to invalid arguments");
                }
                System.out.println("Verified that arguments are valid");
                System.out.println("File Path 1: " + args[0]);
                System.out.println("File Path 2: " + args[1]);
            }
            catch (Exception e){
                throw e;
            }

            // load file into arrays
            try {
                System.out.println("Loading files...");
                if(recon.loadFiles() != true){
                    throw new Exception("Reconciliation System exiting due to failing to load file(s)");
                }
                else {
                    System.out.println("All files successfully loaded");
                }
            }
            catch (Exception e){
                throw e;
            }


            // compare file
            try {
                System.out.println("Comparing files...");
                if(recon.compareFiles() != true){
                    throw new Exception("Reconciliation System exiting due to failing to compare files");
                }
                else {
                    System.out.println("Files successfully compared");
                }
            }
            catch (Exception e){
                throw e;
            }


            //generate csv
            try {
                System.out.println("Generating output file...");
                String outputFilePath = "output.csv";
                if(recon.generateOutputFile(outputFilePath) != true){
                    throw new Exception("Reconciliation System exiting due to failing to output reconciliation exception files");
                }
                else {
                    System.out.println(outputFilePath +" file successfully generated");
                }
            }
            catch (Exception e){
                throw e;
            }

        }
        catch (Exception e)
        {
            System.out.println("Error Exception Message:");
            System.out.println(e.toString());
            System.out.println("Reconciliation System exiting with error..");
            System.exit(-1);
        }
        finally
        {
            System.out.println("Reconciliation System exiting..");
            System.exit(0);
        }


    }


}



