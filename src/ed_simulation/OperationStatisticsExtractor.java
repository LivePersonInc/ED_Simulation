package ed_simulation;


import java.io.FileInputStream;
import java.util.HashMap;

import static ed_simulation.ServerAssignmentMode.FIXED_SERVER_CAPACITY;
import static ed_simulation.ServerWaitingQueueMode.TWO_INFTY_QUEUES;

import java.io.File;
import java.util.Properties;

/**
 * Receives a period time-dependent input parameters  (e.g. a week's parameters, such as arrival rates, service rates etc.) and:
 * 1. Converges to a steady state realization extracts a "steady state" single period operation.
 * 2. Repeteats this procedure to generate the "average steady state" single period operation.
 * 3. Extracts relevant statistics from this "averate steady state realization", for example: queue size, wait time distribution etc.
 * Currently works only with the ED_Simulation_ReturnToServer
 *
 */
public class OperationStatisticsExtractor {



    private static class RunProperties{
        String inputFolderName;
        String outpuFolderName;

        public RunProperties(String inputFolderName, String outpuFolderName, int numPeriodsRepetitionsTillSteadyState,
                             int numRepetitionsToStatistics, AbandonmentModelingScheme abandonmentModelingScheme,
                             ServerAssignmentMode serverAssignmentMode, ServerWaitingQueueMode serverWaitingQueueMode) {
            this.inputFolderName = inputFolderName;
            this.outpuFolderName = outpuFolderName;
            this.numPeriodsRepetitionsTillSteadyState = numPeriodsRepetitionsTillSteadyState;
            this.numRepetitionsToStatistics = numRepetitionsToStatistics;
            this.abandonmentModelingScheme = abandonmentModelingScheme;
            this.serverAssignmentMode = serverAssignmentMode;
            this.serverWaitingQueueMode = serverWaitingQueueMode;
        }

        int numPeriodsRepetitionsTillSteadyState;
        int numRepetitionsToStatistics;
        AbandonmentModelingScheme abandonmentModelingScheme;
        ServerAssignmentMode serverAssignmentMode;
        ServerWaitingQueueMode serverWaitingQueueMode;


        static RunProperties fromProperties( Properties textualProperties){
            String inputFolderName = textualProperties.getProperty("inputFolderName");
            String skillId = textualProperties.getProperty("skillId");
            if( skillId.isEmpty() ){
                skillId = "skillsUnion";
            }
            String outputFolderName = textualProperties.getProperty("outputFolderName");
            if( outputFolderName.isEmpty() ){
                outputFolderName = inputFolderName + "/SimResults" + "/"  + skillId;
            }
            int numPeriodsRepetitionsTillSteadyState = Integer.parseInt(textualProperties.getProperty("numPeriodsRepetitionsTillSteadyState"));
            int numRepetitionsToStatistics = Integer.parseInt(textualProperties.getProperty("numRepetitionsToStatistics"));
            AbandonmentModelingScheme abandonmentModelingScheme = AbandonmentModelingScheme.valueOf(textualProperties.getProperty("abandonmentModelingScheme"));
            ServerAssignmentMode serverAssignmentMode = ServerAssignmentMode.valueOf(textualProperties.getProperty("serverAssignmentMode"));
            ServerWaitingQueueMode serverWaitingQueueMode = ServerWaitingQueueMode.valueOf(textualProperties.getProperty("serverWaitingQueueMode"));

            return new RunProperties(
                    inputFolderName, outputFolderName,
                    numPeriodsRepetitionsTillSteadyState,
                    numRepetitionsToStatistics, abandonmentModelingScheme,
                    serverAssignmentMode,serverWaitingQueueMode

                    );
        }
    }

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("I need the input folder as input parameter!! Aborting...");
            return;
        }

        //This is assumed to be a parent folder, containing the raw extracted data from hadoop and the raw data processed for this program
        //TODO: have it implemented with some argParser.
        String inputFolderName = args[0];
        Properties defaultProps = new Properties();

        try {


            FileInputStream in = new FileInputStream(inputFolderName + "/properties.txt");
            defaultProps.load(in);
            defaultProps.put( "inputFolderName", inputFolderName);
            in.close();
        }
        catch (Exception e){
            e.printStackTrace();
        }

        RunProperties runProperties = RunProperties.fromProperties(defaultProps);

        String skillId;
        if (args.length >= 2) {
            skillId = args[1];
        } else {
            skillId = "skillsUnion";
        }

        String outfolder;
        if (args.length >= 3) {
            outfolder = args[1];
        } else {
            outfolder = inputFolderName + "/SimResults" + "/"  + skillId;
        }


        File directory = new File(outfolder);
        if (! directory.exists()){
            directory.mkdirs();
        }
        //A folder for statistics identical to the diagnostics extracted from hadoop.
//        String compiledOutputFolder = outfolder + "/FormattedResults";
//        File compiledStatisticsDir = new File( compiledOutputFolder);
//        if (! compiledStatisticsDir.exists()){
//            compiledStatisticsDir.mkdir();
//        }

        int numPeriodsRepetitionsTillSteadyState = 1;
        int numRepetitionsToStatistics = 5;

        AbandonmentModelingScheme abandonmentModelingScheme = AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA; //AbandonmentModelingScheme.EXPONENTIAL_SILENT_MARKED; //
        boolean fastMode = false;

        String paramsFolderName = inputFolderName + "/FetchedDiagnostics-InputToJava/" + skillId;
        try {
            SimParams inputs = SimParams.fromInputFolder(paramsFolderName);

            ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
            ServerWaitingQueueMode serverWaitingQueueMode = TWO_INFTY_QUEUES; //Currently not used.


            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode );

//            int singlePeriodDurationInSecs = inputs.getPeriodDurationInSecs();
            //TODO: Later on enable choosing the bin size independently of how the data is extracted from hadoop. This requires interpolation etc.
            TimeDependentSimResults result = sim.simulate(inputs.getPeriodDurationInSecs(), numPeriodsRepetitionsTillSteadyState ,
                    (numPeriodsRepetitionsTillSteadyState + numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(),
                    inputs, abandonmentModelingScheme, fastMode, 0);

            result.writeToFile(outfolder  /*, compiledOutputFolder*/);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
