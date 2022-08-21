import java.text.SimpleDateFormat;
import java.util.*;
import java.io.*;

public final class RecordChecker {
    static final String DUP_COL_ERR = "DUPLICATE COLUMN NAMES";
    static final String COL_MISMATCH = "COLUMN HEADERS MISMATCH";
    static final String COMB_NOT_FOUND = "COMBINATION NOT FOUND IN CSV";
    static final String EMPTY_COMB = "EMPTY COMBINATION";
    static final String BLANK_COLUMN = "COLUMN IS BLANK";
    static final String NULL_COLUMN = "COLUMN IS NULL";

    static final String NO_HEADERS = "NO HEADERS FOUND";
    static final String INSUFFICIENT_COLUMNS = "INSUFFICIENT COLUMNS";

    private RecordChecker() {}

    public static void main(String[] args) {
        System.out.println("Hello campaign world");
        // RecordChecker.print_file("files/sample_file_1.csv");
        RecordChecker.run_interactive();
    }

    public static void run_interactive() {
        /*
        sample_file_1.csv
        sample_file_3.csv
        Customer ID#, Account No., Currency, Type

        sample_file_4.csv
        sample_file_5.csv
        Customer ID#,Account No.,Currency,Type,Balance

        Enter first csv file path: sample_file_1.csv
        Enter second csv file path: sample_file_3.csv
        Enter unique combination (comma separated):
        Customer ID#, Account No., Currency, Type
        Customer ID#, Account No., Type
        */
        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter first csv file path: ");
        String filename1 = scanner.nextLine();
        System.out.print("Enter second csv file path: ");
        String filename2 = scanner.nextLine();

        final String prompt = "Enter unique combination (comma separated): ";
        System.out.print(prompt);
        String combi_input = scanner.nextLine();

        try {
            String export_path = RecordChecker.generate_diffs(
                filename1, filename2, combi_input
            );
            System.out.print("Successfully wrote mismatches to ");
            System.out.println(export_path);
        } catch (Exception e) {
            System.out.println("ERROR ENCOUNTERED");
            e.printStackTrace();
        }
    }

    public static String generate_diffs(
        String filename1, String filename2, String raw_combination
    ) throws BadCombination, BadFileFormat,
        IOException, FilesMismatch
    {
        /*
        this is the main entry function to the program.
        we take in two file names (or file paths), a comma-delimited
        unique combination of columns and it will output a
        csv file containing all the mismatching rows, while returning
        the file path of the exported mismatches csv file

        Things checked for:
            1. everything handled by read_csv
            2. * if the columns differ between files
            (might be too severe / strict)

        TODO:
            1. / intelligently reorder columns in both csv files
            in case they were ordered different from each other
            2. / should balance be treated as a number than a string? NO
            3. / all the values are wrapped in quotes like "12.32"
            should values without quotes be treated like they're equal?
            i.e. "12.32" == 12.32? NO
        */
        String[] combination = parse_combination(raw_combination);
        CsvFile csv_file1 = read_csv(filename1);
        CsvFile csv_file2 = read_csv(filename2);
        // get rows where corresponding entries are mismatched
        ArrayList<String[]> all_mismatch_rows = get_mismatch_rows(
            csv_file1, csv_file2, combination
        );
        /*
        // get rows without corresponding entries in the other file
        ArrayList<String[]> all_nomatch_rows = get_cross_nomatch_rows(
            csv_file1, csv_file2, combination
        );

        all_mismatch_rows.addAll(all_nomatch_rows);
        */
        return RecordChecker.export_mismatches(all_mismatch_rows);
    }

    public static String[] parse_combination(
        String raw_combination_input
    ) throws BadCombination {
        /*
        Given a string containing a bunch of unique columns delimited
        by commas (,), return a string array of the columns.
        Whitespace around column names are trimmed, and whitespace around
        the start and end of the string are ignored as well
        */
        if (raw_combination_input == null) {
            throw new BadCombination(NULL_COLUMN);
        }

        String combination_input = raw_combination_input.trim();
        if (combination_input.endsWith(",")) {
            // example input: "column1, column2,"
            // this was actually caught using the fuzzing unittest
            throw new BadCombination(BLANK_COLUMN);
        } else if (combination_input.startsWith(",")) {
            // example input: ",column1, column2"
            // this was actually caught using the fuzzing unittest
            throw new BadCombination(BLANK_COLUMN);
        }

        String[] combination = combination_input.split(",");
        // remove whitespace around each combination's values
        String[] trim_combination = new String[combination.length];
        for (int k=0; k<combination.length; k++) {
            trim_combination[k] = combination[k].trim();
            if (trim_combination[k].length() == 0) {
                throw new BadCombination(BLANK_COLUMN);
            }
        }

        System.out.print("COMBINATION ");
        System.out.println(Arrays.toString(trim_combination));
        if (!RecordChecker.is_unique_arr(trim_combination)) {
            throw new BadCombination(DUP_COL_ERR);
        }

        return trim_combination;
    }

    public static ArrayList<String[]> get_mismatch_rows(
        CsvFile csv_file1, CsvFile csv_file2, String[] combination
    ) throws BadCombination, FilesMismatch {
        /*
        return rows between both files where the values in the columns
        that are not in the unique combination don't match between
        rows that have the same column values for columns that are
        in the unique combination
        */
        String[] headers1 = csv_file1.get_headers();
        String[] headers2 = csv_file2.get_headers();

        if (!is_unique_arr(headers1)) {
            // header columns for file 1 are not unique
            throw new FilesMismatch(DUP_COL_ERR);
        } else if (!is_unique_arr(headers2)) {
            // header columns for file 2 are not unique
            throw new FilesMismatch(DUP_COL_ERR);
        } else if (!Set.of(headers1).equals(Set.of(headers2))) {
            // set of columns for both files don't match
            throw new FilesMismatch(COL_MISMATCH);
        }

        CsvFile cp_csv_file2 = csv_file2.reorder_columns(headers1);
        String[] ordered_headers2 = cp_csv_file2.get_headers();
        ArrayList<String[]> all_mismatch_rows = new ArrayList<>();

        if (!Arrays.equals(headers1, ordered_headers2)) {
            // header columns between the two files are not unique
            throw new FilesMismatch(COL_MISMATCH);
        } else if (!csv_file1.has_columns(combination)) {
            // unique combination columns aren't found in file
            throw new BadCombination(COMB_NOT_FOUND);
        }

        for (int k=0; k<csv_file1.num_rows(); k++) {
            String[] current_row = csv_file1.get_row(k);
            String[] compare_values = csv_file1.exclude_row_columns(
                k, combination
            );
            String[] column_values = csv_file1.select_row_columns(
                k, combination
            );

            ArrayList<String[]> mismatches = cp_csv_file2.get_mismatch_rows(
                compare_values, combination, column_values
            );

            if (mismatches.size() == 0) { continue; }
            mismatches.add(current_row);
            all_mismatch_rows.addAll(mismatches);
        }

        return all_mismatch_rows;
    }

    public static ArrayList<String[]> get_cross_nomatch_rows(
        CsvFile csv_file1, CsvFile csv_file2, String[] combination
    ) throws BadCombination, FilesMismatch {
        /*
        return rows in both files where each row that is returned
        does not have a corresponding entry in the other csv file
        */
        // get rows file csv file 1 with nothing corresponding in file 2
        ArrayList<String[]> mismatches_file1 = get_nomatch_rows(
            csv_file1, csv_file2, combination
        );
        // get rows file csv file 2 with nothing corresponding in file 1
        ArrayList<String[]> mismatches_file2 = get_nomatch_rows(
            csv_file2, csv_file1, combination
        );

        ArrayList<String[]> all_mismatch_rows = new ArrayList<>();
        all_mismatch_rows.addAll(mismatches_file1);
        all_mismatch_rows.addAll(mismatches_file2);
        return all_mismatch_rows;
    }

    public static ArrayList<String[]> get_nomatch_rows(
        CsvFile csv_file1, CsvFile csv_file2, String[] combination
    ) throws BadCombination, FilesMismatch {
        /*
        return rows in csv file 1 where each row that is returned
        does not have a corresponding entry in the csv file 2
        */
        String[] headers1 = csv_file1.get_headers();
        String[] headers2 = csv_file2.get_headers();

        if (!is_unique_arr(headers1)) {
            // header columns for file 1 are not unique
            throw new FilesMismatch(DUP_COL_ERR);
        } else if (!is_unique_arr(headers2)) {
            // header columns for file 2 are not unique
            throw new FilesMismatch(DUP_COL_ERR);
        } else if (!Set.of(headers1).equals(Set.of(headers2))) {
            // set of columns for both files don't match
            throw new FilesMismatch(COL_MISMATCH);
        }

        CsvFile cp_csv_file2 = csv_file2.reorder_columns(headers1);
        String[] ordered_headers2 = cp_csv_file2.get_headers();
        ArrayList<String[]> all_mismatch_rows = new ArrayList<>();

        if (!Arrays.equals(headers1, ordered_headers2)) {
            // header columns between the two files are not unique
            throw new FilesMismatch(COL_MISMATCH);
        } else if (!csv_file1.has_columns(combination)) {
            // unique combination columns aren't found in file
            throw new BadCombination(COMB_NOT_FOUND);
        }

        // get all rows in csv file 1 with no corresponding
        // rows in csv file 2 (matched based on combination)
        for (int k=0; k<csv_file1.num_rows(); k++) {
            String[] current_row = csv_file1.get_row(k);
            String[] column_values = csv_file1.select_row_columns(
                k, combination
            );

            ArrayList<String[]> matches = cp_csv_file2.search_row_matches(
                combination, column_values
            );

            // add current row as a mismatch if there are no
            // corresponding rows in the other file
            if (matches.size() == 0) {
                all_mismatch_rows.add(current_row);
            }
        }

        return all_mismatch_rows;
    }

    public static String export_mismatches(
        ArrayList<String[]> all_mismatch_rows
    ) throws IOException {
        String pattern = "yyMMdd-HHmmss";
        Date date_now = new java.util.Date();
        String stamp = new SimpleDateFormat(pattern).format(date_now);
        String dirname = "mismatches";

        File directory = new File(dirname);
        if (!directory.exists()) {
            System.out.println("created directory " + dirname);
            boolean dir_created = directory.mkdir();
            if (!dir_created) {
                throw new IOException(
                    "FAILED TO MAKE OUTPUT DIRECTORY"
                );
            }
        }

        String filename = "mismatches-" + stamp + ".csv";
        String export_path = dirname + "/" + filename;

        try {
            FileWriter writer = new FileWriter(export_path);
            // columns are not written in example output
            // String header_line = String.join(",", headers);
            // writer.write(header_line);
            // writer.write("\n");

            for (String[] row : all_mismatch_rows) {
                String line = String.join(",", row);
                writer.write(line);
                writer.write("\n");
            }

            writer.close();

        } catch (IOException e) {
            System.out.println("File write error occurred.");
            e.printStackTrace();
            throw new IOException("FILE WRITE FAILED");
        }

        return export_path;
    }

    public static boolean is_unique_arr(String[] arr) {
        /*
        return true if all the elements in the
        array are unique, return false otherwise
        null is considered to not be a unique array,
        because it's not even an array to begin with
        */
        if (arr == null) { return false; }
        String[] copied_arr = new String[arr.length];
        System.arraycopy(arr, 0, copied_arr, 0, arr.length);
        Arrays.sort(copied_arr);

        // check for duplicate columns in data
        for (int k=1; k<copied_arr.length; k++) {
            if (copied_arr[k-1].equals(copied_arr[k])) {
                return false;
            }
        }

        return true;
    }

    public static void print_file(String filename) {
        /*
        parse the csv file and print out every row of the
        parsed csv file
        */
        RecordChecker checker = new RecordChecker();
        CsvFile csv_data;

        try {
            csv_data = read_csv(filename);
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        csv_data.print_file();
    }

    public static CsvFile read_csv(
        String filename
    ) throws IOException, BadFileFormat {
        return read_csv(filename, null);
    }

    public static CsvFile read_csv(
        String raw_filename, String[] inject_headers
    ) throws IOException, BadFileFormat {
        /*
        Given a filename (or file path), this method returns
        an ArrayList of String Arrays, where each string array
        is a row of csv values, and each element in the string
        array is a single column value of the column, row 0 should
        represent a list of headers

        the header columns is set to inject_headers if it's not null,
        else its set to the first row of the file we're reading

        things checked for:
            1. columns mismatch within file
            2. file not having any rows
            3. file not having at least 2 rows (including headers)
            4. header columns not being unique
        */
        String split_by = ",";
        String filename = raw_filename.trim();

        //parsing a CSV file into BufferedReader class constructor
        FileReader file_obj = new FileReader(filename);
        BufferedReader br = new BufferedReader(file_obj);
        ArrayList<String[]> csv_data = new ArrayList<>();

        String[] headers = null;
        if (inject_headers != null) {
            headers = inject_headers.clone();
            csv_data.add(headers);
        }

        while (true) {
            String raw_line = br.readLine();
            if (raw_line == null) {
                break;
            }

            String line = raw_line.trim();
            if (csv_data.size() == 0) {
                assert(headers == null);
                headers = line.split(split_by);
                csv_data.add(headers);
                continue;
            }

            String[] columns = line.split(split_by);
            if (columns.length != headers.length) {
                // this row has more columns than were in the headers row
                throw new BadFileFormat(COL_MISMATCH);
            }
            csv_data.add(columns);
        }

        if (headers == null) {
            throw new BadFileFormat(NO_HEADERS);
        } else if (headers.length < 1) {
            // We need to have one column at the least
            throw new BadFileFormat(INSUFFICIENT_COLUMNS);
        }

        System.out.println("HEADERS");
        System.out.println(Arrays.toString(headers));

        // Convert String Array to List
        List<String> headers_list = Arrays.asList(headers);
        if (!RecordChecker.is_unique_arr(headers)) {
            throw new BadFileFormat(DUP_COL_ERR);
        }

        return new CsvFile(csv_data);
    }
}
