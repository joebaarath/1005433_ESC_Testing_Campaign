import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class Reconciliation {

    public Path File1;
    public Path File2;

    String[][] array1;
    String[][] array2;
    Boolean hasHeaders = true;
    Boolean hasOutputHeaders = true;
    Boolean addHelperColumnsAtTheEnd = true;
    List<String> outputArray;
    List<String> outputDuplicateArray;
    List<String> outputMismatchArray;
    List<String> outputFile1MissingArray;
    List<String> outputFile2MissingArray;
    ArrayList valueColsIndexes;
    ArrayList tempStr_valueColsIndexes= new ArrayList<>();


    public boolean isArgumentsValid(String[] args) throws Exception {
        try{
            if (args == null){
                System.out.println("Reconciliation System expected 2 file path arguments but received none!" );
                throw new Exception("Reconciliation System expected 2 file path arguments but received none!" );
//                return false;
            }
            //verify number of arguments == 2
            if(args.length < 2){
                System.out.println("Reconciliation System expected 2 file path arguments but received " + args.length + "!" );
                throw new Exception("Reconciliation System expected 2 file path arguments but received " + args.length + "!");
//                return false;
            }

            if(((args.length >= 3) && (args.length < 5)) || args.length > 5)  {
                throw new Exception("Reconciliation System received " + args.length + " arguments when 2 or 5 arguments expected ! Please refer to github page for syntax.");
            }

            if(args.length >= 3){
                //check if arguments are valid
                //arg3 == list of cols to check value for
                verifyAndSetValueColumns(args[2]);
            }

            if(args.length >= 4){
                //check if arguments are valid
                // arg4 == set headers
                verifyAndSetInputHeaders(args[3]);
            }

            if(args.length == 5){
                //check if arguments are valid
                // arg4 == set headers
                verifyAndSetOutputHeaders(args[4]);
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

    private void verifyAndSetValueColumns(String arg) throws Exception {
        tempStr_valueColsIndexes= new ArrayList<>();
        //contains comma
        if (arg.toLowerCase().contains(",")) {
            tempStr_valueColsIndexes = new ArrayList<String>(Arrays.asList(arg.replaceAll("\\s","").split(",")));
        }
        else {
            tempStr_valueColsIndexes.add(arg.replaceAll("\\s",""));
        }

        //check if all are valid int
        for (int counter = 0; counter < tempStr_valueColsIndexes.size(); counter++) {
            try{
                Integer.parseInt( (String) tempStr_valueColsIndexes.get(counter));
            }
            catch (NumberFormatException e){
                System.out.println("Input String cannot be parsed to Integer. Input String value:"+arg);
                throw new Exception("Invalid 3rd argument for Value Columns passed. Argument did not contain numeric value/s. 3rd argument received was: " + arg);
            }
        }

    }

    private void setValueColumnsIndexes(int size_of_data_cols) throws Exception {
        //check if all are valid int
        valueColsIndexes = new ArrayList<>();
        if(tempStr_valueColsIndexes.size() == 0){
            tempStr_valueColsIndexes.add("-1");
        }
        for (int counter = 0; counter < tempStr_valueColsIndexes.size(); counter++) {
            try{
                Integer myIndexInt = Integer.parseInt( (String) tempStr_valueColsIndexes.get(counter));
                if(myIndexInt>=size_of_data_cols){
                    throw new Exception("ValueColIndex with value of "+ myIndexInt + ", is beyond last valid ValueColIndex of " + String.valueOf(size_of_data_cols-1) );
                }
                if(myIndexInt<0){
                    myIndexInt = myIndexInt + size_of_data_cols;
                }
                if(!valueColsIndexes.contains(myIndexInt)){
                    valueColsIndexes.add(myIndexInt);
                }
            }
            catch (NumberFormatException e){
                throw new Exception("Invalid myIndexInt Value:" + String.valueOf((String) tempStr_valueColsIndexes.get(counter)));
            }
        }

        Collections.sort(valueColsIndexes);
        System.out.println("Size of data columns:" + String.valueOf(size_of_data_cols));
        System.out.println("ValueColIndexes:" + String.valueOf(valueColsIndexes));

        if(valueColsIndexes.size() >= size_of_data_cols){
            throw new Exception("Can't set all available columns as ValueCols. No unique identifier columns left.");
        }



    }

    private void verifyAndSetInputHeaders(String arg) throws Exception {
        arg = arg.toLowerCase();
        if ( arg.equals("inputheader") ) {
            hasHeaders = true;
        }
        else if ( arg.equals("inputnoheader") ) {
            hasHeaders = false;
        }
        else{
            throw new Exception("Invalid 4th argument for inputheader/inputnoheader passed. 4th argument recieved was: " + arg);
        }
    }

    private void verifyAndSetOutputHeaders(String arg) throws Exception {
        arg = arg.toLowerCase();
        if ( arg.equals("outputheader") ) {
            hasOutputHeaders = true;
        }
        else if ( arg.equals("outputnoheader") ) {
            hasOutputHeaders = false;
        }
        else{
            throw new Exception("Invalid 5th argument for outputheader/outputnoheader passed. 5th argument recieved was: " + arg);
        }
    }

    private String getCommaSeperatedStringFromArray(String[] arr1, int start_index,int end_index){
        String concat_str = "";
        for(int i=start_index;i<=end_index;i++){
            concat_str+=","+String.valueOf(arr1[i]);
        }
        return concat_str;
    }

    private boolean checkArrayStringMatch(String[] arr1, String[] arr2,int start_index,int end_index, boolean use_valueColsIndexes){


        for(int i=start_index;i<=end_index;i++){

            int temp_array_index = i-start_index;
            if (use_valueColsIndexes == true)
            {
                if(valueColsIndexes.contains(temp_array_index))
                {
                    continue;
                }
            }

            if(!arr1[i].equals(arr2[i])){
                return false;
            }


        }
        return true;
    }


    private boolean doesFileExists(int index, String[] args) throws Exception {
        Path file = Path.of(args[index]);
        boolean fileExists = Files.exists(file);
        if(!fileExists) {
            System.out.println("File doesn't exist for the " + ordinal(index+1) + " file path ("+file.toAbsolutePath()+") argument: " +  args[index] );
            throw new Exception("File doesn't exist for the " + ordinal(index+1) + " file path ("+file.toAbsolutePath()+") argument: " +  args[index] );
        }
        else if(!file.toString().toLowerCase().endsWith(".csv")){
            System.out.println(ordinal(index+1) + " file path ("+file.toAbsolutePath()+") argument doesn't end with .csv: " +  args[index] );
            fileExists = false;
            throw new Exception(ordinal(index+1) + " file path ("+file.toAbsolutePath()+") argument doesn't end with .csv: " +  args[index] );
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
            //checking for comma consistency


            //count commas not within double quotes
            //Pattern pattern = Pattern.compile(",\\s*(?=([^\"]*\"[^\"]*\")*[^\"]*$)", Pattern.CASE_INSENSITIVE);
            Matcher matcher;


            try (BufferedReader br = Files.newBufferedReader(file)) {
                list1 = br.lines().map(s -> s.trim().replaceAll("\"","")).filter(s -> !s.isBlank()).collect(Collectors.toList());
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
                String[] statusColArray = new String[2];
                statusColArray[0]="unchecked";
                Pattern pattern = Pattern.compile(",", Pattern.CASE_INSENSITIVE);
                Long matches = null;
                Integer countOfCommas = null;
                for(int i=0;i<list1.size();i++){
                    statusColArray[1]=String.valueOf(i+1);
                    //checking for comma consistency
                    String myString = list1.get(i);
                    matcher = pattern.matcher(list1.get(i));
                    if (matches == null) {
                        matches = matcher.results().count();
                    }
                    else
                    {
                        long new_matches = matcher.results().count();
                        if(matches != new_matches){
                            System.out.println("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has inconsistent commas in line " + String.valueOf(i) + " (" + String.valueOf(matches) + " commas) against line " + String.valueOf(i+1) + " (" + String.valueOf(new_matches) + " commas)");
                            throw new Exception("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has inconsistent commas in line " + String.valueOf(i) + " (" + String.valueOf(matches) + " commas) against line " + String.valueOf(i+1) + " (" + String.valueOf(new_matches) + " commas)");
                        }
                    }
//                    if (countOfCommas == null) {
//                        //count just commas
////                        countOfCommas = getCountOfCommas(myString);
//                    }
//                    else
//                    {
//                        long new_countOfCommas = getCountOfCommas(myString);
//                        if(countOfCommas != new_countOfCommas){
//                            System.out.println("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has inconsistent commas");
//                            throw new Exception("File "+String.valueOf(count)+" ("+file.toAbsolutePath()+") has inconsistent commas");
//                        }
//                    }

//                    String[] dataArray = list1.get(i).replaceAll("\"", "").split(",");
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

    private Integer getCountOfCommas(String myString) {
        Integer countOfCommas;
        countOfCommas = 0;
        List<String> tokens = new ArrayList<String>();
        int startPosition = 0;
        boolean isInQuotes = false;
        for (int currentPosition = 0; currentPosition < myString.length(); currentPosition++) {
            if (myString.charAt(currentPosition) == '\"') {
                isInQuotes = !isInQuotes;
            }
            else if (myString.charAt(currentPosition) == ',' && !isInQuotes) {
                tokens.add(myString.substring(startPosition, currentPosition));
                startPosition = currentPosition + 1;
            }
        }

        String lastToken = myString.substring(startPosition);
        if (lastToken.equals(",")) {
            tokens.add("");
        } else {
            tokens.add(lastToken);
        }
        countOfCommas = tokens.size();
        return countOfCommas;
    }

    public boolean compareFiles() throws Exception {
        try{
            //verify if cols in Array1 and Array2 are the same
            if(array1[0].length != array2[0].length){
                throw new Exception("Inconsistent Number of Columns between files. Number of cols in file 1 ("+File1.toAbsolutePath()+") is "
                        + String.valueOf(array1[0].length-2) + " while number of cols in file 2 ("+File2.toAbsolutePath()+") is "
                        + String.valueOf(array2[0].length-2));

            }


            setValueColumnsIndexes(array1[0].length-2);

            //currently, assuming there are no duplicates in each of the files
            outputMismatchArray = new ArrayList<>();
            outputFile1MissingArray = new ArrayList<>();
            outputFile2MissingArray = new ArrayList<>();

            //get all duplicates
            outputDuplicateArray = new ArrayList<>();

            ArrayList array1_duplicates_list = checkAndMarkDuplicates(array1);
            ArrayList array1_duplicates = (ArrayList) array1_duplicates_list.get(0);
            ArrayList array1_duplicates_unique_identifiers = (ArrayList) array1_duplicates_list.get(1);

            ArrayList array2_duplicates_list = checkAndMarkDuplicates(array2);
            ArrayList array2_duplicates = (ArrayList) array2_duplicates_list.get(0);
            ArrayList array2_duplicates_unique_identifiers = (ArrayList) array2_duplicates_list.get(1);

            checkAndMarkDuplicatesAcrossArrays(array2, array1_duplicates_unique_identifiers, array2_duplicates);
            checkAndMarkDuplicatesAcrossArrays(array1, array2_duplicates_unique_identifiers, array1_duplicates);

            mergeDuplicateArrays(array1_duplicates, 1, File1);
            mergeDuplicateArrays(array2_duplicates, 2, File2);

            //if input has header check and see if the same
            if (hasHeaders) {
                //see if headers match
                if(!checkArrayStringMatch(array1[0],array2[0],0,array1[0].length-1, false)){
                    throw new Exception("Headers expected in Input CSV files, but the 2 Input CSV files' headers do not match.");
                }
            }

            for (int i = 0; i < array1.length; i++) {
                //assuming columns are in the same order for the 2 files
                if(array1[i][0] == "unchecked"){

                    for (int j = 0; j < array2.length; j++) {
                        if(array2[j][0] == "unchecked"){

                            if(checkArrayStringMatch(array1[i],array2[j],2,array1[0].length-1, true)){

                                //check if balance is same or not
                                //assumption: string comparison is sufficient

                                //use valueColsIndexes
                                String mismatched_err_msg = "";
                                for (int x=0; x<valueColsIndexes.size(); x++)
                                {
                                    Integer valueColsIndexes_val = (Integer)valueColsIndexes.get(x);
                                    if(array1[i][valueColsIndexes_val+2].equals(array2[j][valueColsIndexes_val+2])){
                                        //match
                                        if(!array1[i][0].equals("mismatched")) {
                                            array1[i][0]="matched";
                                            array2[j][0]="matched";
                                        }
                                    }
                                    else
                                    {
                                        String temp_col_name = "Column "+String.valueOf(valueColsIndexes_val+1);
                                        if(hasHeaders == true){
                                            temp_col_name=array1[0][valueColsIndexes_val+2].replaceAll("\"","");
                                        }

                                        //mismatch
                                        if(array1[i][0].equals("matched") || array1[i][0].equals("unchecked")) {
                                            array1[i][0]="mismatched";
                                            array2[j][0]="mismatched";
                                            mismatched_err_msg=temp_col_name+" mismatched";
                                        }
                                        else{
                                            mismatched_err_msg+=";"+temp_col_name+" mismatched";
                                        }

                                    }

                                }

                                if(array1[i][0].equals("mismatched"))
                                {
                                    //add to new outputarray
                                    outputMismatchArray.add(mismatched_err_msg+",file1,"+File1.toAbsolutePath().toString()+getCommaSeperatedStringFromArray(array1[i],1,array1[i].length-1));
                                    outputMismatchArray.add(mismatched_err_msg+",file2,"+File2.toAbsolutePath().toString()+getCommaSeperatedStringFromArray(array2[j],1,array2[j].length-1));

                                }

                                //break out of for loop
                                break;

                            }

                        }
                    }

                    if(array1[i][0] == "unchecked"){
                        array1[i][0] = "missing";
                        //add to new outputarray
                        outputFile1MissingArray.add("Exists in file1 but missing in file2,file1,"+File1.toAbsolutePath().toString()+getCommaSeperatedStringFromArray(array1[i],1,array1[i].length-1));
                    }
                }
            }

            for (int j = 0; j < array2.length; j++) {
                if (array2[j][0] == "unchecked") {
                    array2[j][0] = "missing";
                    //add to new outputarray
                    outputFile2MissingArray.add("Exists in file2 but missing in file1,file2,"+File2.toAbsolutePath().toString()+getCommaSeperatedStringFromArray(array2[j],1,array2[j].length-1));
                }
            }


            System.out.println("Number of Duplicates Detected: " + String.valueOf(outputDuplicateArray.size()));
            System.out.println("Number of Mismatches Detected: " + String.valueOf(outputMismatchArray.size()));
            System.out.println("Number of Records in File 1 but Missing from File 2 Detected: " + String.valueOf(outputFile1MissingArray.size()));
            System.out.println("Number of Records in File 2 but Missing from File 1 Detected: " + String.valueOf(outputFile2MissingArray.size()));

            return true;
        }
        catch (Exception e){
            System.out.println("Caught exception in compareFiles");
            throw e;
        }

    }

    private void mergeDuplicateArrays(ArrayList array1_duplicates, int fileCount, Path File1 ) {
//        array1_duplicates.sort(Comparator.comparing(o -> o[1]));

        for (int i = 0; i < array1_duplicates.size(); i++) {
            String[] array1_duplicates_arr = (String[]) array1_duplicates.get(i);
            outputDuplicateArray.add("Ambiguous duplicate identifier,file"+String.valueOf(fileCount)+","+File1.toAbsolutePath().toString()+getCommaSeperatedStringFromArray(array1_duplicates_arr,1,array1_duplicates_arr.length-1));
        }
    }

    private void checkAndMarkDuplicatesAcrossArrays(String array_2[][],ArrayList array_1_duplicates_unique_identifiers, ArrayList array_2_duplicates) {
        for (int j = 0; j < array_2.length; j++) {
            if (array_2[j][0] == "unchecked") {
                for (int i = 0; i < array_1_duplicates_unique_identifiers.size(); i++) {
                    String [] array1_duplicates_unique_identifiers_arr = (String[]) array_1_duplicates_unique_identifiers.get(i);
                    if(checkArrayStringMatch(array1_duplicates_unique_identifiers_arr,array_2[j],2,array_2[0].length-1, true)){
                        array_2[j][0] = "duplicate";
                        array_2_duplicates.add(array_2[j]);
                    }
                }
            }
        }
    }

    private ArrayList checkAndMarkDuplicates(String[][] array1) {
        ArrayList myList = new ArrayList<>();
        ArrayList array1_list_dup = new ArrayList<>();
        ArrayList array1_list_dup_unique_identifier = new ArrayList<>();
        for (int i = 0; i < array1.length; i++) {
            //assuming columns are in the same order for the 2 files
            if (array1[i][0] == "unchecked") {
                for (int i_dup = 0; i_dup < array1.length; i_dup++) {
                    //assuming columns are in the same order for the 2 files
                    if (i != i_dup && array1[i_dup][0] == "unchecked") {
                        if(checkArrayStringMatch(array1[i],array1[i_dup],2,array1[0].length-1, true)){
                            if (array1[i][0] == "unchecked") {
                                array1[i][0] = "duplicate";
                                array1_list_dup.add(array1[i]);
                                array1_list_dup_unique_identifier.add(array1[i]);
                            }
                            array1[i_dup][0] = "duplicate";
                            array1_list_dup.add(array1[i_dup]);
                        }
                    }
                }
            }
        }
        myList.add(array1_list_dup);
        myList.add(array1_list_dup_unique_identifier);
        return myList;
    }

    public boolean generateOutputFile(String OutputFilePath) throws IOException  {
        try{
            outputArray = new ArrayList<>();
            List<String>  formattedOutputArray = new ArrayList<>();
            String colString = "";
            if (hasOutputHeaders == true) {
                if(hasHeaders == false){
                    for (int i=1; i<array1[0].length-1; i++){
                        colString+=",Column"+i;
                    }
                }
                else{
                    for (int i=2; i<array1[0].length; i++){
                        colString+="," + array1[0][i];
                    }
//                colString=colString.substring(1);

                }
                outputArray.add("MismatchType,File,FilePath,LineNumber"+colString);
            }

            outputArray.addAll(outputDuplicateArray);
            outputArray.addAll(outputMismatchArray);
            outputArray.addAll(outputFile1MissingArray);
            outputArray.addAll(outputFile2MissingArray);


            if (addHelperColumnsAtTheEnd == true){
                //Rearrange
                Matcher matcher;
                Pattern pattern = Pattern.compile("(^(?:[^,]*,){4})", Pattern.CASE_INSENSITIVE);

                for (int l = 0; l < outputArray.size(); l++) {
                    //use regex to get the first character all the way to the fourth comma
                    // remove last character, add comma in the front of the string
                    //merge string
                    String myFullString =  outputArray.get(l);
                    String subStr = "";
                    matcher = pattern.matcher(myFullString);
                    while (matcher.find()) {
                        subStr = matcher.group(1);
                    }
                    myFullString =myFullString.substring(subStr.length());
                    myFullString+= "," + subStr.substring(0, subStr.length() - 1);
                    formattedOutputArray.add(myFullString);
                }
            }
            else
            {
                formattedOutputArray = outputArray;
            }


            FileWriter writer = new FileWriter(OutputFilePath);
            for(String line: formattedOutputArray) {
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
