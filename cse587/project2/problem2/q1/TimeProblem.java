import java.io.IOException;
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
//Q1.What is the busiest/most popular hour between 8am and 12pm in each semester?

public class TimeProblem {

	public static class Mapper1 extends Mapper<Object, Text, Text, IntWritable> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] input = line.split(",");

			// Ignore unformatted rows
			if (isNumeric(input[7].trim()) && isNumeric(input[8].trim())) {
				String[] hall = input[2].split(" ");

				// Ignore fields with ARR and Unknowns, and classes before 8am
				if (!(hall[0].trim().equalsIgnoreCase("Arr")
						|| hall[0].trim().equalsIgnoreCase("Unknown")
						|| input[4].trim().equalsIgnoreCase("Unknown") || input[4]
						.substring(0, 1).equalsIgnoreCase("B"))) {
					
					// Ignore zero student capacity and seats
					if ((Integer.parseInt(input[7].trim()) > 0)
							&& (Integer.parseInt(input[8].trim()) > 0)) {
						String[] sem_year = input[1].split(" ");

						StringBuilder sb = new StringBuilder();
						sb.append(sem_year[0]);
						sb.append("_");
						sb.append(sem_year[1]);
						sb.append("_");
						String[] time = input[4].split(" ");
						sb.append(time[0].trim());
						sb.append(time[1].trim());
						sb.append(time[2].trim());

						Text map1Out = new Text(sb.toString());
						context.write(map1Out, new IntWritable(1));
					}
				}
			}
		}
	}

	public static class Reducer1 extends
			Reducer<Text, IntWritable, Text, IntWritable> {

		public void reduce(Text key, Iterable<IntWritable> values,
				Context context) throws IOException, InterruptedException {
			int sum = 0;
			for (IntWritable val : values){
				sum += val.get();
				}
			//Output the number of classes in each time period for every semester
			context.write(key, new IntWritable(sum));
		}
	}

	public static class Mapper2 extends Mapper<Object, Text, Text, Text> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			
			String[] fields = value.toString().split("\\t");
			
			StringBuilder sb = new StringBuilder();
			String[] keyfields = fields[0].split("_");
			sb.append(keyfields[0]);
			sb.append("_");
			sb.append(keyfields[1]);

			Text map2Out = new Text(sb.toString());

			StringBuilder sb1 = new StringBuilder();
			sb1.append(keyfields[2]);
			sb1.append(",");
			sb1.append(fields[1].trim());
			Text value_time_count = new Text(sb1.toString());

			context.write(map2Out, value_time_count);

		}
	}

	public static class Reducer2 extends Reducer<Text, Text, Text, IntWritable> {

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			int max = 0;
			String max_time = new String();

			for (Text val : values) {
				String value_input = val.toString();
				String value_fields[] = value_input.split(",");
				int value = Integer.parseInt(value_fields[1]);
				String time = value_fields[0];

				if (value > max) {
					max = value;
					max_time = time;
				}
			}

			StringBuilder sb = new StringBuilder();
			sb.append(key.toString());
			sb.append(" ");
			sb.append(max_time);
			key.set(sb.toString());
			
			//Output the time period with max number of classes along with the count
			context.write(key, new IntWritable(max));
		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf1 = new Configuration();
		Job job1 = Job.getInstance(conf1, "job1");

		job1.setJarByClass(TimeProblem.class);
		job1.setMapperClass(Mapper1.class);
		job1.setReducerClass(Reducer1.class);

		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]));

		job1.waitForCompletion(true);

		Configuration conf2 = new Configuration();
		Job job2 = Job.getInstance(conf2, "job2");

		job2.setJarByClass(TimeProblem.class);
		job2.setMapperClass(Mapper2.class);
		job2.setReducerClass(Reducer2.class);

		job2.setMapOutputKeyClass(Text.class);
		job2.setMapOutputValueClass(Text.class);

		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(IntWritable.class);

		FileInputFormat.addInputPath(job2, new Path(args[1]));
		FileOutputFormat.setOutputPath(job2, new Path(args[2]));

		System.exit(job2.waitForCompletion(true) ? 0 : 1);
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