package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
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

public class LetterCount {

    // Mapper class to count letters
    public static class LetterCountMapper extends Mapper<LongWritable, Text, Text, LongWritable> {
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
        public void map(LongWritable key, Text value, Context context) {
            // Normalize and convert the line to lowercase based on the specified language
            String line = LanguageNormalizer.normalize(value.toString(), language);
            // Iterate over each character in the line
            for (char c : line.toCharArray()) {
                // Get the count of the character from the map or initialize it to 0 and increment it by 1
                String character = String.valueOf(c);
                Long charCount = charCountMap.getOrDefault(character, 0L) + 1;
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
    public static class LetterCountReducer extends Reducer<Text, LongWritable, Text, LongWritable> {
        private LongWritable result = new LongWritable();

        @Override
        public void reduce(Text key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum = 0;
            // Sum the counts of each letter
            for (LongWritable val : values) {
                sum += val.get();
            }
            result.set(sum);

            // Write the result to the context
            context.write(key, result);
        }
    }

    public static Job configureCountJob(String inputFile, String countFolder, Configuration conf) throws Exception {
        System.out.println("Configuring letter count job");
        Job job = Job.getInstance(conf, "letter count");

        job.setJarByClass(RunProcess.class);
        job.setMapperClass(LetterCount.LetterCountMapper.class);
        job.setReducerClass(LetterCount.LetterCountReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        job.setNumReduceTasks(conf.getInt("numReducers", 1));

        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(countFolder));
        System.out.println("Configured letter count job");

        return job;
    }
}
