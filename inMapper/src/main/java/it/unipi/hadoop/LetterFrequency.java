package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LetterFrequency {

    // Mapper class to count letters
    public static class LetterFrequencyMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
        private Map<Text, LongWritable> charCountMap;
        private Pattern charPattern;
        private static final LongWritable zero = new LongWritable(0);

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
            String line = LanguageNormalizer.normalize(value.toString(), language);
            // Iterate over each character in the line
            for (char c : line.toCharArray()) {
                // Check if the character matches the pattern
                if (charPattern.matcher(String.valueOf(c)).matches()) {
                    Text character = new Text(String.valueOf(c));
                    // Get the count of the character from the map or initialize it to 0 and increment it by 1
                    long count = charCountMap.getOrDefault(character, zero).get() + 1;
                    // Update the character count map
                    charCountMap.put(character, new LongWritable(count));
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // Write the character counts to the context
            for (Map.Entry<Text, LongWritable> entry : charCountMap.entrySet()) {
                context.write(entry.getKey(), entry.getValue());
            }
        }
    }
    // Partitioner is the default one

    // Reducer class to sum the counts of letters
    public static class LetterFrequencyReducer extends Reducer<Text, LongWritable, Text, DoubleWritable> {
        private final DoubleWritable result = new DoubleWritable();
        private static long TEXT_LENGTH;
        @Override
        protected void setup(Context context) {
            // Get the total letter count from the context configuration
            TEXT_LENGTH = context.getConfiguration().getLong("totalLetterCount", 0);
        }

        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum = 0;
            // Sum the counts of each letter
            for (LongWritable val : values) {
                sum += val.get();
            }
            result.set((double) sum / TEXT_LENGTH);

            // Write the result to the context
            context.write(key, result);
        }
    }

    public static Job configureFrequencyJob(String inputFile, long totalLetterCount, String outputFile, Configuration conf) throws Exception {
        System.out.println("Configuring letter frequency job");
        conf.setLong("totalLetterCount", totalLetterCount);
        Job job = Job.getInstance(conf, "letter frequency");

        job.setJarByClass(RunProcess.class);
        job.setMapperClass(LetterFrequency.LetterFrequencyMapper.class);
        job.setReducerClass(LetterFrequency.LetterFrequencyReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);
        job.setOutputValueClass(DoubleWritable.class);

        job.setNumReduceTasks(conf.getInt("numReducers", 1));

        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(outputFile));
        System.out.println("Configured letter frequency job");
        return job;
    }
}
