package ed_simulation;

import java.io.FileWriter;
import java.io.IOException;

import java.util.*;
import java.util.stream.*;

public class TimeDependentSimResults {

//    private final int numPeriodsToSimulate;
    //Keep statistics per each time bin.
//    private HashMap<Integer, SimResults> simStatisticsPerTimeBin;
    int binSize;
    int numBins;
    private SimResults[] simStatisticsPerTimeBin;
    double exchangesDurations;
    double interExchangesDurations;
    int numExchanges;
    //Histogram of the number of exchanges per conversation.
    int[] numExchangesPerConv = new int[1000];
    int numInterExchanges;
    private int netNumPeriodsToSimulate;
    private double referenceWaitTime;

    /**
     * Entry j in the input array is the left edge of timebin j. The bins are assumed to commence at 0 and represent equally spaced intervals.
     *
     * @param
     */
    public TimeDependentSimResults(int binSize, int numBins, int numPeriodsToIgnore, int maxTotalCapacity, double timeToRunSim, double referenceWaitTime) throws Exception {
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
        this.netNumPeriodsToSimulate = (int)Math.ceil(timeToRunSim/(binSize*numBins)) - numPeriodsToIgnore;

//        simStatisticsPerTimeBin = new HashMap<Integer, SimResults>(timeBins.length);
        simStatisticsPerTimeBin = new SimResults[numBins];
        for (int i = 0; i < simStatisticsPerTimeBin.length; i++) {
            simStatisticsPerTimeBin[i] = new SimResults(maxTotalCapacity, (int)Math.ceil(timeToRunSim/(binSize*numBins)), referenceWaitTime); //TODO: It's completely awkward that I need to pass the capacity to the constructor. Change that later on.
        }
        this.referenceWaitTime = referenceWaitTime;

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

    public void writeToFile(String outfolder, int numRepetitionsToTruncate,  long[] timestamps) {

        FileWriter fileWriterStatistics = null;
        FileWriter fileWriterQueueRealization = null;
        FileWriter fileWriterQueueRealizationNumSamples = null;
//        FileWriter fileWriterQueueRealizationNumSamples = null;
        FileWriter fileWriterTimeInQueueRealization = null;
        FileWriter fileWriterTimeInQueueRealizationNumSamples = null;
        FileWriter fileWriterArivalRateRealization = null;
        FileWriter fileWriterAssignRateRealization = null;
        FileWriter fileWriterOnlineAgentLoad = null;
        FileWriter fileWriterAllAgentsLoad = null;
        FileWriter fileWriterAllAgentsLoadNumSamples = null;
        FileWriter fileWriterOnlineAgentsMaxCapacity = null;
        FileWriter fileWriterOnlineAgentsMaxCapacityNumSamples = null;
        FileWriter fileWriterOnlineAgentLoadNumSamples = null;
        FileWriter fileWriterStaffing = null;
        FileWriter fileWriterStaffingNumSamples = null;
        FileWriter fileWriterNumConvExchanges = null;
        FileWriter fileWriterSingleExchangeRatio = null;
        FileWriter fileWriterKnownAbandonmentRate = null;
        FileWriter fileWriterAvgExchangeDuration = null;
        FileWriter fileWriterAvgInterExchangeDuration = null;
        FileWriter fileWriterExchangesStatistics = null;



        try {

            //Write per Bin statistics
            fileWriterStatistics = new FileWriter(outfolder + "/runStatistics.csv");

            fileWriterArivalRateRealization = new  FileWriter(outfolder + "/AverageArrivalRate(Hrz)_sim.csv");
            fileWriterAssignRateRealization = new  FileWriter(outfolder + "/AverageAssignRate(Hrz)_sim.csv");
            fileWriterOnlineAgentLoad = new  FileWriter(outfolder + "/AverageTotalAssignedConvWeight_sim.csv");
            fileWriterOnlineAgentLoadNumSamples = new  FileWriter(outfolder + "/AverageTotalAssignedConvWeightNumSamples_sim.csv");
            fileWriterAllAgentsLoad = new  FileWriter(outfolder + "/occupancy_sim.csv");
            fileWriterAllAgentsLoadNumSamples = new  FileWriter(outfolder + "/occupancyNumSamples_sim.csv");
            fileWriterOnlineAgentsMaxCapacity = new   FileWriter(outfolder + "/AverageAgentMaxLoad_sim.csv");
            fileWriterOnlineAgentsMaxCapacityNumSamples = new   FileWriter(outfolder + "/AverageAgentMaxLoadNumSamples_sim.csv");
//            fileWriterAllAgentsMaxCapacity = new  FileWriter(outfolder + "/AllAgentMaxCapacity_sim.csv");
            fileWriterQueueRealization = new  FileWriter(outfolder + "/AverageUnassigned_sim.csv");
            fileWriterQueueRealizationNumSamples = new  FileWriter(outfolder + "/AverageUnassignedNumSamples_sim.csv");
            fileWriterStaffingNumSamples = new  FileWriter(outfolder + "/averageStaffingNumSamples_sim.csv");
            fileWriterTimeInQueueRealization = new FileWriter(outfolder + "/queueTimeHuman_sim.csv");
            fileWriterTimeInQueueRealizationNumSamples = new FileWriter(outfolder + "/queueTimeHumanNumSamples_sim.csv");
            fileWriterStaffing = new FileWriter( outfolder + "/averageStaffing_sim.csv");
            fileWriterNumConvExchanges = new FileWriter( outfolder + "/NumExchangesPerConv_sim.csv");
            fileWriterSingleExchangeRatio = new FileWriter( outfolder + "/SingleExchangeRatio_sim.csv");
            fileWriterKnownAbandonmentRate = new FileWriter( outfolder + "/AbanBeforeAgentRatio_sim.csv");
            fileWriterExchangesStatistics = new FileWriter( outfolder + "/Num Exchanges Per Segment Counts_sim.csv");
            //In the meantime Exchanges are counted over the entire realization, not per period.
            fileWriterAvgExchangeDuration = new FileWriter( outfolder + "/AvgExchangeDuration.csv");
            fileWriterAvgInterExchangeDuration = new FileWriter( outfolder + "/AvgInterExchangeDuration.csv");


            List<Integer>  numPeriodsQueueRealizations = IntStream.rangeClosed(1, simStatisticsPerTimeBin[0].getNumPeriods()).boxed().collect(Collectors.toList());
            fileWriterStatistics.append("#BinSize," + Integer.toString(binSize) + "\n");
            fileWriterStatistics.append("TimeBin,MeanServiceQueueLength,MeanHoldingTime,MeanWaitingTime,HoldingProbability,WaitingProbability,MeanTotalInSystem,MeanAllInSystem\n");

            fileWriterQueueRealization.append("#TimeBin(sec),AverageQueueSize X numPeriods\n");
            //It just can't get more cumbersome...
            fileWriterQueueRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterQueueRealizationNumSamples.append("#TimeBin(sec),numSamples X numPeriods\n");
            fileWriterQueueRealizationNumSamples.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");




            fileWriterArivalRateRealization.append("#TimeBin(sec),Arrival Rate X numPeriods\n");
            fileWriterArivalRateRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterAssignRateRealization.append("#TimeBin(sec),Assign Rate X numPeriods\n");
            fileWriterAssignRateRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");


            fileWriterOnlineAgentLoad.append("#TimeBin(sec),AgentLoad X numPeriods\n");
            fileWriterOnlineAgentLoad.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterOnlineAgentsMaxCapacity.append("#TimeBin(sec),MaxAgentCapacity X numPeriods\n");
            fileWriterOnlineAgentsMaxCapacity.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterAllAgentsLoad.append("#TimeBin(sec),AgentLoadAllAgents X numPeriods\n");
            fileWriterAllAgentsLoad.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterAllAgentsLoadNumSamples.append("#TimeBin(sec),numSamples X numPeriods\n");
            fileWriterAllAgentsLoadNumSamples.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterOnlineAgentsMaxCapacityNumSamples.append("#TimeBin(sec),numSamples X numPeriods\n");
            fileWriterOnlineAgentsMaxCapacityNumSamples.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterOnlineAgentLoadNumSamples.append("#TimeBin(sec),numSamples X numPeriods\n");
            fileWriterOnlineAgentLoadNumSamples.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");


            fileWriterStaffing.append("#TimeBin(sec), Online Agents X numPeriods\n");
            fileWriterStaffing.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterStaffingNumSamples.append("#TimeBin(sec),numSamples X numPeriods\n");
            fileWriterStaffingNumSamples.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");


            fileWriterTimeInQueueRealization.append("#TimeBin(sec),AverageQueueTime(sec) X numPeriods\n");
            fileWriterTimeInQueueRealization.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");


            fileWriterTimeInQueueRealizationNumSamples.append("#TimeBin(sec),numSamples X numPeriods\n");
            fileWriterTimeInQueueRealizationNumSamples.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterNumConvExchanges.append("#TimeBin(sec),AverageNumExchangesPerConversation(sec) X numPeriods\n");
            fileWriterNumConvExchanges.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterSingleExchangeRatio.append("#TimeBin(sec),SingleExchangeRatio(sec) X numPeriods\n");
            fileWriterSingleExchangeRatio.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

            fileWriterKnownAbandonmentRate.append("#TimeBin(sec),OverallAbanRate(sec) X numPeriods\n");
            fileWriterKnownAbandonmentRate.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");

//            fileWriterAvgExchangeDuration.append("#TimeBin(sec),AverageExchangeDuration(sec) X numPeriods\n");
//            fileWriterAvgExchangeDuration.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");
//
//            fileWriterAvgInterExchangeDuration.append("#TimeBin(sec),AvgInterExchangeDuration(sec) X numPeriods\n");
//            fileWriterAvgInterExchangeDuration.append("," + String.join(",", numPeriodsQueueRealizations.stream().map(Object::toString).collect(Collectors.toList()) ) + "\n");




            int currTimeBin = 0;
            long currTimestamp;
            for( SimResults sr : simStatisticsPerTimeBin  )
            {
                currTimestamp =  timestamps[currTimeBin];
//                System.out.println("Now writing results of time bin: " + currTimeBin);
                fileWriterStatistics.append( currTimestamp + "," + sr.getMeanServiceQueueLength() + ","+sr.getMeanHoldingTime() + "," + sr.getMeanWaitingTime() + ","+ sr.getHoldingProbability()
                        + ","+ sr.getWaitingProbability() + ","+ sr.getMeanTotalInSystem() +  ","+ sr.getMeanAllInSystem() + "\n" );

                fileWriterArivalRateRealization.append( currTimestamp +    sr.getArrivalRateRealizationAsCsv(this.binSize, numRepetitionsToTruncate) + "\n" );
                fileWriterAssignRateRealization.append( currTimestamp +    sr.getAssignRateRealizationAsCsv(this.binSize, numRepetitionsToTruncate) + "\n" );
                fileWriterOnlineAgentLoad.append( currTimestamp +    sr.getOnlineAgentLoadRealizationAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterOnlineAgentsMaxCapacity.append( currTimestamp +    sr.getOnlineAgentMaxCapacityRealizationAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterAllAgentsLoad.append( currTimestamp +    sr.getAllAgentLoadRealizationAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterQueueRealization.append( currTimestamp +    sr.getQueueSizeRealizationAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterQueueRealizationNumSamples.append( currTimestamp +    sr.getQueueSizeRealizationNumSamplesAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterStaffingNumSamples.append( currTimestamp +    sr.getStaffingRealizationNumSamplesAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterTimeInQueueRealization.append( currTimestamp +   sr.getTimeInQueueRealizationAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterTimeInQueueRealizationNumSamples.append( currTimestamp +   sr.getTimeInQueueRealizationNumSamplesAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterAllAgentsLoadNumSamples.append( currTimestamp +   sr.getAllAgentLoadNumSamplesAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterOnlineAgentsMaxCapacityNumSamples.append( currTimestamp +   sr.getAllAgentLoadNumSamplesAsCsv(numRepetitionsToTruncate) + "\n" );
                fileWriterOnlineAgentLoadNumSamples.append( currTimestamp +   sr.getAllAgentLoadNumSamplesAsCsv(numRepetitionsToTruncate) + "\n" );



                fileWriterStaffing.append( currTimestamp + sr.getStaffingRealizationAsCsv(numRepetitionsToTruncate) + "\n");
                fileWriterNumConvExchanges.append( currTimestamp + sr.getNumExchangesPerConvRealizationAsCsv(numRepetitionsToTruncate) + "\n");
                fileWriterSingleExchangeRatio.append( currTimestamp + sr.getSingleExchangeRatioAsCsv(numRepetitionsToTruncate) + "\n");

                fileWriterKnownAbandonmentRate.append( currTimestamp + sr.getAbanBeforeAgentRatioAsCsv(numRepetitionsToTruncate) + "\n");
//                fileWriterAvgExchangeDuration.append( currTimestamp + sr.getAvgExchangeDuration() + "\n");
//                fileWriterAvgInterExchangeDuration.append( currTimestamp + sr.getAvgInterExchangeDuration() + "\n");


                currTimeBin += 1;
            }

            int totalNumConvs = 0;
            int totalNumExchanges = 0;
            for( int i = 0 ; i < this.numExchangesPerConv.length ; i++ )
            {
                totalNumConvs += numExchangesPerConv[i];
                totalNumExchanges += i*numExchangesPerConv[i];
            }
            String numExchangePerConvDist = "";
            int averageNumExchangesPerPeriod = totalNumConvs/this.getNetNumPeriodsToSimulate();
            for( int i = 0 ; i < this.numExchangesPerConv.length ; i++ )
            {

                numExchangePerConvDist += i + "," + ((double)numExchangesPerConv[i])/this.getNetNumPeriodsToSimulate() /*totalNumConvs*/ + "\n";
            }


            System.out.println("Average exchange duration: " + this.exchangesDurations/numExchanges + ". Average Inter-Exchange duration: " + this.interExchangesDurations/numInterExchanges);
            System.out.println("Total num exchanges counted directly: " + this.numExchanges + ". Total num exchanges counted from nrVisits: " + totalNumExchanges);
            System.out.println("Total num conversations counted from nrVisits: " + totalNumConvs);
            fileWriterExchangesStatistics.append( "#BinSize,1, Num exchanges per segment counts");
            fileWriterExchangesStatistics.append( ",AverageNumExchanges");
            fileWriterExchangesStatistics.append( numExchangePerConvDist);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                fileWriterStatistics.flush();
                fileWriterStatistics.close();
                fileWriterQueueRealization.flush();
                fileWriterQueueRealization.close();
                fileWriterQueueRealizationNumSamples.flush();
                fileWriterQueueRealizationNumSamples.close();
                fileWriterTimeInQueueRealization.flush();
                fileWriterTimeInQueueRealizationNumSamples.flush();
                fileWriterTimeInQueueRealization.close();
                fileWriterTimeInQueueRealizationNumSamples.close();
                fileWriterArivalRateRealization.flush();
                fileWriterArivalRateRealization.close();
                fileWriterAssignRateRealization.flush();
                fileWriterAssignRateRealization.close();
                fileWriterOnlineAgentLoad.flush();
                fileWriterOnlineAgentLoad.close();
                fileWriterAllAgentsLoadNumSamples.flush();
                fileWriterAllAgentsLoadNumSamples.close();
                fileWriterOnlineAgentsMaxCapacityNumSamples.flush();
                fileWriterOnlineAgentsMaxCapacityNumSamples.close();
                fileWriterOnlineAgentLoadNumSamples.flush();
                fileWriterOnlineAgentLoadNumSamples.close();
                fileWriterOnlineAgentsMaxCapacity.flush();
                fileWriterOnlineAgentsMaxCapacity.close();
                fileWriterAllAgentsLoad.flush();
                fileWriterAllAgentsLoad.close();
                fileWriterStaffing.flush();
                fileWriterStaffing.close();
                fileWriterStaffingNumSamples.flush();
                fileWriterStaffingNumSamples.close();
                fileWriterNumConvExchanges.flush();
                fileWriterNumConvExchanges.close();
                fileWriterSingleExchangeRatio.flush();
                fileWriterSingleExchangeRatio.close();
                fileWriterKnownAbandonmentRate.flush();
                fileWriterKnownAbandonmentRate.close();
                fileWriterExchangesStatistics.flush();
                fileWriterExchangesStatistics.close();
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

    private int getNetNumPeriodsToSimulate() {
        return this.netNumPeriodsToSimulate;
    }

    public void registerQueueLengths(int holdingQueueSize, int serviceQueueSize, int contentQueueSize,
                                     int serviceQueueSizeOnlineServers, int contentQueueSizeOnlineAgents, double currentTime, int currentNumAgents, int agentsMaxCapacity) {

//        if(((int) Math.floor((currentTime % getPeriodDuration()) / binSize)) == 88 && getCurrTimePeriod(currentTime) == 2 ) {
//            System.out.println("The time is: " + currentTime + ". The period number is: " + getCurrTimePeriod(currentTime) +
//                    ". The time bin within the period is: " + ((int) Math.floor((currentTime % getPeriodDuration()) / binSize)) +
//                    "The holding queue size is: " + holdingQueueSize);
//        }
        getCurrTimeSimResult(currentTime).registerQueueLengths(holdingQueueSize, serviceQueueSize, contentQueueSize, serviceQueueSizeOnlineServers, contentQueueSizeOnlineAgents,
                currentTime,
                getCurrTimePeriod(currentTime), currentNumAgents, agentsMaxCapacity);
    }


    //Time waited in the holding (external queue)
    public void registerHoldingTime(Patient newPatient, double currTime) {
        //When registering the Wait time (time till assignment) we register this at the bin corresponding to the arrival time.
        //This is in order to be consistent with the ds-messaging data, in which we're indicating the wait time experienced by consumers arriving at time t.
        //!!! Important - note this is not the TTFR, only the time till assignment!!

//        if(((int) Math.floor((newPatient.getArrivalTime() % getPeriodDuration()) / binSize)) == 88 && getCurrTimePeriod(newPatient.getArrivalTime()) == 2 )
//        {
//
//            System.out.println(" I have " + (currTime - newPatient.getArrivalTime()) +  " registered wait times in the second time period");
//        }
        getCurrTimeSimResult(newPatient.getArrivalTime()).registerHoldingTime( newPatient, currTime, getCurrTimePeriod(newPatient.getArrivalTime()) );
        getCurrTimeSimResult(currTime).registerAssignmentToAgent(  getCurrTimePeriod(currTime) );
//        System.out.println(" I have registered an assignment that took place in timebin "  +  getCurrTimeSimResult(currTime) + "I've associated it with timebin " +
//                getCurrTimePeriod(newPatient.getArrivalTime() ));

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
    //TODO: add registration of (known) abandoned conversations.
    public void registerDeparture(Patient serviceCompletedPatient, double currTime ) {
        getCurrTimeSimResult(serviceCompletedPatient.getArrivalTime()).registerDeparture(serviceCompletedPatient, currTime, getCurrTimePeriod(serviceCompletedPatient.getArrivalTime()));
        this.numExchangesPerConv[serviceCompletedPatient.getNrVisits()] += 1;
    }

//   public void registerDepartures( double currTime, int numDepartures)

    public void registerArrival(double currentTime) {
        getCurrTimeSimResult(currentTime).registerArrival( getCurrTimePeriod(currentTime));
    }

    public void registerArrivals( double currentTime, int numArrivals ){
        getCurrTimeSimResult(currentTime).registerArrivals(getCurrTimePeriod(currentTime), numArrivals);
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


    public Vector<double[]> getAllInSystemDistribution( boolean onlineAgentsOnly) {
        Vector<double[]> res = new Vector<double[]>(this.simStatisticsPerTimeBin.length);
        for( SimResults currTimeBin : this.simStatisticsPerTimeBin){
            res.add( currTimeBin.getAllInSystemProbabilities(onlineAgentsOnly));
        }
        return res;
    }

    public int getBinSize() {
        return binSize;
    }

    public double[] getQueueTimes() {
        double[] res = new double[this.simStatisticsPerTimeBin.length];
        for( int i = 0 ; i < this.simStatisticsPerTimeBin.length ; i++ ){
            res[i] = this.simStatisticsPerTimeBin[i].getAvgHoldingTime();
        }
        return res;
    }

    //Returns the per-timebin probability of wait time (i.e. arriving at a non-empty system)
    public double[] getHoldingProbabilities() {
        double[] res = new double[this.simStatisticsPerTimeBin.length];
        for( int i = 0 ; i < this.simStatisticsPerTimeBin.length ; i++ ){
            res[i] = this.simStatisticsPerTimeBin[i].getHoldingProbability();
        }
        return res;
    }

    public double getHoldingProbability( int i) {
        if( i >= this.simStatisticsPerTimeBin.length )
        {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.simStatisticsPerTimeBin[i].getHoldingProbability();
    }

    public double getHoldingProbabilityBasedOnAllInSystem( int i) {
        if( i >= this.simStatisticsPerTimeBin.length )
        {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.simStatisticsPerTimeBin[i].getHoldingProbabilityBasedOnAllInSystem();
    }

    public double[] getHoldingProbabilityBasedOnAllInSystem() {
        double[] res = new double[this.simStatisticsPerTimeBin.length];
        for( int i = 0 ; i < this.simStatisticsPerTimeBin.length ; i++ ){
            res[i] = this.simStatisticsPerTimeBin[i].getHoldingProbabilityBasedOnAllInSystem();
        }
        return res;
    }

    public double getHoldingTime(int i) {
        if( i >= this.simStatisticsPerTimeBin.length )
        {
            throw new ArrayIndexOutOfBoundsException();
        }
        return this.simStatisticsPerTimeBin[i].getAvgHoldingTime();
    }

    public double[] getMeanNumInSystem(boolean onlineAgentsOnly) {
        double[] res = new double[this.simStatisticsPerTimeBin.length];
        for( int i = 0 ; i < this.simStatisticsPerTimeBin.length ; i++ ){
            res[i] = this.simStatisticsPerTimeBin[i].getMeanAllInSystem(onlineAgentsOnly);
        }
        return res;

    }

    public double[] getExcessWaitProbabilities() {
        double[] res = new double[this.simStatisticsPerTimeBin.length];
        for( int i = 0 ; i < this.simStatisticsPerTimeBin.length ; i++ ){
            res[i] = this.simStatisticsPerTimeBin[i].getExcessWaitProbabilities();
        }
        return res;

    }

    public void append(TimeDependentSimResults anotherResult,  int numPeriodsRepetitionsTillSteadyState) throws Exception {
        if( anotherResult == null ){
            throw new Exception("Got a null anotherResult!!");
        }
        if( this.binSize != anotherResult.binSize ){
            throw new Exception("anotherResult has a different binSize than mine!");
        }
        if( this.numBins != anotherResult.numBins ){
            throw new Exception("anotherResult has a different numBins than mine!");
        }
        exchangesDurations += anotherResult.exchangesDurations;
        numExchanges += anotherResult.numExchanges;
        interExchangesDurations += anotherResult.interExchangesDurations;
        numInterExchanges += anotherResult.numInterExchanges;
        if( numExchangesPerConv.length != anotherResult.numExchangesPerConv.length ){
            throw new Exception("anotherResult has a different  numExchangesPerConv.length than mine!");
        }
        for( int i = 0 ; i < numExchangesPerConv.length ; i++){
            numExchangesPerConv[i] += anotherResult.numExchangesPerConv[i];
        }
        if( simStatisticsPerTimeBin.length != anotherResult.simStatisticsPerTimeBin.length ){
            throw new Exception("anotherResult has a different  simStatisticsPerTimeBin.length than mine!");
        }
        for( int i = 0 ; i < simStatisticsPerTimeBin.length; i++){
            simStatisticsPerTimeBin[i].append(anotherResult.simStatisticsPerTimeBin[i], numPeriodsRepetitionsTillSteadyState);
        }

    }
}
