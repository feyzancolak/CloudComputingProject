package it.unipi.dsmt;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;

public class RunProcess {
    // Default values
    final static int DEFAULT_NUM_REDUCERS = 1;

    //parses the command line arguments and ensures that requires arguments are provided (input, letterCountOutput, letterFrequencyOutput)
    public static Map<String, String> parsing(String[] args) {
        Map<String, String> argMap = new HashMap<>(); //to store the parsed arguments as key-value pairs
        for (String arg : args) {
            if (arg.startsWith("it.unipi.dsmt")) {
                //If an argument starts with "it.unipi.dsmt", it is ignored and the loop continues to the next argument.
                continue;
            }
            //each argument string is split into two parts using the = delimiter. This results in
            //an array 'parts' where 'parts[0]' is the key and 'parts[1]' is the value.
            String[] parts = arg.split("=");
            //check if the split resulted in two parts correctly otherwise give an error
            if (parts.length == 2) {
                argMap.put(parts[0], parts[1]);
            } else {
                System.err.println("Invalid argument: " + arg);
                System.exit(1);
            }
        }

        //check if the required arguments are provided
        if (!argMap.containsKey("input") || !argMap.containsKey("letterCountOutput") || !argMap.containsKey("letterFrequencyOutput")) {
            System.err.println("Usage: LetterFrequency input=<input> letterCountOutput=<output> letterFrequencyOutput=<output> [numReducers=<num of reducer tasks>]");
            System.exit(1);
        }

        System.out.println("args[0]: <input>="  + argMap.get("input"));
        System.out.println("args[1]: <letterCountOutput>=" + argMap.get("letterCountOutput"));
        System.out.println("args[2]: <letterFrequencyOutput>=" + argMap.get("letterFrequencyOutput"));

        return argMap;
    }

    //Reads the total length of the text from the output files of the 'LetterCount' job. It iterates over
    //the output files, ignoring the '_success' file , reads the first line to get the text length and sums up the lengths
    public static long getTextLength(Configuration conf, String outputDirectory) throws IOException {
        // Read the output of the first job
        FileSystem fs = FileSystem.get(conf);
        Path outputDirPath = new Path(outputDirectory);

        // Initialize the total text length
        long totalTextLength = 0;

        // Get a list of all files in the output directory
        FileStatus[] status = fs.listStatus(outputDirPath);
        for (FileStatus fileStatus : status) {
            String fileName = fileStatus.getPath().getName();

            // Ignore the _SUCCESS file
            if (!fileName.equals("_SUCCESS")) {
                // Open the file
                FSDataInputStream inputStream = fs.open(fileStatus.getPath());
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

                // The result is on the first line of the output
                String firstLine = bufferedReader.readLine();
                if (firstLine != null) {
                    long textLength = Long.parseLong(firstLine);
                    totalTextLength += textLength;
                }

                // Close the input stream
                bufferedReader.close();
                inputStream.close();
            }
        }

        // Display the total text length
        System.out.println("Letter Count - Total text length: " + totalTextLength);

        return totalTextLength;
    }



    public static void main(String[] args) throws Exception {

        // Configuration of the job
        Configuration conf = new Configuration();

        // Parse the arguments to get a map of the arguments
        Map<String, String> argMap = parsing(args);

        // Create a letter count job
        Job letterCountJob = LetterCount.getJob(conf, argMap, DEFAULT_NUM_REDUCERS);
        // Wait for the first job to complete and exists if it fails
        if (!letterCountJob.waitForCompletion(true)) {
            System.exit(1);
        }

        // Read the text length to get the total text length from the first job's output
        long textLength = getTextLength(conf, argMap.get("letterCountOutput"));

        // Create a letter frequency job
        Job letterFrequencyJob = LetterFrequency.getJob(conf, argMap, textLength, DEFAULT_NUM_REDUCERS);
        // Wait for the second job to complete
        System.exit(letterFrequencyJob.waitForCompletion(true) ? 0 : 1);
    }
}
