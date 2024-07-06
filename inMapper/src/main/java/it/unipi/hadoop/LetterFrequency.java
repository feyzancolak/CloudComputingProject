package it.unipi.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import it.unipi.hadoop.LanguageNormalizer;

public class LetterFrequency {

    // Mapper class to count letter frequencies
    public static class LetterFrequencyMapper extends Mapper<LongWritable, Text, Text, DoubleWritable> {
        private Map<String, Integer> charFrequencyMap;
        private Pattern charPattern;

        @Override
        protected void setup(Context context) {
            // Initialize the character frequency map and pattern for valid characters
            charFrequencyMap = new HashMap<>();
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
                    // Emit each character with a DoubleWritable value of 1.0
                    context.write(new Text(character), new DoubleWritable(1.0));
                }
            }
            System.out.println("Line: " + line);
        }
    }


    // Reducer class to calculate letter frequencies
    public static class LetterFrequencyReducer extends Reducer<Text, DoubleWritable, Text, DoubleWritable> {
        private long totalLetterCount;
        private static final Log LOG = LogFactory.getLog(LetterFrequencyReducer.class);


        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            // Get the total text length from the context configuration
            totalLetterCount = context.getConfiguration().getLong("totalLetterCount", 0);
            LOG.info("Total letter count: " + totalLetterCount);

        }

        @Override
        public void reduce(Text key, Iterable<DoubleWritable> values, Context context) throws IOException, InterruptedException {
            double sum = 0.0;
            // Sum the counts of each letter
            for (DoubleWritable val : values) {
                sum += val.get();
            }
            // Log the sum for debugging
            LOG.info("Sum for key " + key.toString() + ": " + sum);

            // Calculate the frequency of the letter
            double freq = sum / totalLetterCount;
            LOG.info("Frequency for key " + key + ": " + freq);
            context.write(key, new DoubleWritable(freq));
        }
    }



}
