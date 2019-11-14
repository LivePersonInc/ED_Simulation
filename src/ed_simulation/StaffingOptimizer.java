package ed_simulation;


import com.sun.tools.javac.util.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static ed_simulation.DefraeyeNexIterationScheme.OnlyCurrentBin;
import static ed_simulation.OptimizationScheme.*;
import static ed_simulation.ServerAssignmentMode.FIXED_SERVER_CAPACITY;
import static ed_simulation.ServerWaitingQueueMode.WITH_WAITING_QUEUE;
import static statistics.commons.coefficientOfVariation;

/**
 Finds an optimal staffing for a contact center given performance measures (e.g. upper bound of the waiting probability)
 *
 */
public class StaffingOptimizer {


    public static int timeBinToPrint = 23;// 145;
    //Simulation configuration.
    public static OptimizationScheme optimizationScheme = DEFRAEYE;// FELDMAN_ALPHA; // BINARY_WAIT_TIME;//
    static int numPeriodsRepetitionsTillSteadyState = 2;
    static int numRepetitionsToStatistics = 30; //50;
    static boolean fastMode = true;
    //All Schemes
    public static int LimitIters = 100;
    static double toleranceAlpha = 0.1; //Probability of waiting in queue.
    //Feldman
    static double convergenceTau = 2; //Convergence condition Feldman
    static int initialStaffingPerBin = 1000;
    //Binary search
    static double holdingTimeEps = 100;
    //Defraeye
    static double targetHoldingTime =  900; //900; //In seconds.
    static double beta = 2;
    static int minIcsvCycleLength = 8;
    static int agentLaborhourCost = 1; //TODO: improve this model, e.g. by capacity?
    static double convergeceSpeedFactorDefraeye = 4;
    static boolean defraeyeDecreaseOverStaffing = false;
    static int howManyRealizationsToPhaseIIDefraeye = 2;
//    static boolean calcMeanInsteadOfMax = false;
    static DefraeyeNexIterationScheme defraeyeNexIterationScheme = OnlyCurrentBin;


    static ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
    static ServerWaitingQueueMode serverWaitingQueueMode = WITH_WAITING_QUEUE;
    static AbandonmentModelingScheme abandonmentModelingScheme = AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA; //AbandonmentModelingScheme.EXPONENTIAL_SILENT_MARKED; //



    abstract class ConvergenceData{
        public abstract boolean notConverged(  );

    }
    public class FeldmanConvergenceData extends ConvergenceData{
        int[] currIterationStaffing;
        int[] nextIterationStaffing;
        double convergenceTau;

        public FeldmanConvergenceData(double convergenceTau) {
            super();
            this.convergenceTau = convergenceTau;
        }

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
        public boolean notConverged() {
            for (int i = 0; i < currIterationStaffing.length; i++) {
                if (Math.abs(currIterationStaffing[i] - nextIterationStaffing[i]) > convergenceTau) {
//                System.out.println("We haven't converged since I've found a delta of: " + Math.abs( currIterationStaffing[i] - nextIterationStaffing[i]) + " at index: " + i );
                    return true;
                }
            }
            return false;
        }
    }

    public class DefraeyeConvergenceData extends ConvergenceData{

        Vector<ArrayList> allHithertoStaffings;
        Vector<Double> allHithertoSCVs;
        Vector<Integer> allHithertoISCVs;
        double[] excessWaitProbabilities;
        int minIcsvCycleLength;

        public Vector<ArrayList> getAllHithertoStaffings() {
            return allHithertoStaffings;
        }

        public void setAllHithertoStaffings(Vector<ArrayList> allHithertoStaffings) {
            this.allHithertoStaffings = allHithertoStaffings;
        }

        public Vector<Double> getAllHithertoSCVs() {
            return allHithertoSCVs;
        }

        public void setAllHithertoSCVs(Vector<Double> allHithertoSCVs) {
            this.allHithertoSCVs = allHithertoSCVs;
        }

        public Vector<Integer> getAllHithertoISCVs() {
            return allHithertoISCVs;
        }

        public void setAllHithertoISCVs(Vector<Integer> allHithertoISCVs) {
            this.allHithertoISCVs = allHithertoISCVs;
        }

        public double[] getExcessWaitProbabilities() {
            return excessWaitProbabilities;
        }

        public void setExcessWaitProbabilities(double[] excessWaitProbabilities) {
            this.excessWaitProbabilities = excessWaitProbabilities;
        }

        public int getMinIcsvCycleLength() {
            return minIcsvCycleLength;
        }

        public void setMinIcsvCycleLength(int minIcsvCycleLength) {
            this.minIcsvCycleLength = minIcsvCycleLength;
        }

        @Override
        public boolean notConverged() {
            double scv = coefficientOfVariation(excessWaitProbabilities);
            int numIters = allHithertoStaffings.size();
            for( int i = 0 ; i < numIters - 1  ; i++ ){
                if(allHithertoStaffings.get(i).equals(allHithertoStaffings.get(numIters - 1))){
                    return false;
                }
            }

            if( (allHithertoSCVs.get(numIters - 1)) <= 1 && (isAlternating(allHithertoISCVs))){
                System.out.println("IsAlternating: " + isAlternating(allHithertoISCVs));
                return false;
            }
            return true;
        }

        //They don't quite define the length of the cycle...
        private boolean isAlternating(Vector<Integer> allHithertoISCVs) {
            if(minIcsvCycleLength >= allHithertoISCVs.size()){
                return false;
            }
//            int howMuchToCheckBack = Math.min(minIcsvCycleLength, Math.max(allHithertoISCVs.size() - 1, 0));
            return isAlternatingRec( allHithertoISCVs, allHithertoISCVs.size() - 1, minIcsvCycleLength);
        }

        private boolean isAlternatingRec( Vector<Integer> Icsvs, int currInd, int howMuchToCheck ){
            if( howMuchToCheck <= 0 ){
                return false;
            }
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


    class DefraeyeStaffingData{
        private double laborTimeUnitCost = StaffingOptimizer.agentLaborhourCost; //TODO: this should be fixed according to the time interval.
        private double toleranceAlpha = StaffingOptimizer.toleranceAlpha;
        private ArrayList<Integer> staffing = null;
        private double[] excessWaitProbabilities = null;
        private double staffingCost;
        private Vector<ArrayList<Integer>> staffingHistory = new Vector();
        private Vector<ArrayList> queueTimesHistory = new Vector();
        private Vector<ArrayList> excessProbsHistory = new Vector();

        public DefraeyeStaffingData(double cost)
        {
            staffingCost = cost;
        }

        public DefraeyeStaffingData(ArrayList staffing, double[] pMax) throws Exception {
            this.staffing = staffing;
            this.excessWaitProbabilities = pMax;
            this.staffingCost = calcStaffingCost();
        }

        private double calcStaffingCost() throws Exception {
            if(staffing == null)
            {
                throw new Exception("Can't calculate staffing cost of a null staffing");
            }
            double res = 0;
            for( Integer n : staffing){
                res += n;
            }
            return res * laborTimeUnitCost;
        }
        public double getStaffingCost() { return this.staffingCost; }

        //Modifies the staffing represented by this object by adding one capacity unit (agent) at required times. Keeps a snapshot of the
        //pre-amended staffing.
        public void amendStaffingByUnit(boolean decreaseOverStaffing, boolean storeSnapshot) {
            //Save the last staffing
            if( storeSnapshot ) {
                saveStaffingSnapshot();
            }

            for(int i = 0 ; i < staffing.size() ; i++ )
            {
                if(excessWaitProbabilities[i] > toleranceAlpha ){
                    staffing.set(i, staffing.get(i) + 1);
                    this.staffingCost += laborTimeUnitCost;
                }
                //!!! Important!! - as is this doesn't converge. Use with care!!!
                else if( decreaseOverStaffing && excessWaitProbabilities[i] < toleranceAlpha){
                    staffing.set(i, staffing.get(i) - 1);
                    this.staffingCost -= laborTimeUnitCost;
                }
            }

        }

        public void amendStaffingByUnit(boolean decreaseOverStaffing){
            amendStaffingByUnit(decreaseOverStaffing, true);
        }

        public int[] getStaffing() {
            int[] res = new int[staffing.size()];
            for( int i = 0 ; i < staffing.size() ; i++ )
            {
                res[i] = staffing.get(i);
            }
            return res;
        }

        public void setExcessWaitProbabilities(double[] excessWaitProbabilities) {
            ArrayList<Double> res = new ArrayList<>(excessWaitProbabilities.length);
            for( int i = 0 ; i < excessWaitProbabilities.length ; i++ ){
                res.add(excessWaitProbabilities[i]);
            }
            excessProbsHistory.add(res);
            this.excessWaitProbabilities = excessWaitProbabilities;
        }

        public void storeQueueTimes( double[] queueTimesSnapshot ){
            ArrayList<Double> res = new ArrayList<>(queueTimesSnapshot.length);
            for( int i = 0 ; i < queueTimesSnapshot.length ; i++ ){
                res.add(queueTimesSnapshot[i]);
            }
            queueTimesHistory.add(res);

        }

        public boolean isFeasible() {
            boolean res = true;
            for( int i = 0 ; i < excessWaitProbabilities.length ; i++ ){
                if(excessWaitProbabilities[i] > toleranceAlpha )
                {
                    return false;
                }
            }
            return res;
        }

        public Vector<ArrayList<Integer>> getStaffingHistory() {
            return staffingHistory;

        }

        public Vector<ArrayList> getQueueTimesHistory() {
            return queueTimesHistory;
        }

        public Vector<ArrayList> getExcessProbsHistory() {
            return excessProbsHistory;
        }

        public void saveStaffingSnapshot() {
            ArrayList<Integer> snapshot = new ArrayList<>(staffing.size());
            for (int i = 0; i < staffing.size(); i++) {
                snapshot.add(i, staffing.get(i));
            }
            staffingHistory.add(snapshot);
        }
    }


//    class StaffingComparator implements Comparator<DefraeyeStaffingData>{
//        public int compare( DefraeyeStaffingData staffing1, DefraeyeStaffingData staffing2 ){
//            int cmp = Double.compare( staffing1.getMaxPmax(), staffing2.getMaxPmax() );
//            if( cmp == 0 ){
//                cmp = Double.compare( staffing1.getCostCorrected(), staffing2.getCostCorrected());
//            }
//            return cmp;
//        }
//    }



    public String generateDefraeyeHeader(int n1, String s1, int n2, String s2){
        String res = "";
        int i = 1;
        for( ; i <= n1 ; i++ ){
            res += s1 + ",";
        }
        for( i = 1 ; i <= n2 ; i++ ){
            res += s2 + ",";
        }
        return res.substring(0, res.length() );
    }

    //StaffingOptimizer methods
    private int staffincCost(int[] staffingVec, int agentLaborhourCost){
        int res = 0;
        for( int i = 0 ; i < staffingVec.length ; i++ ){
            res += staffingVec[i] * agentLaborhourCost;
        }
        return res;
    }

    private int numExceedingTolerance(double[] pMax, double toleranceAlpha){
        int res = 0;
        for( int i = 0 ; i < pMax.length ; i++ ){
            res += pMax[i] >= toleranceAlpha ? 1 : 0;
        }
        return res;
    }


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

    //The Binary search optimizer.
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

        String suff = "";
        if(args.length >= 3){
            suff = args[2];
        }
        File directory = new File(outfolder);
        if (!directory.exists()) {
            directory.mkdir();
        }


        String paramsFolderName = inputFolderName + "/FetchedDiagnostics-InputToJava" + suff;
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
            Vector<ArrayList> allExcessiveProbs = new Vector<>();
            Vector<double[]> allExcessWaitProbabilities = new Vector<>();
            Vector<Double> allSCVs = new Vector<>();
            Vector<Integer> allISCVs = new Vector<>();

            int[] nextIterationStaffing;
            int[] currIterationStaffing;
            double[] currIterationHoldingTimes;
            double[] currIterationExcessiveProbs;
            TimeDependentSimResults result;

            ConvergenceData convergenceData;
            if(optimizationScheme == FELDMAN_ALPHA || optimizationScheme == BINARY_WAIT_TIME ){
                convergenceData =  staffingOptimizer.new FeldmanConvergenceData(convergenceTau);
            }
            else if(optimizationScheme == DEFRAEYE){
                convergenceData =  staffingOptimizer.new DefraeyeConvergenceData();
            }
            else{
                throw new InputMismatchException("Optimization schem not supported: " + optimizationScheme.toString());
            }


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
                            targetHoldingTime, toleranceAlpha, iterationNum + 1, calculationIntervalSize, defraeyeNexIterationScheme);
                    allExcessWaitProbabilities.add(result.getExcessWaitProbabilities());
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

                currIterationExcessiveProbs = result.getExcessWaitProbabilities();
                ArrayList excessProbsArr = new ArrayList<Double>();
                for (double d : currIterationExcessiveProbs) {
                    excessProbsArr.add(d);
                }
                allExcessiveProbs.add(excessProbsArr);


                double currSCV = coefficientOfVariation(result.getExcessWaitProbabilities());
                currSCV = currSCV*currSCV;
                allISCVs.add( allSCVs.isEmpty() ? 1 : currSCV <= allSCVs.lastElement() ? 1 : 0);
                allSCVs.add(currSCV);


                //TODO: have the staffingOptimizers inherit from a parent, and each one should have its own Convergence Data.
                if(optimizationScheme == DEFRAEYE  ){

                    ((DefraeyeConvergenceData) convergenceData).setAllHithertoStaffings(allStaffings);
                    ((DefraeyeConvergenceData) convergenceData).setAllHithertoSCVs(allSCVs);
                    ((DefraeyeConvergenceData) convergenceData).setAllHithertoISCVs(allISCVs);
                    ((DefraeyeConvergenceData) convergenceData).setExcessWaitProbabilities(result.getExcessWaitProbabilities());
                    ((DefraeyeConvergenceData) convergenceData).setMinIcsvCycleLength(minIcsvCycleLength);


                }
                else if(optimizationScheme == FELDMAN_ALPHA || optimizationScheme == BINARY_WAIT_TIME  ) {
                    ((FeldmanConvergenceData) convergenceData).setCurrIterationStaffing(currIterationStaffing);
                    ((FeldmanConvergenceData) convergenceData).setNextIterationStaffing(nextIterationStaffing);
                }


                System.out.println("The number of agents for timebin " + timeBinToPrint + " was: " + currIterationStaffing[timeBinToPrint]);
//                System.out.println("The actual holding probability in time bin: " + timeBinToPrint + " was: " + result.getHoldingProbability(timeBinToPrint));
//                System.out.println("The actual holding probability based on allInSystem in time bin: " + timeBinToPrint + " was: " + result.getHoldingProbabilityBasedOnAllInSystem(timeBinToPrint));
//                if (result.getHoldingProbabilityBasedOnAllInSystem(timeBinToPrint) > result.getHoldingProbability(timeBinToPrint)) {
//                    int x = 0;
//                }
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
            } while (convergenceData.notConverged() && iterationNum <= LimitIters);

            String header = null;
            if(optimizationScheme == DEFRAEYE){
               System.out.println("Starting phase II...");
               System.out.println("The ExcessWaitProbabilities are: ");
               double[] tmp = result.getExcessWaitProbabilities();
               for( int i = 0 ; i < tmp.length ; i++ )
               {
                   System.out.println( i + "," + tmp[i]);
               }
               Pair<DefraeyeStaffingData, TimeDependentSimResults> cheapestFeasibleStaffing = staffingOptimizer.applyPhaseII( allStaffings,
                       allExcessWaitProbabilities, inputs, howManyRealizationsToPhaseIIDefraeye, defraeyeDecreaseOverStaffing);
               result = cheapestFeasibleStaffing.snd;
               DefraeyeStaffingData staffingData = cheapestFeasibleStaffing.fst;
               header = staffingOptimizer.generateDefraeyeHeader(allStaffings.size(), "phase1", staffingData.getStaffingHistory().size(), "phase2");
               allStaffings.addAll(staffingData.getStaffingHistory());
               allQueueTimes.addAll(staffingData.getQueueTimesHistory());
               allExcessiveProbs.addAll(staffingData.getExcessProbsHistory());
               System.out.println("Finished phase II");
               System.out.println("The ExcessWaitProbabilities are: ");
               tmp = result.getExcessWaitProbabilities();
               for( int i = 0 ; i < tmp.length ; i++ )
               {
                   System.out.println( i + "," + tmp[i]);
               }

            }

            result.writeToFile(outfolder, 1, inputs.timestamps);
            staffingOptimizer.writeStaffingsToFile(allStaffings, outfolder, result.getBinSize(), header);
            staffingOptimizer.writeQueueTimesToFile(allQueueTimes, outfolder, result.getBinSize(), header);
            staffingOptimizer.writeExcessiveProbsToFile(allExcessiveProbs, outfolder, result.getBinSize(), header);
            staffingOptimizer.writeSCVs(allSCVs, allISCVs, outfolder, 1);
//            double[] finalRealizationAlphas = result.getHoldingProbabilities();
//            double[] finalRealizationAlphasBasedOnAllInSystem = result.getHoldingProbabilityBasedOnAllInSystem();
//            System.out.println("The alphas (holding probabilities) of the final realization are: ");
//            for (int i = 0; i < finalRealizationAlphas.length; i++) {
//                System.out.println("bin " + i + ": " + finalRealizationAlphas[i] + ". BasedOnAllInSystem: " + finalRealizationAlphasBasedOnAllInSystem[i]);
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void amendStaffingSlow(DefraeyeStaffingData currStaffingData, SimParams inputs) throws Exception {

//        while (hasSomeOverstaffedBins(currStaffingData.getLastExcessiveProb())) {
//            inputs.setStaffing(currStaffingData.getStaffing());
//            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
//                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode);
//            TimeDependentSimResults result = sim.simulate(inputs.getPeriodDurationInSecs(), numPeriodsRepetitionsTillSteadyState,
//                    (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(), inputs,
//                    abandonmentModelingScheme, fastMode, targetHoldingTime);
//            currStaffingData.setExcessWaitProbabilities(result.getExcessWaitProbabilities());
//            currStaffingData.storeQueueTimes(result.getQueueTimes());
//            if (!currStaffingData.isFeasible()) {
//                currStaffingData.amendStaffingByUnit(decreaseOverStaffing);
//                System.out.println("Realization not feasible. Amending again...");
//            }
//
//        }
    }

    //TODO: have Defraeye and Feldman extend StaffingOptimizer and implement their appropriate methods
    //I've omitted the initial sorting specified in the paper, since we're iterating over all solutions anyhow, and I'm limiting their number apriori.
    private Pair<DefraeyeStaffingData, TimeDependentSimResults> applyPhaseII(Vector<ArrayList> allStaffings, Vector<double[]> allExcessWaitProbabilities, SimParams inputs,
                                       int howManySolutionsToCheck, boolean decreaseOverStaffing) throws Exception {
        DefraeyeStaffingData bestSoFar = new DefraeyeStaffingData(Double.POSITIVE_INFINITY);
        TimeDependentSimResults bestSoFarSimResult = null;
//        Vector<StaffingData> sortedStaffings = Collections.sort( allStaffingsData, new StaffingComparator());
        for(int i = allStaffings.size() - 1; i > allStaffings.size() - howManySolutionsToCheck ; i-- ){
            System.out.println("");
            System.out.println("Starting to work on staffing number " + i);
            DefraeyeStaffingData currStaffingData = new DefraeyeStaffingData(allStaffings.get(i), allExcessWaitProbabilities.get(i));
            currStaffingData.amendStaffingByUnit(decreaseOverStaffing, false);
            if(decreaseOverStaffing){
               amendStaffingSlow(currStaffingData, inputs );

            }

            //TODO: this condition may not be appropriate when allowing staffing reduction (i.e., when decreaseOverStaffing == True), not only staffing increase.
            //This is so since currStaffingData may have a higher score, but still potentially amended to a lower score staffing (by reducing over staffing)
            while(currStaffingData.getStaffingCost() < bestSoFar.getStaffingCost() ){
                inputs.setStaffing(currStaffingData.getStaffing());
                ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                        new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode);
                TimeDependentSimResults result = sim.simulate(inputs.getPeriodDurationInSecs(), numPeriodsRepetitionsTillSteadyState,
                        (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(), inputs,
                        abandonmentModelingScheme, fastMode, targetHoldingTime);
                currStaffingData.setExcessWaitProbabilities(result.getExcessWaitProbabilities());
                currStaffingData.storeQueueTimes(result.getQueueTimes());
                if( !currStaffingData.isFeasible() ){
                    currStaffingData.amendStaffingByUnit(decreaseOverStaffing);
                    System.out.println("Realization not feasible. Amending again...");
                }
                else
                {
                    if(currStaffingData.getStaffingCost() < bestSoFar.getStaffingCost())
                    {
                        //We've found a cheaper feasible staffing.
                        currStaffingData.saveStaffingSnapshot();
                        bestSoFar = currStaffingData;
                        bestSoFarSimResult = result;
                        System.out.println("Realization feasible and improving. Setting it as the best one so far.");
                    }
                }

            }

        }
        return Pair.of(bestSoFar,bestSoFarSimResult) ;

    }



    //TODO: Notice that currIterationStaffing and excessWaitProbabilitiesP may have different dimensions. The former is in staffing interval units (e.g. 1 hour),
    // and the latter is in calculation intervals (e.g. 15 mins)
    private int[] determineNextIterationStaffingDefraeye(int[] currIterationStaffing, double[] excessWaitProbabilitiesP, double targetHoldingTime,
                                                         double toleranceAlpha, int iterationNumber, int calculationIntervalSize, DefraeyeNexIterationScheme defraeyeNexIterationScheme) throws Exception {
        double[] pMax = new double[currIterationStaffing.length];
        int targetHoldingTimeInIndices = (int) Math.ceil(targetHoldingTime / calculationIntervalSize);

        for (int i = 0; i < pMax.length; i++) {
            int leftInd = i - targetHoldingTimeInIndices; // toCalculationIndex( i, targetHoldingTime, currIterationStaffing.length, excessWaitProbabilitiesP.length);
            int rightInd =   i  + 1 - targetHoldingTimeInIndices;
            int numEntries = rightInd - leftInd + 1; //Notice that the number of entries doesn't depend on the targetHoldingTime - only on the ratio between the calculation and saffing interval.
            leftInd = leftInd >= 0 ? leftInd : excessWaitProbabilitiesP.length + leftInd;
            boolean calcMeanInsteadOfMax = false;
            if( defraeyeNexIterationScheme == OnlyCurrentBin)
            {
                leftInd = i;
                numEntries = 1;
                calcMeanInsteadOfMax = false;
            }
            pMax[i] = myMax(excessWaitProbabilitiesP, leftInd, numEntries, calcMeanInsteadOfMax);
        }

        double Ai;
        int[] nextStaffing = new int[currIterationStaffing.length];
        int numIncreasedStaffing = 0;
        int numDecreasedStaffing = 0;
        for (int i = 0; i < pMax.length; i++) {
            Ai = 1 + (pMax[i] - toleranceAlpha) / (toleranceAlpha * iterationNumber/convergeceSpeedFactorDefraeye);
            numDecreasedStaffing += (Ai < 1 ? 1 :0);
            numIncreasedStaffing += (Ai >= 1 ? 1 : 0);
            //If the current staffing was 1, and we want to increase it, we can't use the multiplication scheme.
            nextStaffing[i] = (int) Math.max(0, Ai >= 1 ? Math.ceil((currIterationStaffing[i] > 0 ? currIterationStaffing[i] : 1) * Ai) : Math.floor(currIterationStaffing[i] * Ai));
        }
        System.out.println("Finished determining the next iteration staffing. I've increased the staffing of " + numIncreasedStaffing + " agents, and decreased " + numDecreasedStaffing + " agents.");
        return nextStaffing;
    }

    private double myMax(double[] arr, int startInd, int numEntries, boolean calcMeanInsteadOfMax) throws Exception {
        if (arr == null || arr.length == 0) {
            throw new Exception("Got a null or empty array to myMax");
        }

        double currMax = calcMeanInsteadOfMax ? 0 : -Double.MAX_VALUE;
        for (int i = 0; i < numEntries; i++) {
            currMax = calcMeanInsteadOfMax  ? currMax + arr[(startInd + i) % arr.length] :  Math.max(currMax, arr[(startInd + i) % arr.length]) ;
        }
        return calcMeanInsteadOfMax ? currMax/numEntries : currMax;

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

    private void writeSimIterationsToFile(Vector<ArrayList> allData, String outfolder, String outFilename, int binSize, String header) {
        FileWriter fileWriterStaffingIterations = null;

        try {

            //Write per Bin statistics
            fileWriterStaffingIterations = new FileWriter(outfolder + "/" + outFilename);

            if( header != null ){
                fileWriterStaffingIterations.append( header + "\n");
            }

            for (int i = 0; i < allData.get(0).size(); i++) {
                String res = "";
                for (int j = 0; j < allData.size(); j++) {
                    Object curr = allData.get(j).get(i);
                    res += "," + ((curr instanceof Double && (double)curr == Double.POSITIVE_INFINITY) ? "inf" : curr);
                }
                fileWriterStaffingIterations.append(i /* * binSize*/ + res + "\n");

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


    private void writeQueueTimesToFile(Vector<ArrayList> allQueueTimes, String outfolder, int binSize, String header) {
        writeSimIterationsToFile(allQueueTimes, outfolder, "queueTimeIterations.csv", binSize, header);
    }

    private void writeStaffingsToFile(Vector<ArrayList> allStaffings, String outfolder, int binSize, String header) {

        writeSimIterationsToFile(allStaffings, outfolder, "staffingIterations.csv", binSize, header);

    }

    private void writeExcessiveProbsToFile(Vector<ArrayList> allExcessiveProbs, String outfolder, int binSize, String header) {

        writeSimIterationsToFile(allExcessiveProbs, outfolder, "excessiveProbsIterations.csv", binSize, header);

    }



    private void writeSCVs(Vector<Double> allSCVs, Vector<Integer> allISCVs, String outfolder, int binSize) {
        Vector<ArrayList> toFile = new Vector<ArrayList>();
        toFile.add(new ArrayList(allSCVs));
        toFile.add(new ArrayList(allISCVs));
        writeSimIterationsToFile(toFile, outfolder, "SCV.csv", binSize, null);

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
