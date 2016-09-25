import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

//UB ID - aniredn@buffalo.edu, smunagal@buffalo.edu
//Q5.What is the change in number of students writing exams in each department in consecutive years?

public class ExamEnroll {

	public static class Mapper1 extends Mapper<Object, Text, Text, Text> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {

			String[] input = value.toString().split("\\t");

			System.out.println(input[0]);
			
			if(isNumeric(input[0].trim())){
			String[] year = input[3].trim().split(" ");

			StringBuilder sb = new StringBuilder();
			sb.append(year[1].trim());
			sb.append("_");
			sb.append(input[12].trim());

			Text key1 = new Text(input[4].trim());
			Text value1 = new Text(sb.toString());

			context.write(key1, value1);
			}
		}
	}

	public static class Reducer1 extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			// key = cse , values = 2011_22;
			int sum_2011 = 0;
			int sum_2012 = 0;
			int sum_2013 = 0;
			int sum_2014 = 0;
			int sum_2015 = 0;
			int sum_2016 = 0;

			for (Text val : values) {
				String[] value = val.toString().split("_");
				if (Integer.parseInt(value[0]) == 2011)
					sum_2011 += Integer.parseInt(value[1]);
				if (Integer.parseInt(value[0]) == 2012)
					sum_2012 += Integer.parseInt(value[1]);
				if (Integer.parseInt(value[0]) == 2013)
					sum_2013 += Integer.parseInt(value[1]);
				if (Integer.parseInt(value[0]) == 2014)
					sum_2014 += Integer.parseInt(value[1]);
				if (Integer.parseInt(value[0]) == 2015)
					sum_2015 += Integer.parseInt(value[1]);
				if (Integer.parseInt(value[0]) == 2016)
					sum_2016 += Integer.parseInt(value[1]);
			}

			Text value_2011 = new Text(2011 + "_" + String.valueOf(sum_2011));
			Text value_2012 = new Text(2012 + "_" + String.valueOf(sum_2012));
			Text value_2013 = new Text(2013 + "_" + String.valueOf(sum_2013));
			Text value_2014 = new Text(2014 + "_" + String.valueOf(sum_2014));
			Text value_2015 = new Text(2015 + "_" + String.valueOf(sum_2015));
			Text value_2016 = new Text(2016 + "_" + String.valueOf(sum_2016));

			context.write(key, value_2011);
			context.write(key, value_2012);
			context.write(key, value_2013);
			context.write(key, value_2014);
			context.write(key, value_2015);
			context.write(key, value_2016);

		}
	}
	
	public static class Mapper2 extends Mapper<Object, Text, Text, Text> {

		public void map(Object key, Text value, Context context)
				throws IOException, InterruptedException {

			String[] input = value.toString().split("\\t");
			
			Text key1 = new Text(input[0].trim());
			Text value1 =  new Text(input[1].trim());

			context.write(key1, value1);
		}
	}

	public static class Reducer2 extends Reducer<Text, Text, Text, Text> {

		public void reduce(Text key, Iterable<Text> values, Context context)
				throws IOException, InterruptedException {
			// input key = cse , values = 2011_22;
			int[] students = new int[6];
			int count = 0;
			for (Text val : values) {
				String[] value = val.toString().split("_");
				if(value.length > 1){
					if(value[0].trim().equalsIgnoreCase("2011"))
						students[0] = Integer.parseInt(value[1].trim());

					if(value[0].trim().equalsIgnoreCase("2012"))
						students[1] = Integer.parseInt(value[1].trim());

					if(value[0].trim().equalsIgnoreCase("2013"))
						students[2] = Integer.parseInt(value[1].trim());

					if(value[0].trim().equalsIgnoreCase("2014"))
						students[3] = Integer.parseInt(value[1].trim());

					if(value[0].trim().equalsIgnoreCase("2015"))
						students[4] = Integer.parseInt(value[1].trim());

					if(value[0].trim().equalsIgnoreCase("2016"))
						students[5] = Integer.parseInt(value[1].trim());
				}
			}

			String course = key.toString();

			context.write(new Text(course + "_" + "2011"+"-"+"2012"), new Text(String.valueOf(Math.abs(students[0] - students[1]))));
			context.write(new Text(course + "_" + "2012"+"-"+"2013"), new Text(String.valueOf(Math.abs(students[1] - students[2]))));
			context.write(new Text(course + "_" + "2013"+"-"+"2014"), new Text(String.valueOf(Math.abs(students[2] - students[3]))));
			context.write(new Text(course + "_" + "2014"+"-"+"2015"), new Text(String.valueOf(Math.abs(students[3] - students[4]))));
			context.write(new Text(course + "_" + "2015"+"-"+"2016"), new Text(String.valueOf(Math.abs(students[4] - students[5]))));
			
			for(int i=0;i<students.length;i++)
				students[i] = 0;
			course ="";

		}
	}

	public static void main(String[] args) throws Exception {

		Configuration conf1 = new Configuration();
		Job job1 = Job.getInstance(conf1, "job1");
        
		job1.setJarByClass(ExamEnroll.class);
		job1.setMapperClass(Mapper1.class);
		job1.setReducerClass(Reducer1.class);
		
        job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(Text.class);
		
        FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]));

		job1.waitForCompletion(true);


		Configuration conf2 = new Configuration();
		Job job2 = Job.getInstance(conf2, "job2");
        
		job2.setJarByClass(ExamEnroll.class);
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