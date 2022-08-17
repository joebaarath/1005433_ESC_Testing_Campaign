import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.platform.commons.util.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class SystemTest {
    File jar_file;
    ProcessBuilder pb;
    Process proc;
    File outputFile = new File("output.csv");

    @BeforeAll
    public void preTest() throws Exception {
        // try to get gradle build recon jar file
        // note: build is from gradle output
        jar_file = new File("build/Recon-1.0-SNAPSHOT.jar");
        if (!jar_file.exists()) {
            // try to get previous stable built jar file
            jar_file = new File("Recon.jar");
            if (!jar_file.exists()) {
                throw new Exception("Recon.jar File not found, not able to perform tests");
            }
        }

        if (outputFile.exists() && !outputFile.isDirectory()) {
            if (outputFile.delete()) {
                System.out.println("Deleted the file: " + outputFile.getName());
            } else {
                System.out.println("Failed to delete the file.");
                throw new Exception("unable to delete file " + outputFile.getName() + " for system test");
            }
        }

    }

    @AfterEach
    public void postTest() throws Exception {
        if (outputFile.exists() && !outputFile.isDirectory()) {
            if (outputFile.delete()) {
                System.out.println("Deleted the file: " + outputFile.getName());
            } else {
                System.out.println("Failed to delete the file.");
                throw new Exception("unable to delete file " + outputFile.getName() + " for system test");
            }
        }

        if(proc.isAlive()){
            proc.destroy();
            proc = null;
        }

    }

    public static Stream<Arguments> generateExpectedComparisonInFile() {
        return Stream.of(
                Arguments.of("sample_file_1.csv", "sample_file_3.csv", 0, 4, 1, 1),
                Arguments.of("sample_file_1.csv", "sample_file_1.csv", 0, 0, 0, 0),
                Arguments.of("sample_file_3.csv", "sample_file_3.csv", 0, 0, 0, 0),
                Arguments.of("valid_file_4_5cols_6rows_duplicate_row.csv", "valid_file_5_5cols_6rows_duplicate_row_2.csv", 10, 0, 0, 0),
                Arguments.of("valid_file_6_10cols_5rows.csv","valid_file_7_10cols_5rows_2.csv",4,2,1,2)
        );
    }

    @ParameterizedTest(name = "{index} - generateExpectedComparisonInFile file1={0}, file2={1}, expectedDuplicateCount={2} expectedMismatchedCount={3}, expected_in_file1_but_missing_in_file2_count={4}, expected_in_file2_but_missing_in_file1_count={5} ")
    @MethodSource
    void generateExpectedComparisonInFile(String filepath1, String filepath2, Integer expected_ambiguous_duplicate_identifier_count, Integer expected_mismatched_differences_count, Integer expected_in_file1_but_missing_in_file2_count, Integer expected_in_file2_but_missing_in_file1_count) throws Exception {
        int mismatched_differences_count = 0;
        int exists_in_file2_but_missing_in_file1_count = 0;
        int exists_in_file1_but_missing_in_file2_count = 0;
        int ambiguous_duplicate_identifier_count = 0;
        pb = new ProcessBuilder("java",  "-jar", jar_file.getAbsolutePath(), filepath1, filepath2);
        proc = pb.start();
            if(proc.waitFor(2, TimeUnit.MINUTES)) {
                if (outputFile.exists() && !outputFile.isDirectory()) {
                    try {
                        FileReader fr = new FileReader(outputFile);   //reads the file
                        BufferedReader br = new BufferedReader(fr);  //creates a buffering character input stream
                        String line;

                        while ((line = br.readLine()) != null) {
                            if(line.contains(","))
                            {
                                //num of commas in each line
                                int number_of_commas = 0;
                                for (int i = 0; i < line.length(); i++) {
                                    if (line.charAt(i) == ',') {
                                        number_of_commas++;
                                    }
                                }
                                // get the 4th last comma to 3rd last comma
                                String [] line_arr = line.split(",");
                                String diff_type = line_arr[number_of_commas-3].toLowerCase();
                                if(diff_type.contains("mismatched")){
                                    mismatched_differences_count++;
                                }
                                if(diff_type.contains("missing in file1")){
                                    exists_in_file2_but_missing_in_file1_count++;
                                }
                                if(diff_type.contains("missing in file2")){
                                    exists_in_file1_but_missing_in_file2_count++;
                                }
                                if(diff_type.contains("duplicate")){
                                    ambiguous_duplicate_identifier_count++;
                                }

                            }
                        }
                        fr.close();    //closes the stream and release the resources

                        assertEquals(expected_mismatched_differences_count,mismatched_differences_count);
                        assertEquals(expected_in_file2_but_missing_in_file1_count,exists_in_file2_but_missing_in_file1_count);
                        assertEquals(expected_in_file1_but_missing_in_file2_count,exists_in_file1_but_missing_in_file2_count);
                        assertEquals(expected_ambiguous_duplicate_identifier_count,ambiguous_duplicate_identifier_count);

                    } catch (IOException e) {
                        e.printStackTrace();
                        fail("unexpected error");
                    }
            }
            else
            {
                fail("Output CSV not generated");
            }
        }
        else
        {
            fail("Process Timeout");
        }
    }

    @Test
    void generateExpectedOutputFile() throws Exception {
        pb = new ProcessBuilder("java",  "-jar", jar_file.getAbsolutePath(), "sample_file_1.csv", "sample_file_3.csv");
        proc = pb.start();
        if(proc.waitFor(2, TimeUnit.MINUTES)) {
            assertEquals(true, outputFile.exists() && !outputFile.isDirectory());
        }
        else
        {
            fail("Process Timeout");
        }
    }

}
