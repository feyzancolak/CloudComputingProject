import static org.junit.Assert.*;
import java.io.IOException;
import java.util.Arrays;

import hadoop.LetterFrequency;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;

import org.junit.Before;
import org.junit.Test;

public class LetterFrequencyTest {

    private MapDriver<LongWritable, Text, Text, IntWritable> mapDriver;
    private ReduceDriver<Text, IntWritable, Text, DoubleWritable> reduceDriver;

    @Before
    public void setUp() {
        LetterFrequency.LetterFrequencyMapper mapper = new LetterFrequency.LetterFrequencyMapper();
        LetterFrequency.LetterFrequencyReducer reducer = new LetterFrequency.LetterFrequencyReducer();
        mapDriver = MapDriver.newMapDriver(mapper);
        reduceDriver = ReduceDriver.newReduceDriver(reducer);
    }

    @Test
    public void testMapper() throws IOException {
        mapDriver.withInput(new LongWritable(), new Text("Ciao mondo"));
        Map<String, Integer> expectedOutput = new HashMap<>();
        expectedOutput.put("c", 1);
        expectedOutput.put("i", 1);
        expectedOutput.put("a", 1);
        expectedOutput.put("o", 2);
        expectedOutput.put("m", 1);
        expectedOutput.put("n", 1);
        expectedOutput.put("d", 1);

        for (Map.Entry<String, Integer> entry : expectedOutput.entrySet()) {
            mapDriver.withOutput(new Text(entry.getKey()), new IntWritable(entry.getValue()));
        }

        mapDriver.runTest();
    }

    @Test
    public void testReducer() throws IOException {
        Configuration conf = new Configuration();
        conf.setLong("textLength", 10);
        reduceDriver.setConfiguration(conf);
        reduceDriver.withInput(new Text("a"), Arrays.asList(new IntWritable(1), new IntWritable(1)));
        reduceDriver.withOutput(new Text("a"), new DoubleWritable(0.2));
        reduceDriver.runTest();
    }
}
