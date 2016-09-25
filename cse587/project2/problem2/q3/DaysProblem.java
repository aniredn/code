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

//UB ID - aniredn@buffalo.edu, smunugal@buffalo.edu
//Q3.Which is the most used hall by seat capacity on each working day in every year?

public class DaysProblem {

	public static class Mapper1 extends Mapper<Object, Text, Text, IntWritable> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] input = line.split(",");

			if (isNumeric(input[7].trim()) && isNumeric(input[8].trim())) {

				String[] hall = input[2].split(" ");

				// Remove Arr and Unknowns
				if (!(hall[0].trim().equalsIgnoreCase("Arr")
						|| hall[0].trim().equalsIgnoreCase("Unknown")
						|| input[4].trim().equalsIgnoreCase("Unknown") || input[4]
						.substring(0, 1).equalsIgnoreCase("B"))) {
					// Remove zero Student capacity and seats
					if ((Integer.parseInt(input[7].trim()) > 0)
							&& (Integer.parseInt(input[8].trim()) > 0)) {
						// Splitting semester and year
						String[] sem_year = input[1].split(" ");
						// last 10 years data only used here for summarization
						if ((Integer.parseInt(sem_year[1].trim()) > 2005)
								&& (Integer.parseInt(sem_year[1].trim()) < 2017)) {

							StringBuilder sb = new StringBuilder();

							sb.append(sem_year[1]);
							sb.append("_");
							sb.append(hall[0]);
							sb.append("_");

							int student_enrolled = Integer.parseInt(input[7]);

							String tempKey = sb.toString();
							if (input[3].trim().equalsIgnoreCase("M-F")) {
								Text map1Out = new Text(tempKey + "M");
								context.write(map1Out, new IntWritable(
										student_enrolled));
								Text map2Out = new Text(tempKey + "T");
								context.write(map2Out, new IntWritable(
										student_enrolled));
								Text map3Out = new Text(tempKey + "W");
								context.write(map3Out, new IntWritable(1));
								Text map4Out = new Text(tempKey + "R");
								context.write(map4Out, new IntWritable(
										student_enrolled));
								Text map5Out = new Text(tempKey + "F");
								context.write(map5Out, new IntWritable(
										student_enrolled));

							} else if (isday(input[3].trim())) {

								char[] ca = input[3].trim().toCharArray();
								for (char c : ca) {
									Text map1Out = new Text(tempKey + c);
									context.write(map1Out, new IntWritable(
											student_enrolled));

								}
							}
						}
					}
				}
			}
		}
	}

	// Grouping multiple entries in Reducer 1
	public static class Reducer1 extends
			Reducer<Text, IntWritable, Text, IntWritable> {

		public void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values)
				sum += val.get();
			context.write(key, new IntWritable(sum));
		}
	}

	// Summarizing grouped keys on days and halls
	public static class Mapper2 extends Mapper<Object, Text, Text, Text> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {

			String[] fields = value.toString().split("\\t");
			String[] keyfields = fields[0].split("_");

			StringBuilder sb = new StringBuilder();

			sb.append(keyfields[0]);
			sb.append("_");
			sb.append(keyfields[2]);
			Text map2Out = new Text(sb.toString());

			context.write(map2Out, new Text(fields[1] + "_" + keyfields[1]));

		}
	}

	public static class Reducer2 extends Reducer<Text, Text, Text, Text> {
		int max = 0;
		String hall = "";

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			String[] keys = key.toString().split("_");
			if (!keys[1].equalsIgnoreCase("A")) {
				for (Text val1 : values) {
					String[] val = val1.toString().split("_");
					if (Integer.parseInt(val[0]) > max)
						{max = Integer.parseInt(val[0]);
					hall = val[1];
						}
				}
				context.write(key, new Text(String.valueOf(max) + "_" + hall));
				max = 0;
				hall = "";
			}
		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf1 = new Configuration();
		Job job1 = Job.getInstance(conf1, "job1");

		job1.setJarByClass(DaysProblem.class);
		job1.setMapperClass(Mapper1.class);
		job1.setCombinerClass(Reducer1.class);
		job1.setReducerClass(Reducer1.class);

		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]));

		job1.waitForCompletion(true);

		Configuration conf2 = new Configuration();
		Job job2 = Job.getInstance(conf2, "job2");

		job2.setJarByClass(DaysProblem.class);
		job2.setMapperClass(Mapper2.class);
		job2.setCombinerClass(Reducer2.class);
		job2.setReducerClass(Reducer2.class);

		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(Text.class);

		FileInputFormat.addInputPath(job2, new Path(args[1]));
		FileOutputFormat.setOutputPath(job2, new Path(args[2]));

		System.exit(job2.waitForCompletion(true) ? 0 : 1);
	}

	public static boolean isday(String str) {
		// TODO Auto-generated method stub
		String s = str.substring(0, 1);
		if (str.contains("A") || str.contains("H")) {
			return false;
		} else if ((s.trim().equalsIgnoreCase("M"))
				|| (s.trim().equalsIgnoreCase("T"))
				|| (s.trim().equalsIgnoreCase("W"))
				|| (s.trim().equalsIgnoreCase("R"))
				|| (s.trim().equalsIgnoreCase("F"))) {

			return true;
		}

		return false;
	}

	public static boolean isNumeric(String str) {
		try {
			int d = Integer.parseInt(str);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

}