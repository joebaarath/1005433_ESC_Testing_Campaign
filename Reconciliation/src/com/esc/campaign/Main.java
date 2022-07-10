package com.esc.campaign;

import java.io.IOException;

public class Main {

    public static void main(String[] args) throws IOException {
        System.out.println("Reconciliation System starting...");
        Reconciliation recon = new Reconciliation();

        System.out.println("Verifying if arguments are valid...");
        if (recon.isArgumentsValid(args) == false){
            System.out.println("Reconciliation System exiting due to invalid arguments");
            return;
        } else {
            System.out.println("Verified that arguments are valid");
            System.out.println("File Path 1: " + args[0]);
            System.out.println("File Path 2: " + args[1]);
        }

        // load file into arrays
        System.out.println("Loading files...");
        if (recon.loadFiles() == false){
            System.out.println("Reconciliation System exiting due to failing to load file(s)");
            return;
        } else {
            System.out.println("Files successfully loaded");
        }

        // compare file
        System.out.println("Comparing files...");
        if (recon.compareFiles() == false){
            System.out.println("Reconciliation System exiting due to failing to compare files");
            return;
        } else {
            System.out.println("Files successfully compared");
        }

        //generate csv
        System.out.println("Generating output file...");
        if (recon.generateOutputFile() == false){
            System.out.println("Reconciliation System exiting due to failing to output reconcillation exception files");
            return;
        } else {
            System.out.println("output.csv file successfully generated");
            return;
        }

    }


}



