package ed_simulation;

import java.io.*;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.util.Arrays;
import java.util.List;

import javafx.util.Pair;

public class SimParams {
    public long[] timestamps;
    //lambda
    public double[] arrivalRates;
    //mu - the service rate of a single consumer need (which differs from a single message by the former being composed of one or more messages)
    public double[] singleConsumerNeedServiceRate;
    //delta
    public double[] contentDepartureRates;
    //s
    public int[] numAgents;
    //Per agent average concurrency
    public int[] singleAgentCapacity;
    // p
    public double[] convEndProbs;
    // Known abandonment rate.
    public double[] patienceTheta;


    public int getPeriodDurationInSecs() {
        //Timestamps are in seconds, and they consist of maxTotalCapacity entries, (maxTotalCapacity being the number of time bins), since the last bin's
        //right edge represents the same period time of the first bin's left entry (basically the bins array spcifies a single period)
//        if(timestamps.length == 2) //There should be at least two entries, defining a single time bin.
//        {
            return (int)(timestamps[timestamps.length - 1] - timestamps[0]) ;
//        }
//        else {
//            return (int) (timestamps[timestamps.length - 2] - timestamps[0]);
//        }
    }

    /**
     * Reads the data from the file specified by filename
     * @param filename
     * @return a pair of timestamps and its corresponding values.
     * Important: the size of the timestamps is 1+size of values. This is since, in general, we want to support bins not necessarily equal, so that the
     * last bin has both left and right boundaries. This is how TimeInhomogeneousPoissonProcess works.
     */
    public static Pair<long[], double[]> readCsvData(String filename, String valueColName,  long[] expectedTimeBins)
    {

        try {

            BufferedReader reader = new BufferedReader(new FileReader(filename)); //Files.newBufferedReader(Paths.get(filename), null);
            String headerComment = reader.readLine();
            int timebinSize = Integer.parseInt(headerComment.split(",")[1]);
            CSVParser csvParser = new CSVParser(reader, CSVFormat.DEFAULT
                    .withFirstRecordAsHeader()
                    .withIgnoreHeaderCase()
                    .withTrim()
                    .withCommentMarker('#'));

            int timeBinColIndex = csvParser.getHeaderMap().get("TimeBin");
            int valueColIndex = csvParser.getHeaderMap().get(valueColName);
            List<CSVRecord> allRecords = csvParser.getRecords();
            long[] timebins = new long[allRecords.size() + 1];
            double[] vals = new double[allRecords.size()];

            int i = 0;
            for (CSVRecord csvRecord : allRecords) {
                timebins[i] = Long.parseLong(csvRecord.get(timeBinColIndex));
                vals[i] = Double.parseDouble(csvRecord.get(valueColIndex));
                i+=1;
            }
            timebins[i] = timebins[i-1] + timebinSize;
            //If there's a single value, it means the realization is constant.
            if(i == 1)
            {
                if(expectedTimeBins != null)
                {
                    timebins = expectedTimeBins;
                    double[] newvals = new double[timebins.length-1];
                    for(int k = 0 ; k < timebins.length-1; k++ )
                    {
                        newvals[k] = vals[0];
                    }
                    vals = newvals;
                }

            }
            //Verify the time bins are identical
            if( expectedTimeBins != null )
            {
                if(!Arrays.equals(expectedTimeBins, timebins ) )
                {
                    throw new Exception("The actual timeBins are not identical to expected timeBins!! Filename is: " + filename);
                }

            }

            return new Pair<>(timebins, vals);
        }
        catch (Exception e){
            e.printStackTrace();
            return null;
        }


    }
    /**
     *
     * @param inputFolderName assumes to consist of per-feature files, each of which spans the same, single period (for example, a typical week's realization).
     * @return
     * @throws Exception
     */
    public static SimParams fromInputFolder(String inputFolderName) throws Exception {
        File f = new File(inputFolderName);
        if (f.exists() && f.isDirectory()) {
            SimParams smp = new SimParams();

            Pair<long[], double[]> arrivals = smp.readCsvData( inputFolderName + "/ArrivalRate.csv", "AverageArrivalRate(Hrz)",  null);
            smp.timestamps = arrivals.getKey();
            smp.arrivalRates = arrivals.getValue();

//            Pair<long[], double[]> services = ;
            smp.singleConsumerNeedServiceRate = smp.readCsvData(inputFolderName + "/ServiceRate.csv", "ServiceRate", smp.timestamps  ).getValue();

            smp.contentDepartureRates = readCsvData(inputFolderName + "/ContentDepartureRate.csv", "ContentDepartureRate", smp.timestamps).getValue();

            double[] numAgentsDouble = readCsvData(inputFolderName + "/NumAgents.csv", "NumAgents", smp.timestamps).getValue();
            smp.numAgents = new int[numAgentsDouble.length];
            for( int i = 0 ; i < numAgentsDouble.length; i++ )
            {
                smp.numAgents[i] = (int)numAgentsDouble[i];
            }

            double[] singleAgentCapacityDouble = readCsvData(inputFolderName + "/SingleAgentCapacity.csv", "AverageAgentMaxLoad", smp.timestamps).getValue();
            smp.singleAgentCapacity = new int[singleAgentCapacityDouble.length];
            for( int i = 0 ; i < singleAgentCapacityDouble.length; i++ )
            {
                smp.singleAgentCapacity[i] = (int)singleAgentCapacityDouble[i];
            }

            smp.convEndProbs = readCsvData(inputFolderName + "/ConvEndProbs.csv", "ConvEndProb", smp.timestamps).getValue();
            smp.patienceTheta = readCsvData(inputFolderName + "/KnownAbandonementRate.csv", "KnownAbandonementRate", smp.timestamps).getValue();


            return smp;
        }
        else
        {
            throw new Exception("Input folder: " + inputFolderName + " doesn't exist!");
        }
    }

    public int[] getTimeBins()
    {
        int[] res = new int[timestamps.length];
        for( int i = 0 ; i < timestamps.length ; i++)
        {
            res[i] = (int)(timestamps[i] - timestamps[0]);
        }
        return res;
    }

    public int[] getNumBinsAndSize()
    {
        int[] tmp =  new int[2];
        tmp[0] = timestamps.length-1;
        tmp[1] = (int)(timestamps[1]-timestamps[0]);
        return tmp;
    }

}