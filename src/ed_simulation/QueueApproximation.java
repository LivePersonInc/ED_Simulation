package ed_simulation;

import statistics.TimeInhomogeneousPoissionProcess;

import java.util.HashMap;
import java.util.LinkedList;

public abstract class QueueApproximation {


    //Bloody Java. I can't believe I need to write this by myself.
    public int myMax(int[] arr) throws Exception {
        if (arr == null) {
            throw new Exception("Got a null array to myMax");
        }
        if (arr.length == 0) {
            return 0;
        }
        int currMax = -Integer.MAX_VALUE;
        for (int i = 0; i < arr.length; i++) {
            currMax = Math.max(currMax, arr[i]);
        }
        return currMax;
    }


    protected TimeInhomogeneousPoissionProcess arrivalProcess;
    //Service process (time dependent) per each service rate type (e.g. silence abandoned v.s. non-abandoned.)
    protected HashMap<String, TimeInhomogeneousPoissionProcess> serviceProcess;
    protected int binSize;
    protected int numBins;

    public QueueApproximation(SimParams simParams) throws Exception {

        this.arrivalProcess = new TimeInhomogeneousPoissionProcess(simParams.getTimeBins(), simParams.arrivalRates);
        this.serviceProcess = new HashMap<>();
        for (String serviceRateIdentifier : simParams.serviceRatesTable.keySet()) {
            this.serviceProcess.put(serviceRateIdentifier, new TimeInhomogeneousPoissionProcess(simParams.getTimeBins(), simParams.serviceRatesTable.get(serviceRateIdentifier)));
        }

        int[] numBinsAndBinSize = simParams.getNumBinsAndSize();
        this.binSize = numBinsAndBinSize[1];
        this.numBins = numBinsAndBinSize[0];

    }

    public TimeDependentSimResults simulate(double singlePeriodDuration, int numPeriodsToIgnore, double timeToRunSim, SimParams simParams,
                                            AbandonmentModelingScheme abandonmentModelingScheme, boolean fastMode, double referenceWaitTime) throws Exception {
//        double ignoreUpToTime = singlePeriodDuration*numPeriodsToIgnore;
//
////        SimResults results = new SimResults(perAgentMaxCapacity * serversManager.getNumServers());
//        int[] numBinsAndBinSize = simParams.getNumBinsAndSize();
//        TimeDependentSimResults results = new TimeDependentSimResults( numBinsAndBinSize[1],  numBinsAndBinSize[0], numPeriodsToIgnore,
//                Math.min(1000, myMax(simParams.numAgents))*myMax(simParams.singleAgentCapacity), timeToRunSim, referenceWaitTime);
//        FES fes = new FES();
//        LinkedList<Patient> holdingQueue = new LinkedList<Patient>();
//        StringBuilder logString;
//        BinnedProbFunction binnedIsSingleExchange = new BinnedProbFunction(simParams.singleExchangeHistTimeBinSize, simParams.singleExchangeHist);
//        BinnedProbFunction binnedIsKnownAban = new BinnedProbFunction(simParams.knownAbanHazardTimeBinSize, simParams.knownAbanSurvivalFunction);
//        BinnedProbFunction convEndDeterminator = new BinnedProbFunction( 1, simParams.convEndHazard);

        return null;
    }
}