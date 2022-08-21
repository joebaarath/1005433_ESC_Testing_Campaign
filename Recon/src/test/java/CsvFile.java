import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class CsvFile {
    /*
    wrapper class to hold the csv data with convenient utilities
    for accessing the csv file data. Note that I do not claim to offer
    nor intend to offer comprehensive error checks on the data here
    because I plan to do so in the RecordChecker methods that parse
    the raw csv data before dumping them into a CsvFile instance
    */
    private ArrayList<String[]> raw_data;

    public CsvFile(ArrayList<String[]> raw_data) {
        assert raw_data.size() >= 1;
        // String[] headers = raw_data.get(0);
        // List<String> headers_list = Arrays.asList(headers);
        this.raw_data = raw_data;
    }

    public CsvFile reorder_columns(String[] new_columns) {
        CsvFile new_file = this.clone();
        new_file.reorder_columns_inplace(new_columns);
        return new_file;
    }

    @Override
    public CsvFile clone() {
        ArrayList<String[]> new_rows = new ArrayList<>();
        for (String[] raw_datum : this.raw_data) {
            new_rows.add(raw_datum.clone());
        }

        return new CsvFile(new_rows);
    }

    public ArrayList<String[]> copy_rows() {
        ArrayList<String[]> new_rows = new ArrayList<>();
        for (int k=0; k<this.num_rows(); k++) {
            new_rows.add(this.get_row(k).clone());
        }
        return new_rows;
    }

    public void scramble_columns_inplace() {
        // scramble the columns in the headers and in the data
        // inplace. Only used by junit test methods
        ArrayList<String> headers = new ArrayList<>(
                Arrays.asList(this.get_headers())
        );

        Collections.shuffle(headers);
        String[] reordered = headers.toArray(new String[0]);
        this.reorder_columns_inplace(reordered);
    }

    public void reorder_columns_inplace(String[] new_columns) {
        /*
        Given an array of columns in a certain order,
        reorder our entire csv file to the specified column ordering
        (both header columns and row data columns are reordered)

        This is used by unit tests for csv file generation, but
        is not actually used in the actual program code
        */
        String[] column_names = this.get_headers();
        assert(column_names.length == new_columns.length);
        assert(Set.of(new_columns).equals(Set.of(column_names)));

        int[] column_indexes = get_column_indexes(new_columns);
        ArrayList<String[]> raw_data = new ArrayList<>();
        String[] reorder_column_names = new String[column_names.length];
        for (int k=0; k<column_names.length; k++) {
            reorder_column_names[k] = column_names[column_indexes[k]];
        }

        // add reordered columns to new raw data
        raw_data.add(reorder_column_names);
        for (int k=0; k<this.num_rows(); k++) {
            String[] row = this.get_row(k);
            String[] reordered_row = new String[row.length];

            for (int i=0; i<row.length; i++) {
                reordered_row[i] = row[column_indexes[i]];
            }

            raw_data.add(reordered_row);
        }

        this.raw_data = raw_data;
    }

    public void export_csv(String export_path) throws IOException {
        FileWriter writer = new FileWriter(export_path);
        for (int k=0; k<this.raw_data.size(); k++) {
            String[] row = this.raw_data.get(k);
            String line = String.join(",", row);

            writer.write(line);
            if (k < this.raw_data.size() - 1) {
                writer.write('\n');
            }
        }

        writer.close();
    }

    public int get_column_index(String column_name) {
        String[] headers = raw_data.get(0);
        // System.out.print("I-HEADERS ");
        // System.out.println(Arrays.toString(headers));
        // System.out.println(column_name);

        for (int k=0; k<headers.length; k++) {
            /*
            System.out.print(k);
            System.out.print(" -- ");
            System.out.print(headers[k]);
            System.out.print(" -- |");
            System.out.print(column_name);
            System.out.print("| -- ");
            System.out.println(headers[k].charAt(0));
            */

            if (headers[k].equals(column_name)) {
                return k;
            }
        }
        return -1;
    }

    public boolean has_columns(String[] columns) {
        int[] column_indexes = this.get_column_indexes(columns);
        for (int column_index : column_indexes) {
            if (column_index == -1) { return false; }
        }
        return true;
    }

    public int[] get_column_indexes(String[] columns) {
        int[] column_indexes = new int[columns.length];
        for (int k=0; k<columns.length; k++) {
            String column = columns[k];
            int column_index = this.get_column_index(column);
            column_indexes[k] = column_index;
        }

        return column_indexes;
    }

    public String[] select_row_columns(int index, String[] columns) {
        // get columns values corresponding to the columns passed in
        // for the row at index
        int[] column_indexes = this.get_column_indexes(columns);
        String[] column_values = new String[columns.length];
        String[] row = this.get_row(index);

        for (int k=0; k<column_values.length; k++) {
            column_values[k] = row[column_indexes[k]];
        }

        assert column_indexes.length == column_values.length;
        return column_values;
    }

    public String[] exclude_row_columns(int index, String[] columns) {
        // get columns values for columns whose names are NOT the
        // names of the columns passed in, for the row at index
        String[] row = this.get_row(index);
        int[] int_indexes = this.get_column_indexes(columns);
        Integer[] integer_indexes = Arrays.stream(int_indexes)
                .boxed().toArray(Integer[]::new);

        List<Integer> indexes = Arrays.asList(integer_indexes);
        ArrayList<String> excluded_values = new ArrayList<>();

        for (int k=0; k<this.num_columns(); k++) {
            if (indexes.contains(k)) { continue; }
            excluded_values.add(row[k]);
        }

        // dump the arraylist back into an array
        String[] exclude_arr = new String[excluded_values.size()];
        for (int k=0; k<excluded_values.size(); k++) {
            exclude_arr[k] = excluded_values.get(k);
        }
        return exclude_arr;
    }

    public ArrayList<String[]> search_row_matches(
            String[] columns, String[] column_values
    ) {
        /*
        return all rows where the columns specified in columns
        have the column values the same as specified in column_values
        Each row is represent as a string array of its column values
        */
        ArrayList<Pair<String[], String[]>> matches = search_matches(
                columns, column_values
        );

        ArrayList<String[]> match_rows = new ArrayList<>();
        for (Pair<String[], String[]> row_pair: matches) {
            match_rows.add(row_pair.first);
        }

        return match_rows;
    }

    public ArrayList<Pair<String[], String[]>> search_matches(
            String[] columns, String[] column_values
    ) {
        /*
        return all pairs of <
            row (all column values),
            column values for columns that are NOT in columns
            >
        for all rows with where the columns specified in columns
        have the column values the same as specified in column_values
        Each row is represent as a string array of its column values
        */
        assert columns.length == column_values.length;
        ArrayList<Pair<String[], String[]>> matches = new ArrayList<>();

        for (int k=0; k<this.num_rows(); k++) {
            String[] row_column_values = this.select_row_columns(k, columns);
            String[] row_exclude_values = this.exclude_row_columns(k, columns);
            String[] row = this.get_row(k);

            if (Arrays.equals(column_values, row_column_values)) {
                matches.add(new Pair<>(row, row_exclude_values));
            }
        }

        return matches;
    }

    public ArrayList<String[]> get_mismatch_rows(
            String[] compare_values, String[] columns, String[] column_values
    ) {
        /*
        return rows in this csv file that have column values in
        columns that don't have the same values are those in compare_values
        */
        ArrayList<Pair<String[], String[]>> matches = this.search_matches(
                columns, column_values
        );

        ArrayList<String[]> rows = new ArrayList<>();
        for (Pair<String[], String[]> match : matches) {
            String[] row = match.first;
            String[] compare_row_values = match.second;

            if (!Arrays.equals(compare_values, compare_row_values)) {
                rows.add(row);
            }
        }

        return rows;
    }

    public void print_file() {
        /*
        print out every row of the parsed csv file
        */
        for (int k=0; k<this.raw_data.size(); k++) {
            String[] column = this.raw_data.get(k);
            System.out.print(k);
            System.out.print(" ");
            System.out.println(Arrays.toString(column));
        }
    }

    public String[] get_headers() {
        return this.raw_data.get(0).clone();
    }

    public int num_rows() {
        return this.raw_data.size() - 1;
    }

    public int num_columns() {
        String[] headers = this.get_headers();
        return headers.length;
    }

    public String[] get_row(int index) {
        assert index >= 0;
        return this.raw_data.get(index + 1).clone();
    }
}
