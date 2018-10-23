package ed_simulation;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;

import static ed_simulation.ServerAssignmentMode.FIXED_SERVER_CAPACITY;
import static ed_simulation.ServerWaitingQueueMode.TWO_INFTY_QUEUES;

/**
 Finds an optimal staffing for a contact center given performance measures (e.g. upper bound of the waiting probability)
 *
 */
public class StaffingOptimizer {


    private int calcNumAgentsInBin(double[] totalInSystemDistribution, double toleranceAlpha){
        int returnedStaffing = 0;
        double accumulatedProb = 0;
        do{
           accumulatedProb += totalInSystemDistribution[returnedStaffing];
           returnedStaffing += 1;
        }while(accumulatedProb <= toleranceAlpha);
        return returnedStaffing - 1;
    }



    /**
     *
     * @param totalInSystemDistribution - keeps the distribution of the number of conversations in the system per each time bin
     * @param toleranceAlpha - the tolerance over the probability to wait
     * @param singleAgentCapacity - the per-agent avergare capacity (number of slots)
     * @return The staffing per each timebin to achieve the required tolerance.
     * TODO: check the option of replacing singleAgentCapacity (also in the simulation itself) by a constant capacity per agent, or just work with slots units.
     */
    private int[] determineNextIterationStaffing(Vector<double[]> totalInSystemDistribution, double toleranceAlpha, int[] singleAgentCapacity) throws Exception {
        if(!(toleranceAlpha <= 1 && toleranceAlpha >= 0 ))
        {
            throw  new Exception("toleranceAlpha should be between 0 and 1!!!");
        }
        int[] returnedStaffing = new int[totalInSystemDistribution.size()];
        for( int i = 0 ; i < totalInSystemDistribution.size(); i++ ){
            returnedStaffing[i] = calcNumAgentsInBin(totalInSystemDistribution.get(i), toleranceAlpha)/singleAgentCapacity[i];
        }
        return returnedStaffing;
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
        if (! directory.exists()){
            directory.mkdir();
        }

        int numPeriodsRepetitionsTillSteadyState = 1;
        int numRepetitionsToStatistics = 1;

        AbandonmentModelingScheme abandonmentModelingScheme = AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA; //AbandonmentModelingScheme.EXPONENTIAL_SILENT_MARKED; //

        String paramsFolderName = inputFolderName + "/FetchedDiagnostics-InputToJava";
        try {
            SimParams inputs = SimParams.fromInputFolder(paramsFolderName);

            ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
            ServerWaitingQueueMode serverWaitingQueueMode = TWO_INFTY_QUEUES;


//            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
//                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode );

            double toleranceAlpha = 0.1; //Probability of waiting in queue.
            double convergenceTau = 10; //Convergence condition
            int initialStaffingPerBin = 1000;
            int[] numBinsAndSizes = inputs.getNumBinsAndSize();
            int[] initialStaffing = new int[numBinsAndSizes[0]];
            for(int i = 0 ; i < initialStaffing.length ; i++ )
            {
                initialStaffing[i] = initialStaffingPerBin;

            }
            inputs.setStaffing(initialStaffing);

            Vector<int[]> allStaffings = new Vector<int[]>();
            Vector<double[]> allQueueTimes = new Vector<double[]>();
            int[] nextIterationStaffing;
            int[] currIterationStaffing;
            TimeDependentSimResults result;

            int iterationNum = 0;
            do{
                ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                        new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode );
                result = sim.simulate(inputs.getPeriodDurationInSecs(), numPeriodsRepetitionsTillSteadyState ,
                        (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(), inputs, abandonmentModelingScheme);
                System.out.println("Finished the simulation...");
                Vector<double[]> totalInSystemDistribution = result.getAllInSystemDistribution();
                nextIterationStaffing = staffingOptimizer.determineNextIterationStaffing(totalInSystemDistribution, toleranceAlpha, inputs.getSingleAgentCapacity());
                currIterationStaffing = inputs.getStaffing();
                inputs.setStaffing(nextIterationStaffing);
                allStaffings.add(nextIterationStaffing);
                allQueueTimes.add(result.getQueueTimes());
                System.out.println("Just finished iteration number " + iterationNum + "\n");
                iterationNum += 1;
            }while( staffingOptimizer.notConverged(currIterationStaffing, nextIterationStaffing, convergenceTau) && iterationNum <= 30);


            result.writeToFile(outfolder);
            staffingOptimizer.writeStaffingsToFile(allStaffings, outfolder, result.getBinSize());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private void writeStaffingsToFile(Vector<?> allData, String outfolder, int binSize) {
    private void writeStaffingsToFile(Vector<int[]> allStaffings, String outfolder, int binSize) {

        FileWriter fileWriterStaffingIterations = null;


        try {

            //Write per Bin statistics
            fileWriterStaffingIterations = new FileWriter(outfolder + "/staffingIterations.csv");

            for( int i = 0 ; i < allStaffings.get(0).length ; i++ ){
                String res = "";
                for(int j = 0; j < allStaffings.size() ; j++) {
                    res += "," + allStaffings.get(j)[i];
                }
                fileWriterStaffingIterations.append(i * binSize + res +"\n");

            }
        }catch (Exception e) {
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


    private boolean notConverged(int[] currIterationStaffing, int[] nextIterationStaffing, double convergenceTau) {

        for(int i = 0 ; i < currIterationStaffing.length ; i++ ){
            if( Math.abs( currIterationStaffing[i] - nextIterationStaffing[i]) > convergenceTau ){
                return true;
            }
        }
        return true;
    }

}
