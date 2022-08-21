import org.junit.jupiter.api.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.security.Permission;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


/*
change the working directory in the run configuration
to be src before running the test
*/
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class PotatoTest {
    @BeforeAll
    public void setup() {
        // make sure System.exit() in main doesn't crash junit
        MySecurityManager secManager = new MySecurityManager();
        System.setSecurityManager(secManager);
    }

    @Test
    public void test_main() throws IOException {
        // test that running main with existing working files
        // works without error
        try {
            Main.main(new String[]{
                "sample_file_1.csv", "sample_file_3.csv"
            });
        } catch (MySecurityException e) {
            assertEquals(0, e.status);
            return;
        }

        fail("MAIN PROGRAM SHOULD HAVE EXITED");
    }

    @Test
    public void test_main_v2() throws IOException {
        // test that running main with existing working files
        // and 3 params (only 2 or 5 are allowed) fails
        // test that running main with existing working files
        // works without error
        try {
            Main.main(new String[]{
                "sample_file_1.csv", "sample_file_3.csv",
                "1, 3"
            });
        } catch (MySecurityException e) {
            assertEquals(-1, e.status);
            return;
        }

        fail("MAIN PROGRAM SHOULD HAVE EXITED");
    }

    @Test
    public void fuzz_valid_files_v3() throws IOException {
        /*
        fuzz random csv files with randomised characters,
        randomised number of columns, rows, column length
        but where the files have the same header column, and
        they have the same number of columns and all rows have
        the same number of columns

        basically fuzz random valid csv files that can be compared
        to each other without error, and make sure that no errors
        are raised when we run our program through the files

        we randomly reorder the columns on both files
        as well to see that it make the comparison even with differently
        ordered columns
        */
        Random generator = new Random();
        String filename1 = "fuzz_test_file_1.csv";
        String filename2 = "fuzz_test_file_2.csv";

        for (int k = 0; k < 100; k++) {
            // fuzz generate valid and cross-comparable files
            // 100 times, and make sure they can be compared without error
            int num_rows1 = 1 + generator.nextInt(25);
            int num_rows2 = 1 + generator.nextInt(25);
            int num_columns = 1 + generator.nextInt(25);
            int min_length = 2 + generator.nextInt(10);
            int max_length = min_length + generator.nextInt(15);

            // randomly generate headers
            String[] headers = RandomString.gen_multi_exc_arr(
                    num_columns, min_length, max_length
            );

            // generate unique combination
            String unique_combination = String.join(",", headers);
            ArrayList<String[]> raw_data1 = RandomString.gen_unique_str_arrs(
                    num_rows1, num_columns, min_length, max_length
            );
            ArrayList<String[]> raw_data2 = RandomString.gen_unique_str_arrs(
                    num_rows2, num_columns, min_length, max_length
            );

            raw_data1.add(0, headers);
            raw_data2.add(0, headers);
            CsvFile csv_file_1 = new CsvFile(raw_data1);
            // shuffle the columns of csv_file_1
            csv_file_1.scramble_columns_inplace();
            CsvFile csv_file_2 = new CsvFile(raw_data2);
            // shuffle the columns of csv_file_2
            csv_file_2.scramble_columns_inplace();
            csv_file_1.export_csv(filename1);
            csv_file_2.export_csv(filename2);

            try {
                Main.main(new String[]{
                    filename1, filename2
                });
            } catch (MySecurityException e) {
                e.printStackTrace();
                assertEquals(0, e.status);
            }
        }
    }
}