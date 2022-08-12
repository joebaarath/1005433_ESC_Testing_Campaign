from random import *
import subprocess
import csv
import os
import string
import json

def generateValidCsv(pathToFolder, file_name, numOfCols,numOfLines):
    filepath=pathToFolder+"/"+file_name+".csv"
    #if file exists delete
    if os.path.exists(filepath):
        os.remove(filepath)

    data = []
    for i in range(numOfLines):
        rowArr = []
        for j in range(numOfCols):
            numOfcharacters = randint(1, 10)
            col = ''.join(choice(string.ascii_letters + string.digits + string.punctuation) for x in range(numOfcharacters))
            #col = "abc"
            col = col.replace(",","1")
            col = col.replace("\"","2")
            # col.replace("]","")
            rowArr.append(col)
        data.append(rowArr)
    print(data)

    #1: generate all cell with random characters of special characters
    #2: generate duplicate lines

    # open the file in the write mode
    with open(filepath, 'w', encoding='UTF8', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(data)
    
    return str(filepath), data

# def generateValidCsv(pathToFolder,fileContent):
#     #1: generate all cell with random characters of special characters
#     #2: generate duplicate lines
#     return

def generateInvalidFuzzedFile(file1):
    #0: Generate Random number num between 1, 2, 3
    #1: generate 1 random Comma in between a line
    #2: remove half a line
    #3: replace 1 line with random characters of special characters
    return

def runFuzzer():
    print("Running FuzzerPython!")
    # DeleteAllCsvFiles
    
    numOfCols = randint(5, 10)
    numOfLines = randint(5, 10)
    numOfLoopTest = 1
    pathToFolder = "./FuzzerGeneratedCsv"
    count_success = 0
    count_failure = 0
    ## for i in range(numOfLoopTest):
    ##     file1path, file1Content = generateValidCsv(pathToFolder,numOfCols,numOfLines)
    ##     file2path = generateValidCsv(pathToFolder,file1Content)
    ##     pass file1 and file2 to Recon.Jar
    ##     ensure exit == 0
    ##     ensure output csv generated
    ##     if so, count_success++
    ##     else, move_csv
    for i in range(numOfLoopTest):
        file1, file1Content = generateValidCsv(pathToFolder, "file1", numOfCols,numOfLines)
        # file2, errorOfContent = generateInvalidFuzzedFile(pathToFolder, "file2", file1Content)
        # pass file1 and file2 to Recon.Jar
        # Load Recon.Jar File
        #subprocess.call(['java', '-jar', 'Recon.jar', file1 , file2])
        # ensure exit != 0
        # ensure no csv generated
        # if so, count_success++
        # else, move_csv to SoftwareGeneratedInvalidUnexpectedCsv and count_failure++
        # delete file

def main():
    print("Starting FuzzerPython!")
    try:
        runFuzzer() 
    except Exception as e:
        print("Error in FuzzerPython!")
        print(str(e))
        exit(-1)
    finally:
        print("Exiting Fuzzer Python!")
        exit(0)

if __name__ == "__main__":
    main()
