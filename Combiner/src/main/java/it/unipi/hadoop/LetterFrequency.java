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
        private static final LongWritable one = new LongWritable(1);
        private Text character = new Text();
        private static String language;

        @Override
        protected void setup(Context context) {
            // Get the language from the context configuration
            if (language == null)
                language = context.getConfiguration().get("language");
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
        private LongWritable result = new LongWritable();

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
        private DoubleWritable result = new DoubleWritable();
        private static Long TEXT_LENGTH;

        @Override
        protected void setup(Context context) {
            // Get the total letter count from the context configuration
            if (TEXT_LENGTH == null)
                TEXT_LENGTH = context.getConfiguration().getLong("totalLetterCount", 0);
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

        // Set the total letter count in the configuration
        conf.setLong("totalLetterCount", totalLetterCount);

        Job letterFrequencyJob = Job.getInstance(conf, "LetterFrequency");

        // Set classes for job
        letterFrequencyJob.setJarByClass(LetterFrequency.class);
        letterFrequencyJob.setMapperClass(MapperFrequency.class);
        letterFrequencyJob.setReducerClass(ReducerFrequency.class);
        letterFrequencyJob.setCombinerClass(CombinerFrequency.class);

        // Set output types
        letterFrequencyJob.setOutputKeyClass(Text.class);
        letterFrequencyJob.setMapOutputValueClass(LongWritable.class);
        letterFrequencyJob.setOutputValueClass(DoubleWritable.class);

        // Set the number of reducers
        letterFrequencyJob.setNumReduceTasks(conf.getInt("numReducers", 1));

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
