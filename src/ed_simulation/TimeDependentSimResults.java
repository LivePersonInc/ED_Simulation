package ed_simulation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import java.util.*;
import java.util.stream.*;

public class TimeDependentSimResults {

    //Keep statistics per each time bin.
//    private HashMap<Integer, SimResults> simStatisticsPerTimeBin;
    int binSize;
    int numBins;
    private SimResults[] simStatisticsPerTimeBin;

    /**
     * Entry j in the input array is the left edge of timebin j. The bins are assumed to commence at 0 and represent equally spaced intervals.
     *
     * @param
     */
    public TimeDependentSimResults(int binSize, int numBins, int maxTotalCapacity, double timeToRunSim) throws Exception {
//        if( timeBins == null || timeBins.length < 1)
//        {
//            throw new Exception("The time bins array should consist of at least a single element");
//        }
//        if( timeBins[0] != 0 )
//        {
//            throw new Exception("The time bins array should commence in 0");
//        }


        if (binSize <= 0) {
            throw new Exception("binSize must be >= 0!");
        }
        if (numBins <= 0) {
            throw new Exception("numBins must be >= 0");
        }
        this.binSize = binSize;
        this.numBins = numBins;

//        simStatisticsPerTimeBin = new HashMap<Integer, SimResults>(timeBins.length);
        simStatisticsPerTimeBin = new SimResults[numBins];
        for (int i = 0; i < simStatisticsPerTimeBin.length; i++) {
            simStatisticsPerTimeBin[i] = new SimResults(maxTotalCapacity, (int)Math.ceil(timeToRunSim/(binSize*numBins))); //TODO: It's completely awkward that I need to pass the capacity to the constructor. Change that later on.
        }

    }

    private int getPeriodDuration() { return  binSize*numBins;} //we count as of 0

    private SimResults getCurrTimeSimResult(double t) {
        return simStatisticsPerTimeBin[((int) Math.floor((t % getPeriodDuration()) / binSize))];
    }

    public int getCurrTimePeriod(double currentTime) {
        int tmp =  (int)Math.floor( currentTime /(getPeriodDuration()));
//        if(tmp == 13)
//        {
//            int x = 0;
//        }
        return tmp;
    }

    public void writeToFile(String outfolder) {
        //Skip this for the time being - just override.
//        File f = new File(outfolder);
//        if (f.exists() ) {
//            SimParams smp = new SimParams();
//            //.... Read inputs.
//
//        }
        FileWriter fileWriterStatistics = null;
        FileWriter fileWriterQueueRealization = null;
        FileWriter fileWriterWaitTimeRealization = null;
        FileWriter fileWriterArivalRateRealization = null;
        FileWriter fileWriterAssignRateRealization = null;

        try {

            //Write per Bin statistics
            fileWriterStatistics = new FileWriter(outfolder + "/runStatistics.csv");

            fileWriterArivalRateRealization = new  FileWriter(outfolder + "/ArrivalRate_sim.csv");
            fileWriterAssignRateRealization = new  FileWriter(outfolder + "/AssignRate_sim.csv");
            fileWriterQueueRealization = new  FileWriter(outfolder + "/QueueSize_sim.csv");
            fileWriterWaitTimeRealization = new FileWriter(outfolder + "/WaitTime_sim.csv");


            List<Integer>  numPeriodsQueueRealizations = IntStream.rangeClosed(1, simStatisticsPerTimeBin[0].getNumPeriods()).boxed().collect(Collectors.toList());
            fileWriterStatistics.append("#BinSize," + Integer.toString(binSize) + "\n");
            fileWriterStatistics.append("TimeBin,MeanServiceQueueLength,MeanHoldingTime,MeanWaitingTime,HoldingProbability,WaitingProbability,MeanTotalInSystem,MeanAllInSystem\n");
            fileWriterQueueRealization.append("#TimeBin(sec),AverageQueueSize X numPeriods\n");
            //It just can't get more cumbersome...
            fileWriterQueueRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterArivalRateRealization.append("#TimeBin(sec),Arrival Rate X numPeriods\n");
            fileWriterArivalRateRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterAssignRateRealization.append("#TimeBin(sec),Assign Rate X numPeriods\n");
            fileWriterAssignRateRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterWaitTimeRealization.append("#TimeBin(sec),AverageWaitTime(sec) X numPeriods\n");
            fileWriterWaitTimeRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");
            int currTimeBin = 0;
            for( SimResults sr : simStatisticsPerTimeBin  )
            {
                fileWriterStatistics.append( currTimeBin*binSize + "," + sr.getMeanServiceQueueLength() + ","+sr.getMeanHoldingTime() + "," + sr.getMeanWaitingTime() + ","+ sr.getHoldingProbability()
                        + ","+ sr.getWaitingProbability() + ","+ sr.getMeanTotalInSystem() +  ","+ sr.getMeanAllInSystem() + "\n" );

                fileWriterArivalRateRealization.append( currTimeBin*binSize +    sr.getArrivalRateRealizationAsCsv(this.binSize) + "\n" );
                fileWriterAssignRateRealization.append( currTimeBin*binSize +    sr.getAssignRateRealizationAsCsv(this.binSize) + "\n" );
                fileWriterQueueRealization.append( currTimeBin*binSize +    sr.getQueueSizeRealizationAsCsv() + "\n" );
                fileWriterWaitTimeRealization.append( currTimeBin*binSize +   sr.getWaitTimeRealizationAsCsv() + "\n" );

                currTimeBin += 1;
            }




        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                fileWriterStatistics.flush();
                fileWriterStatistics.close();
                fileWriterQueueRealization.flush();
                fileWriterQueueRealization.close();
                fileWriterWaitTimeRealization.flush();
                fileWriterWaitTimeRealization.close();
                fileWriterArivalRateRealization.flush();
                fileWriterArivalRateRealization.close();
                fileWriterAssignRateRealization.flush();
                fileWriterAssignRateRealization.close();

            } catch (IOException e) {

                System.out.println("Error while flushing/closing fileWriterStatistics !!!");

                e.printStackTrace();

            }

        }

    }

    public void registerQueueLengths(int holdingQueueSize, int serviceQueueSize, int contentQueueSize, double currentTime) {
        getCurrTimeSimResult(currentTime).registerQueueLengths(holdingQueueSize, serviceQueueSize, contentQueueSize, currentTime, getCurrTimePeriod(currentTime));
    }


    public void registerHoldingTime(Patient newPatient, double currTime) {
        //When registering the Wait time (time till assignment) we register this at the bin corresponding to the arrival time.
        //This is in order to be consistent with the ds-messaging data, in which we're indicating the wait time experienced by consumers arriving at time t.
        //!!! Important - note this is not the TTFR, only the time till assignment!!
        getCurrTimeSimResult(currTime).registerHoldingTime( newPatient, currTime, getCurrTimePeriod(newPatient.getArrivalTime()) );

    }

    public void registerWaitingTime(Patient nextPatientToService, double currTime) {
        getCurrTimeSimResult(currTime).registerWaitingTime( nextPatientToService, currTime);
    }

    public void registerDeparture(Patient serviceCompletedPatient, double currTime) {
        getCurrTimeSimResult(currTime).registerDeparture(serviceCompletedPatient, currTime);
    }

    public void registerArrival(double currentTime) {
        getCurrTimeSimResult(currentTime).registerArrival( getCurrTimePeriod(currentTime));
    }
}