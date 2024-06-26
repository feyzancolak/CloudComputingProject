package it.unipi.hadoop;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import org.apache.hadoop.conf.Configuration;

//the Mapper emits key-value pairs where the key is the character itself,
// and the value is 1. The Reducer then sums up the counts for each
// character. This approach allows you to see the count for each character
// separately.

public class LetterCount{

    //processes each line of the input text, identifies individual characters, and emita each letter a key with a count of '1' as the value.
    public static class MapperCounter extends Mapper<Object, Text,Text, LongWritable>{

        private final static LongWritable one = new LongWritable(1); //A constant longwrirable object with value 1 for each letter found
        private static Pattern CHARACTER_PATTERN = null;
        private Text character;
        private Map<String, Long> characterCounts;

        @Override
        protected void setup(Context context)throws IOException, InterruptedException {
            //initialize the character count map and pattern for valid characters
            CHARACTER_PATTERN = Pattern.compile("[a-zğüşıöç]", Pattern.CASE_INSENSITIVE); //regex pattern to match letters with case insensitive
            character = new Text(); //hadoop text object to hold single character
            characterCounts = new HashMap<>(); // Initialize the map to store character counts
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException{
            String language = context.getConfiguration().get("language");

            String line = LanguageNormalizer.normalize(value.toString().toLowerCase(),language); //convert the line to lowercase
            for(char ch: line.toCharArray()){ //iterate through each character in the line
                if(CHARACTER_PATTERN.matcher(String.valueOf(ch)).matches()){  //use regex to check if the character is a letter
                    character.set(String.valueOf(ch)); //set the character as the key
                    context.write(character, one); //write the key-value pair to the context with value of '1'

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


    public static class PartitionerCounter extends Partitioner<Text, LongWritable>{
        //even distribution of data among the reducer rather than random distribution.
        //determines which reducer will process which key-value pairs
        @Override
        public int getPartition(Text key, LongWritable value, int numReduceTasks){
            return (key.hashCode() & Integer.MAX_VALUE) % numReduceTasks;
            //calculate the has coode of the letter.The same key will always go to the same partition.
            //use and with Integer.MAX_VALUE to ensure the hash code is non-negative
            //compute partition by taking the modulo of the hash code with the number of reducers.
            // This operation takes the non-negative hash code and computes the modulus with the
            // number of reduce tasks. This ensures that the resulting partition number is within
            // the range 0 to numReduceTasks - 1. Essentially, it maps the hash code to one of the
            // available reducer indices, ensuring an even distribution.
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

    public static Job getJob(Configuration conf, String tempOutputFile, String inputFile) throws IOException{
        Job letterCountJob = Job.getInstance(conf, "Letter Count");

        //Set the main classes
        letterCountJob.setJarByClass(LetterCount.class);
        letterCountJob.setMapperClass(MapperCounter.class);
        letterCountJob.setCombinerClass(ReducerCounter.class);
        letterCountJob.setReducerClass(ReducerCounter.class);

        //set the partitioner
        letterCountJob.setPartitionerClass(PartitionerCounter.class);
        letterCountJob.setNumReduceTasks(conf.getInt("numReducers", 1));

        // Set the output key and value classes for the mapper
        letterCountJob.setMapOutputKeyClass(Text.class);
        letterCountJob.setMapOutputValueClass(LongWritable.class);

        // Set the output key and value classes for the reducer
        letterCountJob.setOutputKeyClass(Text.class);
        letterCountJob.setOutputValueClass(LongWritable.class);

        // Set the input and output paths
        FileInputFormat.addInputPath(letterCountJob, new Path(inputFile));
        FileOutputFormat.setOutputPath(letterCountJob, new Path(tempOutputFile));

        // Set the input and output formats
        letterCountJob.setInputFormatClass(TextInputFormat.class);
        letterCountJob.setOutputFormatClass(TextOutputFormat.class);

        return letterCountJob;
    }
}

















