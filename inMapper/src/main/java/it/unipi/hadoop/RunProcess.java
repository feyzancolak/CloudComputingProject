package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Job;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;

public class RunProcess {

    public static void main(String[] args) throws Exception {
        if (args.length < 5 || args.length > 6) {
            System.err.println("Usage: RunProcess <inputFile> <language> <outputFolder> <finalOutputFile> [<numReducers>]");
            System.exit(2);
        }

        //First arg is the main class RunProcess, so it's not considered
        String inputFile = args[1];
        String language = args[2];
        String outputFolder = args[3];
        String finalOutputFile = args[4];
        int numReducers = (args.length == 6) ? Integer.parseInt(args[5]) : 1;

        System.out.println("Input file: " + inputFile);
        System.out.println("Language: " + language);
        System.out.println("Output folder: " + outputFolder);
        System.out.println("Final output file: " + finalOutputFile);
        System.out.println("Number of reducers: " + numReducers);

        Configuration conf = new Configuration();
        conf.set("language", language);
        conf.setInt("numReducers", numReducers);

        // Define the folder paths for the intermediate and final output
        String countFolder = outputFolder + "/count";
        String frequencyFolder = outputFolder + "/frequency";

        // Step 1: Run Letter Count Job
        Job letterCountJob = LetterCount.configureCountJob(inputFile, countFolder, conf);
        System.out.println("Running Letter Count job");
        if (!letterCountJob.waitForCompletion(true)) {
            System.err.println("Letter Count job failed");
            System.exit(1);
        }
        System.out.println("Letter Count job completed successfully");

        // Step 2: Read total letter count from the output of Letter Count job
        long totalLetterCount = getTotalLetterCount(countFolder, conf);
        System.out.println("Total letter count: " + totalLetterCount);

        // Step 3: Run Letter Frequency Job and append results to the output file
        Job letterFrequencyJob = LetterFrequency.configureFrequencyJob(inputFile, totalLetterCount, frequencyFolder, conf);
        System.out.println("Running Letter Frequency job");
        if (!letterFrequencyJob.waitForCompletion(true)) {
            System.err.println("Letter Frequency job failed");
            System.exit(1);
        }
        System.out.println("Letter Frequency job completed successfully");

        // Step 4: Create the final output file
        createFinalFile(totalLetterCount, frequencyFolder, finalOutputFile, conf);

        // Step 5: Delete the temporary output directory for the Letter Count job
        deleteFileOrDirectory(countFolder, conf);

        System.exit(0);
    }

    private static long getTotalLetterCount(String countFolder, Configuration conf) throws IOException {
        // Take the output of the Letter Count job
        FileSystem fs = FileSystem.get(conf);
        Path letterCountFilePath = new Path(countFolder);

        long totalLetterCount = 0;

        // Get a list of all files in the output directory
        FileStatus[] status = fs.listStatus(letterCountFilePath);

        for (FileStatus fileStatus : status) {
            String fileName = fileStatus.getPath().getName();

            // Ignore the _SUCCESS file
            if (!fileName.equals("_SUCCESS")) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(fileStatus.getPath())))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        String[] parts = line.split("\\s+");
                        totalLetterCount += Long.parseLong(parts[1]);
                    }
                }
            }
        }

        return totalLetterCount;
    }

    private static void createFinalFile(long totalLetterCount, String frequencyFolder, String outputFile, Configuration conf) throws IOException {
        // Take the output of the Letter Count job
        FileSystem fs = FileSystem.get(conf);
        Path letterCountFilePath = new Path(frequencyFolder);

        // Get a list of all files in the output directory
        FileStatus[] status = fs.listStatus(letterCountFilePath);

        int validFileCount = 0;
        Path singleFilePath = null;

        for (FileStatus fileStatus : status) {
            String fileName = fileStatus.getPath().getName();
            if (!fileName.equals("_SUCCESS")) {
                validFileCount++;
                singleFilePath = fileStatus.getPath();
            }
        }

        // Output file path
        Path outputPath = new Path(outputFile);

        if (validFileCount == 1) {
            // If there is only one valid file, copy its content directly and append the total letter count
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(singleFilePath)));
                 BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(outputPath, true)))) {

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.write(line);
                    writer.newLine();
                }
                // Write a white line
                writer.newLine();
                // Write the total letter count
                writer.write("Total Letter Count:\t" + totalLetterCount + "\n");
            }
        } else {
            // If there are multiple files, use a TreeMap to merge and sort frequencies
            Map<String, Double> letterFrequencyMap = new TreeMap<>();

            for (FileStatus fileStatus : status) {
                String fileName = fileStatus.getPath().getName();
                if (!fileName.equals("_SUCCESS")) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(fs.open(fileStatus.getPath())))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            // Assume the format is "letter frequency"
                            String[] parts = line.split("\\s+");
                            String letter = parts[0];
                            double frequency = Double.parseDouble(parts[1]);

                            // Merge frequencies
                            letterFrequencyMap.put(letter, letterFrequencyMap.getOrDefault(letter, 0.0) + frequency);
                        }
                    }
                }
            }

            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(fs.create(outputPath, true)))) {
                // Write sorted letters and their frequencies
                for (Map.Entry<String, Double> entry : letterFrequencyMap.entrySet()) {
                    writer.write(entry.getKey() + "\t" + entry.getValue());
                    writer.newLine();
                }
                // Write a white line
                writer.newLine();
                // Write the total letter count
                writer.write("Total Letter Count:\t" + totalLetterCount + "\n");
            }
        }
    }

    private static void deleteFileOrDirectory(String path, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path targetPath = new Path(path);

        if (fs.exists(targetPath)) {
            fs.delete(targetPath, true);
        }
    }
}