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

////UB ID - aniredn@buffalo.edu, smunagal@buffalo.edu
//Q4.What is the difference in course enrollments in Spring and Fall semesters every year?

public class CourseEnroll {
	
public static class Mapper1 extends Mapper<Object, Text, Text, IntWritable>{
		
		public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
				String line = value.toString();
				String[] fields = line.split(",");
				
				if(isNumeric(fields[7].trim()) && isNumeric(fields[8].trim())){
				StringBuilder sb = new StringBuilder();
				String[] hall_room = fields[2].split(" ");
				String sem_year[]= fields[1].split(" ");
				
				// Remove Arr and Unknowns
				if(!(hall_room[0].trim().equalsIgnoreCase("Arr") || hall_room[0].trim().equalsIgnoreCase("Unknown") || 
					fields[4].trim().equalsIgnoreCase("Unknown") ||  fields[4].substring(0,1).equalsIgnoreCase("B")
					|| fields[3].equalsIgnoreCase("ARR"))){
						sb.append(sem_year[0]);
						sb.append("_");
						sb.append(sem_year[1]);
						
						int num = Integer.parseInt(fields[7]);
					
						context.write(new Text(sb.toString()), new IntWritable(num));
					}
				}
			}
		}
	

	public static class Reducer1 extends Reducer<Text,IntWritable,Text,IntWritable> {
		
			public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException, InterruptedException {
				int sum = 0;
				for (IntWritable val : values)
					sum += val.get();
				//Output the total enrollments in every semester 
				context.write(key, new IntWritable(sum));
			}
	}

	public static class Mapper2 extends Mapper<Object, Text, Text, Text>{

		public void map(Object key, Text value, Context context) throws IOException, InterruptedException{
			String[] fields=value.toString().split("\\t");
			String sem_year[] = fields[0].split("_");

			if(sem_year[0].equalsIgnoreCase("Spring") || sem_year[0].equalsIgnoreCase("Fall")){
				
				context.write(new Text(sem_year[1]), new Text(sem_year[0]+"_"+fields[1]));
			}
		}
	}


	public static class Reducer2 extends Reducer<Text,Text,Text,IntWritable>{

		public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
    	
			int result = 0;
			int spring_val =0;
			int fall_val = 0;
			
			for(Text val:values){
				
				String[] sem_value = val.toString().split("_");
				
				if(sem_value[0].equalsIgnoreCase("SPRING"))
					spring_val = Integer.parseInt(sem_value[1]);
				else if(sem_value[0].equalsIgnoreCase("FALL"))
					fall_val = Integer.parseInt(sem_value[1]);
			}

			if(fall_val >0 && spring_val>0){
				result = fall_val - spring_val;
				StringBuilder sb = new StringBuilder();
				sb.append(key.toString());
				sb.append(" Fall - Spring");
				context.write(new Text(sb.toString()),new IntWritable(result));
			}

    	}
	}


	public static void main(String[] args) throws Exception {
		
		Configuration conf1 = new Configuration();
		Job job1 = Job.getInstance(conf1, "job1");
        
		job1.setJarByClass(CourseEnroll.class);
		job1.setMapperClass(Mapper1.class);
		job1.setReducerClass(Reducer1.class);
		
        job1.setOutputKeyClass(Text.class);
		job1.setOutputValueClass(IntWritable.class);
		
        FileInputFormat.addInputPath(job1, new Path(args[0]));
		FileOutputFormat.setOutputPath(job1, new Path(args[1]));

		job1.waitForCompletion(true);


		Configuration conf2 = new Configuration();
		Job job2 = Job.getInstance(conf2, "job2");
        
		job2.setJarByClass(CourseEnroll.class);
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
