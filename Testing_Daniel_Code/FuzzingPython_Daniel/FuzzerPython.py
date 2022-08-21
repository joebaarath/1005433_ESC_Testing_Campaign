from copy import deepcopy
from multiprocessing.connection import wait
from random import *
import subprocess
import csv
import os
import string
import json
import math
import logging
import time
from datetime import datetime
from tokenize import String

duplicate_key_word="duplicate" #Default: "duplicate"
mismatch_key_word="mismatch" #Default: "mismatch"
missing_key_word="missing" #Default: "missing"
no_mismatch_type_display_use_only_mismatch_count=True #Default: False
check_error_code=True #Default: True
csv_key_word_col_check=0 #Default: 0
output_csv_has_header=False #Default: False
input_csv_has_header=True #Default: True
errorLogFileName="FuzzerLog.log"  #Default: "FuzzerLog.log"
global enable_punctuation_in_generated_csv
enable_punctuation_in_generated_csv=True #Default: True
global prog
prog="java_daniel" #Default: "jar_joe" #options: "java_daniel", "java_charles", "jar_joe"
global numOfLoopTest
numOfLoopTest = 25

dir_path = os.path.dirname(os.path.realpath(__file__))
OutputFolderName="FuzzerGeneratedCsv"
pathToFuzzerGeneratedCsvFolder = dir_path + f"\\{OutputFolderName}\\"
shortPathToFuzzerGeneratedCsvFolder = f"{OutputFolderName}\\"


with open(dir_path + '\\' + errorLogFileName, 'w'):
    pass
logging.basicConfig(filename=dir_path + '\\' + errorLogFileName, level=logging.INFO)
logging.info('Fuzzed Executed On: ' + datetime.now().strftime("%Y-%m-%d %H:%M:%S"))

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
global progOutputCsvFile
global progFullOutputCsvFile
global input_csv_header_arr

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
progOutputCsvFile = None
input_csv_header_arr = []

def runFuzzer():
    print("Running FuzzerPython!")
    global numOfLoopTest
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
    global progOutputCsvFile
    global progFullOutputCsvFile
    global input_csv_header_arr
    global validity_of_output_csv
    global line_count
    global count_expected_mismatches
    global count_mismatches
    global count_expected_duplicates
    global count_mismatches
    global count_duplicates
    global count_expected_missing
    global count_missing
    global validity_of_output_csv
    global started_err_log
    global errorcode
    global numOfCols
    global numOfLines

    # DeleteAllCsvFiles
    delete_all_csv_in_directory()
    
    numOfCols = randint(1, 100)
    numOfLines = randint(1, 100)
    
    count_expected_total_count = numOfLoopTest * 6
    for i in range(numOfLoopTest):
        file0, file0Content = generateValidCsv("file0")
        file1, file1Content, count_expected_duplicates, count_expected_mismatches, count_expected_missing = generateValidCsv_basedOnFileContent("file1", file0Content)
        file2, file2Content = generateInvalidFuzzedFile("file2", file0Content, 1)
        file3, file3Content = generateInvalidFuzzedFile("file3", file0Content, 2)

        compare_valid_files_end_to_end(file0, file1)
        compare_valid_files_end_to_end(file1, file0)

        compare_invalid_files_add_comma_end_to_end(file0, file2)
        compare_invalid_files_add_comma_end_to_end(file2, file0)

        compare_invalid_files_line_reduce_end_to_end(file0, file3, file0Content, file3Content)
        compare_invalid_files_line_reduce_end_to_end(file3, file0, file3Content, file0Content)

    logTestStatements()
    printTestStatements()
   
def generateValidCsv(filename):
    global numOfCols
    global numOfLines
    global prog
    global enable_punctuation_in_generated_csv
    filepath=shortPathToFuzzerGeneratedCsvFolder+filename+".csv"
    #if file exists delete
    if os.path.exists(filepath):
        os.remove(filepath)

    data = []
    if input_csv_has_header == True:
        numOfLines+=1

    for i in range(numOfLines):
        rowArr = []
        for j in range(numOfCols):
            numOfcharacters = randint(1, 500)
            if (enable_punctuation_in_generated_csv == False):
                col = ''.join(choice(string.ascii_letters + string.digits) for x in range(numOfcharacters))
            else:
                col = ''.join(choice(string.ascii_letters + string.digits + string.punctuation) for x in range(numOfcharacters))
            col = col.replace(",","1")
            rowArr.append(col)
        data.append(rowArr)

    #1: generate all cell with random characters of special characters
    #2: generate duplicate lines

    # open the file in the write mode
    csv_write(filepath, data)
    
    return str(filepath), data

def generateValidCsv_basedOnFileContent(filename,fileContent):
    filepath=shortPathToFuzzerGeneratedCsvFolder+filename+".csv"
    #1: mutate the last column's cell
    #2: generate duplicate lines
    #if file exists delete
    if os.path.exists(filepath):
        os.remove(filepath)

    data = []
    list_of_line_indexes_to_duplicate = []
    list_of_line_indexes_to_delete = []
    expected_duplicates=0
    expected_mutations=0
    data = fileContent
    numOfLines = len(data)
    numOfCols = 0
    if numOfLines>0:
        numOfCols = len(data[0])
    if numOfCols > 0 :
        # select x random columns to delete line
        if input_csv_has_header == True:
            num_of_lines_to_delete = randint(0, numOfLines-2)
            list_of_line_indexes_to_delete = sample(range(1,numOfLines), num_of_lines_to_delete)
        else:
            num_of_lines_to_delete = randint(0, numOfLines-1)
            list_of_line_indexes_to_delete = sample(range(numOfLines), num_of_lines_to_delete)
        list_of_line_indexes_to_delete = sorted(list_of_line_indexes_to_delete,reverse=True)
        for a in list_of_line_indexes_to_delete:
            del data[a]
        
        numOfLines = len(data)
        if numOfLines>0:
            # select x random columns to duplicate line
            num_of_lines_to_duplicate = randint(1, numOfLines)
            list_of_line_indexes_to_duplicate = sample(range(numOfLines), num_of_lines_to_duplicate)

            # select x random columns to mutate
            if input_csv_has_header == True:
                num_of_lines_to_mutate = randint(1, numOfLines-1)
                list_of_line_indexes_to_mutate = sample(range(1,numOfLines), num_of_lines_to_mutate)
            else:
                num_of_lines_to_mutate = randint(1, numOfLines)
                list_of_line_indexes_to_mutate = sample(range(numOfLines), num_of_lines_to_mutate)

            #remove overlap of duplicate indexes
            for y in list_of_line_indexes_to_duplicate[:]:
                if y in list_of_line_indexes_to_mutate:
                    list_of_line_indexes_to_duplicate.remove(y)

            #mutate
            # print(list_of_line_indexes_to_mutate)
            for x in list_of_line_indexes_to_mutate:
                numOfcharacters = randint(1, 500)
                str = ''.join(choice(string.ascii_letters + string.digits + string.punctuation) for x in range(numOfcharacters))
                str = str.replace(",","1")
                # str = str.replace("[","2")
                # str.replace("]","3")
                data[x][numOfCols-1]= str

            #duplicate
            temp_duplicate_arr = []
            for m in list_of_line_indexes_to_duplicate:
                temp_duplicate_arr.append(data[m])
            for n in range(len(temp_duplicate_arr)):
                if input_csv_has_header == True:
                    line_num_to_insert_to = randint(1, len(data)-1)
                else:
                    line_num_to_insert_to = randint(0, len(data)-1)
                data.insert(line_num_to_insert_to, temp_duplicate_arr[n])

            expected_duplicates=len(list_of_line_indexes_to_duplicate)*3
            expected_mutations=len(list_of_line_indexes_to_mutate)*2
    
    # open the file in the write mode
    csv_write(filepath, data)
    
    return filepath, data, expected_duplicates, expected_mutations, len(list_of_line_indexes_to_delete)

def csv_write(filepath, data):
    for f in range(5):
        try:
            with open(filepath, 'w', newline='') as f:
                writer = csv.writer(f)
                writer.writerows(data)
        except:
            print("waiting")
            time.sleep(5)

def generateInvalidFuzzedFile(filename, fileContent, error_type_int):
    filepath=shortPathToFuzzerGeneratedCsvFolder+filename+".csv"
    org_fileContent = deepcopy(fileContent)

    if input_csv_has_header == True:
        fileContent.insert(0, org_fileContent[0])

    #if file exists delete
    if os.path.exists(filepath):
        os.remove(filepath)
    if len(fileContent) > 0:
        line_index = randint(0, len(fileContent)-1)
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
    csv_write(filepath, fileContent)

    return str(filepath), fileContent

def delete_all_csv_in_directory():
    for f in range(5):
        try:
            temp_dir = ""
            if prog == "java_charles":
                temp_dir = dir_path + "\\mismatches\\"
            else:
                temp_dir = dir_path
            filelist = [ f for f in os.listdir(temp_dir) if f.endswith(".csv") ]
            for f in filelist:
                    os.remove(os.path.join(temp_dir, f))
            return
        except:
                print("waiting")
                time.sleep(5)

def log_file_output_error(filepath):
    for f in range(5):
        try:
            with open(filepath) as csv_file:
                csv_reader = csv.reader(csv_file, delimiter=',')
                for row in csv_reader:
                    row_str=",".join(row)
                    # print(row_str)
                    logging.error(row_str)
            print()
            break
        except:
            print("waiting")
            time.sleep(5)

def sub_proc(file1, file2):
    global prog
    global errorcode
    global input_csv_has_header
    global output_csv_has_header
    global numOfCols
    global numOfLines
    errorcode=-1
    if input_csv_has_header:
        input_csv_has_header_str="inputheader"
    else:
        input_csv_has_header_str="inputnoheader"

    if output_csv_has_header:
        output_csv_has_header_str="outputheader"
    else:
        output_csv_has_header_str="outputnoheader"

    for x in range(5):
        try:
            if(prog=="java_daniel"):
                file1=file1+'\n'
                file2=file2+'\n'
                numOfCols_identity_arr=[*range(numOfCols-1)]
                numOfCols_identity_arr=[str(x) for x in numOfCols_identity_arr]
                numOfCols_identity_str=','.join(numOfCols_identity_arr)
                numOfCols_identity_str=numOfCols_identity_str+'\n'
                p1 = subprocess.Popen(['java', 'DataRecon.java'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                os.write(p1.stdin.fileno(), file1.encode())
                #print(os.read(p1.stdout.fileno(), 4096))
                os.write(p1.stdin.fileno(), file2.encode())
                #print(os.read(p1.stdout.fileno(), 4096))
                os.write(p1.stdin.fileno(), numOfCols_identity_str.encode())
                #print(os.read(p1.stdout.fileno(), 4096))
                # time.sleep(5)
                try:
                    outs, errs = p1.communicate(timeout=15)
                except subprocess.TimeoutExpired:
                    p1.kill()
                    outs, errs = p1.communicate()
                    print("waiting")
                    time.sleep(5)
                    continue
                errorcode = str(p1.returncode)
                p1.kill
            elif(prog=="java_charles"):
                numOfCols_identity_arr=[]
                with open(file1) as csv_file:
                    csv_reader = csv.reader(csv_file, delimiter=',')
                    for row in csv_reader:
                        numOfCols_identity_arr=deepcopy(row)
                        numOfCols_identity_arr.pop()
                        break
                file1=file1+'\n'
                file2=file2+'\n'
                numOfCols_identity_str=','.join(numOfCols_identity_arr)
                numOfCols_identity_str=numOfCols_identity_str+'\n'
                p1 = subprocess.Popen(['java', 'RecordChecker.java'], stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
                os.write(p1.stdin.fileno(), file1.encode())
                #print(os.read(p1.stdout.fileno(), 4096))
                os.write(p1.stdin.fileno(), file2.encode())
                #print(os.read(p1.stdout.fileno(), 4096))
                os.write(p1.stdin.fileno(), numOfCols_identity_str.encode())
                #print(os.read(p1.stdout.fileno(), 4096))
                # time.sleep(5)
                try:
                    outs, errs = p1.communicate(timeout=15)
                except subprocess.TimeoutExpired:
                    p1.kill()
                    outs, errs = p1.communicate()
                    print("waiting")
                    time.sleep(5)
                    continue
                errorcode = str(p1.returncode)
                p1.kill
            elif(prog=="jar_joe"):
                errorcode = subprocess.call(['java', '-jar', 'Recon.jar', file1 , file2, '-1', input_csv_has_header_str, output_csv_has_header_str])
            break
        except:
            print("waiting")
            time.sleep(5)
            continue

    return errorcode

def verify_output_valid_csv():
    global prog
    global shortPathToFuzzerGeneratedCsvFolder
    global progOutputCsvFile
    global progFullOutputCsvFile
    global line_count
    global count_duplicates
    global count_mismatches
    global count_missing
    filepath=progFullOutputCsvFile
    line_count = 0
    count_duplicates=0
    count_mismatches=0
    count_missing=0
    with open(filepath) as csv_file:
        csv_reader = csv.reader(csv_file, delimiter=',')
        line_count = 0
        for row in csv_reader:
            if line_count == 0 and output_csv_has_header == True:
                #check if headers has value
                # print(f'Column Header are {", ".join(row)}')
                temp_header_str = "".join(row)
                if(not temp_header_str.strip()):
                    print(f'Output column headers are empty')
                    break
                line_count += 1
            else:
                temp_csv_str = ""
                if csv_key_word_col_check == 0:
                    #use entire line
                    # print(f'\t{row[0]} works in the {row[1]} department, and was born in {row[2]}.')
                    temp_csv_str = ",".join(row)
                    temp_csv_str = temp_csv_str.strip()
                elif csv_key_word_col_check > 0:
                    index_val = csv_key_word_col_check - 1
                    temp_csv_str = str(row[index_val]).lower()
                    temp_csv_str = temp_csv_str.strip()
                elif csv_key_word_col_check < 0:
                    index_val = csv_key_word_col_check
                    temp_csv_str = str(row[index_val]).lower()
                    temp_csv_str = temp_csv_str.strip()
                elif csv_key_word_col_check == None:
                    #todo: use last last col value 
                    index_val = csv_key_word_col_check
                    temp_csv_str = str(row[index_val]).lower()
                    temp_csv_str = temp_csv_str.strip()

                if duplicate_key_word in temp_csv_str:
                    count_duplicates+=1
                if mismatch_key_word in temp_csv_str:
                    count_mismatches+=1
                if missing_key_word in temp_csv_str:
                    count_missing+=1
                line_count += 1
    return line_count, count_duplicates, count_mismatches, count_missing

def log_verify_output_valid_csv():
    global no_mismatch_type_display_use_only_mismatch_count
    global line_count
    global count_mismatches
    global count_success
    global count_success_case0_valid_w_valid
    global count_expected_mismatches
    global count_expected_duplicates
    global count_duplicates
    global count_expected_missing
    global count_missing
    global validity_of_output_csv
    global started_err_log
    global errorcode
    global check_error_code
    global progOutputCsvFile
    global progFullOutputCsvFile
    filepath=progFullOutputCsvFile
    started_err_log = False
    # print(f'Processed {line_count} lines.')
    if(((check_error_code == True and str(errorcode) ==str(0)) or (check_error_code == False)) and 
        ((no_mismatch_type_display_use_only_mismatch_count == True and line_count == count_expected_mismatches) or
        (no_mismatch_type_display_use_only_mismatch_count == False and count_expected_duplicates == count_duplicates 
        and count_expected_mismatches == count_mismatches
        and count_expected_missing == count_missing)
        )):
        count_success+=1
        count_success_case0_valid_w_valid+=1
        validity_of_output_csv=True
    elif(no_mismatch_type_display_use_only_mismatch_count == True and line_count != count_expected_mismatches ):
        started_err_log = True
        validity_of_output_csv=False
        logging.error(f"")
        logging.error(f"############### Start of Error: Comparing Valid CSV files (ONLY MISMATCH COUNT) ###############")
        print(f"count_mismatches {line_count} while expected is {count_expected_mismatches}")
        logging.error(f"count_mismatches {line_count} while expected is {count_expected_mismatches}")
        # print(f"Failed File Output (" + filepath + ") CSV content:")
        logging.error(f"")
        logging.error(f"##### Failed File Output (" + filepath + ") CSV content:")
        log_file_output_error(filepath)
    else:
        started_err_log = True
        validity_of_output_csv=False
        logging.error(f"")
        logging.error(f"############### Start of Error: Comparing Valid CSV files ###############")
        print(f"count_output_lines {line_count} while expected is {count_expected_duplicates+count_expected_mismatches+count_expected_missing}")
        logging.error(f"count_output_lines {line_count} while expected is {count_expected_duplicates+count_expected_mismatches+count_expected_missing}")
        print(f"count_duplicates {count_duplicates} while expected is {count_expected_duplicates}")
        logging.error(f"count_duplicates {count_duplicates} while expected is {count_expected_duplicates}")
        print(f"count_mismatches {count_mismatches} while expected is {count_expected_mismatches}")
        logging.error(f"count_mismatches {count_mismatches} while expected is {count_expected_mismatches}")
        print(f"count_missing {count_missing} while expected is {count_expected_missing}")
        logging.error(f"count_missing {count_missing} while expected is {count_expected_missing}")
        # print()
        # print(f"Failed File Output (" + filepath + ") CSV content:")
        logging.error(f"")
        logging.error(f"##### Failed File Output (" + filepath + ") CSV content:")
        log_file_output_error(filepath)
        print()
    return 

def compare_valid_files_end_to_end(file1,file2):
    global count_executed
    global errorcode
    global validity_of_output_csv
    global check_error_code
    global count_failure
    global count_failure_case0_valid
    global numOfCols
    global numOfLines
    global started_err_log
    started_err_log = False
    errorcode = sub_proc(file1, file2)
    count_executed+=1
    # ensure exit == 0 and csv generated
    validity_of_output_csv=False
    
    if isProgOutputCsvGenerated() == True:
        #verify output csv file with expected values
        verify_output_valid_csv()
        log_verify_output_valid_csv()
        if check_error_code == True and str(errorcode) != str(0):
            validity_of_output_csv=False

    if(not validity_of_output_csv):
        logging.error(f"")
        if (started_err_log == False):
            logging.error(f"############### Start of Error: Comparing Valid CSV files - Validity of output CSV ###############")
        logging.error(f"isProgOutputCsvGenerated {isProgOutputCsvGenerated()} while expected is True")
        if check_error_code == True:
            logging.error(f"errorcode {str(errorcode)} while expected is 0")

        count_failure+=1
        count_failure_case0_valid +=1
        
        logging.error(f"")
        logging.error(f"##### Failed Input File1 ({file1}) CSV content:")
        log_file_output_error(file1)
        logging.error(f"")
        logging.error(f"##### Failed Input File2 ({file2}) CSV content:")
        log_file_output_error(file2)
    
    printTestStatements()
    delete_all_csv_in_directory()

def compare_invalid_files_add_comma_end_to_end(file0, file2):
    global count_executed
    global errorcode
    global validity_of_output_csv
    global check_error_code
    global count_success
    global count_success_case1_valid_w_invalid_extra_comma
    global count_failure
    global count_failure_case1_valid_w_invalid_extra_comma
    global numOfCols
    global numOfLines
    # pass file1 and file2 to prog
    errorcode = sub_proc(file0, file2)
    count_executed+=1
    # ensure exit != 0 and no csv generated
    if (check_error_code == False and isProgOutputCsvGenerated() == False) or (check_error_code == True and str(errorcode) != str(0) and isProgOutputCsvGenerated() == False):
        count_success+=1
        count_success_case1_valid_w_invalid_extra_comma+=1
    else:
        logging.error(f"")
        logging.error(f"############### Start of Error: Add Comma into CSV file ###############")
        count_failure+=1
        count_failure_case1_valid_w_invalid_extra_comma+=1
        # print()
        logging.error(f"")
        # print(f"Failed File1 ({file0}) CSV content:")
        logging.error(f"")
        logging.error(f"##### Failed Input File1 ({file0}) CSV content:")
        log_file_output_error(file0)
        # print()
        logging.error(f"")
        # print(f"Failed File2 ({file2}) CSV content:")
        logging.error(f"")
        logging.error(f"##### Failed Input File2 ({file2}) CSV content:")
        log_file_output_error(file2)
        # print()
        logging.error(f"")
    
    printTestStatements()
    delete_all_csv_in_directory()

def compare_invalid_files_line_reduce_end_to_end(file0, file3, file0Content, file3Content):
    global count_executed
    global errorcode
    global validity_of_output_csv
    global check_error_code
    global count_success
    global count_success_case2_valid_w_invalid_single_line_reduced
    global count_failure
    global count_failure_case2_valid_w_invalid_single_line_reduced
    global numOfCols
    global numOfLines
    errorcode = sub_proc(file0, file3)
    # proc_exit_code_line_reduced = subprocess.call(['java', '-jar', 'Recon.jar', file0 , file3, '-1', 'inputnoheader', 'outputheader'])
    count_executed+=1
    # ensure exit != 0 and no csv generated
    if (file0Content == file3Content) or (check_error_code == False and isProgOutputCsvGenerated() == False) or (check_error_code == True and str(errorcode) != str(0) and isProgOutputCsvGenerated() == False):
        count_success+=1
        count_success_case2_valid_w_invalid_single_line_reduced+=1
    else:
        logging.error(f"")
        logging.error(f"############### Start of Error: Reduction in Line from CSV file ###############")
        count_failure+=1
        count_failure_case2_valid_w_invalid_single_line_reduced+=1
        # print()
        # print(f"Failed File1 ({file0}) CSV content:")
        logging.error(f"")
        logging.error(f"##### Failed Input File1 ({file0}) CSV content:")
        log_file_output_error(file0)
        # print()
        # print(f"Failed File2 ({file3}) CSV content:")
        logging.error(f"")
        logging.error(f"##### Failed Input File2 ({file3}) CSV content:")
        log_file_output_error(file3)
        # print()
        
        printTestStatements()
        delete_all_csv_in_directory()

def printTestStatements():
    print()
    print(("Executed Tests:" + '\t' + str(count_executed) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print()
    print(("Overall Success:" + '\t' + str(count_success) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print(("Test Success Breakdown - Valid CSV:" + '\t' + str(count_success_case0_valid_w_valid) + "/" + str(numOfLoopTest*2) + " Fuzzer Cases").expandtabs(50))
    print(("Test Success Breakdown - Extra Comma:" + '\t' + str(count_success_case1_valid_w_invalid_extra_comma) + "/" + str(numOfLoopTest*2) + " Fuzzer Cases").expandtabs(50))
    print(("Test Success Breakdown - Line Reduced:" + '\t' + str(count_success_case2_valid_w_invalid_single_line_reduced) + "/" + str(numOfLoopTest*2) + " Fuzzer Cases").expandtabs(50))
    print()
    print(("Overall Failure:" + '\t' + str(count_failure) + "/" + str(count_expected_total_count) + " Fuzzer Cases").expandtabs(50))
    print(("Test Failure Breakdown - Valid CSV:" + '\t' + str(count_failure_case0_valid) + "/" + str(numOfLoopTest*2) + " Fuzzer Cases").expandtabs(50))
    print(("Test Failure Breakdown - Extra Comma:" + '\t' + str(count_failure_case1_valid_w_invalid_extra_comma) + "/" + str(numOfLoopTest*2) + " Fuzzer Cases").expandtabs(50))
    print(("Test Failure Breakdown - Line Reduced:" + '\t' + str(count_failure_case2_valid_w_invalid_single_line_reduced) + "/" + str(numOfLoopTest*2) + " Fuzzer Cases").expandtabs(50))
    print()

def logTestStatements():
    logging.info('')
    logging.info('')
    logging.info('')
    logging.info("Executed Tests:" + '\t' + str(count_executed) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))
    logging.info("Overall Success:" + '\t' + str(count_success) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))
    logging.info("Test Success Breakdown - Valid CSV:" + '\t' + str(count_success_case0_valid_w_valid) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))
    logging.info("Test Success Breakdown - Extra Comma:" + '\t' + str(count_success_case1_valid_w_invalid_extra_comma) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))
    logging.info("Test Success Breakdown - Line Reduced:" + '\t' + str(count_success_case2_valid_w_invalid_single_line_reduced) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))
    logging.info("Overall Failure:" + '\t' + str(count_failure) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))
    logging.info("Test Failure Breakdown - Valid CSV:" + '\t' + str(count_failure_case0_valid) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))
    logging.info("Test Failure Breakdown - Extra Comma:" + '\t' + str(count_failure_case1_valid_w_invalid_extra_comma) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))
    logging.info("Test Failure Breakdown - Line Reduced:" + '\t' + str(count_failure_case2_valid_w_invalid_single_line_reduced) + "/" + str(count_expected_total_count) + " Fuzzer Cases".expandtabs(50))

def isProgOutputCsvGenerated():
    global prog
    global progOutputCsvFile
    global progFullOutputCsvFile
    if prog == "java_charles":
        for f in os.listdir(dir_path + "\\mismatches\\"):
                if f.endswith(".csv"):
                    progOutputCsvFile = f
                    progFullOutputCsvFile = dir_path + "\\mismatches\\" + progOutputCsvFile
                    return True
    else:
        for f in os.listdir(dir_path):
                if f.endswith(".csv"):
                    progOutputCsvFile = f
                    progFullOutputCsvFile = dir_path + "\\" + f
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
