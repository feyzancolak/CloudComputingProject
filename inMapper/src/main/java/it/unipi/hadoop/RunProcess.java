package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.FSDataOutputStream;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.*;
import java.util.Arrays;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;

public class RunProcess {

    public static void main(String[] args) throws Exception {
        if (args.length < 4 || args.length > 5) {
            System.err.println("Usage: RunProcess <inputfile> <language> <outputfile> [<numReducers>]");
            System.exit(2);
        }

        //First arg is RunProcess, so it's not considered
        String inputFile = args[1];
        String language = args[2];
        String outputFile = args[3];
        int numReducers = (args.length == 5) ? Integer.parseInt(args[4]) : 1;

        System.out.println("Input file: " + inputFile);
        System.out.println("Language: " + language);
        System.out.println("Output file: " + outputFile);
        System.out.println("Number of reducers: " + numReducers);

        Configuration conf = new Configuration();
        conf.set("language", language);
        conf.setInt("numReducers", numReducers);

        //Temp output file to write letterCount result
        String tempOutputFile = outputFile + "_temp";


        // Clean output directory before running letter count job
        clearFileContent(outputFile, conf);
        clearFileContent(tempOutputFile, conf);

        // Step 1: Run Letter Count Job
        runLetterCountJob(inputFile, tempOutputFile, conf);

        // Step 2: Read total letter count from the output of Letter Count job
        long totalLetterCount = getTotalLetterCount(tempOutputFile, conf);
        System.out.println("Total letter count: " + totalLetterCount);

        // Step 3: Run Letter Frequency Job and append results to the output file
        runLetterFrequencyJob(tempOutputFile, totalLetterCount, outputFile, conf);

        // Step 4: Append the output of Letter Count job to the final output file
        appendLetterCountToFile(totalLetterCount, outputFile, conf);

        // Step 5: Delete the temporary output directory
        deleteFileOrDirectory(tempOutputFile, conf);

        System.exit(0);
    }


    private static void runLetterCountJob(String inputFile, String tempOutputFile, Configuration conf) throws Exception {
        System.out.println("Running letter count job");
        Job job = Job.getInstance(conf, "letter count");

        conf = job.getConfiguration();

        job.setJarByClass(RunProcess.class);
        job.setMapperClass(LetterCount.LetterCountMapper.class);
        job.setReducerClass(LetterCount.LetterCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        job.setPartitionerClass(LetterCount.CounterPartitioner.class);
        job.setNumReduceTasks(conf.getInt("numReducers", 1));

        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(tempOutputFile));

        if (!job.waitForCompletion(true)) {
            System.out.println("Count Job failed");
            System.exit(1);
        }
        System.out.println("Count Job is done");
    }

    private static long getTotalLetterCount(String tempOutputFile, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        //Take the hadoop path
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

    private static void runLetterFrequencyJob(String inputFile, long totalLetterCount, String outputFile, Configuration conf) throws Exception {
        conf.setLong("totalLetterCount", totalLetterCount);

        Job job = Job.getInstance(conf, "letter frequency");
        job.setJarByClass(RunProcess.class);
        job.setMapperClass(LetterFrequency.LetterFrequencyMapper.class);
        job.setReducerClass(LetterFrequency.LetterFrequencyReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);


        // Input and output file
        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(outputFile));

        if (!job.waitForCompletion(true)) {
            System.out.println("Frequency Job failed");
            System.exit(1);
        }
        System.out.println("Frequency Job is done");
    }


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


    private static void deleteFileOrDirectory(String path, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path targetPath = new Path(path);

        if (fs.exists(targetPath)) {
            fs.delete(targetPath, true);
        }
    }

    private static void clearFileContent(String filePath, Configuration conf) throws IOException {
        FileSystem fs = FileSystem.get(conf);
        Path targetPath = new Path(filePath);

        if (fs.exists(targetPath)) {
            FSDataOutputStream out = fs.create(targetPath, true); // Overwrites the existing file
            out.close();
        }
    }
}

