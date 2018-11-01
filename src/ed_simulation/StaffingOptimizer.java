package ed_simulation;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static ed_simulation.OptimizationScheme.*;
import static ed_simulation.ServerAssignmentMode.FIXED_SERVER_CAPACITY;
import static ed_simulation.ServerWaitingQueueMode.WITH_WAITING_QUEUE;
import static statistics.commons.coefficientOfVariation;

/**
 Finds an optimal staffing for a contact center given performance measures (e.g. upper bound of the waiting probability)
 *
 */
public class StaffingOptimizer {

    abstract class ConvergenceData{
        public abstract boolean notConverged( ConvergenceData convergenceData );

    }
    class FeldmanConvergenceData extends ConvergenceData{
        int[] currIterationStaffing;
        int[] nextIterationStaffing;
        double convergenceTau;

        public int[] getCurrIterationStaffing() {
            return currIterationStaffing;
        }

        public void setCurrIterationStaffing(int[] currIterationStaffing) {
            this.currIterationStaffing = currIterationStaffing;
        }

        public int[] getNextIterationStaffing() {
            return nextIterationStaffing;
        }

        public void setNextIterationStaffing(int[] nextIterationStaffing) {
            this.nextIterationStaffing = nextIterationStaffing;
        }

        public double getConvergenceTau() {
            return convergenceTau;
        }

        public void setConvergenceTau(double convergenceTau) {
            this.convergenceTau = convergenceTau;
        }

        @Override
        public boolean notConverged(ConvergenceData convergenceData) {
            for (int i = 0; i < currIterationStaffing.length; i++) {
                if (Math.abs(currIterationStaffing[i] - nextIterationStaffing[i]) > convergenceTau) {
//                System.out.println("We haven't converged since I've found a delta of: " + Math.abs( currIterationStaffing[i] - nextIterationStaffing[i]) + " at index: " + i );
                    return true;
                }
            }
            return false;
        }
    }

    class DefraeyeConvergenceData extends ConvergenceData{

        Vector<ArrayList> allHithertoStaffings;
        Vector<Double> allHithertoSCVs;
        Vector<Integer> allHithertoISCVs;
        double[] excessWaitProbabilities;
        int minIcsvCycleLength;


        @Override
        public boolean notConverged(ConvergenceData convergenceData) {
            double scv = coefficientOfVariation(excessWaitProbabilities);
            int numIters = allHithertoStaffings.size();
            for( int i = 0 ; i < numIters - 1  ; i++ ){
                if(allHithertoStaffings.get(i).equals(allHithertoStaffings.get(numIters - 1))){
                    return false;
                }
            }

            if( (allHithertoSCVs.get(numIters - 1)) <= 1 && (isAlternating(allHithertoISCVs))){
                return false;
            }
            return true;
        }

        //They don't quite define the length of the cycle...
        private boolean isAlternating(Vector<Integer> allHithertoISCVs) {
            int howMuchToCheckBack = Math.min(minIcsvCycleLength, allHithertoISCVs.size() - 1);
            return isAlternatingRec( allHithertoISCVs, allHithertoISCVs.size() - 1, howMuchToCheckBack);
        }

        private boolean isAlternatingRec( Vector<Integer> Icsvs, int currInd, int howMuchToCheck ){
            if( Icsvs.size() - currInd == howMuchToCheck ){
                return true;
            }
            if(  Icsvs.get(currInd) == Icsvs.get(currInd - 1) ){
                return false;
            }
            else
            {
                return isAlternatingRec(Icsvs, currInd - 1, howMuchToCheck);
            }
        }
    }

    public static int timeBinToPrint = 23;// 145;
    public static int LimitIters = 20;
    public static OptimizationScheme optimizationScheme = DEFRAEYE;// FELDMAN_ALPHA; // BINARY_WAIT_TIME;//
    static int numPeriodsRepetitionsTillSteadyState = 1;
    static int numRepetitionsToStatistics = 5;
    static boolean fastMode = true;
    static double toleranceAlpha = 0.5; //Probability of waiting in queue.
    static double convergenceTau = 2; //Convergence condition
    static int initialStaffingPerBin = 1000;
    static ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
    static ServerWaitingQueueMode serverWaitingQueueMode = WITH_WAITING_QUEUE;
    static AbandonmentModelingScheme abandonmentModelingScheme = AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA; //AbandonmentModelingScheme.EXPONENTIAL_SILENT_MARKED; //
    static double holdingTimeEps = 100;
    static double targetHoldingTime = 900; //In seconds.
    static double beta = 0.5;


    private int calcNumSlotsInBin(double[] totalInSystemDistribution, double toleranceAlpha, boolean print) {
        int returnedStaffing = 0;
        double accumulatedProb = 0;
        do {
            accumulatedProb += totalInSystemDistribution[returnedStaffing];
            returnedStaffing += 1;
            if (print) {
//                System.out.println("staffing: " + returnedStaffing + " accumulatedProb: " + accumulatedProb);
            }
        } while ((1 - accumulatedProb) > toleranceAlpha);
        if (print) {
//            System.out.println("Required num Slots: " + (returnedStaffing ));
        }
        return returnedStaffing;
    }


    /**
     * @param totalInSystemDistribution - keeps the distribution of the number of conversations in the system per each time bin
     * @param toleranceAlpha            - the tolerance over the probability to wait
     * @param singleAgentCapacity       - the per-agent avergare capacity (number of slots)
     * @return The staffing per each timebin to achieve the required tolerance.
     * TODO: check the option of replacing singleAgentCapacity (also in the simulation itself) by a constant capacity per agent, or just work with slots units.
     */
    private int[] determineNextIterationStaffing(Vector<double[]> totalInSystemDistribution, double toleranceAlpha, int[] singleAgentCapacity) throws Exception {
        if (!(toleranceAlpha <= 1 && toleranceAlpha >= 0)) {
            throw new Exception("toleranceAlpha should be between 0 and 1!!!");
        }
        int[] returnedStaffing = new int[totalInSystemDistribution.size()];
//        int printBinNum = 145;
        for (int i = 0; i < totalInSystemDistribution.size(); i++) {

            returnedStaffing[i] = calcNumSlotsInBin(totalInSystemDistribution.get(i), toleranceAlpha, i == timeBinToPrint) / singleAgentCapacity[i];
//            if( i == timeBinToPrint ){
//                System.out.println("The number of agents that's supposed to produce wait time probability below " + toleranceAlpha + " is: " + returnedStaffing[i]);
//            }
        }
        return returnedStaffing;
    }

//    public TimeDependentSimResults optimizeFeldman(){
//
//
//    }


    int[] determineNextIterationStaffingBinaryWaitTime(int[] currStaffing, double targetHoldingTime, SimParams inputs) throws Exception {

        for (int i = 0; i < currStaffing.length; i++) {
            currStaffing[i] = findStaffingForBin(i, targetHoldingTime, currStaffing, inputs);
        }
        return currStaffing;

    }

    private int findStaffingForBin(int i, double targetHoldingTime, int[] currStaffing, SimParams inputs) throws Exception {
        int left = 0;
        int right = 2 * initialStaffingPerBin; //2*currStaffing[i];//
        System.out.println("################################################ ");
        System.out.println("Starting iteration for bin " + i + ".");
        do {
            //The first attempt is always the previous staffing.


            int currStaffingForBin = Math.max((right + left) / 2, 1);
            currStaffing[i] = currStaffingForBin;
            inputs.setStaffing(currStaffing);
            System.out.println("Curr staffing: " + currStaffing[i]);
            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode);
            TimeDependentSimResults result = sim.simulate(inputs.getPeriodDurationInSecs(), numPeriodsRepetitionsTillSteadyState,
                    (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(), inputs,
                    abandonmentModelingScheme, fastMode, 0);
            double holdingTimeForCurrBin = result.getHoldingTime(i);
            if (Math.abs(holdingTimeForCurrBin - targetHoldingTime) <= holdingTimeEps) {
                System.out.println("Resulting staffing for bin: " + i + ": " + currStaffingForBin);
                System.out.println("Corresponding wait time: " + holdingTimeForCurrBin + "\n");
                return currStaffingForBin;
            } else if (holdingTimeForCurrBin > targetHoldingTime) {
                left = currStaffingForBin;
            } else {
                right = currStaffingForBin;
            }
            System.out.println("Resulting holding time: " + holdingTimeForCurrBin);
        }
        while (true);
    }


    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("I need the input folder as input parameter!! Aborting...");
            return;
        }
        StaffingOptimizer staffingOptimizer = new StaffingOptimizer();
        //This is assumed to be a parent folder, containing the raw extracted data from hadoop and the raw data processed for this program
        String inputFolderName = args[0];
        String outfolder;
        if (args.length >= 2) {
            outfolder = args[1];
        } else {
            outfolder = inputFolderName + "/RecommendedStaffing";
        }
        File directory = new File(outfolder);
        if (!directory.exists()) {
            directory.mkdir();
        }


        String paramsFolderName = inputFolderName + "/FetchedDiagnostics-InputToJava";
        try {
            SimParams inputs = SimParams.fromInputFolder(paramsFolderName);


//            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
//                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode );


            int[] numBinsAndSizes = inputs.getNumBinsAndSize();
            //In Defraeye, the calculation bins may differ in size from the staffing bins. For example, I may determine staffing on an hourly basis, but perform the simulation
            //calculation on a 15 mins basis. So in this example, calculationIntervalSize == 15 mins.
            int numCalculationBins = numBinsAndSizes[0];
            int calculationIntervalSize = numBinsAndSizes[1];
            int[] initialStaffing = staffingOptimizer.generateInitialStaffing(numCalculationBins, optimizationScheme, inputs);
            inputs.setStaffing(initialStaffing);

            Vector<ArrayList> allStaffings = new Vector<>();
            Vector<ArrayList> allQueueTimes = new Vector<>();


            int[] nextIterationStaffing;
            int[] currIterationStaffing;
            double[] currIterationHoldingTimes;
            TimeDependentSimResults result;

            int iterationNum = 0;
            do {
                System.out.println("#################### Iteration " + iterationNum + " ################################\n");
                System.out.println("Info for timebin " + timeBinToPrint + ":\n");
                ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                        new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode);
                result = sim.simulate(inputs.getPeriodDurationInSecs(), numPeriodsRepetitionsTillSteadyState,
                        (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(), inputs,
                        abandonmentModelingScheme, fastMode, targetHoldingTime);
                Vector<double[]> totalInSystemDistribution = result.getAllInSystemDistribution(true);
                currIterationStaffing = inputs.getStaffing();
                if (optimizationScheme == FELDMAN_ALPHA) {
                    nextIterationStaffing = staffingOptimizer.determineNextIterationStaffing(totalInSystemDistribution, toleranceAlpha, inputs.getSingleAgentCapacity());
                } else if (optimizationScheme == BINARY_WAIT_TIME) {
                    nextIterationStaffing = staffingOptimizer.determineNextIterationStaffingBinaryWaitTime(currIterationStaffing, targetHoldingTime, inputs);
                } else if (optimizationScheme == DEFRAEYE) {
                    nextIterationStaffing = staffingOptimizer.determineNextIterationStaffingDefraeye(currIterationStaffing, result.getExcessWaitProbabilities(),
                            targetHoldingTime, toleranceAlpha, iterationNum + 1, calculationIntervalSize);
                } else {
                    throw new InputMismatchException("We don't support optimization scheme " + optimizationScheme.toString());
                }


                inputs.setStaffing(nextIterationStaffing);
                ArrayList staffingArr = new ArrayList<Integer>();
                for (int i : currIterationStaffing) {
                    staffingArr.add(i);
                }
                allStaffings.add(staffingArr);
                currIterationHoldingTimes = result.getQueueTimes();
                ArrayList queueTimeArr = new ArrayList<Double>();
                for (double d : currIterationHoldingTimes) {
                    queueTimeArr.add(d);
                }
                allQueueTimes.add(queueTimeArr);

                System.out.println("The number of agents for timebin " + timeBinToPrint + " was: " + currIterationStaffing[timeBinToPrint]);
                System.out.println("The actual holding probability in time bin: " + timeBinToPrint + " was: " + result.getHoldingProbability(timeBinToPrint));
                System.out.println("The actual holding probability based on allInSystem in time bin: " + timeBinToPrint + " was: " + result.getHoldingProbabilityBasedOnAllInSystem(timeBinToPrint));
                if (result.getHoldingProbabilityBasedOnAllInSystem(timeBinToPrint) > result.getHoldingProbability(timeBinToPrint)) {
                    int x = 0;
                }
                System.out.println("The number of agents in the next iteration for timebin " + timeBinToPrint + " is: " + nextIterationStaffing[timeBinToPrint]);

                System.out.println("Global info: \n");
                double[] currAlphasRealization = result.getHoldingProbabilities();
                double accAlpha = 0;
                for (int i = 0; i < currAlphasRealization.length; i++) {
                    accAlpha += currAlphasRealization[i];
                }

                double accQueueTime = 0;
                for (int i = 0; i < currIterationHoldingTimes.length; i++) {
                    accQueueTime += currIterationHoldingTimes[i];
                }
                System.out.println("Average holding probability: " + accAlpha / currAlphasRealization.length + ". Average holding time: " + accQueueTime / currIterationHoldingTimes.length);
                System.out.println("####################################################\n");
                iterationNum += 1;
            } while (staffingOptimizer.notConverged(currIterationStaffing, nextIterationStaffing, convergenceTau,
                    currIterationHoldingTimes, targetHoldingTime, toleranceAlpha, result.getExcessWaitProbabilities(), optimizationScheme) && iterationNum <= LimitIters);


            result.writeToFile(outfolder);
            staffingOptimizer.writeStaffingsToFile(allStaffings, outfolder, result.getBinSize());
            staffingOptimizer.writeQueueTimesToFile(allQueueTimes, outfolder, result.getBinSize());
            double[] finalRealizationAlphas = result.getHoldingProbabilities();
            double[] finalRealizationAlphasBasedOnAllInSystem = result.getHoldingProbabilityBasedOnAllInSystem();
            System.out.println("The alphas (holding probabilities) of the final realization are: ");
            for (int i = 0; i < finalRealizationAlphas.length; i++) {
                System.out.println("bin " + i + ": " + finalRealizationAlphas[i] + ". BasedOnAllInSystem: " + finalRealizationAlphasBasedOnAllInSystem[i]);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private int[] determineNextIterationStaffingDefraeye(int[] currIterationStaffing, double[] excessWaitProbabilitiesP, double targetHoldingTime,
                                                         double toleranceAlpha, int iterationNumber, int calculationIntervalSize) throws Exception {
        double[] pMax = new double[currIterationStaffing.length];
        int targetHoldingTimeInIndices = (int) Math.ceil(targetHoldingTime / calculationIntervalSize);

        for (int i = 0; i < pMax.length; i++) {
            int leftInd = i - targetHoldingTimeInIndices;
            int rightInd = i + 1 - targetHoldingTimeInIndices;
            int numEntries = rightInd - leftInd + 1;
            leftInd = leftInd >= 0 ? leftInd : excessWaitProbabilitiesP.length + leftInd;

//            rightInd = rightInd >= 0? rightInd : excessWaitProbabilitiesP.length + rightInd;

            pMax[i] = myMax(excessWaitProbabilitiesP, leftInd, numEntries);
        }

        double Ai;
        int[] nextStaffing = new int[currIterationStaffing.length];

        for (int i = 0; i < pMax.length; i++) {
            Ai = 1 + (pMax[i] - toleranceAlpha) / (toleranceAlpha * iterationNumber);
            //If the current staffing was 1, and we want to increase it, we can't use the multiplication scheme.
            nextStaffing[i] = (int) (Ai >= 1 ? Math.ceil((currIterationStaffing[i] > 0 ? currIterationStaffing[i] : 1) * Ai) : Math.floor(currIterationStaffing[i] * Ai));
        }

        return nextStaffing;
    }

    private double myMax(double[] arr, int startInd, int numEntries) throws Exception {
        if (arr == null || arr.length == 0) {
            throw new Exception("Got a null or empty array to myMax");
        }

        double currMax = -Double.MAX_VALUE;
        for (int i = 0; i < numEntries; i++) {
            currMax = Math.max(currMax, arr[(startInd + i) % arr.length]);
        }
        return currMax;

    }

    private int[] generateInitialStaffing(int numBins, OptimizationScheme optimizationScheme, SimParams inputs) throws Exception {

        int[] initialStaffing = new int[numBins];

        for (int i = 0; i < initialStaffing.length; i++) {
            initialStaffing[i] = initialStaffingPerBin;

        }

        if (optimizationScheme == DEFRAEYE) {
            //Analytical calculation is problematic, since I don't know the single exchange resolved probability, thus the
            //distribution of the number of exchanges per conversation.
//            double averageNumExchangesPerConv = 0;
//            double[] numExchangesPerConvDistribution = simParams.numExchangesPerConvDistribution;
//            //!!! TODO: if this is time dependent, the calculation needs to be amended!!!
//            double singleExchangeDuration = 1/simParams.singleConsumerNeedServiceRate[0];
//            for( int i = 0 ; i < numExchangesPerConvDistribution)
            inputs.setStaffing(initialStaffing);
            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode);
            TimeDependentSimResults result = sim.simulate(inputs.getPeriodDurationInSecs(), numPeriodsRepetitionsTillSteadyState,
                    (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(), inputs,
                    abandonmentModelingScheme, fastMode, 0);
            //That's the offered load (rho), per bin
            int[] singleAgentCapacity = inputs.singleAgentCapacity;
            double[] meanNumInSystem = result.getMeanNumInSystem(true);
            for (int i = 0; i < initialStaffing.length; i++) {
//                int currNumInSystem = (int)Math.round(Math.max( 1, meanNumInSystem[i]));
                //Convert the load to Agents units (each agent has concurrency > 1, in general)
                int currNumInSystem = (int) Math.round(meanNumInSystem[i] / singleAgentCapacity[i]);
                //TODO!!! Check why this produces huge results!!
                initialStaffing[i] = Math.min(initialStaffingPerBin, (int) Math.ceil(currNumInSystem + beta * Math.sqrt(currNumInSystem)));

            }

        }
        return initialStaffing;

    }

    private void writeSimIterationsToFile(Vector<ArrayList> allData, String outfolder, String outFilename, int binSize) {
        FileWriter fileWriterStaffingIterations = null;

        try {

            //Write per Bin statistics
            fileWriterStaffingIterations = new FileWriter(outfolder + "/" + outFilename);

            for (int i = 0; i < allData.get(0).size(); i++) {
                String res = "";
                for (int j = 0; j < allData.size(); j++) {
                    res += "," + allData.get(j).get(i);
                }
                fileWriterStaffingIterations.append(i * binSize + res + "\n");

            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {

                fileWriterStaffingIterations.flush();
                fileWriterStaffingIterations.close();


            } catch (IOException e) {

                System.out.println("Error while flushing/closing fileWriterStaffingIterations !!!");

                e.printStackTrace();

            }

        }
    }


    private void writeQueueTimesToFile(Vector<ArrayList> allStaffings, String outfolder, int binSize) {
        writeSimIterationsToFile(allStaffings, outfolder, "queueTimeIterations.csv", binSize);
    }

    private void writeStaffingsToFile(Vector<ArrayList> allStaffings, String outfolder, int binSize) {

        writeSimIterationsToFile(allStaffings, outfolder, "staffingIterations.csv", binSize);

    }


    private boolean notConverged(int[] currIterationStaffing, int[] nextIterationStaffing, double convergenceTau,
                                 double[] currIterationHoldingTimes, double targetHoldingTime, double toleranceAlpha, double[] excessWaitProbabilities,
                                 OptimizationScheme optimizationScheme) {

        if (optimizationScheme == DEFRAEYE) {
            double scv = coefficientOfVariation(excessWaitProbabilities);
            // Very inefficient. Should be improved if we proceed with this algo.

            for (int i = 0; i < currIterationStaffing.length; i++) {

                if (Math.abs(currIterationHoldingTimes[i] - targetHoldingTime) > toleranceAlpha) {
                    return true;
                }
            }
            return false;
        } else {
            for (int i = 0; i < currIterationStaffing.length; i++) {
                if (Math.abs(currIterationStaffing[i] - nextIterationStaffing[i]) > convergenceTau) {
//                System.out.println("We haven't converged since I've found a delta of: " + Math.abs( currIterationStaffing[i] - nextIterationStaffing[i]) + " at index: " + i );
                    return true;
                }
            }
            return false;
        }

    }
}
