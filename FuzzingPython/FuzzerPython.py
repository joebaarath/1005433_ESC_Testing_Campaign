from copy import deepcopy
from random import *
import subprocess
import csv
import os
import string
import json
import math

dir_path = os.path.dirname(os.path.realpath(__file__))
pathToFuzzerGeneratedCsvFolder = dir_path + "\\FuzzerGeneratedCsv\\"
count_executed = 0
count_success = 0
count_success_case0_valid_w_valid = 0
count_success_case1_valid_w_invalid_extra_comma = 0
count_success_case2_valid_w_invalid_single_line_reduced = 0
count_failure = 0
count_failure_case0_valid = 0
count_failure_case1_valid_w_invalid_extra_comma = 0
count_failure_case2_valid_w_invalid_single_line_reduced = 0
count_expected_total_count = 0

def generateValidCsv(file_name, numOfCols,numOfLines):
    filepath=pathToFuzzerGeneratedCsvFolder+file_name+".csv"
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
            col = col.replace("[","2")
            col.replace("]","3")
            rowArr.append(col)
        data.append(rowArr)
    # print(data)

    #1: generate all cell with random characters of special characters
    #2: generate duplicate lines

    # open the file in the write mode
    with open(filepath, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(data)
    
    return str(filepath), data

def generateValidCsv_basedOnFileContent(filename,fileContent):
    filepath=pathToFuzzerGeneratedCsvFolder+filename+".csv"
    print(pathToFuzzerGeneratedCsvFolder)
    #1: mutate the last column's cell
    #2: generate duplicate lines
    #if file exists delete
    if os.path.exists(filepath):
        os.remove(filepath)

    data = []
    list_of_line_indexes_to_duplicate = []
    list_of_line_indexes_mutated_wo_duplicate = []
    list_of_line_indexes_to_delete = []
    data = fileContent
    numOfLines = len(data)
    numOfCols = 0
    if numOfLines>0:
        numOfCols = len(data[0])
    if numOfCols > 0 :
        # select x random columns to delete line
        print("DATA BEFORE")
        print(data)
        num_of_lines_to_delete = randint(1, numOfLines-1)
        list_of_line_indexes_to_delete = sample(range(numOfLines), num_of_lines_to_delete)
        list_of_line_indexes_to_delete = sorted(list_of_line_indexes_to_delete,reverse=True)
        for a in list_of_line_indexes_to_delete:
            del data[a]
        print("DATA AFTER DELETE")
        print(data)
        
        numOfLines = len(data)
        if numOfLines>0:
            # select x random columns to duplicate line
            num_of_lines_to_duplicate = randint(1, numOfLines)
            list_of_line_indexes_to_duplicate = sample(range(numOfLines), num_of_lines_to_duplicate)

            # select x random columns to mutate
            num_of_lines_to_mutate = randint(1, numOfLines)
            list_of_line_indexes_to_mutate = sample(range(numOfLines), num_of_lines_to_mutate)

            #remove overlap of duplicate indexes
            for y in list_of_line_indexes_to_duplicate[:]:
                if y in list_of_line_indexes_to_mutate:
                    list_of_line_indexes_to_duplicate.remove(y)

            #mutate
            print(list_of_line_indexes_to_mutate)
            for x in list_of_line_indexes_to_mutate:
                numOfcharacters = randint(1, 100)
                str = ''.join(choice(string.ascii_letters + string.digits + string.punctuation) for x in range(numOfcharacters))
                str = str.replace(",","1")
                str = str.replace("[","2")
                str.replace("]","3")
                data[x][numOfCols-1]= str
            print("DATA AFTER MUTATE")
            print(data)

            num_of_lines_to_mutate_and_duplicate = randint(1, len(list_of_line_indexes_to_mutate))
            list_of_line_indexes_mutated_to_duplicate = sample(list_of_line_indexes_to_mutate, num_of_lines_to_mutate_and_duplicate)

            #remove overlap of duplicate indexes
            list_of_line_indexes_mutated_wo_duplicate = deepcopy(list_of_line_indexes_to_duplicate)
            for z in list_of_line_indexes_mutated_wo_duplicate[:]:
                if z in list_of_line_indexes_mutated_to_duplicate:
                    list_of_line_indexes_mutated_wo_duplicate.remove(z)

            #duplicate
            temp_duplicate_arr = []
            list_of_line_indexes_to_duplicate = list_of_line_indexes_to_duplicate + list_of_line_indexes_mutated_to_duplicate
            for m in list_of_line_indexes_to_duplicate:
                temp_duplicate_arr.append(data[m])
            for n in range(len(temp_duplicate_arr)):
                line_num_to_insert_to = randint(0, len(data)-1)
                data.insert(line_num_to_insert_to, temp_duplicate_arr[n])
            print("DATA AFTER DUPLICATES")
            print(data)

            print(f"len(list_of_line_indexes_to_duplicate): {len(list_of_line_indexes_to_duplicate)}")
            print(f"len(list_of_line_indexes_mutated_wo_duplicate): {len(list_of_line_indexes_mutated_wo_duplicate)}")
            print(f"len(list_of_line_indexes_to_delete): {len(list_of_line_indexes_to_delete)}")
    
    # open the file in the write mode
    with open(filepath, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(data)
    
    return filepath, data, len(list_of_line_indexes_to_duplicate), len(list_of_line_indexes_mutated_wo_duplicate), len(list_of_line_indexes_to_delete)

def generateInvalidFuzzedFile(filename, fileContent, error_type_int):
    filepath=pathToFuzzerGeneratedCsvFolder+filename+".csv"
    #if file exists delete
    if os.path.exists(filepath):
        os.remove(filepath)

    line_index = randint(1, len(fileContent)-1)
    row_data = fileContent[line_index]
    match error_type_int:
        case 1: 
            #1: generate 1 random Comma in between a line
            string_index = randint(0, len(row_data)-1)
            row_data.insert(string_index, "")
        case 2: 
            #2: remove half a line
            mid_point = math.floor(len(row_data)/2)
            if(mid_point>1):
                string_index = randint(0, mid_point)
                for x in range(0,string_index+1):
                    row_data.pop()
    fileContent[line_index] = row_data
    # open the file in the write mode
    with open(filepath, 'w', newline='') as f:
        writer = csv.writer(f)
        writer.writerows(fileContent)

    return str(filepath), fileContent

def delete_all_csv_in_directory():
    filelist = [ f for f in os.listdir(dir_path) if f.endswith(".csv") ]
    for f in filelist:
        os.remove(os.path.join(dir_path, f))
    return

def runFuzzer():
    print("Running FuzzerPython!")
    # DeleteAllCsvFiles
    delete_all_csv_in_directory()
    
    numOfCols = randint(5, 10)
    numOfLines = randint(5, 10)
    numOfLoopTest = 5

    global count_executed
    global count_success 
    global count_success_case0_valid_w_valid
    global count_success_case1_valid_w_invalid_extra_comma 
    global count_success_case2_valid_w_invalid_single_line_reduced 
    global count_failure 
    global count_failure_case0_valid 
    global count_failure_case1_valid_w_invalid_extra_comma 
    global count_failure_case2_valid_w_invalid_single_line_reduced 
    global count_expected_total_count
    count_expected_total_count = numOfLoopTest * 3
    for i in range(numOfLoopTest):
        file0, file0Content = generateValidCsv("file0", numOfCols,numOfLines)
        file1, file1Content, count_expected_duplicates, count_expected_mismatches, count_expected_missing = generateValidCsv_basedOnFileContent("file1", file0Content)
        # file2, file2Content = generateInvalidFuzzedFile("file2", file0Content, 1)
        # file3, file3Content = generateInvalidFuzzedFile("file3", file0Content, 2)

        proc_exit_code_valid = subprocess.call(['java', '-jar', 'Recon.jar', file0 , file1])
        count_executed+=1
        # ensure exit != 0 and no csv generated
        if proc_exit_code_valid == 0 & isOutputCsvGenerated() == True:
            count_success+=1
            count_success_case0_valid_w_valid+=1
        else:
            print(f"proc_exit_code_valid {proc_exit_code_valid} while expected is 0")
            print(f"isOutputCsvGenerated {isOutputCsvGenerated()} while expected is true")
            count_failure+=1
            count_failure_case0_valid +=1
        
        printTestStatements()

        # # pass file1 and file2 to Recon.Jar
        # proc_exit_code_extra_comma = subprocess.call(['java', '-jar', 'Recon.jar', file0 , file2])
        # count_executed+=1
        # # ensure exit != 0 and no csv generated
        # if proc_exit_code_extra_comma != 0 & isOutputCsvGenerated() == False:
        #     count_success+=1
        #     count_success_case1_valid_w_invalid_extra_comma+=1
        # else:
        #     count_failure+=1
        #     count_failure_case1_valid_w_invalid_extra_comma+=1
        
        # printTestStatements()

        # proc_exit_code_line_reduced = subprocess.call(['java', '-jar', 'Recon.jar', file0 , file3])
        # count_executed+=1
        # # ensure exit != 0 and no csv generated
        # if proc_exit_code_line_reduced != 0 & isOutputCsvGenerated() == False:
        #     count_success+=1
        #     count_success_case2_valid_w_invalid_single_line_reduced+=1
        # else:
        #     count_failure+=1
        #     count_failure_case2_valid_w_invalid_single_line_reduced+=1
        
        # printTestStatements()

        delete_all_csv_in_directory()

def printTestStatements():
    print()
    print(("Executed Tests:" + '\t' + str(count_executed) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print()
    print(("Overall Success:" + '\t' + str(count_success) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print(("Test Success Breakdown - Valid CSV:" + '\t' + str(count_success_case0_valid_w_valid) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print(("Test Success Breakdown - Extra Comma:" + '\t' + str(count_success_case1_valid_w_invalid_extra_comma) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print(("Test Success Breakdown - Line Reduced:" + '\t' + str(count_success_case2_valid_w_invalid_single_line_reduced) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print()
    print(("Overall Failure:" + '\t' + str(count_failure) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print(("Test Failure Breakdown - Valid CSV:" + '\t' + str(count_failure_case0_valid) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print(("Test Failure Breakdown - Extra Comma:" + '\t' + str(count_failure_case1_valid_w_invalid_extra_comma) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print(("Test Failure Breakdown - Line Reduced:" + '\t' + str(count_failure_case2_valid_w_invalid_single_line_reduced) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print()

def isOutputCsvGenerated():
    for f in os.listdir(dir_path):
            if f.endswith(".csv"):
                return True
    return False  

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
