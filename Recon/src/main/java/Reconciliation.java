import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Reconciliation {

    public Path File1;
    public Path File2;

    String[][] array1;
    String[][] array2;

    List<String> outputArray;
    List<String> outputMismatchArray;
    List<String> outputFile1MissingArray;
    List<String> outputFile2MissingArray;

    public boolean isArgumentsValid(String[] args) {
        try{
            if (args == null){
                System.out.println("Reconciliation System expected 2 file path arguments but received none!" );
                return false;
            }
            //verify number of arguments == 2
            if(args.length != 2){
                System.out.println("Reconciliation System expected 2 file path arguments but received " + args.length + "!" );
                return false;
            }
            //verify file path exists
            boolean file1Exists = doesFileExists(0, args);
            boolean file2Exists = doesFileExists(1, args);

            if( !(file1Exists && file2Exists) ){
                return false;
            }
            else
            {
                File1 = Path.of(args[0]);
                File2 = Path.of(args[1]);
                return true;
            }
        }
        catch (Exception e){
            System.out.println("Caught exception in isArgumentsValid");
            System.out.println("isArgumentsValid arguments:");
            int counter = 0;
            for (String arg_str : args)
            {
                counter++;
                System.out.println(ordinal(counter) + "argument: " + arg_str );
            }
            throw e;
        }


    }

    private boolean doesFileExists(int index, String[] args) {
        Path file = Path.of(args[index]);
        boolean fileExists = Files.exists(file);
        if(!fileExists) {
            System.out.println("File doesn't exist for the " + ordinal(index+1) + " file path argument: " +  args[index] );
        }
        else if(!file.toString().toLowerCase().endsWith(".csv")){
            System.out.println(ordinal(index+1) + " file path argument doesn't end with .csv: " +  args[index] );
            fileExists = false;
        }
        return fileExists;
    }

    public static String ordinal(int i) {
        String[] suffixes = new String[] { "th", "st", "nd", "rd", "th", "th", "th", "th", "th", "th" };
        switch (i % 100) {
            case 11:
            case 12:
            case 13:
                return i + "th";
            default:
                return i + suffixes[i % 10];
        }
    }

    public boolean loadFiles() throws Exception {
        try{
            array1 = loadFileToArray(File1, 1);
            array2 = loadFileToArray(File2, 2);

            return true;

        }
        catch (Exception e){
            System.out.println("Caught exception in loadFiles");
            throw e;
        }

    }

    private String[][] loadFileToArray(Path file, int count) throws Exception {
        try{
            List<String> list1 = new ArrayList<>();
            String[][] array;
            Pattern pattern = Pattern.compile(",\\s*(?=([^\"]*\"[^\"]*\")*[^\"]*$)", Pattern.CASE_INSENSITIVE);
            Matcher matcher;


            try (BufferedReader br = Files.newBufferedReader(file)) {
                list1 = br.lines().map(s -> s.trim()).filter(s -> !s.isBlank()).collect(Collectors.toList());
            } catch (IOException e) {
                System.out.println("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") failed to load!");
                throw e;
            }

            //check if empty
            if(list1.isEmpty())
            {
                System.out.println("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has no content");
                throw new Exception("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has no content");
            }
            else
            {

                array = new String[list1.size()][];
                String[] statusColArray = new String[1];
                statusColArray[0]="unchecked";
                Long matches = null;
                for(int i=0;i<list1.size();i++){
                    matcher = pattern.matcher(list1.get(i));
                    if (matches == null) {
                        matches = matcher.results().count();
                    }
                    else
                    {
                        long new_matches = matcher.results().count();
                        if(matches != new_matches){
                            System.out.println("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has inconsistent commas");
                            throw new Exception("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has inconsistent commas");
                        }
                    }

                    String[] dataArray = list1.get(i).split(",");
                    String[] combinedArray =Stream.of(statusColArray,dataArray).flatMap(Stream::of)
                            .toArray(String[]::new);

                    array[i]=combinedArray;
                }
                //System.out.println(Arrays.deepToString(array1));
                //System.out.println(array1[0][1]);
                System.out.println("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has successfully loaded!");
            }
            return array;
        }
        catch (Exception e){
            System.out.println("Unable to loadFile file "+ String.valueOf(count) + " ("+file.toAbsolutePath()+") into array" );
            throw e;
        }

    }

    public boolean compareFiles() throws Exception {
        try{
            //verify if cols in Array1 and Array2 are the same
            if(array1[0].length != array2[0].length){
                throw new Exception("Inconsistent Number of Columns between files. Number of cols in file 1 ("+File1.toAbsolutePath()+") is "
                        + String.valueOf(array1[0].length-1) + " while number of cols in file 2 ("+File2.toAbsolutePath()+") is "
                        + String.valueOf(array2[0].length-1));

            }

            //currently, assuming there are no duplicates in each of the files
            outputMismatchArray = new ArrayList<>();
            outputFile1MissingArray = new ArrayList<>();
            outputFile2MissingArray = new ArrayList<>();

            for (int i = 0; i < array1.length; i++) {
                //assuming columns are in the same order for the 2 files
                if(array1[i][0] == "unchecked"){

                    for (int j = 0; j < array2.length; j++) {
                        if(array2[j][0] == "unchecked"){

                            if(array1[i][1].equals(array2[j][1]) && array1[i][2].equals(array2[j][2]) && array1[i][3].equals(array2[j][3]) && array1[i][4].equals(array2[j][4])  ){

                                //check if balance is same or not
                                //assumption: string comparison is sufficient
                                if(array1[i][5].equals(array2[j][5])){
                                    //match
                                    array1[i][0]="matched";
                                    array2[j][0]="matched";
                                }
                                else
                                {
                                    //mismatch
                                    array1[i][0]="mismatched";
                                    array2[j][0]="mismatched";
                                    //add to new outputarray
                                    outputMismatchArray.add("mismatched,file1,line"+Integer.toString(i+1)+","+array1[i][1]+","+array1[i][2]+","+array1[i][3]+","+array1[i][4]+","+array1[i][5]);
                                    outputMismatchArray.add("mismatched,file2,line"+Integer.toString(j+1)+","+array2[j][1]+","+array2[j][2]+","+array2[j][3]+","+array2[j][4]+","+array2[j][5]);
                                }

                                //break out of for loop
                                break;
                            }

                        }
                    }

                    if(array1[i][0] == "unchecked"){
                        array1[i][0] = "missing";
                        //add to new outputarray
                        outputFile1MissingArray.add("missing,file1,line"+Integer.toString(i+1)+","+array1[i][1]+","+array1[i][2]+","+array1[i][3]+","+array1[i][4]+","+array1[i][5]);
                    }
                }
            }

            for (int j = 0; j < array2.length; j++) {
                if (array2[j][0] == "unchecked") {
                    array2[j][0] = "missing";
                    //add to new outputarray
                    outputFile2MissingArray.add("missing,file2,line"+Integer.toString(j+1)+","+array2[j][1]+","+array2[j][2]+","+array2[j][3]+","+array2[j][4]+","+array2[j][5]);
                }
            }

            return true;
        }
        catch (Exception e){
            System.out.println("Caught exception in compareFiles");
            throw e;
        }

    }

    public boolean generateOutputFile(String OutputFilePath) throws IOException {
        try{
            outputArray = new ArrayList<>();
            outputArray.addAll(outputMismatchArray);
            outputArray.addAll(outputFile1MissingArray);
            outputArray.addAll(outputFile2MissingArray);

            FileWriter writer = new FileWriter(OutputFilePath);
            for(String line: outputArray) {
                writer.write(line + System.lineSeparator());
            }
            writer.close();

            return true;
        }
        catch (Exception e)
        {
            System.out.println("Caught exception in generateOutputFile");
            throw e;
        }

    }
}
