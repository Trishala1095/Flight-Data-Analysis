import java.io.IOException;
import java.util.StringTokenizer;
import java.util.*;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;

import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class FlightAnalysisMainFunction {

    public static class ScheduleFMapper extends Mapper<Object, Text, Text, IntWritable>{

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            final int max_delay = 10;
            String line = value.toString();
            String[] tokens = line.split(","); 
            String year = tokens[0];
            String flight_carrier = tokens[8];
            String arrival_delay = tokens[14];
            String depart_delay = tokens[15];
             //When arrival delay and departure delay are less than maximum delay, flight is on schedule and give 1 else give 0 and delayed
          
      	    if(!year.equals("NA") && !year.equals("Year") &&
                      !flight_carrier.equals("NA") && !flight_carrier.equals("Flight Carrier") &&
                      !arrival_delay.equals("NA") && !arrival_delay.equals("Arrival Delays") &&
                      !depart_delay.equals("NA") && !depart_delay.equals("Departure Delays") )
            {
                if ( (Integer.parseInt(arrival_delay) + Integer.parseInt(depart_delay) ) <= max_delay)
                    context.write(new Text(flight_carrier), new IntWritable(1));
                else
                    context.write(new Text(flight_carrier), new IntWritable(0));
            }
      }
    }

    public static class ScheduleFReducer extends Reducer<Text,IntWritable,Text,DoubleWritable> {
        List<FlightSchedulingProb> list = new ArrayList<>();
        public void reduce(Text key, Iterable<IntWritable> values,Context context) throws IOException, InterruptedException {
          int count = 0;
          int ontimeflights = 0;
          String arrival_depart_delay;
          for (IntWritable val : values) {
              count += 1;
              if(val.get() == 1)
              {
                ontimeflights += 1;
              }
          }
          double prob = (double)ontimeflights/(double)count;
          list.add(new FlightSchedulingProb(prob,key.toString()));
       }

       //The following class store the probability of flight carrier

       class FlightSchedulingProb {
           double probability;
           String flight_carrier;
           FlightSchedulingProb(double prob, String carrier) 
           {
                this.probability = prob;
                this.flight_carrier = carrier;
           }
       }

       // The following is the comparator class for reverse sorting of probabilities of flight carriers

       class ReverseSort implements Comparator<FlightSchedulingProb> {
            @Override
            public int compare(FlightSchedulingProb p1, FlightSchedulingProb p2) {
              if( p1.probability > p2.probability )
                return -1;
              else if( p1.probability < p2.probability )
                return 1;
              else 
                return 0;
            }
       }

       //When the reducer process is done, write all the flight carrier on schedule probabilities in reverse order
          protected void cleanup(Context context) throws IOException, InterruptedException {
          Collections.sort(list, new ReverseSort());
          for (FlightSchedulingProb element : list) {
              context.write(new Text(element.flight_carrier), new DoubleWritable(element.probability ));
          }
      }
    }

    public static class TaxiInOutTimeMapper extends Mapper<Object, Text, Text, IntWritable>{

        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] tokens = line.split(",");
            String origin = tokens[16];
            String destination = tokens[17];
            String taxiIn = tokens[19];
            String taxiOut = tokens[20];
            if(!origin.equals("NA") && !origin.equals("Origin") &&
                      !destination.equals("NA") && !destination.equals("Destination") &&
                      !taxiIn.equals("NA") && !taxiIn.equals("TaxiIn") &&
                      !taxiOut.equals("NA") && !taxiOut.equals("TaxiOut") )
         
            {
                int in = Integer.parseInt(taxiIn);
                int out = Integer.parseInt(taxiOut);
                context.write(new Text(origin), new IntWritable(out));
                context.write(new Text(destination), new IntWritable(in));
            }
      }
    }

    public static class TaxiInOutTimeReducer extends Reducer<Text,IntWritable,Text,DoubleWritable> {
        List<Averagetaxitime> list = new ArrayList<>();
        public void reduce(Text key, Iterable<IntWritable> values,Context context) throws IOException, InterruptedException {
          int count = 0;
          int taxiTime = 0;
          for (IntWritable val : values) {
              count += 1;
              taxiTime += val.get();
          }
          double avg = (double)taxiTime/(double)count;
          list.add(new Averagetaxitime(avg,key.toString()));
       }

       //The following class stores average taxi times of an airport 
       
       class Averagetaxitime {
           double avg;
           String airport;
           Averagetaxitime(double avg, String airport)
           {
                this.avg = avg;
                this.airport = airport;
           }
       }

       //The following comparator class is for reverse sorting the average taxi time of airports 
       
       class ReverseSort implements Comparator<Averagetaxitime> {
            @Override
            public int compare(Averagetaxitime p1, Averagetaxitime p2) {
              if( p1.avg > p2.avg )
                return -1;
              else if( p1.avg < p2.avg )
                return 1;
              else
                return 0;
            }
       }

       
       
       protected void cleanup(Context context) throws IOException, InterruptedException {
          Collections.sort(list, new ReverseSort());
          for (Averagetaxitime element : list) {
              context.write(new Text(element.airport), new DoubleWritable(element.avg ));
          }
      }
    }

    public static class CancellationFMapper extends Mapper<Object, Text, Text, IntWritable>{
        IntWritable one = new IntWritable(1);
        public void map(Object key, Text value, Context context) throws IOException, InterruptedException {
            String line = value.toString();
            String[] tokens = line.split(",");
            String cancelled_flight = tokens[21];
            String flight_code = tokens[22];
           
            if(!cancelled_flight.equals("NA") && !cancelled_flight.equals("Cancel flights") && cancelled_flight.equals("1") &&
                      !flight_code.equals("NA") && !flight_code.equals("CancellationCode") && !flight_code.isEmpty())
            {
                context.write(new Text(flight_code), one);
            }
      }
    }

    public static class CancellationFReducer extends Reducer<Text,IntWritable,Text,IntWritable> {
        public void reduce(Text key, Iterable<IntWritable> values,Context context) throws IOException, InterruptedException {
          int count = 0;
          for (IntWritable val : values) {
              count += val.get();
          }
          context.write(new Text(key), new IntWritable(count));
       }
    }

}
