package it.unipi.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import it.unipi.hadoop.LanguageNormalizer;

public class LetterCount {

    // Mapper class to count letters
    public static class LetterCountMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
        private Map<String, Integer> charCountMap;
        private Pattern charPattern;

        @Override
        protected void setup(Context context) {
            // Initialize the character count map and pattern for valid characters
            charCountMap = new HashMap<>();
            charPattern = Pattern.compile("[a-zçğışöü]"); // Include Turkish characters
        }

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException {
            // Get the language from the context configuration
            String language = context.getConfiguration().get("language");
            // Normalize and convert the line to lowercase based on the specified language
            String line = LanguageNormalizer.normalize(value.toString().toLowerCase(), language);
            // Iterate over each character in the line
            for (char c : line.toCharArray()) {
                // Check if the character matches the pattern
                if (charPattern.matcher(String.valueOf(c)).matches()) {
                    String character = String.valueOf(c);
                    // Update the character count map
                    charCountMap.put(character, charCountMap.getOrDefault(character, 0) + 1);
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // Write the character counts to the context
            for (Map.Entry<String, Integer> entry : charCountMap.entrySet()) {
                context.write(new Text(entry.getKey()), new IntWritable(entry.getValue()));
            }
        }
    }

    // Custom partitioner class
    public static class CounterPartitioner extends Partitioner<Text, IntWritable> {
        @Override
        public int getPartition(Text key, IntWritable value, int numReducers) {
            // Simple hash-based partitioning
            return (key.hashCode() & Integer.MAX_VALUE) % numReducers;
        }
    }

    // Reducer class to sum the counts of letters
    public static class LetterCountReducer extends Reducer<Text, IntWritable, Text, IntWritable> {
        private IntWritable result = new IntWritable();

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            // Sum the counts of each letter
            for (IntWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            // Write the result to the context
            context.write(key, result);
        }
    }

    // Job configurator class to set up and run the letter count job
    public static class LetterCountJobConfigurator {
        public static void main(String[] args) throws Exception {
            Configuration conf = new Configuration();
            String[] otherArgs = new GenericOptionsParser(conf, args).getRemainingArgs();
            if (otherArgs.length < 3) {
                System.err.println("Usage: lettercount <in> [<in>...] <out> <language>");
                System.exit(2);
            }

            // Set the language in the configuration
            conf.set("language", otherArgs[otherArgs.length - 1]);

            // Configure the MapReduce job
            Job job = Job.getInstance(conf, "letter count");
            job.setJarByClass(LetterCountJobConfigurator.class);
            job.setMapperClass(LetterCount.LetterCountMapper.class);
            job.setReducerClass(LetterCountReducer.class);
            job.setOutputKeyClass(Text.class);
            job.setOutputValueClass(IntWritable.class);

            job.setPartitionerClass(CounterPartitioner.class); // Set custom partitioner
            job.setNumReduceTasks(3); // Example: Set number of reducers

            // Add input paths to the job
            for (int i = 0; i < otherArgs.length - 2; ++i) {
                FileInputFormat.addInputPath(job, new Path(otherArgs[i]));
            }
            // Set the output path for the job
            FileOutputFormat.setOutputPath(job, new Path(otherArgs[otherArgs.length - 2]));

            // Exit after job completion
            System.exit(job.waitForCompletion(true) ? 0 : 1);
        }
    }
}