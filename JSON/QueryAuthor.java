package org.hwone;
import java.io.IOException;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.GenericOptionsParser;
import org.json.*;


//TODO import necessary components

/*
*  Modify this file to return single combined books from the author which
*  is queried as QueryAuthor <in> <out> <author>. 
*  i.e. QueryAuthor in.txt out.txt Tobias Wells 
*  {"author": "Tobias Wells", "books": [{"book":"A die in the country"},{"book": "Dinky died"}]}
*  Beaware that, this may work on anynumber of nodes! 
*
*/

public class QueryAuthor {

  //TODO define variables and implement necessary components

 public static class Map extends Mapper<LongWritable, Text, Text, Text>{

        public void map(LongWritable key, Text value, Context context) throws IOException, InterruptedException{
		
	    Configuration conf = context.getConfiguration();
	    //Get the whole name in mapper
	    String authorIP = conf.get("wholename");
	    String author;
	    String book;
	    //Replace wildcard with nothing
	    String strippedInput=authorIP.replaceAll("[\\W]","");
	    String line=value.toString();
            String[] tuple = line.split("\\n");
            try{
                for(int i=0;i<tuple.length; i++){
                    JSONObject obj = new JSONObject(tuple[i]);
                    author = obj.getString("author");
                    book = obj.getString("book");
	//	    System.out.println(authorInput);
        	    //Strip author value to match with author name entered
             	    String authorname=author.replaceAll("[\\W]","");

		    if (authorname.compareTo(strippedInput)==0)
		    {
                    context.write(new Text(author), new Text(book));
		    }
                }
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
    }

public static class Combine extends Reducer<Text,Text,Text,Text>{

        public void combine(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{

        String booklist = null;

        for(Text val : values){
            if(booklist.equals(null)){
                booklist = booklist + val.toString();
            }
            else{
                booklist = booklist + "," + val.toString();
            }

        }
        context.write(key, new Text(booklist));
    }
    }

 public static class Reduce extends Reducer<Text,Text,NullWritable,Text>{

        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException{

            try{
                JSONObject obj = new JSONObject();
                JSONArray ja = new JSONArray();
                for(Text val : values){
                    JSONObject jo = new JSONObject().put("book", val.toString());
                    ja.put(jo);
                }
                obj.put("books", ja);
                obj.put("author", key.toString());
                context.write(NullWritable.get(), new Text(obj.toString()));
            }catch(JSONException e){
                e.printStackTrace();
            }
        }
    }


  public static void main(String[] args) throws Exception {
	  
    
    Configuration conf = new Configuration();
    String[] otherArgs = new GenericOptionsParser(conf, args)
                                .getRemainingArgs();

        if (args.length > 6) {
            System.err.println("Usage: CombineBooks <in> <out> <first name> <middle name> <last name> <suffix>");
            System.exit(2);
        }
	String wholename="";
	//Using switch case to determine how many parts of names were entered. 
	switch(args.length)
	{
	case 3:
	conf.set("authorFname",args[2]);
        wholename=conf.get("authorFname");
	break;
	case 4:
	conf.set("authorFname",args[2]);
	conf.set("authorMname",args[3]);
	wholename=conf.get("authorFname")+""+conf.get("authorMname");
	break;
	case 5:
	conf.set("authorFname",args[2]);
	conf.set("authorMname",args[3]);
	conf.set("authorLname",args[4]);
        wholename=conf.get("authorFname")+""+conf.get("authorMname")+""+conf.get("authorLname");
	break;
	case 6:
	conf.set("authorFname",args[2]);
	conf.set("authorMname",args[3]);
	conf.set("authorLname",args[4]);
	conf.set("authorSuf",args[5]);
        wholename=conf.get("authorFname")+""+conf.get("authorMname")+""+conf.get("authorLname")+""+conf.get("authorSuf");
	break;
	
	}
	conf.set("wholename",wholename);

	System.out.println(wholename);
        Job job = new Job(conf, "QueryAuthor");
        job.setJarByClass(QueryAuthor.class);
        job.setMapperClass(Map.class);
        job.setCombinerClass(Combine.class);
        job.setReducerClass(Reduce.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        job.setInputFormatClass(TextInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));


        System.exit(job.waitForCompletion(true) ? 0 : 1);

  }
}
