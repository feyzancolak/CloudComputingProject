package it.unipi.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

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
    public static class LetterFrequencyMapper extends Mapper<LongWritable, Text, Text, IntWritable> {
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
                    // Update the character frequency map
                    charFrequencyMap.put(character, charFrequencyMap.getOrDefault(character, 0) + 1);
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // Write the character frequencies to the context
            for (Map.Entry<String, Integer> entry : charFrequencyMap.entrySet()) {
                context.write(new Text(entry.getKey()), new IntWritable(entry.getValue()));
            }
        }
    }

    // Reducer class to calculate letter frequencies
    public static class LetterFrequencyReducer extends Reducer<Text, IntWritable, Text, DoubleWritable> {
        private DoubleWritable frequency = new DoubleWritable();
        private long totalLetterCount;

        @Override
        protected void setup(Context context) throws IOException, InterruptedException {
            // Get the total text length from the context configuration
            totalLetterCount = context.getConfiguration().getLong("totalLetterCount", 0);
        }

        @Override
        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
            int sum = 0;
            // Sum the counts of each letter
            for (IntWritable val : values) {
                sum += val.get();
            }
            // Calculate the frequency of the letter
            double freq = (double) sum / totalLetterCount;
            frequency.set(freq);
            // Write the result to the context
            context.write(key, frequency);
        }
    }

}
