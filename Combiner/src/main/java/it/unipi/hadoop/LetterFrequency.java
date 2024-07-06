package it.unipi.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.util.regex.Pattern;

public class LetterFrequency {

    public static class MapperFrequency extends Mapper<Object, Text, Text, LongWritable> {
        private static final Log LOG = LogFactory.getLog(MapperFrequency.class);

        private Text reducerKey;
        private static LongWritable reducerValue ;
        private static Pattern CHARACTER_PATTERN ;
        private Map<String, Long> characterCounts;

        @Override
        protected void setup(Context context)throws IOException, InterruptedException{
            reducerKey = new Text();
            reducerValue = new LongWritable(1);
            CHARACTER_PATTERN = Pattern.compile("[a-zğüşıöç]", Pattern.CASE_INSENSITIVE);
            characterCounts = new HashMap<>();
            LOG.info("Mapper setup completed.");
        }

        @Override
        protected void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String language = context.getConfiguration().get("language");
            // Convert the line to lower case and remove accents
            String line = LanguageNormalizer.normalize((value.toString()).toLowerCase(),language);
            LOG.info("Processing line: " + line);

            //Emits each letter found in the input text with a count of 1
            for (char ch : line.toCharArray()) {
                // Check if the character is a letter
                if (CHARACTER_PATTERN.matcher(String.valueOf(ch)).matches()) {
                    reducerKey.set(String.valueOf(ch));
                    context.write(reducerKey, reducerValue);

                    String charStr = String.valueOf(ch);
                    characterCounts.put(charStr, characterCounts.getOrDefault(charStr, 0L) + 1);
                    LOG.debug("Character found: " + charStr);
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // Write the character frequencies to the context
            for (Map.Entry<String, Long> entry : characterCounts.entrySet()) {
                reducerKey.set(entry.getKey());
                reducerValue.set(entry.getValue());
                context.write(reducerKey, reducerValue);
            }
            LOG.info("Mapper cleanup completed.");
        }
    }




    public static class CombinerFrequency extends Reducer<Text, LongWritable, Text, LongWritable> {
        private static final Log LOG = LogFactory.getLog(CombinerFrequency.class);
        private LongWritable result = new LongWritable();


        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            //Uses a primitive long variable to sum up the counts.
            long sum = 0;

            //Sets the sum to a reusable LongWritable object (result) and writes the key and the
            //aggregated count to the context.

            // Iterate over the values and sum them up
            for (LongWritable value : values) {
                sum += value.get();
            }

            // Set the sum to the result LongWritable object
            result.set(sum);

            // Write the key and the aggregated count to the context
            context.write(key, result);
            LOG.info("Combined key: " + key + " with sum: " + sum);
        }
    }



    public static class ReducerFrequency extends Reducer<Text, LongWritable, Text, DoubleWritable> {
        private static final Log LOG = LogFactory.getLog(ReducerFrequency.class);
        private static long TEXT_LENGTH;

        //Reads the total text length from the job configuration during the setup phase.
        @Override
        public void setup(Context context) {
            // Configuration
            Configuration conf = context.getConfiguration();
            TEXT_LENGTH = Long.parseLong(conf.get("totalLetterCount"));
            LOG.info("Reducer setup completed with TEXT_LENGTH: " + TEXT_LENGTH);
        }

        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            // Variables
            long sum = 0;

            // Iterate over the values
            for (LongWritable value : values) {
                sum += value.get();
            }

            //Sums the counts of each letter and calculates the relative frequency, writing the result as a DoubleWritable.
            // Write the output
            context.write(key, new DoubleWritable((double) sum / (double) TEXT_LENGTH));
            LOG.info("Reduced key: " + key + " with relative frequency: " + (double) sum / (double) TEXT_LENGTH);
        }

    }



    public static Job getJob(String tempOutputFile, long totalLetterCount, String outputFile, Configuration conf) throws IOException {
        // Set the configuration
        conf.setLong("totalLetterCount", totalLetterCount);

        Job letterFrequencyJob = Job.getInstance(conf, "LetterFrequency");

        // Set the main classes
        letterFrequencyJob.setJarByClass(LetterFrequency.class);
        letterFrequencyJob.setMapperClass(MapperFrequency.class);
        letterFrequencyJob.setReducerClass(ReducerFrequency.class);

        // Set the combiner class
        letterFrequencyJob.setCombinerClass(CombinerFrequency.class);


        // Set the output key and value classes for the mapper
        letterFrequencyJob.setMapOutputKeyClass(Text.class);
        letterFrequencyJob.setMapOutputValueClass(LongWritable.class);

        // Set the output key and value classes for the reducer
        letterFrequencyJob.setOutputKeyClass(Text.class);
        letterFrequencyJob.setOutputValueClass(DoubleWritable.class);

        // Set the input and output paths
        FileInputFormat.addInputPath(letterFrequencyJob, new Path(tempOutputFile));
        FileOutputFormat.setOutputPath(letterFrequencyJob, new Path(outputFile));

        // Set the input and output formats
        letterFrequencyJob.setInputFormatClass(TextInputFormat.class);
        letterFrequencyJob.setOutputFormatClass(TextOutputFormat.class);

        return letterFrequencyJob;
    }


}

