package ed_simulation;


import java.util.HashMap;

import static ed_simulation.ServerAssignmentMode.FIXED_SERVER_CAPACITY;
import static ed_simulation.ServerWaitingQueueMode.TWO_INFTY_QUEUES;

import java.io.File;

/**
 * Receives a period time-dependent input parameters  (e.g. a week's parameters, such as arrival rates, service rates etc.) and:
 * 1. Converges to a steady state realization extracts a "steady state" single period operation.
 * 2. Repeteats this procedure to generate the "average steady state" single period operation.
 * 3. Extracts relevant statistics from this "averate steady state realization", for example: queue size, wait time distribution etc.
 * Currently works only with the ED_Simulation_ReturnToServer
 *
 */
public class OperationStatisticsExtractor {




    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("I need the input folder as input parameter!! Aborting...");
            return;
        }
        //This is assumed to be a parent folder, containing the raw extracted data from hadoop and the raw data processed for this program
        String inputFolderName = args[0];
        String outfolder;
        if (args.length >= 2) {
            outfolder = args[1];
        } else {
            outfolder = inputFolderName + "/SimResults";
        }
        File directory = new File(outfolder);
        if (! directory.exists()){
            directory.mkdir();
        }
        //A folder for statistics identical to the diagnostics extracted from hadoop.
//        String compiledOutputFolder = outfolder + "/FormattedResults";
//        File compiledStatisticsDir = new File( compiledOutputFolder);
//        if (! compiledStatisticsDir.exists()){
//            compiledStatisticsDir.mkdir();
//        }

        int numPeriodsRepetitionsTillSteadyState = 1;
        int numRepetitionsToStatistics = 5;


        String paramsFolderName = inputFolderName + "/FetchedDiagnostics-InputToJava";
        try {
            SimParams inputs = SimParams.fromInputFolder(paramsFolderName);

            ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
            ServerWaitingQueueMode serverWaitingQueueMode = TWO_INFTY_QUEUES;
            AbandonmentModelingScheme abandonmentModelingScheme = AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA; //AbandonmentModelingScheme.EXPONENTIAL_SILENT_MARKED; //

            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode );

//            int singlePeriodDurationInSecs = inputs.getPeriodDurationInSecs();
            //TODO: Later on enable choosing the bin size independently of how the data is extracted from hadoop. This requires interpolation etc.
            TimeDependentSimResults result = sim.simulate(inputs.getPeriodDurationInSecs(), numPeriodsRepetitionsTillSteadyState ,
                    (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(), inputs, abandonmentModelingScheme);

            result.writeToFile(outfolder/*, compiledOutputFolder*/);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
