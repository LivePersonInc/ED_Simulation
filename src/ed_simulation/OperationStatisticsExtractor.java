package ed_simulation;


import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

import static ed_simulation.ServerAssignmentMode.FIXED_SERVER_CAPACITY;
import static ed_simulation.ServerWaitingQueueMode.TWO_INFTY_QUEUES;

import java.util.List;
import java.util.concurrent.*;
import java.util.Properties;

 class RunProperties implements  Cloneable {

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
    boolean parallelRun;
    int numIterationsPerExecutor;

    public RunProperties(String inputFolderName, String outpuFolderName, int numPeriodsRepetitionsTillSteadyState,
                         int numRepetitionsToStatistics, int numRepetitionsToTruncate, AbandonmentModelingScheme abandonmentModelingScheme,
                         ServerAssignmentMode serverAssignmentMode, ServerWaitingQueueMode serverWaitingQueueMode,
                         boolean fastMode, boolean parallelRun, int numIterationsPerExecutor) {
        this.inputFolderName = inputFolderName;
        this.outpuFolderName = outpuFolderName;
        this.numPeriodsRepetitionsTillSteadyState = numPeriodsRepetitionsTillSteadyState;
        this.numRepetitionsToStatistics = numRepetitionsToStatistics;
        this.numRepetitionsToTruncate = numRepetitionsToTruncate;
        this.abandonmentModelingScheme = abandonmentModelingScheme;
        this.serverAssignmentMode = serverAssignmentMode;
        this.serverWaitingQueueMode = serverWaitingQueueMode;
        this.fastMode = fastMode;
        this.parallelRun = parallelRun;
        this.numIterationsPerExecutor = numIterationsPerExecutor;
    }

    public RunProperties(RunProperties otherRunProperties) {
        inputFolderName = otherRunProperties.inputFolderName;
        outpuFolderName = otherRunProperties.outpuFolderName;
        numPeriodsRepetitionsTillSteadyState = otherRunProperties.numPeriodsRepetitionsTillSteadyState;
        numRepetitionsToStatistics = otherRunProperties.numRepetitionsToStatistics;
        numRepetitionsToTruncate = otherRunProperties.numRepetitionsToTruncate;
        abandonmentModelingScheme = otherRunProperties.abandonmentModelingScheme;
        serverAssignmentMode = otherRunProperties.serverAssignmentMode;
        serverWaitingQueueMode = otherRunProperties.serverWaitingQueueMode;
        fastMode = otherRunProperties.fastMode;
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
        boolean parallelRun = Boolean.parseBoolean(textualProperties.getProperty("parallelRun"));
        int  numIterationsPerExecutor = -1;
        String numIters = textualProperties.getProperty("numIterationsPerExecutor");
        if(numIters != null)
            numIterationsPerExecutor = Integer.parseInt(numIters);
        else{
            if(parallelRun){
                throw new Exception("This is defined to be a parallel run, but numIterationsPerExecutor wasn't defined! I can't continue...");
            }
        }


        return new RunProperties(
                inputFolderName, outputFolderName,
                numPeriodsRepetitionsTillSteadyState,
                numRepetitionsToStatistics, numRepetitionsToTruncate, abandonmentModelingScheme,
                serverAssignmentMode,serverWaitingQueueMode, fastMode, parallelRun, numIterationsPerExecutor
        );
    }
}

/**
 * Receives a period time-dependent input parameters  (e.g. a week's parameters, such as arrival rates, service rates etc.) and:
 * 1. Converges to a steady state realization extracts a "steady state" single period operation.
 * 2. Repeteats this procedure to generate the "average steady state" single period operation.
 * 3. Extracts relevant statistics from this "averate steady state realization", for example: queue size, wait time distribution etc.
 * Currently works only with the ED_Simulation_ReturnToServer
 *
 */
public class OperationStatisticsExtractor {

    //TODO - make them command-line arguments
//    private static int numIterationsPerExecutor = 25;
//    private static boolean ParallelRun = true;



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
            TimeDependentSimResults result = null;

            long startTimeMillis = System.currentTimeMillis();
            if( runProperties.parallelRun){
                result = runSimParallel(inputs, serverAssignmemtMode, serverWaitingQueueMode, runProperties.numIterationsPerExecutor, runProperties);
            }
            else{
                ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                        new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode );

//            int singlePeriodDurationInSecs = inputs.getPeriodDurationInSecs();
                //TODO: Later on enable choosing the bin size independently of how the data is extracted from hadoop. This requires interpolation etc.
                result = sim.simulate(inputs.getPeriodDurationInSecs(), runProperties.numPeriodsRepetitionsTillSteadyState ,
                        (runProperties.numPeriodsRepetitionsTillSteadyState + runProperties.numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(),
                        inputs, runProperties.abandonmentModelingScheme, runProperties.fastMode, 0);

            }

            long endTimeMillis = System.currentTimeMillis();

            System.out.println("Execution time in seconds: " + (endTimeMillis - startTimeMillis)/1000);

            result.writeToFile(runProperties.outpuFolderName, runProperties.numRepetitionsToTruncate, inputs.timestamps  );

            writeAdditionalOutputs(args, inputFolderName + "/properties.txt", defaultProps);





        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static TimeDependentSimResults runSimParallel(SimParams inputs, ServerAssignmentMode serverAssignmemtMode,
                                                          ServerWaitingQueueMode serverWaitingQueueMode, int numIterationsPerExecutor,
                                                          RunProperties runProperties) {
        //Runs a given (small) number of iterations and computes the statistics for these iterations.
        class SimTask implements Callable<TimeDependentSimResults>
        {

            private TimeDependentSimResults result;
            private RunProperties myRunProperties;

            public SimTask(RunProperties _runProperties)
            {
                this.myRunProperties = _runProperties;
                result = null;
            }


            public TimeDependentSimResults call() throws Exception {


                    System.out.println("Started task!!");
                    System.out.println("runProperties.numRepetitionsToStatistics: " + myRunProperties.numRepetitionsToStatistics);
                    ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(inputs,
                            new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode, serverWaitingQueueMode);

                    //TODO: Later on enable choosing the bin size independently of how the data is extracted from hadoop. This requires interpolation etc.
                    result = sim.simulate(inputs.getPeriodDurationInSecs(), myRunProperties.numPeriodsRepetitionsTillSteadyState,
                            (myRunProperties.numPeriodsRepetitionsTillSteadyState + myRunProperties.numRepetitionsToStatistics) * inputs.getPeriodDurationInSecs(),
                            inputs, myRunProperties.abandonmentModelingScheme, myRunProperties.fastMode, 0);
                    return result;

            }
        }


        int numPeriodsRepetitionsTillSteadyState; //Warm-up repetitions of the basic period (e.g. a week) which are ignored when calculating the statistics.
        int numRepetitionsToStatistics; //The number of repetitions after the warm up. The actual number of repetitions that will be considered is this number - numRepetitionsToTruncate
        //The last repetition may contain some anomalies hence is ignored. This is since, for example, the wait time of a conversation is registered upon its entry to service, which, in the case of the last repetitions, take place after the simulation period.
        int numRepetitionsToTruncate;

        //Create executors.
        int totalNumRelevantIterations = runProperties.numRepetitionsToStatistics;
        int numExecutors = totalNumRelevantIterations/numIterationsPerExecutor;
        int curry = totalNumRelevantIterations - numExecutors*numIterationsPerExecutor;
        numExecutors += curry > 0 ? 1 : 0;
        RunProperties runPropertiesPerExecutor =  new RunProperties(runProperties);
        runPropertiesPerExecutor.numRepetitionsToStatistics = numIterationsPerExecutor;
        ExecutorService taskExecutor = Executors.newFixedThreadPool(numExecutors);
        TimeDependentSimResults res = null;

        List<SimTask> lst = new ArrayList<SimTask>();
        for(int i = 0 ; i < numExecutors; i++) {
            lst.add(new SimTask(runPropertiesPerExecutor));
        }
        List<Future<TimeDependentSimResults>> tasks = null;
        try{
            // returns a list of Futures holding their status and results when all complete
            tasks = taskExecutor.invokeAll(lst);
            if( tasks == null || tasks.isEmpty()){
                return null;
            }
//            TimeDependentSimResults mergedResult = new TimeDependentSimResults()
            for(Future<TimeDependentSimResults> task : tasks)
            {
                if( res == null){
                    res = task.get();
                }
                else{
                    res.append(task.get());
                }
            }

        }
        catch (InterruptedException e) {
            System.out.println(e.getStackTrace());
        }
        catch (Exception ee){
            ee.printStackTrace();
        }

        taskExecutor.shutdown();

        return res;





//        for(int i = 0 ; i < numExecutors; i++) {
//            taskExecutor.execute(new SimTask(runPropertiesPerExecutor));
//        }
//        taskExecutor.shutdown();
//        try {
//            taskExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
//        } catch (InterruptedException e) {
//            System.out.println(e.getStackTrace());
//        }
//        taskExecutor.shutdown();



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
