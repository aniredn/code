import java.io.IOException;
import java.util.StringTokenizer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

//UB ID - aniredn@buffalo.edu, smunagal@buffalo.edu

public class WordCount {
	public static class TokenizerMapper extends Mapper<Object, Text, Text, IntWritable>{
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {

				String line = value.toString();
				String[] input = line.split(",");
				if(isNumeric(input[7].trim()))
				{
					StringBuilder sb = new StringBuilder();
					String[] hall = input[2].split(" ");
					if(!(hall[0].trim().equalsIgnoreCase("Arr") || hall[0].trim().equalsIgnoreCase("Unknown")    ))
					{
						sb.append(hall[0]);
						sb.append("_");
						sb.append(input[1]);
						Text mapOut = new Text(sb.toString());
						context.write(mapOut, new IntWritable(Integer.parseInt(input[7])));
					}
				}
		}
	}

	public static class IntSumReducer extends Reducer<Text,IntWritable,Text,IntWritable> {
		private IntWritable result = new IntWritable();
			public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
				int sum = 0;
				for (IntWritable val : values)
					sum += val.get();
				result.set(sum);
				context.write(key, result);
			}
		}
	public static void main(String[] args) throws Exception {
		Configuration conf = new Configuration();
		Job job = Job.getInstance(conf, "word count");
		job.setJarByClass(WordCount.class);
		job.setMapperClass(TokenizerMapper.class);
		job.setCombinerClass(IntSumReducer.class);
		job.setReducerClass(IntSumReducer.class);
		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(IntWritable.class);
		FileInputFormat.addInputPath(job, new Path(args[0]));
		FileOutputFormat.setOutputPath(job, new Path(args[1]));
		System.exit(job.waitForCompletion(true) ? 0 : 1);
	}
	public static boolean isNumeric(String str){  
	  try  
	  {  
	    int d = Integer.parseInt(str);  
	  }catch(NumberFormatException nfe)  
	  {  
	    return false;  
	  }  
	  return true;  
	}

}
