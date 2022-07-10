package com.esc.campaign;

import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

        //verify number of arguments == 2
        if(args.length != 2){
            System.out.println("Reconciliation System expected 2 file path arguments but received " + args.length + "!" );
            return false;
        }

        //verify file path exists
        File1 = Path.of(args[0]);
        File2 = Path.of(args[1]);
        boolean file1Exists = Files.exists(File1);
        if(file1Exists == false) {
            System.out.println("File doesn't exist for the 1st file path argument: " + args[0] );
        }
        boolean file2Exists = Files.exists(File2);
        if(file2Exists == false) {
            System.out.println("File doesn't exist for the 2nd file path argument: " + args[1] );
        }

        if( !(file1Exists && file2Exists) ){
            return false;
        }


        return true;
    }

    public boolean loadFiles() {
        List<String> list1 = new ArrayList<>();
        List<String> list2 = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(File1)) {
            list1 = br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("File 1 failed to load!");
            return false;
        }

        //check if empty
        if(list1.isEmpty())
        {
            System.out.println("File 1 has no content");
            return false;
        }
        else
        {
            array1 = new String[list1.size()][];
            String[] statusColArray = new String[1];
            statusColArray[0]="unchecked";
            for(int i=0;i<list1.size();i++){
                String[] dataArray = list1.get(i).split(",");
                String[] combinedArray =Stream.of(statusColArray,dataArray).flatMap(Stream::of)
                        .toArray(String[]::new);

                array1[i]=combinedArray;
            }
            //System.out.println(Arrays.deepToString(array1));
            //System.out.println(array1[0][1]);
            System.out.println("File 1 has successfully loaded!");
        }




        try (BufferedReader br = Files.newBufferedReader(File2)) {
            list2 = br.lines().collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("File 2 failed to load!");
            return false;
        }

        //check if empty
        if(list2.isEmpty())
        {
            System.out.println("File 2 has no content");
            return false;
        }
        else
        {
            array2 = new String[list2.size()][];
            String[] statusColArray = new String[1];
            statusColArray[0]="unchecked";
            for(int i=0;i<list2.size();i++){
                String[] dataArray = list2.get(i).split(",");
                String[] combinedArray =Stream.of(statusColArray,dataArray).flatMap(Stream::of)
                        .toArray(String[]::new);

                array2[i]=combinedArray;
            }
            //System.out.println(Arrays.deepToString(array2));
            //System.out.println(array2[0][1]);
            System.out.println("File 2 has successfully loaded!");
        }

        return true;
    }

    public boolean compareFiles(){
        //currently assuming there are no duplicates in each of the files

        outputMismatchArray = new ArrayList<>();
        outputFile1MissingArray = new ArrayList<>();
        outputFile2MissingArray = new ArrayList<>();

        for (int i = 0; i < array1.length; i++) {
            //currently assuming columns are in the same order for the 2 files
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

    public boolean generateOutputFile() throws IOException {
        outputArray = new ArrayList<>();
        outputArray.addAll(outputMismatchArray);
        outputArray.addAll(outputFile1MissingArray);
        outputArray.addAll(outputFile2MissingArray);

        FileWriter writer = new FileWriter("output.csv");
        for(String line: outputArray) {
            writer.write(line + System.lineSeparator());
        }
        writer.close();

        return true;
    }
}
