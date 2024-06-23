package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.mapreduce.Job;

public class RunProcess {
    // Default values
    final static int DEFAULT_NUM_REDUCERS  = 1;

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
        if (args.length < 3) {
            System.err.println("Usage: <input path> <language> <output path> [<num reducers>]");
            System.exit(-1);
        }

        String inputPath = args[0];
        String language = args[1];
        String outputPath = args[2];
        int numReducers = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_NUM_REDUCERS;

        System.out.println("Input Path: " + inputPath);
        System.out.println("Language: " + language);
        System.out.println("Output Path: " + outputPath);
        System.out.println("Number of Reducers: " + numReducers);

        // Create configuration and set the language
        Configuration conf = new Configuration();
        conf.set("language", language);


        // Create and configure the Letter Count job
        Job letterCountJob = LetterCount.getJob(conf, args);
        System.out.println("Letter Count job configured");

        // Wait for the Letter Count job to complete
        if (!letterCountJob.waitForCompletion(true)) {
            System.err.println("Letter Count job failed");
            System.exit(1);
        }

        System.out.println("Letter Count job completed successfully");

        // Get the total count of characters from the Letter Count job output
        //.getCounter() retrieves Counter object associated with the job that tracks and reports the progress
        //.findCounter() searches for specific counters by group and name
        //"org.apache.hadoop.mapreduce.TaskCounter" is the group name for built-in counters related to tasks in Hadoop MapReduce jobs
        //"MAP_OUTPUT_RECORDS" is the name of the counter that tracks the number of records output by the mappers
        //.getValue() retrieves the value of the counter found by findCounter()
        long textLength = letterCountJob.getCounters().findCounter("org.apache.hadoop.mapreduce.TaskCounter", "MAP_OUTPUT_RECORDS").getValue();
        System.out.println("Total number of characters: " + textLength);

        // Create and configure the Letter Frequency job
        Job letterFrequencyJob = LetterFrequency.getJob(conf, args, textLength);
        System.out.println("Letter Frequency job configured");

        // Wait for the Letter Frequency job to complete
        if (!letterFrequencyJob.waitForCompletion(true)) {
            System.err.println("Letter Frequency job failed");
            System.exit(1);
        }

        System.out.println("Letter Frequency job completed successfully");

        System.exit(0);
    }
}

