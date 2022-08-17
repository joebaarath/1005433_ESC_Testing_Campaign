

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class ReconciliationTest {
    Reconciliation recon;
    String OutputFilePath = "output_unit_test.csv";

    @BeforeEach
    public void preTest(){
        recon = new Reconciliation();
    }

    @AfterEach
    public void postTest(){
        recon = null;


    }

    private void deleteOutputTestFile() {
        Path outputFile = Path.of(OutputFilePath);
        File myFile = new File(outputFile.toAbsolutePath().toString());
        if( myFile.exists()){
            myFile.delete();
        }
    }

    public static Stream<Arguments> isArgumentsValid() {
        return Stream.of(
            // Category 1: Number of arguements
                // Invalid Partition 1: No argument passed to system
                Arguments.of(null, false),
                Arguments.of(new String[]{""}, false),
                Arguments.of(new String[]{"               "}, false),
                // Invalid Partition 2: 1 argument passed to system
                Arguments.of(new String[]{"arg1"}, false),
                // Invalid Partition 3: >2 arguments passed to system
                Arguments.of(new String[]{"arg1", "arg2", "arg3"}, false),

            // Category 2: Validity of argument
                // Invalid Partition: Argument is a file path but not .csv file extenion
                Arguments.of(new String[]{"sample_file_1.txt", "sample_file_3.txt"}, false),
                // Invalid Partition 5: Argument is a file path with .csv file extension, with <= 256 character but file doesn’t exist
                Arguments.of(new String[]{"arg1.csv", "arg2.csv"}, false),

                // Valid Partition 1: Argument is a file path to an existing csv file and system is able to read file
                Arguments.of(new String[]{"sample_file_1.csv", "sample_file_3.csv"}, true),
                Arguments.of(new String[]{"sample_file_1.csv", "sample_file_1.csv"}, true),
                Arguments.of(new String[]{"sample_file_3.csv", "sample_file_5_long_name_zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz.csv"}, true),
                Arguments.of(new String[]{"sample_file_5_long_name_zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz.csv", "sample_file_5_long_name_zzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzz.csv"}, true)
        );
    }

    @ParameterizedTest(name = "{index} - isArgumentsValid input_args={0}, expectedValidity={1}")
    @MethodSource
    void isArgumentsValid(String[] input, boolean expected) throws Exception {

        if(expected == true)
        {
            assertEquals(expected,recon.isArgumentsValid(input) );
        }
        else
        {
            // expect to throw exception
            assertThrows(Exception.class,() -> recon.isArgumentsValid(input) );
        }
    }

    public static Stream<Arguments> loadFiles() {
        return Stream.of(
                //	Invalid Partition 1: File is empty and has no content
                Arguments.of(new String[]{"invalid_file_1_empty.csv"}, false),
                //	Invalid Partition 3: File contents are in UTF-8 format, but file only contains whitespaces and/or only newlines
                Arguments.of(new String[]{"invalid_file_2_white_space.csv"}, false),
                Arguments.of(new String[]{"invalid_file_3_newline.csv"}, false),
                //	Valid Parition 1: File contents are in UTF-8 encoding and contains 1 line of non-whitespace/newlines content
                Arguments.of(new String[]{"valid_file_1_single_line.csv"}, true),
                //	Invalid Parition 4: File contents are in UTF-8 encoding and contains more than 1 line of non-whitespace/newlines content but the number of commas (which are not wrapped within double quotes) between lines are inconsistent
                Arguments.of(new String[]{"invalid_file_4_inconsistent_commas_between_lines.csv"}, false),
                //	Valid Partition 2: File contents are in UTF-8 encoding and contains more than 1 line of non-whitespace/newlines content and the number of commas (which are not wrapped within double quotes) between lines are consistent for all lines
                Arguments.of(new String[]{"sample_file_1.csv"}, true)
        );
    }

    @ParameterizedTest(name = "{index} - loadFiles input_args={0}, expectedValidity={1}")
    @MethodSource
    void loadFiles(String[] args, boolean expected) throws Exception {
        recon.File1 = Path.of(args[0]);
        recon.File2 = Path.of(args[0]);
        if (expected ==  true){
            assertEquals(expected,recon.loadFiles() );
        }
        else{
            // expect to throw exception
            assertThrows(Exception.class,() -> recon.loadFiles() );
        }

    }


    public static Stream<Arguments> compareFiles() {
        return Stream.of(
                //valid
                Arguments.of("sample_file_1.csv","sample_file_3.csv",true,0,4,1,1),
                Arguments.of("sample_file_1.csv","sample_file_1.csv",true,0,0,0,0),
                Arguments.of("sample_file_3.csv","sample_file_3.csv",true,0,0,0,0),
                Arguments.of("valid_file_2_5cols_5rows.csv","valid_file_3_3cols_3rows.csv",false,0,0,0,0),
                Arguments.of("valid_file_4_5cols_6rows_duplicate_row.csv","valid_file_5_5cols_6rows_duplicate_row_2.csv",true,10,0,0,0),
                Arguments.of("valid_file_6_10cols_5rows.csv","valid_file_7_10cols_5rows_2.csv",true,4,2,1,2)

        );
    }
    @ParameterizedTest(name = "{index} - compareFiles file1={0},file1={1}, expectedDuplicates={2}, expectedMismatches={3}, expectedMissingInFile1={4}, expectedMissingInFile1={5}, expectedValidity={6}")
    @MethodSource
    void compareFiles(String filepath1, String filepath2, boolean expected, Integer expected_duplicate_count, Integer expected_mismatch_count, Integer expected_missing_in_file_1_count, Integer expected_missing_in_file_2_count) throws Exception {
        // No matching ID columns in other file
        recon.File1 = Path.of(filepath1);
        recon.File2 = Path.of(filepath2);

        assertTrue(recon.loadFiles());

        if(expected == true){
            assertTrue(recon.compareFiles());
            assertEquals(expected_mismatch_count, recon.outputMismatchArray.size());
            assertEquals(expected_missing_in_file_1_count, recon.outputFile1MissingArray.size());
            assertEquals(expected_missing_in_file_2_count, recon.outputFile2MissingArray.size());
            assertEquals(expected_duplicate_count, recon.outputDuplicateArray.size());
        }
        else{
            assertThrows(Exception.class,() -> recon.compareFiles() );
        }
    }

    @Test
    void generateOutputFile() throws Exception {
        recon.File1 = Path.of("sample_file_1.csv");
        recon.File2 = Path.of("sample_file_3.csv");
        assertTrue(recon.loadFiles());
        assertTrue(recon.compareFiles());
        //delete output file
        File outputFile = new File(OutputFilePath);
        if(outputFile.exists() && !outputFile.isDirectory()) {
            if (outputFile.delete()) {
                System.out.println("Deleted the file: " + outputFile.getName());
            } else {
                System.out.println("Failed to delete the file.");
                throw new Exception("unable to delete file " + outputFile.getName() + " for unit test");
            }
        }

        assertTrue(recon.generateOutputFile(OutputFilePath));
        assertTrue(outputFile.exists());
    }
}