import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

//UB ID - aniredn@buffalo.edu, smunagal@buffalo.edu

//Which hall has highest seat utilization in each year? 
//(Only halls which have more than 20 classes in a year are considered)

public class Overbooked {

	public static class Mapper1 extends
			Mapper<Object, Text, Text, DoubleWritable> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {
			String line = value.toString();
			String[] fields = line.split(",");

			// Remove all rows which are formatted incorrectly
			if (isNumeric(fields[7].trim()) && isNumeric(fields[8].trim())) {
				StringBuilder sb = new StringBuilder();
				String[] hall_room = fields[2].split(" ");

				// Remove Arr and Unknowns
				if (!(hall_room[0].trim().equalsIgnoreCase("Arr")
						|| hall_room[0].trim().equalsIgnoreCase("Unknown")
						|| fields[4].trim().equalsIgnoreCase("Unknown")
						|| fields[4].substring(0, 1).equalsIgnoreCase("B") || fields[3]
						.trim().equalsIgnoreCase("Arr"))) {

					// Remove zero Student capacity and seats
					if ((Double.parseDouble(fields[7].trim()) > 0)
							&& (Double.parseDouble(fields[8].trim()) > 0)) {
						String[] sem_year = fields[1].split(" ");
						double cur_students = Double.parseDouble(fields[7]);
						double max_students = Double.parseDouble(fields[8]);

						Double result = cur_students / max_students;

						sb.append(sem_year[1]);
						sb.append("_");
						sb.append(hall_room[0]);

						Text map1Out = new Text(sb.toString());
						context.write(map1Out, new DoubleWritable(result));
					}
				}
			}
		}
	}

	public static class Reducer1 extends
			Reducer<Text, DoubleWritable, Text, DoubleWritable> {

		public void reduce(Text key, Iterable<DoubleWritable> values,
				Context context) throws IOException, InterruptedException {
			int count = 0;
			double sum = 0.0;

			for (DoubleWritable val : values) {
				count = count + 1;
				sum = sum + Double.parseDouble(val.toString());
			}

			//Average seat utilization for each hall
			double ratio = sum / count;

			//Only halls with at least 20 classes are considered
			if (count > 20) {
				context.write(key, new DoubleWritable(ratio));
			}
		}
	}

	public static class Mapper2 extends Mapper<Object, Text, Text, Text> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {

			String key_value[] = value.toString().split("\\t");

			String year_hall[] = key_value[0].split("_");

			StringBuilder sb = new StringBuilder();
			sb.append(year_hall[1]);
			sb.append("_");
			sb.append(key_value[1]);

			context.write(new Text(year_hall[0]), new Text(sb.toString()));

		}
	}

	public static class Reducer2 extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {

			Double max = 0.0;
			String max_hall = new String();

			for (Text val : values) {

				String[] fields = val.toString().split("_");

				Double ratio = Double.parseDouble(fields[1]);

				if (ratio > max) {
					max = ratio;
					max_hall = fields[0];
				}
			}

			StringBuilder sb = new StringBuilder();
			sb.append(max_hall);
			sb.append(" ");
			sb.append(max);
			context.write(key, new Text(sb.toString()));

		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf1 = new Configuration();
		Job job1 = Job.getInstance(conf1, "job1");

		job1.setJarByClass(Overbooked.class);
		job1.setMapperClass(Mapper1.class);
		job1.setReducerClass(Reducer1.class);

		job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(DoubleWritable.class);

		FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]));

		job1.waitForCompletion(true);

		Configuration conf2 = new Configuration();
		Job job2 = Job.getInstance(conf2, "job2");

		job2.setJarByClass(Overbooked.class);
		job2.setMapperClass(Mapper2.class);
		job2.setReducerClass(Reducer2.class);

		job2.setOutputKeyClass(Text.class);
		job2.setOutputValueClass(Text.class);

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