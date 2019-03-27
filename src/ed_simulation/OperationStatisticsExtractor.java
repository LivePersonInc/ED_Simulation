package ed_simulation;


import java.io.*;
import java.util.HashMap;

import static ed_simulation.ServerAssignmentMode.FIXED_SERVER_CAPACITY;
import static ed_simulation.ServerWaitingQueueMode.TWO_INFTY_QUEUES;

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
        int numPeriodsRepetitionsTillSteadyState; //Warm-up repetitions of the basic period (e.g. a week) which are ignored when calculating the statistics.
        int numRepetitionsToStatistics; //The number of repetitions after the warm up. The actual number of repetitions that will be considered is this number - numRepetitionsToTruncate
        //The last repetition may contain some anomalies hence is ignored. This is since, for example, the wait time of a conversation is registered upon its entry to service, which, in the case of the last repetitions, take place after the simulation period.
        int numRepetitionsToTruncate;
        AbandonmentModelingScheme abandonmentModelingScheme;
        ServerAssignmentMode serverAssignmentMode;
        ServerWaitingQueueMode serverWaitingQueueMode;
        boolean fastMode;

        public RunProperties(String inputFolderName, String outpuFolderName, int numPeriodsRepetitionsTillSteadyState,
                             int numRepetitionsToStatistics, int numRepetitionsToTruncate,  AbandonmentModelingScheme abandonmentModelingScheme,
                             ServerAssignmentMode serverAssignmentMode, ServerWaitingQueueMode serverWaitingQueueMode,
                             boolean fastMode) {
            this.inputFolderName = inputFolderName;
            this.outpuFolderName = outpuFolderName;
            this.numPeriodsRepetitionsTillSteadyState = numPeriodsRepetitionsTillSteadyState;
            this.numRepetitionsToStatistics = numRepetitionsToStatistics;
            this.abandonmentModelingScheme = abandonmentModelingScheme;
            this.serverAssignmentMode = serverAssignmentMode;
            this.serverWaitingQueueMode = serverWaitingQueueMode;
            this.fastMode = fastMode;
        }

        static RunProperties fromProperties( Properties textualProperties) throws Exception {
            String inputFolderName = textualProperties.getProperty("inputFolderName");
            if( inputFolderName.isEmpty() ){
                throw new Exception("Must have an input folder name to run. This folder contains the model input files.");
            }
            String skillId = textualProperties.getProperty("skillId");
            if( skillId.isEmpty() ){
                skillId = "skillsUnion";
            }
            String outputFolderName = textualProperties.getProperty("outputFolderName");
            if( outputFolderName.isEmpty() ){
                throw new Exception("Must have an output folder name to run.");
//                outputFolderName = inputFolderName + "/SimResults" + "/"  + skillId;
            }
            int numPeriodsRepetitionsTillSteadyState = Integer.parseInt(textualProperties.getProperty("numPeriodsRepetitionsTillSteadyState"));
            int numRepetitionsToStatistics = Integer.parseInt(textualProperties.getProperty("numRepetitionsToStatistics"));
            int numRepetitionsToTruncate = Integer.parseInt(textualProperties.getProperty("numRepetitionsToTruncate"));
            AbandonmentModelingScheme abandonmentModelingScheme = AbandonmentModelingScheme.valueOf(textualProperties.getProperty("abandonmentModelingScheme"));
            //TODO: Later on, have the truncation period determined from the simulation parameters (typically: service times and wait times considerations).
            ServerAssignmentMode serverAssignmentMode = ServerAssignmentMode.valueOf(textualProperties.getProperty("serverAssignmentMode"));
            ServerWaitingQueueMode serverWaitingQueueMode = ServerWaitingQueueMode.valueOf(textualProperties.getProperty("serverWaitingQueueMode"));
            boolean fastMode = Boolean.parseBoolean(textualProperties.getProperty("fastMode"));
            return new RunProperties(
                    inputFolderName, outputFolderName,
                    numPeriodsRepetitionsTillSteadyState,
                    numRepetitionsToStatistics, numRepetitionsToTruncate, abandonmentModelingScheme,
                    serverAssignmentMode,serverWaitingQueueMode, fastMode
                    );
        }
    }

    public static void main(String[] args) throws Exception {
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
            defaultProps.put("inputFolderName", inputFolderName);
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        RunProperties runProperties = RunProperties.fromProperties(defaultProps);




        File directory = new File(runProperties.outpuFolderName);
        if (!directory.exists()) {
            directory.mkdirs();
        }



        try {
            SimParams inputs = SimParams.fromInputFolder(runProperties.inputFolderName);

            ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
            ServerWaitingQueueMode serverWaitingQueueMode = TWO_INFTY_QUEUES; //Currently not used.


            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                    new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode );

//            int singlePeriodDurationInSecs = inputs.getPeriodDurationInSecs();
            //TODO: Later on enable choosing the bin size independently of how the data is extracted from hadoop. This requires interpolation etc.
            TimeDependentSimResults result = sim.simulate(inputs.getPeriodDurationInSecs(), runProperties.numPeriodsRepetitionsTillSteadyState ,
                    (runProperties.numPeriodsRepetitionsTillSteadyState + runProperties.numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(),
                    inputs, runProperties.abandonmentModelingScheme, runProperties.fastMode, 0);

            result.writeToFile(runProperties.outpuFolderName, runProperties.numRepetitionsToTruncate, inputs.timestamps  );

            writeAdditionalOutputs(args, inputFolderName + "/properties.txt", defaultProps);





        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void writeAdditionalOutputs(String[] args, String inputPropertiesFilename, Properties runProperties) {
        //Now copy the properties file, and append the this command arguments and the git revision.
        String commandLineArguments = String.join(" ", args);
        String outputFolderName = runProperties.getProperty("outputFolderName");
        OutputStream outPropsWriter = null;// new FileOutputStream( new File(outputFolderName + "simulation_input.properties"));


        BufferedReader br = null;
        try {
            outPropsWriter = new FileOutputStream( new File(outputFolderName + "/simulation_input.properties"));
            Runtime rt = Runtime.getRuntime();

            //TODO: this is very patchy, since the currentDirectory may be arbitrary. It's best to have an env-setup infrastructure and use it, or at lease set the ED-simulation as an environment variable in bash_profile.
            Process proc = rt.exec("git rev-parse HEAD");
            InputStream inputSteam = proc.getInputStream();

            br = new BufferedReader(new InputStreamReader(inputSteam));


            String gitRevLine = br.readLine();
            runProperties.store(outPropsWriter, "Git repo revision: " + gitRevLine + "\n#Command arguments: " + commandLineArguments);


        } catch (IOException ioe) {
            System.out.println("Exception while reading input " + ioe);
        } finally {
            // close the streams using close method
            try {
                if (br != null) {
                    br.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }
        }
    }

}
