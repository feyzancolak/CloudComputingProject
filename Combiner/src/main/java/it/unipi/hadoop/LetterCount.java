package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
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
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class LetterCount{

    //processes each line of the input text, identifies individual characters, and emit each letter a key with a count of '1' as the value.
    public static class MapperCounter extends Mapper<Object, Text,Text, LongWritable>{

        private static Pattern CHARACTER_PATTERN = null;
        private Text character = new Text();
        private Map<String, Long> characterCounts;

        @Override
        protected void setup(Context context)throws IOException, InterruptedException {
            //initialize the character count map and pattern for valid characters
            CHARACTER_PATTERN = Pattern.compile("[a-zğüşıöç]", Pattern.CASE_INSENSITIVE); //regex pattern to match letters with case insensitive
            characterCounts = new HashMap<>(); // Initialize the map to store character counts
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException{

            String language = context.getConfiguration().get("language");
            String line = LanguageNormalizer.normalize(value.toString().toLowerCase(),language); //convert the line to lowercase

            for(char ch: line.toCharArray()){ //iterate through each character in the line
                if(CHARACTER_PATTERN.matcher(String.valueOf(ch)).matches()){  //use regex to check if the character is a letter

                    String charStr = String.valueOf(ch);
                    characterCounts.put(charStr, characterCounts.getOrDefault(charStr, 0L) + 1); // Update the count for the character
                }
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException{
            // Write the character counts to the context
            for (Map.Entry<String, Long> entry : characterCounts.entrySet()) {
                character.set(entry.getKey());
                context.write(character, new LongWritable(entry.getValue()));
            }
        }
    }


    //The Reducer sums up the counts for each character received from the mapper.
    public static class ReducerCounter extends Reducer<Text, LongWritable, Text, LongWritable>{
        private final LongWritable result = new LongWritable();; //hadoop longwritable object to hold the sum of the counts for each character


        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException{
            long sum = 0;
            for(LongWritable val: values){ //iterate over all values associated with the key
                sum += val.get(); //Sum the key
            }
            result.set(sum); //set the sum as the result
            context.write(key, result); //write the key (letter) and its total count to the context
        }
    }

    public static Job configureCountJob(Configuration conf, String countFolder, String inputFile) throws IOException{
        System.out.println("Configuring Letter Count job");

        Job letterCountJob = Job.getInstance(conf, "Letter Count");

        //Set the main classes
        letterCountJob.setJarByClass(LetterCount.class);
        letterCountJob.setMapperClass(MapperCounter.class);
        letterCountJob.setCombinerClass(ReducerCounter.class);
        letterCountJob.setReducerClass(ReducerCounter.class);

        // Set the number of reducers
        letterCountJob.setNumReduceTasks(conf.getInt("numReducers", 1));

        // Set the output key and value classes for the mapper, combiner and reducer
        letterCountJob.setOutputKeyClass(Text.class);
        letterCountJob.setOutputValueClass(LongWritable.class);

        // Set the input and output paths
        FileInputFormat.addInputPath(letterCountJob, new Path(inputFile));
        FileOutputFormat.setOutputPath(letterCountJob, new Path(countFolder));

        // Set the input and output formats
        letterCountJob.setInputFormatClass(TextInputFormat.class);
        letterCountJob.setOutputFormatClass(TextOutputFormat.class);

        System.out.println("Configured Letter Count job");
        return letterCountJob;
    }
}
