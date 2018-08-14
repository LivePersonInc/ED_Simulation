package ed_simulation;


import java.util.HashMap;

import static ed_simulation.ServerAssignmentMode.FIXED_SERVER_CAPACITY;
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
        String inputFolderName = args[0];
        String outfile;
        if (args.length >= 2) {
            outfile = args[1];
        } else {
            outfile = inputFolderName + "/run_statistics.csv";
        }

        int numPeriodsRepetitionsTillSteadyState = 3;
        int numRepetitionsToStatistics = 1000;


        try {
            SimParams inputs = SimParams.fromInputFolder(inputFolderName);

            ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;

            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);

            int singlePeriodDurationInSecs = inputs.getPeriodDurationInSecs();
            //TODO: Later on enable choosing the bin size independently of how the data is extracted from hadoop. This requires interpolation etc.
            TimeDependentSimResults result = sim.simulate(numPeriodsRepetitionsTillSteadyState * inputs.getPeriodDurationInSecs(),
                    (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(), inputs);

            result.writeToFile(outfile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
