package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class RunProcess {

    /*
        The main method is the entry point of the application. It parses the command line arguments,
        creates and configures the jobs, and submits them to the Hadoop cluster.
        @param args  The command line arguments:
        args[0] = input file (ex. data/input/filename.txt)
        args[1] = language e.g. "en", "it", "tr"
        args[2] = output file (ex. data/output/001/frequency/filename.txt)
        args[3] = number of reducer tasks (optional) es. 1, 2 and 3
    */
    public static void main(String[] args) throws Exception {

        //argument parsing - checks if arguments are passed correctly
        if (args.length < 4 || args.length > 5) {
            System.err.println("Usage: <input path> <language> <output path> [<num reducers>]");
            System.exit(2);
        }

        //extracts the input file path, language, output file path and number of reducers from the arguments
        String inputFile = args[1];
        String language = args[2];
        String outputFile = args[3];
        int numReducers = (args.length == 5) ? Integer.parseInt(args[4]) : 1;

        System.out.println("Input file: " + inputFile);
        System.out.println("Language: " + language);
        System.out.println("Output file: " + outputFile);
        System.out.println("Number of reducers: " + numReducers);

        // Create configuration and set the language abd number of reducers
        Configuration conf = new Configuration();
        conf.set("language", language);
        conf.setInt("numReducers", numReducers);

        //Define Temp output file to write letterCount result
        String tempOutputFile = outputFile + "_temp";

        // Clean output directory before running letter count job to ensure they are empty
        clearFileContent(outputFile, conf);
        clearFileContent(tempOutputFile, conf);

        // Step 1: Run Letter Count Job
        // Create and configure the Letter Count job
        Job letterCountJob = LetterCount.getJob(conf, tempOutputFile,inputFile);
        System.out.println("Letter Count job configured");
        // Wait for the Letter Count job to complete
        if (!letterCountJob.waitForCompletion(true)) {
            System.err.println("Letter Count job failed");
            System.exit(1);
        }
        System.out.println("Letter Count job completed successfully");

        // Step 2: Read total letter count from the output of Letter Count job
        long totalLetterCount = getTotalLetterCount(tempOutputFile, conf);

        // Step 3: Run Letter Frequency Job and append results to the output file
        // Create and configure the Letter Frequency job
        Job letterFrequencyJob = LetterFrequency.getJob(tempOutputFile,totalLetterCount,outputFile,conf);
        System.out.println("Letter Frequency job configured");
        // Wait for the Letter Frequency job to complete
        if (!letterFrequencyJob.waitForCompletion(true)) {
            System.err.println("Letter Frequency job failed");
            System.exit(1);
        }
        System.out.println("Letter Frequency job completed successfully");

        // Step 4: Append the output of Letter Count job to the final output file
        appendLetterCountToFile(totalLetterCount, outputFile, conf);

        // Step 5: Delete the temporary output directory
        deleteFileOrDirectory(tempOutputFile, conf);

        System.exit(0);
    }

    /*
        getTotalLetterCount method reads the total letter count from the output of the Letter Count job.

        @param tempOutputFile  The path of the output directory of the Letter Count job.
        @param conf  The configuration object.
        @return  The total letter count.
     */
    private static long getTotalLetterCount(String tempOutputFile, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path outputPath = new Path(tempOutputFile);
        long totalLetterCount = 0;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(new Path(outputPath, "part-r-00000"))))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\\s+");
                totalLetterCount += Long.parseLong(parts[1]);
            }
        }

        return totalLetterCount;
    }

    /*
        appendLetterCountToFile method appends the total letter count to the final output file.

        @param totalLetterCount  The total letter count.
        @param outputFile  The path of the final output file.
        @param conf  The configuration object.
     */
    private static void appendLetterCountToFile(long totalLetterCount, String outputFile, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path outputPath = new Path(outputFile);

        // Ensure the output file exists, otherwise create it
        if (!fs.exists(outputPath)) {
            fs.create(outputPath).close();
        }

        // Append the total letter count to the final output file
        try (FSDataOutputStream out = fs.append(new Path(outputPath, "part-r-00000"))) {
            out.writeBytes("TotalLetterCount: " + totalLetterCount + "\n");
        }
    }

    /*
        deleteFileOrDirectory method deletes the specified file or directory if it exists

        @param path  The path of the file or directory to delete.
        @param conf  The configuration object.
     */
    private static void deleteFileOrDirectory(String path, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path targetPath = new Path(path);

        if (fs.exists(targetPath)) {
            fs.delete(targetPath, true);
        }
    }

    /*
        Clears the contents of the specified file by overwriting it with an empty file.

        @param filePath  The path of the file to clear.
        @param conf  The configuration object.
     */
    private static void clearFileContent(String filePath, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path targetPath = new Path(filePath);

        if (fs.exists(targetPath)) {
            FSDataOutputStream out = fs.create(targetPath, true); // Overwrites the existing file
            out.close();
        }
    }
}

