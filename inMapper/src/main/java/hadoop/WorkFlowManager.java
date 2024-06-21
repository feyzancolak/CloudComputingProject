package hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;

public class WorkFlowManager {
    public static void main(String[] args) throws Exception {
        // Check if the correct number of arguments are provided
        if (args.length < 5) {
            System.err.println("Usage: WorkflowManager <count|frequency> <in> [<in>...] <temp> <out> <language>");
            System.exit(2);
        }

        String jobType = args[0];
        Configuration conf = new Configuration();
        // Parse the command line arguments
        String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();

        // Check the job type and run the corresponding job
        if (jobType.equals("count")) {
            runLetterCountJob(otherArgs, conf);
        } else if (jobType.equals("frequency")) {
            runLetterFrequencyJob(otherArgs, conf);
        } else {
            System.err.println("Invalid job type. Use 'count' or 'frequency'.");
            System.exit(2);
        }
    }

    private static void runLetterCountJob(String[] args, Configuration conf) throws Exception {
        // Extract input paths, output path, and language from the arguments
        String[] countArgs = Arrays.copyOfRange(args, 1, args.length - 2);
        String outputPath = args[args.length - 2];
        String language = args[args.length - 1];

        // Set the language in the configuration
        conf.set("language", language);

        // Configure the MapReduce job for counting letters
        Job job = Job.getInstance(conf, "letter count");
        job.setJarByClass(WorkFlowManager.class);
        job.setMapperClass(LetterCount.LetterCountMapper.class);
        job.setReducerClass(LetterCount.LetterCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);

        // Set a custom partitioner and number of reducers
        job.setPartitionerClass(LetterCount.CounterPartitioner.class);
        job.setNumReduceTasks(3); // Example: Set number of reducers

        // Add input paths to the job
        for (String inputPath : countArgs) {
            FileInputFormat.addInputPath(job, new Path(inputPath));
        }
        // Set the output path for the job
        FileOutputFormat.setOutputPath(job, new Path(outputPath));

        // Wait for the job to complete
        job.waitForCompletion(true);
    }

    private static void runLetterFrequencyJob(String[] args, Configuration conf) throws Exception {
        // Extract paths and language from the arguments
        String countOutputPath = args[args.length - 3];
        String frequencyOutputPath = args[args.length - 2];
        String language = args[args.length - 1];

        // Set the language in the configuration
        conf.set("language", language);

        // Get the total length of the text from the output of the LetterCount job
        long textLength = getTextLength(conf, countOutputPath);

        // Set the text length in the configuration
        conf.setLong("textLength", textLength);

        // Configure the MapReduce job for calculating letter frequency
        Job job = Job.getInstance(conf, "letter frequency");
        job.setJarByClass(WorkFlowManager.class);
        job.setMapperClass(LetterFrequency.LetterFrequencyMapper.class);
        job.setReducerClass(LetterFrequency.LetterFrequencyReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(DoubleWritable.class);

        // Set the input and output paths for the job
        FileInputFormat.addInputPath(job, new Path(countOutputPath));
        FileOutputFormat.setOutputPath(job, new Path(frequencyOutputPath));

        // Wait for the job to complete
        job.waitForCompletion(true);
    }

    private static long getTextLength(Configuration conf, String countOutputPath) throws IOException {
        // Create the path for the output file from the LetterCount job
        Path outputPath = new Path(countOutputPath + "/part-r-00000"); // Assuming default output name
        FileSystem fs = FileSystem.get(conf);
        long textLength = 0;
        // Iterate over all output files in the output directory
        for (FileStatus status : fs.listStatus(outputPath.getParent())) {
            if (status.getPath().getName().startsWith("part-")) {
                // Read each file and sum the counts of letters
                try (BufferedReader br = new BufferedReader(new InputStreamReader(fs.open(status.getPath())))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        String[] parts = line.split("\\s+");
                        textLength += Long.parseLong(parts[1]);
                    }
                }
            }
        }
        return textLength;
    }
}


