import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;


public class DataRecon {
    public static void main(String[] args) {
        String delimiter = ",";
        List<List<String>> CSV1 = new ArrayList<>();
        List<List<String>> CSV2 = new ArrayList<>();
        List<List<String>> exceptions = new ArrayList<>();
        String file1 = "";
        String file2 = "";
        List<String> unique_combination = new ArrayList<>();
        String[] col_numbers = new String[1];

        System.out.println("\nPlease ensure that your CSV files are in the same directory as this Data Reconciliation Software \n");
        System.out.println("The CSV files should be under the folder: 'Data Reconciliation Software' \n");

        int tries = 0;
        while(tries<=3){
            System.out.println("Please enter the filename of your first CSV file: ");
            System.out.println("Example: sample_file_1.csv\n");
            Scanner sc = new Scanner(System.in);
            file1 = sc.nextLine();
            if (file1=="" || file1==null){
                System.out.println("Please enter a valid filename.\n");
                tries++;
                sc.close();
                continue;
            }
            
            System.out.println("\nPlease enter the filename of your second CSV file: ");
            System.out.println("Example: sample_file_2.csv\n");
            file2 = sc.nextLine();
            if (file2=="" || file2==null){
                System.out.println("Please enter a valid filename.\n");
                tries++;
                sc.close();
                continue;
            }

            System.out.println("\nPlease enter the column numbers of the first file to be referenced (first column is 0): ");
            System.out.println("The program will extract the column headers and compare it to the correct column in the second file");
            System.out.println("The program expects the numbers to be seperated by commas");
            System.out.println("Example: 0, 3, 4\n");
            col_numbers = sc.nextLine().split(",");
            if (col_numbers.length==0){
                System.out.println("Please enter a column number.\n");
                tries++;
                sc.close();
                continue;
            }


            // close scanner after the file names are obtained
            sc.close();

            // check if the files have .csv extension
            try {
                if (isCSV(file1) && isCSV(file2)){
                    break;
                } else{
                    System.out.println("\nPlease enter a CSV file, other file formats are not supported\n");
                }
            }
            catch(IndexOutOfBoundsException e){
                System.out.println("Please enter the filename with its extension, example: test.csv");
            }
            tries++;
        }

        if (tries>3){
            System.out.println("\nNumber of tries exceeded. Program now exiting.\n");
            return;
        }

        // parse the files into 2d arrays
        try
        {
            CSV1 = parseCSV(file1, delimiter);
        } catch (IOException e)
        {
            System.out.println("Program is unable to read file 1");
            return;
        }

        try {
            CSV2 = parseCSV(file2, delimiter);
        } catch (IOException e) {
            System.out.println("Program is unable to read file 2");
            return;
        }

        unique_combination = generate_unique_combination(CSV1, col_numbers);

        if (!satisfyHeaderRequirements(CSV1, CSV2, unique_combination)){
            System.out.println("The files do not satisfy the header requirements\nExiting now...");
            return;
        }

        List<Integer> indexes1 = getIndicesofUniqueCombination(CSV1, unique_combination);
        List<Integer> indexes2 = getIndicesofUniqueCombination(CSV2, unique_combination);
        List<Integer> headerMap = generate_header_map(CSV1, CSV2);

        exceptions = generate_exceptions(CSV1, CSV2, indexes1, indexes2, headerMap);

        try{
            writeCSVToFile(exceptions);
        }
        catch(IOException e){
            System.out.println("Program is unable to write the exceptions to Exceptions.csv, please ensure that the file is not opened in any other program.");;
        }
        return;

    }
    
    public static boolean isCSV(String filename){
        String file_ext = filename.split("\\.")[1];
        return file_ext.equals("csv");
    }

    public static List<List<String>> parseCSV(String filename, String delimiter) throws IOException{
        List<List<String>> csv = new ArrayList<>();
        String line;
        BufferedReader br = new BufferedReader(new FileReader(filename));
        while ((line = br.readLine()) != null)
        {
            String[] row = line.split(delimiter);
            csv.add(Arrays.asList(row));
        }
        
        br.close();
        return csv;
    }

    public static List<String> generate_unique_combination(List<List<String>> csv, String[] col_numbers){
        // get the list of headers first
        List<String> headers = csv.get(0);
        List<String> unique_combination = new ArrayList<>();
        for (String col : col_numbers){
            unique_combination.add(headers.get(Integer. parseInt(col)));
        }
        return unique_combination;
    }

    public static boolean satisfyHeaderRequirements(List<List<String>> csv1, List<List<String>> csv2, List<String> unique_combination){
        List<String> CSV1Row = csv1.get(0);
        List<String> CSV2Row = csv2.get(0);
        if (!CSV1Row.containsAll(unique_combination)){
            System.out.println("The first CSV file does not contain the necessary headers as specified in your unique combination\n");
            return false;
        } else if (!CSV2Row.containsAll(unique_combination)){
            System.out.println("The second CSV file does not contain the necessary headers as specified in your unique combination\n");
            return false;
        }
        if (!CSV1Row.containsAll(CSV2Row) || !CSV2Row.containsAll(CSV1Row)){
            System.out.println("The 2 CSV files do not contain the same headers\n");
            return false;
        }
        return true;
    }

    // basically the inverse of generateUniqueCombination
    public static List<Integer> getIndicesofUniqueCombination(List<List<String>> csv, List<String> unique_combination){
        List<Integer> indices = new ArrayList<>();
        List<String> csvRow = csv.get(0);
        for (String header : unique_combination) {
            Integer index = csvRow.indexOf(header);
            indices.add(index);
        }
        return indices;
    }

    public static List<Integer> generate_header_map(List<List<String>> csv1, List<List<String>> csv2){
        List<Integer> headerMap = new ArrayList<>();
        List<String> CSV1Row = csv1.get(0);
        List<String> CSV2Row = csv2.get(0);
        for (String header : CSV1Row){
            Integer index = CSV2Row.indexOf(header);
            headerMap.add(index);
        }
        return headerMap;
    }

    public static List<List<String>> generate_exceptions(List<List<String>> csv1, List<List<String>> csv2, List<Integer> indexes1, List<Integer> indexes2, List<Integer> headerMap){
        List<List<String>> exceptions = new ArrayList<>();
        for (int i = 0; i<csv1.size(); i++){

            List<String> CSV1Row = csv1.get(i);

            // add the values of the unique combination into data_combination for search in CSV2
            List<String> data_combination1 = new ArrayList<>();
            for (Integer index: indexes1){
                data_combination1.add(CSV1Row.get(index));
            }

            // iterate through all the rows in CSV2
            for (int j = 0; j < csv2.size(); j++) {

                List<String> CSV2Row = csv2.get(j);
                List<String> data_combination2 = new ArrayList<>();
                for (Integer index: indexes2){
                    data_combination2.add(CSV2Row.get(index));
                }

                // check if the data that corresponds to the unique combination matches
                // if not, move on to the next row
                if (!data_combination1.equals(data_combination2)){
                    continue;
                }

                // check if the row from CSV1 can be found in CSV2 and if all elements match
                // if not, add to exceptions
                for (int k = 0; k < CSV1Row.size(); k++) {
                    String CSV1Cell = CSV1Row.get(k);
                    String CSV2Cell = CSV2Row.get(headerMap.get(k));
                    if (!CSV1Cell.equals(CSV2Cell)){
                        exceptions.add(CSV1Row);
                        List<String> transformed_csv2_row = new ArrayList<>();
                        for (int l = 0; l < CSV2Row.size(); l++) {
                            transformed_csv2_row.add(CSV2Row.get(headerMap.get(l)));
                        }
                        exceptions.add(transformed_csv2_row);
                        break;
                    }
                }
            }
        }

        return exceptions;
    }

    public static void writeCSVToFile(List<List<String>> csv) throws IOException{
        String line;
        BufferedWriter bw = new BufferedWriter(new FileWriter("Exceptions.csv"));
            for (int i = 0; i < csv.size(); i++) {
                List<String> row = csv.get(i);

                StringBuilder str = new StringBuilder("");

                // Traversing the ArrayList
                for (String cell : row) {

                    // Each element in ArrayList is appended
                    // followed by comma
                    str.append(cell).append(",");
                }
                line = str.toString();
                System.out.println(line);
                bw.write(line);
                bw.write("\n");
            }
            bw.close();
    }
}
