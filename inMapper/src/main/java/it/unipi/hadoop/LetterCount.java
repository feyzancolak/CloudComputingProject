package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LetterCount {

    // Mapper class to count letters
    public static class LetterCountMapper extends Mapper<Object, Text, IntWritable, LongWritable> {
        private static final IntWritable totalCountKey = new IntWritable();
        private Map<Integer, Long> charCountMap = new HashMap<>();
        private LongWritable charCount = new LongWritable();
        private static String language;
        private static Integer numReducers;

        @Override
        protected void setup(Context context) {
            // Get the language from the context configuration
            if (language == null)
                language = context.getConfiguration().get("language");
            numReducers = context.getNumReduceTasks();
        }

        @Override
        public void map(Object key, Text value, Context context) {
            // Normalize and convert the line to lowercase based on the specified language
            String line = LanguageNormalizer.normalize(value.toString(), language);
            // Iterate over each character in the line
            for (char ignored : line.toCharArray()) {
                Integer selectedReducer = (int)(Math.random() * numReducers);
                // Increment the character count associated with the selected reducer
                charCountMap.put(selectedReducer, charCountMap.getOrDefault(selectedReducer, 0L) + 1);
            }
        }

        @Override
        protected void cleanup(Context context) throws IOException, InterruptedException {
            // Write the character count to the context
            for (Map.Entry<Integer, Long> entry : charCountMap.entrySet()) {
                totalCountKey.set(entry.getKey());
                charCount.set(entry.getValue());
                context.write(totalCountKey, charCount);
            }
        }
    }

    public static class LetterCountPartitioner extends Partitioner<IntWritable, LongWritable> {
        @Override
        public int getPartition(IntWritable key, LongWritable value, int numReduceTasks) {
            return key.get();
        }
    }

    // Reducer class to sum the counts of letters
    public static class LetterCountReducer extends Reducer<IntWritable, LongWritable, Text, LongWritable> {
        private static final Text totalCountKey = new Text("total_count");
        private LongWritable result = new LongWritable();

        @Override
        public void reduce(IntWritable key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum = 0;
            // Sum the counts of each letter
            for (LongWritable val : values) {
                sum += val.get();
            }
            result.set(sum);

            // Write the result to the context
            context.write(totalCountKey, result);
        }
    }

    public static Job configureCountJob(String inputFile, String countFolder, Configuration conf) throws Exception {
        System.out.println("Configuring letter count job");

        Job job = Job.getInstance(conf, "letter count");

        // Set classes for job
        job.setJarByClass(RunProcess.class);
        job.setMapperClass(LetterCount.LetterCountMapper.class);
        job.setPartitionerClass(LetterCount.LetterCountPartitioner.class);
        job.setReducerClass(LetterCount.LetterCountReducer.class);

        // Set output types
        job.setMapOutputKeyClass(IntWritable.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(LongWritable.class);

        // Set the number of reducers
        job.setNumReduceTasks(conf.getInt("numReducers", 1));

        // Set the input and output paths
        FileInputFormat.addInputPath(job, new Path(inputFile));
        FileOutputFormat.setOutputPath(job, new Path(countFolder));

        System.out.println("Configured letter count job");
        return job;
    }
}
