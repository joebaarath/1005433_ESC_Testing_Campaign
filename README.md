# ESC Software Testing Campaign
Name: Baarath S/O Sellathurai  
ID: 1005433  
Cohort/Group: C02G01  

# Reconcillation
This reconcillation system will compare 2 csv files.  
The current implementation expects the following format for the csv file: "Customer ID#",	"Account No.",	"Currency",	"Type",	"Balance"

### Use Case Diagram
<img src="./Documents/WK08_ESC_CAMPAGIN_USECASE_DIAGRAM.jpg" />

### Equivalence class partitioning and boundary value analysis
[BoundaryValueAnalysis_and_EquivalenceClassPartitioning](./Documents/WK09_BoundaryValueAnalysis_and_EquivalenceClassPartitioning_V2.docx)  

### Getting Started
Ensure you are using Java version 17 or higher and Oracle openJdk 18 or higher.
1. Clone repo
```
git clone https://github.com/joebaarath/1005433_ESC_Testing_Campaign
```

### Method 1: Running reconcillation via Command Line using jar artefact
1. Open cmd line and navigate to git project folder
```
java -jar Reconcillation.jar "sample_csv\sample_file_1.csv" "sample_csv\sample_file_3.csv"
```

### Method 2: Running reconcillation via IntelliJ
1. Install IntelliJ 
2. Open IntelliJ
3. Open Project
4. Navigate to git project folder and select the "Reconciliation" folder
5. Click "Trust Project"
6. In the project window, navigate to "src/com.esc.campaign/Main"
7. Under the "Run" file menu, click on the run to execute with preconfigured file path arguements sample_file_1.csv and sample_file_3.csv
8. Under the "Run" file menu, click on "Edit Configurations"
9. Modify the 2 cmd line arguements to other csv file paths and click okay
10. Under the "Run" file menu, click on the run to perform reconciliation on specified csv files

