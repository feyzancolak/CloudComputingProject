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

public class LetterCount {

    //processes each line of the input text, identifies individual characters, and emit each letter a key with a count of '1' as the value.
    public static class MapperCounter extends Mapper<Object, Text,Text, LongWritable>{
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
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = LanguageNormalizer.normalize(value.toString().toLowerCase(),language); //convert the line to lowercase

            for (char ch: line.toCharArray()){ //iterate through each character in the line
                    String charStr = String.valueOf(ch);
                    character.set(charStr);
                    // Emit each character with a count of 1
                    context.write(character, one);
            }
        }
    }

    //The Reducer sums up the counts for each character received from the mapper.
    public static class ReducerCounter extends Reducer<Text, LongWritable, Text, LongWritable>{
        private LongWritable result = new LongWritable();

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
