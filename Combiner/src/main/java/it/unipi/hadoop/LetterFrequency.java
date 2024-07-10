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
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

public class LetterFrequency {

    public static class MapperFrequency extends Mapper<Object, Text, Text, LongWritable> {
        private static Text character;
        private static String language;
        private static final LongWritable one = new LongWritable(1);

        @Override
        protected void setup(Context context) {
            // Get the language from the context configuration
            language = context.getConfiguration().get("language");
            // Initialize the character
            character = new Text();
        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            // Convert the line to lower case and remove accents
            String line = LanguageNormalizer.normalize((value.toString()).toLowerCase(),language);

            //Emits each letter found in the input text with a count of 1
            for (char ch : line.toCharArray()) {
                String charStr = String.valueOf(ch);
                character.set(charStr);
                context.write(character, one);
            }
        }
    }

    public static class CombinerFrequency extends Reducer<Text, LongWritable, Text, LongWritable> {
        private final LongWritable result = new LongWritable();

        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum = 0;

            // Combine the counts of the same letter
            for (LongWritable value : values) {
                sum += value.get();
            }
            result.set(sum);

            // Write the key and the aggregated count to the context
            context.write(key, result);
        }
    }

    public static class ReducerFrequency extends Reducer<Text, LongWritable, Text, DoubleWritable> {
        private static DoubleWritable result;
        private static long TEXT_LENGTH;

        @Override
        protected void setup(Context context) {
            // Get the total letter count from the context configuration
            TEXT_LENGTH = context.getConfiguration().getLong("totalLetterCount", 0);
            // Initialize the result
            result = new DoubleWritable();
        }

        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum = 0;

            // Combine the counts of the same letter and evaluate the frequency
            for (LongWritable value : values) {
                sum += value.get();
            }
            result.set((double) sum / TEXT_LENGTH);

            // Write the result to the context
            context.write(key, result);
        }
    }

    public static Job configureFrequencyJob(String tempOutputFile, long totalLetterCount, String outputFile, Configuration conf) throws IOException {
        System.out.println("Configuring letter frequency job");

        // Set the configuration
        conf.setLong("totalLetterCount", totalLetterCount);

        Job letterFrequencyJob = Job.getInstance(conf, "LetterFrequency");

        // Set the main classes
        letterFrequencyJob.setJarByClass(LetterFrequency.class);
        letterFrequencyJob.setMapperClass(MapperFrequency.class);
        letterFrequencyJob.setReducerClass(ReducerFrequency.class);
        letterFrequencyJob.setCombinerClass(CombinerFrequency.class);

        // Set the number of reducers
        letterFrequencyJob.setNumReduceTasks(conf.getInt("numReducers", 1));

        // Set the output key classes for the mapper, combiner and reducer
        letterFrequencyJob.setOutputKeyClass(Text.class);

        // Set the output key value classes for the mapper, combiner and reducer
        letterFrequencyJob.setMapOutputValueClass(LongWritable.class);
        letterFrequencyJob.setOutputValueClass(DoubleWritable.class);

        // Set the input and output paths
        FileInputFormat.addInputPath(letterFrequencyJob, new Path(tempOutputFile));
        FileOutputFormat.setOutputPath(letterFrequencyJob, new Path(outputFile));

        // Set the input and output formats
        letterFrequencyJob.setInputFormatClass(TextInputFormat.class);
        letterFrequencyJob.setOutputFormatClass(TextOutputFormat.class);

        System.out.println("Configured letter frequency job");
        return letterFrequencyJob;
    }
}
