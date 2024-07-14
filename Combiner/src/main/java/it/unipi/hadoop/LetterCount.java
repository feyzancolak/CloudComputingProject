package it.unipi.hadoop;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Partitioner;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;

public class LetterCount {

    // Mapper: emits a count of 1 for each character on the same key
    public static class LetterCountMapper extends Mapper<Object, Text, NullWritable, LongWritable> {
        private static final LongWritable one = new LongWritable(1);
        private static final NullWritable totalCountKey = NullWritable.get();
        private static String language;

        @Override
        protected void setup(Context context) {
            if (language == null)
                language = context.getConfiguration().get("language");
        }

        @Override
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = LanguageNormalizer.normalize(value.toString().toLowerCase(), language);

            for (char ignored : line.toCharArray()) {
                context.write(totalCountKey, one);
            }
        }
    }

    // Reducer: sums up the counts for each character received from the mapper
    public static class LetterCountCombiner extends Reducer<NullWritable, LongWritable, NullWritable, LongWritable> {
        private LongWritable result = new LongWritable();

        @Override
        public void reduce(NullWritable key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum = 0;
            for (LongWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(key, result);
        }
    }

    // Partitioner: partitions based on a random number between 0 and numReduceTasks
    public static class LetterCountPartitioner extends Partitioner<NullWritable, LongWritable> {
        @Override
        public int getPartition(NullWritable key, LongWritable value, int numReduceTasks) {
            return (int)(Math.random() * numReduceTasks);
        }
    }

    // Reducer: sums up the counts for each character received from the mapper
    public static class LetterCountReducer extends Reducer<NullWritable, LongWritable, Text, LongWritable> {
        private static final Text totalCountKey = new Text("total_count");
        private LongWritable result = new LongWritable();

        @Override
        public void reduce(NullWritable key, Iterable<LongWritable> values, Context context) throws IOException, InterruptedException {
            long sum = 0;
            for (LongWritable val : values) {
                sum += val.get();
            }
            result.set(sum);
            context.write(totalCountKey, result);
        }
    }

    // Configure and run the MapReduce job
    public static Job configureCountJob(Configuration conf, String countFolder, String inputFile) throws IOException {
        System.out.println("Configuring letter count job");

        Job letterCountJob = Job.getInstance(conf, "Letter Count");

        // Set classes for job
        letterCountJob.setJarByClass(LetterCount.class);
        letterCountJob.setMapperClass(LetterCountMapper.class);
        letterCountJob.setCombinerClass(LetterCountCombiner.class);
        letterCountJob.setPartitionerClass(LetterCountPartitioner.class);
        letterCountJob.setReducerClass(LetterCountReducer.class);

        // Set output types
        letterCountJob.setMapOutputKeyClass(NullWritable.class);
        letterCountJob.setOutputKeyClass(Text.class);
        letterCountJob.setOutputValueClass(LongWritable.class);

        // Set the number of reducers
        letterCountJob.setNumReduceTasks(conf.getInt("numReducers", 1));

        // Set the input and output paths
        FileInputFormat.addInputPath(letterCountJob, new Path(inputFile));
        FileOutputFormat.setOutputPath(letterCountJob, new Path(countFolder));

        // Set the input and output formats
        letterCountJob.setInputFormatClass(TextInputFormat.class);
        letterCountJob.setOutputFormatClass(TextOutputFormat.class);

        System.out.println("Configured letter count job");
        return letterCountJob;
    }
}