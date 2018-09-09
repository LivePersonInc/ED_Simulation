package ed_simulation;

import java.io.FileWriter;
import java.io.IOException;

import java.util.*;
import java.util.stream.*;

public class TimeDependentSimResults {

    //Keep statistics per each time bin.
//    private HashMap<Integer, SimResults> simStatisticsPerTimeBin;
    int binSize;
    int numBins;
    private SimResults[] simStatisticsPerTimeBin;
    double exchangesDurations;
    double interExchangesDurations;
    int numExchanges;
    int numInterExchanges;

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

    public void writeToFile(String outfolder, String compiledOutputFolder) {
        //Skip this for the time being - just override.
//        File f = new File(outfolder);
//        if (f.exists() ) {
//            SimParams smp = new SimParams();
//            //.... Read inputs.
//
//        }
        FileWriter fileWriterStatistics = null;
        FileWriter fileWriterQueueRealization = null;
        FileWriter fileWriterTimeInQueueRealization = null;
        FileWriter fileWriterArivalRateRealization = null;
        FileWriter fileWriterAssignRateRealization = null;
        FileWriter fileWriterOnlineAgentLoad = null;
        FileWriter fileWriterAllAgentsLoad = null;
        FileWriter fileWriterOnlineAgentsMaxCapacity = null;
        FileWriter fileWriterAllAgentsMaxCapacity = null;
        FileWriter fileWriterStaffing = null;
        FileWriter fileWriterNumConvExchanges = null;
        FileWriter fileWriterKnownAbandonmentRate = null;
        FileWriter fileWriterAvgExchangeDuration = null;
        FileWriter fileWriterAvgInterExchangeDuration = null;



        try {

            //Write per Bin statistics
            fileWriterStatistics = new FileWriter(outfolder + "/runStatistics.csv");

            fileWriterArivalRateRealization = new  FileWriter(outfolder + "/ArrivalRate_sim.csv");
            fileWriterAssignRateRealization = new  FileWriter(outfolder + "/AverageAssignRate(Hrz)_sim.csv");
            fileWriterOnlineAgentLoad = new  FileWriter(outfolder + "/AverageAgentMaxLoad_sim.csv");
            fileWriterAllAgentsLoad = new  FileWriter(outfolder + "/AllAgentLoads_sim.csv");
//            fileWriterOnlineAgentsMaxCapacity = new   FileWriter(outfolder + "/OnlineAgentMaxCapacity_sim.csv");
//            fileWriterAllAgentsMaxCapacity = new  FileWriter(outfolder + "/AllAgentMaxCapacity_sim.csv");
            fileWriterQueueRealization = new  FileWriter(outfolder + "/QueueSize_sim.csv");
            fileWriterTimeInQueueRealization = new FileWriter(outfolder + "/TimeInQueue_sim.csv");
            fileWriterStaffing = new FileWriter( outfolder + "/Staffing_sim.csv");
            fileWriterNumConvExchanges = new FileWriter( outfolder + "/NumExchangesPerConv_sim.csv");
            fileWriterKnownAbandonmentRate = new FileWriter( outfolder + "/AbanBeforeAgentRatio_sim.csv");
            //In the meantime Exchanges are counted over the entire realization, not per period.
            fileWriterAvgExchangeDuration = new FileWriter( outfolder + "AvgExchangeDuration.csv");
            fileWriterAvgInterExchangeDuration = new FileWriter( outfolder + "AvgInterExchangeDuration.csv");


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


            fileWriterOnlineAgentLoad.append("#TimeBin(sec),AgentLoad X numPeriods\n");
            fileWriterOnlineAgentLoad.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterAllAgentsLoad.append("#TimeBin(sec),AgentLoadAllAgents X numPeriods\n");
            fileWriterAllAgentsLoad.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterStaffing.append("#TimeBin(sec), Online Agents X numPeriods\n");
            fileWriterStaffing.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterTimeInQueueRealization.append("#TimeBin(sec),AverageQueueTime(sec) X numPeriods\n");
            fileWriterTimeInQueueRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterNumConvExchanges.append("#TimeBin(sec),AverageNumExchangesPerConversation(sec) X numPeriods\n");
            fileWriterNumConvExchanges.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterKnownAbandonmentRate.append("#TimeBin(sec),OverallAbanRate(sec) X numPeriods\n");
            fileWriterKnownAbandonmentRate.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

//            fileWriterAvgExchangeDuration.append("#TimeBin(sec),AverageExchangeDuration(sec) X numPeriods\n");
//            fileWriterAvgExchangeDuration.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");
//
//            fileWriterAvgInterExchangeDuration.append("#TimeBin(sec),AvgInterExchangeDuration(sec) X numPeriods\n");
//            fileWriterAvgInterExchangeDuration.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");




            int currTimeBin = 0;
            for( SimResults sr : simStatisticsPerTimeBin  )
            {
//                System.out.println("Now writing results of time bin: " + currTimeBin);
                fileWriterStatistics.append( currTimeBin*binSize + "," + sr.getMeanServiceQueueLength() + ","+sr.getMeanHoldingTime() + "," + sr.getMeanWaitingTime() + ","+ sr.getHoldingProbability()
                        + ","+ sr.getWaitingProbability() + ","+ sr.getMeanTotalInSystem() +  ","+ sr.getMeanAllInSystem() + "\n" );

                fileWriterArivalRateRealization.append( currTimeBin*binSize +    sr.getArrivalRateRealizationAsCsv(this.binSize) + "\n" );
                fileWriterAssignRateRealization.append( currTimeBin*binSize +    sr.getAssignRateRealizationAsCsv(this.binSize) + "\n" );
                fileWriterOnlineAgentLoad.append( currTimeBin*binSize +    sr.getOnlineAgentLoadRealizationAsCsv() + "\n" );
                fileWriterAllAgentsLoad.append( currTimeBin*binSize +    sr.getAllAgentLoadRealizationAsCsv() + "\n" );
                fileWriterQueueRealization.append( currTimeBin*binSize +    sr.getQueueSizeRealizationAsCsv() + "\n" );
                fileWriterTimeInQueueRealization.append( currTimeBin*binSize +   sr.getTimeInQueueRealizationAsCsv() + "\n" );
                fileWriterStaffing.append( currTimeBin*binSize + sr.getStaffingRealizationAsCsv() + "\n");
                fileWriterNumConvExchanges.append( currTimeBin*binSize + sr.getNumExchangesPerConvRealizationAsCsv() + "\n");
                fileWriterKnownAbandonmentRate.append( currTimeBin*binSize + sr.getAbanBeforeAgentRatioAsCsv() + "\n");
//                fileWriterAvgExchangeDuration.append( currTimeBin*binSize + sr.getAvgExchangeDuration() + "\n");
//                fileWriterAvgInterExchangeDuration.append( currTimeBin*binSize + sr.getAvgInterExchangeDuration() + "\n");


                currTimeBin += 1;
            }

            System.out.println("Average exchange duration: " + this.exchangesDurations/numExchanges + ". Average Inter-Exchange duration: " + this.interExchangesDurations/numInterExchanges);


        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                fileWriterStatistics.flush();
                fileWriterStatistics.close();
                fileWriterQueueRealization.flush();
                fileWriterQueueRealization.close();
                fileWriterTimeInQueueRealization.flush();
                fileWriterTimeInQueueRealization.close();
                fileWriterArivalRateRealization.flush();
                fileWriterArivalRateRealization.close();
                fileWriterAssignRateRealization.flush();
                fileWriterAssignRateRealization.close();
                fileWriterOnlineAgentLoad.flush();
                fileWriterOnlineAgentLoad.close();
                fileWriterAllAgentsLoad.flush();
                fileWriterAllAgentsLoad.close();
                fileWriterStaffing.flush();
                fileWriterStaffing.close();
                fileWriterNumConvExchanges.flush();
                fileWriterNumConvExchanges.close();
                fileWriterKnownAbandonmentRate.flush();
                fileWriterKnownAbandonmentRate.close();
//                fileWriterAvgExchangeDuration.flush();
//                fileWriterAvgExchangeDuration.close();
//                fileWriterAvgInterExchangeDuration.flush();
//                fileWriterAvgInterExchangeDuration.close();

            } catch (IOException e) {

                System.out.println("Error while flushing/closing fileWriterStatistics !!!");

                e.printStackTrace();

            }

        }

    }

    public void registerQueueLengths(int holdingQueueSize, int serviceQueueSize, int contentQueueSize,
                                     int serviceQueueSizeOnlineServers, int contentQueueSizeOnlineAgents, double currentTime, int currentNumAgents, int agentsMaxCapacity) {

//        System.out.println("The time is: " + currentTime  + ". The period number is: "  + getCurrTimePeriod(currentTime) +   ". The time bin within the period is: "  + ((int) Math.floor((currentTime % getPeriodDuration()) / binSize)));
        getCurrTimeSimResult(currentTime).registerQueueLengths(holdingQueueSize, serviceQueueSize, contentQueueSize, serviceQueueSizeOnlineServers, contentQueueSizeOnlineAgents,
                currentTime,
                getCurrTimePeriod(currentTime), currentNumAgents, agentsMaxCapacity);
    }


    //Time waited in the holding (external queue)
    public void registerHoldingTime(Patient newPatient, double currTime) {
        //When registering the Wait time (time till assignment) we register this at the bin corresponding to the arrival time.
        //This is in order to be consistent with the ds-messaging data, in which we're indicating the wait time experienced by consumers arriving at time t.
        //!!! Important - note this is not the TTFR, only the time till assignment!!
        getCurrTimeSimResult(newPatient.getArrivalTime()).registerHoldingTime( newPatient, currTime, getCurrTimePeriod(newPatient.getArrivalTime()) );

    }

    //Waiting time in the internal server queue between content phase end to service entry.
    public void registerWaitingTime(Patient nextPatientToService, double currTime) {
        getCurrTimeSimResult(currTime).registerWaitingTime( nextPatientToService, currTime);
    }

    //TODO: I may need to implement silentAbandonment registration, which is caused by waiting additional time to service after assignment (currently it's only TIQ, and not TTFR).
    public void registerAbandonment(Patient patient, double currTime) {
        //Currently - omit registering the holding time, since the spark FetchTimeInQueue currently ignores known abandonment conversations time in Queue.
        // registerHoldingTime(patient, patient.getArrivalTime() +  patient.getPatience());
        //Notice that the abandonment is associated with its arrival time.
        getCurrTimeSimResult(patient.getArrivalTime()).registerAbandonment( getCurrTimePeriod(patient.getArrivalTime()) );
    }


    //Register the departure as associated with its arrival time. This is since we register statistics of all conversations arriving to the system at a given time bin.
    public void registerDeparture(Patient serviceCompletedPatient, double currTime ) {
        getCurrTimeSimResult(serviceCompletedPatient.getArrivalTime()).registerDeparture(serviceCompletedPatient, currTime, getCurrTimePeriod(serviceCompletedPatient.getArrivalTime()));
    }

    public void registerArrival(double currentTime) {
        getCurrTimeSimResult(currentTime).registerArrival( getCurrTimePeriod(currentTime));
    }



    //Currently exchanges and inter-exchanges durations are measured globally, over all time bins.
    public void registerExchange(double exchangeDuration) {
        this.exchangesDurations += exchangeDuration;
        this.numExchanges += 1;
    }

    //Currently inter exchanges and inter-exchanges durations are measured globally, over all time bins.
    public void registerInterExchange(double interExchangeDuration) {
        if( interExchangeDuration > 1000)
        {
            int x = 0;
        }
//        System.out.println("Inter Exchange Duration: " + interExchangeDuration);
        this.interExchangesDurations += interExchangeDuration;
        this.numInterExchanges += 1;
    }


}
