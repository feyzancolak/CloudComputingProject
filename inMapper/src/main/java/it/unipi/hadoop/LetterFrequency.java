package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LetterFrequency {

    // Mapper class to count letters
    public static class LetterFrequencyMapper extends Mapper<Object, Text, Text, LongWritable> {
        private Map<String, Long> charCountMap = new HashMap<>();
        private Text character = new Text();
        private LongWritable charCount = new LongWritable();
        private static String language;

        @Override
        protected void setup(Context context) {
            // Get the language from the context configuration
            if (language == null)
                language = context.getConfiguration().get("language");
        }

        @Override
        public void map(Object key, Text value, Context context) {
            // Normalize and convert the line to lowercase based on the specified language
            String line = LanguageNormalizer.normalize(value.toString(), language);
            // Iterate over each character in the line
            for (char c : line.toCharArray()) {
                String character = String.valueOf(c);
                // Get the count of the character from the map or initialize it to 0 and increment it by 1
                Long charCount = charCountMap.getOrDefault(character, 0L)+ 1;
                // Update the character count map
                charCountMap.put(character, charCount);
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // Write the character counts to the context
            for (Map.Entry<String, Long> entry : charCountMap.entrySet()) {
                character.set(entry.getKey());
                charCount.set(entry.getValue());
                context.write(character, charCount);
            }
        }
    }

    // Reducer class to sum the counts of letters
    public static class LetterFrequencyReducer extends Reducer<Text, LongWritable, Text, DoubleWritable> {
        private static Long TEXT_LENGTH;
        private DoubleWritable result = new DoubleWritable();

        @Override
        protected void setup(Context context) {
            // Get the total letter count from the context configuration
            if (TEXT_LENGTH == null)
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

        // Set the total letter count in the configuration
        conf.setLong("totalLetterCount", totalLetterCount);

        Job job = Job.getInstance(conf, "letter frequency");

        // Set classes for job
        job.setJarByClass(RunProcess.class);
        job.setMapperClass(LetterFrequency.LetterFrequencyMapper.class);
        job.setReducerClass(LetterFrequency.LetterFrequencyReducer.class);

        // Set output types
        job.setOutputKeyClass(Text.class);
        job.setMapOutputValueClass(LongWritable.class);
        job.setOutputValueClass(DoubleWritable.class);

        // Set the number of reducers
        job.setNumReduceTasks(conf.getInt("numReducers", 1));

        // Set the input and output paths
        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(outputFile));

        System.out.println("Configured letter frequency job");
        return job;
    }
}
