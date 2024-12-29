## Building
`mvn clean compile package`
Compiles the code and runs junit tests

## Execution
### Generate a test csv file
`java -cp target/Content-Moderation-System-1.0.0.jar GenerateCSV numberOflines filePath`

Eg.: java -cp target/Content-Moderation-System-1.0.0.jar GenerateCSV 1000000 ./input.csv

### Execute
`java -Xss136k -cp target/Content-Moderation-System-1.0.0.jar Main filePath numberOfWorkersToProcessTheCsv totalNumberOfThreads`

Eg.: java -Xss136k -cp target/Content-Moderation-System-1.0.0.jar Main ./input.csv 10 8000

After the execution, it prints 3 lines:
1. Total Lines Produced;
2. Elapsed Time (ms);
3. Path of the result csv.

**Note**: the total number of threads depends a lot on the value given by 'ulimit -u', the maximum number of user processes.
