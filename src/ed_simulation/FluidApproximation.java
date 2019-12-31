//package ed_simulation;
//
//public class FluidApproximation extends QueueApproximation {
//    public FluidApproximation(SimParams simParams) throws Exception {
//        super(simParams);
//    }
//
//    @Override
//    public TimeDependentSimResults simulate(double singlePeriodDuration, int numPeriodsToIgnore, double timeToRunSim, SimParams simParams,
//                                            AbandonmentModelingScheme abandonmentModelingScheme, boolean fastMode, double referenceWaitTime,
//                                            int initialNumInSystem) throws Exception {
//
//        TimeDependentSimResults results = new TimeDependentSimResults( this.binSize,  this.numBins, 0,
//                Math.min(1000, myMax(simParams.numAgents))*myMax(simParams.singleAgentCapacity), timeToRunSim, referenceWaitTime);
//        double t = 0;
//        double[] arrivals = new double[ this.numBins ];
//        arrivals[0] = 0;
//        double[] departures = new double[this.numBins];
//        departures[0] = 0;
//        double[] currNumInSystem = new double[ this.numBins ];
//        currNumInSystem[0] = initialNumInSystem; //TODO - this doesn't need to be
//        for( int i = 1 ; i < this.numBins ; i++){
//            double currBinArrivals = this.arrivalProcess.getBinRate(i) * this.binSize;
//            double numAgents = simParams.numAgents[i];
//            double currBinDepartures = currNumInSystem[i-1] == 0 ? currBinArrivals :
//                    Math.max( currBinArrivals, this.serviceProcess.get("ServiceRate").getBinRate( i ) * numAgents * this.binSize);
//            currNumInSystem[i] = currNumInSystem[i-1] + (currBinArrivals - currBinDepartures);
//            arrivals[i] =  currBinArrivals;
//            departures[i] =  currBinDepartures;
//            //TODO - I want to see these as floats, not integers. Currently this is just due to the current structure of TimeDependentSimResults.
//            // Maybe need to implement a dedicated version of this class. Currently used mainly due to the already implemented writing to file.
//            results.registerArrivals( i * binSize, (int)(Math.round(currBinArrivals)));
//            results.registerDepartures( i * binSize, (int)(Math.round( currBinDepartures)) );
//            int holdingQueueSize = Math.max( 0, currNumInSystem - numAgents );
//            int serviceQueueSize = Math.min( numAgents, currNumInSystem );
//            results.registerQueueLengths( holdingQueueSize,  serviceQueueSize, 0, 0, 0,
//            i * binSize, numAgents, 1);
//
//            }
//        results.setQueueSize(currNumInSystem);
//        results.setArrivalRate(arrivals);
//        results.setDepartureRate(departures);
////        results.writeToFile(runProperties.outpuFolderName, runProperties.numRepetitionsToTruncate, inputs.timestamps);
////        writeAdditionalOutputs(args, inputFolderName + "/properties.txt", defaultProps);
//        return results;s
//    }
//
//}
