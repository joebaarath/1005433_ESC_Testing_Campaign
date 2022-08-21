import java.security.SecureRandom;
import java.util.*;

/*
random string generator taken and adapted from
https://stackoverflow.com/questions/41107/
*/

public class RandomString {
    /**
     * Generate a random string.
     */
    public String next_string() {
        for (int idx = 0; idx < buf.length; ++idx) {
            buf[idx] = symbols[random.nextInt(symbols.length)];
        }
        return new String(buf);
    }

    public static String generate() {
        return RandomString.generate(5);
    }

    public static String generate(int length) {
        if (length == 0) { return ""; }
        RandomString generator = new RandomString(length);
        return generator.next_string();
    }

    public static ArrayList<String> generate_multi() {
        return RandomString.generate_multi(5, 5);
    }

    public static ArrayList<String> generate_multi(int num_samples) {
        return RandomString.generate_multi(num_samples, 5);
    }

    public static ArrayList<String> generate_multi(
        int num_samples, int length
    ) {
        // System.out.print("NUM_SAMPLES = ");
        // System.out.println(num_samples);
        return generate_multi(num_samples, length, length);
    }

    public static ArrayList<String> generate_multi(
        int num_samples, int min_length, int max_length
    ) {
        /*
        generate random strings (number is num_samples), where
        each string has a length from min_length to max_length
        (inclusive)
        */
        ArrayList<String> rand_strings = new ArrayList<>();
        int span = max_length - min_length + 1;

        for (int k=0; k<num_samples; k++) {
            Random int_generator = new Random();
            int length = min_length + int_generator.nextInt(span);
            String next_string = generate(length);
            rand_strings.add(next_string);
        }

        return rand_strings;
    }

    public static ArrayList<String[]> gen_multi_row_arrs(
        int num_samples, int num_columns, int min_length,
        int max_length
    ) {
        ArrayList<String[]> arrays = new ArrayList<>();
        ArrayList<ArrayList<String>
        > arraylists = generate_multi_rows(
            num_samples, num_columns, min_length, max_length
        );

        for (ArrayList<String> arraylist: arraylists) {
            arrays.add(arraylist_to_arr(arraylist));
        }

        return arrays;
    }

    public static ArrayList<ArrayList<String>> generate_multi_rows(
        int num_samples, int num_columns, int min_length,
        int max_length
    ) {
        ArrayList<ArrayList<String>> rows = new ArrayList<>();
        for (int k=0; k<num_samples; k++) {
            ArrayList<String> row = generate_multi(
                num_columns, min_length, max_length
            );
            rows.add(row);
        }

        return rows;
    }

    public static ArrayList<String> generate_multi_exc(
        int num_samples, int length
    ) {
        return generate_multi_exc(
            num_samples, length, new ArrayList<>()
        );
    }

    public static String[] arraylist_to_arr(
        ArrayList<String> arraylist
    ) {
        String[] string_arr = new String[arraylist.size()];
        arraylist.toArray(string_arr);
        return string_arr;
    }

    public static ArrayList<String> generate_multi_exc(
        int num_samples, int length, List<String> exclusions
    ) {
        /*
        generate a random arraylist of strings that are
        unique and don't appear in the exclusions arraylist either
        */
        ArrayList<String> rand_strings = new ArrayList<>();
        RandomString generator = new RandomString(length);

        while (rand_strings.size() < num_samples) {
            String sample = generator.next_string();
            if (exclusions.contains(sample)) { continue; }
            if (rand_strings.contains(sample)) { continue; }
            rand_strings.add(sample);
        }

        return rand_strings;
    }

    public static ArrayList<String> generate_multi_exc(
        int num_samples, int min_length, int max_length
    ) {
        /*
        generate a random arraylist of strings that are
        unique, where each string has a length between min_length
        to max_length (inclusive)
        */
        return generate_multi_exc(
            num_samples, min_length, max_length, new ArrayList<>()
        );
    }

    public static String[] gen_multi_exc_arr(
        int num_samples, int min_length, int max_length
    ) {
        return gen_multi_exc_arr(
            num_samples, min_length, max_length,
            new ArrayList<String>()
        );
    }

    public static String[] gen_multi_exc_arr(
        int num_samples, int min_length, int max_length,
        List<String> exclusions
    ) {
        return arraylist_to_arr(generate_multi_exc(
            num_samples, min_length, max_length, exclusions
        ));
    }

    public static ArrayList<String> generate_multi_exc(
        int num_samples, int min_length, int max_length,
        List<String> exclusions
    ) {
        /*
        generate a random arraylist of strings that are
        unique and don't appear in the exclusions arraylist either,
        where each string has a length between min_length
        to max_length (inclusive)
        */
        assert max_length >= min_length;
        Random int_generator = new Random();
        ArrayList<String> rand_strings = new ArrayList<>();
        int span = max_length - min_length + 1;

        while (rand_strings.size() < num_samples) {
            int length = min_length + int_generator.nextInt(span);
            String sample = generate(length);

            if (exclusions.contains(sample)) { continue; }
            if (rand_strings.contains(sample)) { continue; }
            rand_strings.add(sample);
        }

        return rand_strings;
    }

    public static String[] gen_unique_str_arr(
        int num_columns, int length
    ) {
        return gen_unique_str_arr(num_columns, length, length);
    }

    public static String[] gen_unique_str_arr(
        int num_columns, int min_length, int max_length
    ) {
        return gen_unique_str_arrs(
            1, num_columns, min_length, max_length
        ).get(0);
    }

    public static ArrayList<String[]> gen_unique_str_arrs(
        int num_samples, int num_columns, int length
    ) {
        return gen_unique_str_arrs(
            num_samples, num_columns, length, length
        );
    }

    public static ArrayList<String[]> gen_unique_str_arrs(
        int num_samples, int num_columns, int min_length,
        int max_length, ArrayList<String[]> exclusions
    ) {
        ArrayList<String[]> arrays = new ArrayList<>();
        ArrayList<ArrayList<String>
        > arraylists = gen_unique_string_arraylists_v2(
            num_samples, num_columns, min_length, max_length,
            exclusions
        );

        for (ArrayList<String> arraylist: arraylists) {
            String[] string_arr = new String[arraylist.size()];
            arraylist.toArray(string_arr);
            arrays.add(string_arr);
        }

        return arrays;
    }

    public static ArrayList<String[]> gen_unique_str_arrs(
        int num_samples, int num_columns, int min_length, int max_length
    ) {
        ArrayList<String[]> arrays = new ArrayList<>();
        ArrayList<ArrayList<String>
        > arraylists = gen_unique_string_arraylists(
            num_samples, num_columns, min_length, max_length
        );

        for (ArrayList<String> arraylist: arraylists) {
            String[] string_arr = new String[arraylist.size()];
            arraylist.toArray(string_arr);
            arrays.add(string_arr);
        }

        return arrays;
    }

    public static ArrayList<
    ArrayList<String>> gen_unique_string_arraylists(
        int num_samples, int num_columns, int length
    ) {
        return gen_unique_string_arraylists(
            num_samples, num_columns, length, length
        );
    }

    public static ArrayList<
        ArrayList<String>> gen_unique_string_arraylists(
        int num_samples, int num_columns, int min_length,
        int max_length
    ) {
        return gen_unique_string_arraylists(
            num_samples, num_columns, min_length, max_length,
            new ArrayList<>()
        );
    }

    public static ArrayList<
    ArrayList<String>> gen_unique_string_arraylists_v2(
        int num_samples, int num_columns, int min_length,
        int max_length, ArrayList<String[]> exclusions
    ) {
        ArrayList<ArrayList<String>> arraylist_exc = new ArrayList<
            ArrayList<String>
        >();

        for (String[] row: exclusions) {
            List<String> row_list = Arrays.asList(row);
            ArrayList<String> arraylist = new ArrayList<>(row_list);
            arraylist_exc.add(arraylist);
        }

        return gen_unique_string_arraylists(
            num_samples, num_columns, min_length, max_length,
            arraylist_exc
        );
    }

    public static ArrayList<
    ArrayList<String>> gen_unique_string_arraylists(
        int num_samples, int num_columns, int min_length,
        int max_length, ArrayList<ArrayList<String>> exclusions
    ) {
        /*
        generate random string arraylists where each arraylist
        is unique relative to all the other arraylists
        */
        ArrayList<ArrayList<String>> rows = new ArrayList<>();

        while (rows.size() < num_samples) {
            ArrayList<String> row = generate_multi(
                num_columns, min_length, max_length
            );
            if (arraylist_contains(rows, row)) { continue; }
            if (arraylist_contains(exclusions, row)) { continue; }
            rows.add(row);
        }

        return rows;
    }

    public static boolean arraylist_contains(
        ArrayList<ArrayList<String>> rows, ArrayList<String> row
    ) {
        for (ArrayList<String> current_row : rows) {
            if (row.equals(current_row)) {
                return true;
            }
        }

        return false;
    }

    public static String unique_random(
        int length, List<String> exclusions
    ) {
        /*
        generate a string whose length is length that
        isn't in the exclusions arraylist
        */
        return generate_multi_exc(
            1, length, exclusions
        ).get(0);
    }

    public static ArrayList<String> sample(
        ArrayList<String> samples, int num_samples
    ) {
        RandomString generator = new RandomString();
        ArrayList<String> new_samples = new ArrayList<>();
        for (int k=0; k<num_samples; k++) {
            int index = generator.random.nextInt(samples.size());
            String sample = samples.get(index);
            new_samples.add(sample);
        }
        return new_samples;
    }

    public static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static final String lower = upper.toLowerCase(Locale.ROOT);

    public static final String digits = "0123456789";

    public static final String alphanum = upper + lower + digits;

    private final Random random;

    private final char[] symbols;

    private final char[] buf;

    public RandomString(int length, Random random, String symbols) {
        if (length < 1) throw new IllegalArgumentException();
        if (symbols.length() < 2) throw new IllegalArgumentException();
        this.random = Objects.requireNonNull(random);
        this.symbols = symbols.toCharArray();
        this.buf = new char[length];
    }

    /**
     * Create an alphanumeric string generator.
     */
    public RandomString(int length, Random random) {
        this(length, random, alphanum);
    }

    /**
     * Create an alphanumeric strings from a secure generator.
     */
    public RandomString(int length) {
        this(length, new SecureRandom());
    }

    /**
     * Create session identifiers.
     */
    public RandomString() {
        this(5);
    }
}
