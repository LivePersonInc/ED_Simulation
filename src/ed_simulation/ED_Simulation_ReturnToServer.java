package ed_simulation;

import statistics.ExponentialDistribution;
import statistics.TimeInhomogeneousPoissionProcess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;


/**
 *
 * @author bmathijs
 */
public class ED_Simulation_ReturnToServer  {


    public static boolean DEBUG = false;
    protected ExponentialDistribution arrivalDist;
    protected ExponentialDistribution serviceDist;
    protected ExponentialDistribution contentDist;
    protected ExponentialDistribution patienceDist;
//    protected int s;
    protected ServersManager serversManager;
    protected int perAgentMaxCapacity;
    protected double p;

    //Time dependent parameters. If it works well - remove the time independent params above.
    protected TimeInhomogeneousPoissionProcess arrivalProcess;
    protected TimeInhomogeneousPoissionProcess serviceProcess;
    protected TimeInhomogeneousPoissionProcess contentProcess;
    protected TimeInhomogeneousPoissionProcess patienceProcess;
    protected double[] convEndProbs;
    protected double[] patienceTheta;

    //The exponential impatience rate.

    Random rng = new Random();

    //Bloody Java. I can't believe I need to write this by myself.
    public int myMax(int[] arr) throws Exception {
        if(  arr == null )
        {
            throw  new Exception("Got a null array to myMax");
        }
        if( arr.length == 0 )
        {
            return 0;
        }
        int currMax = -Integer.MAX_VALUE;
        for( int i = 0 ; i < arr.length ; i++ )
        {
            currMax = Math.max(currMax, arr[i]);
        }
        return currMax;
    }


    public ED_Simulation_ReturnToServer( SimParams simParams,
                                        HashMap<Integer,Double> loadsToAssignmentMap, double loadsToAssignmentGran, ServerAssignmentMode serverAssignmentMode) throws Exception {

        this.arrivalProcess = new TimeInhomogeneousPoissionProcess( simParams.getTimeBins(), simParams.arrivalRates);
        this.serviceProcess = new TimeInhomogeneousPoissionProcess(simParams.getTimeBins(), simParams.singleConsumerNeedServiceRate);
        this.contentProcess = new TimeInhomogeneousPoissionProcess( simParams.getTimeBins(), simParams.contentDepartureRates);
        this.patienceProcess = new TimeInhomogeneousPoissionProcess( simParams.getTimeBins(), simParams.patienceTheta);



        int maxNumServers = myMax(simParams.numAgents);
        if( maxNumServers < 1 )
        {
            throw new Exception("There need to be at least 1 server in the system. Got " + maxNumServers + " servers instead. Aborting...");
        }
        int maxAgentCapacity = myMax(simParams.singleAgentCapacity);
        this.serversManager = new ServersManager(maxNumServers, maxAgentCapacity, loadsToAssignmentMap, loadsToAssignmentGran, serverAssignmentMode);
//        if( perAgentMaxCapacity < 1 )
//        {
//            throw new Exception("Each server in the system should have at least 1 conversation capacity. Got " + perAgentMaxCapacity + " capacity instead. Aborting...");
//        }
        this.perAgentMaxCapacity = perAgentMaxCapacity; //Per agent max capacity (num conversations)
        this.convEndProbs = simParams.convEndProbs;
        this.patienceTheta = simParams.patienceTheta;
    }



    /**
     *
     * @param lambda - arrival rate
     * @param mu - per message service rate.
     * @param delta - rate of leaving the content phase.
     * @param s - number of active servers.
     * @param perAgentMaxCapacity - Per agent max capacity (in FIXED_SERVER_CAPACITY assignment mode).
     * @param p
     * @param loadsToAssignmentMap
     * @param loadsToAssignmentGran
     * @param serverAssignmentMode
     * @throws Exception
     */
    public ED_Simulation_ReturnToServer(double lambda, double mu, double delta, int s, int perAgentMaxCapacity, double p, double patienceTheta,
                                        HashMap<Integer,Double> loadsToAssignmentMap, double loadsToAssignmentGran, ServerAssignmentMode serverAssignmentMode) throws Exception {
        this.arrivalDist = new ExponentialDistribution(lambda, rng);
        this.serviceDist = new ExponentialDistribution(mu, rng);
        this.contentDist = new ExponentialDistribution(delta, rng);
        this.patienceDist = new ExponentialDistribution( patienceTheta, rng);
        if( s < 1 )
        {
            throw new Exception("There need to be at least 1 server in the system. Got " + s + " servers instead. Aborting...");
        }
        this.serversManager = new ServersManager(s, perAgentMaxCapacity, loadsToAssignmentMap, loadsToAssignmentGran, serverAssignmentMode);
        if( perAgentMaxCapacity < 1 )
        {
            throw new Exception("Each server in the system should have at least 1 conversation capacity. Got " + perAgentMaxCapacity + " capacity instead. Aborting...");
        }
        this.perAgentMaxCapacity = perAgentMaxCapacity; //Per agent max capacity (num conversations)
        this.p = p;
    }

    public TimeDependentSimResults simulate(double ignoreUpToTime, double timeToRunSim, SimParams simParams) throws Exception {
//        SimResults results = new SimResults(perAgentMaxCapacity * serversManager.getNumServers());
        int[] numBinsAndBinSize = simParams.getNumBinsAndSize();
        TimeDependentSimResults results = new TimeDependentSimResults( numBinsAndBinSize[1],  numBinsAndBinSize[0], myMax(simParams.numAgents)*myMax(simParams.singleAgentCapacity), timeToRunSim);
        FES fes = new FES();
        LinkedList<Patient> holdingQueue = new LinkedList<Patient>();
        StringBuilder logString;
        PrintWriter pw = null;
        if( DEBUG ) {

            try {
                pw = new PrintWriter(new File("ED_ReturnToServerLog.csv"));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
            logString = new StringBuilder();
            String ColumnNamesList = "time,event type,Patient hash,Patient num visits";

            logString.append(ColumnNamesList + "\n");
        }else
        {
            logString = null;
        }



        double t = 0;
        //MATAN: to test times order. Can remove.
        double prevT = 0;
        // Schedule first event
        fes.addEvent(new Event(Event.ARRIVAL, arrivalProcess.timeToNextEvent(t)));
        int totalNumArrivals = 0;
        int totalNumAddToHoldingQueue = 0;
        int periodDurationInSecs = simParams.getPeriodDurationInSecs();
        int j = -1;
        while (t < timeToRunSim) {
            if((int)t/periodDurationInSecs != j)
            {
                j += 1;
                System.out.println("Now running period number: " + j);
            }

            Event e = fes.nextEvent();
            t = e.getTime();
            if( t >= timeToRunSim )
            {
                break;
            }
            if( DEBUG )
            {
                assert( t >= prevT );
                Patient p = e.getPatient();


                logString.append( t + "," + e.getType() + "," + (p != null ? p.hashCode() : "null") + "," + (p != null ? p.getNrVisits() : "null" ) + "\n");


            }



            if( t > ignoreUpToTime) {
                results.registerQueueLengths(holdingQueue.size(), serversManager.getServiceQueueSize(), serversManager.getContentQueueSize(), t); //TODO: do we want to register the per-agent queues sizes?
            }
            if (e.getType() == Event.ARRIVAL) {
//                System.out.println("Now processing an ARRIVAL event...");
                totalNumArrivals += 1;
                //Assign this arrival to a vacant server, if there is such one, otherwise move to the holding queue.
                //Question: why don't I first check if the holding queue is vacant? This way all arriving conversations bypass the ones in the holding queue. Did I have a good reason to do this?
                //Possible answer: Basically whenever a job ends, the holding queue is checked and a new job is assigned to the agent, so basically if, at the time of this new arrival,
                //there are jobs in the holding queue, it means that all agents are in max capacity, so no need to check the queue. But this seems quite skewed. Maybe it's a legacy from ED_Simulation, but this should
                // Be changed, specifically since dynamic capacity may change behavior and create possible bugs.
                Patient newPatient = new Patient(t);
                newPatient.setPatience(this.patienceProcess.timeToNextEvent(t));
                int assignedServerInd = serversManager.assignPatientToAgent( newPatient );
                if( assignedServerInd != ServersManager.ASSIGNMENT_FAILED )
                {
                    if( t > ignoreUpToTime) {
                        results.registerHoldingTime(newPatient, t);
                    }
                    fes.addEvent(new Event(Event.CONTENT, t, newPatient, assignedServerInd)); //Comment: in our system this is inaccurate, since an arriving conversation is Agent pending (and not content). In Chat it is a reasonable approximation.
                }
                else
                {
                    holdingQueue.add(newPatient);
                    totalNumAddToHoldingQueue += 1;

                }
                //Generate the next arrival
                fes.addEvent(new Event(Event.ARRIVAL, t + arrivalProcess.timeToNextEvent(t)));

            } else if (e.getType() == Event.SERVICE) { //Represents the service completion time of a Patient.

//                System.out.println("Now processing a SERVICE event...");
                Patient serviceCompletedPatient = e.getPatient();
                int serverInd = e.getAssignedServerInd();
                Patient nextPatientToService = serversManager.serviceCompleted(serverInd);
                if( nextPatientToService != null )
                {
                    if( t > ignoreUpToTime) {
                        results.registerWaitingTime(nextPatientToService, t);
                    }
                    nextPatientToService.addWaitingTime(t - nextPatientToService.getLastArrivalTime());
                    fes.addEvent(new Event(Event.SERVICE, t + serviceProcess.timeToNextEvent(t), nextPatientToService, serverInd));

                }

                double U = rng.nextDouble();
                if (U < p) {
                    fes.addEvent(new Event(Event.CONTENT, t + contentProcess.timeToNextEvent(t), serviceCompletedPatient, serverInd));
                    serversManager.contentPhaseStart(serverInd, serviceCompletedPatient);

                } else {
                    if( t > ignoreUpToTime) {
                        results.registerDeparture(serviceCompletedPatient, t);
                    }
                // check holding queue !!! TODO: in the dynamic concurrency mode - I think we need to attempt assignment not only upon departures. That is, it's possible for an agent to become available/unavailable not only upon departures.
                    if (holdingQueue.size() > 0) {

                        Patient patToAssign = getNextPatientFromHoldingQueue( holdingQueue, t);
                        int assignedServerInd = serversManager.assignPatientToAgent( patToAssign );
                        if( assignedServerInd != ServersManager.ASSIGNMENT_FAILED )
                        {
                            if( t > ignoreUpToTime) {
                                results.registerHoldingTime(patToAssign, t);
                            }
                            fes.addEvent(new Event(Event.CONTENT, t, patToAssign, assignedServerInd));
                            holdingQueue.remove();
                        }

                    }
                }

            } else if (e.getType() == Event.CONTENT) { //Content phase end.
//                System.out.println("Now processing a CONTENT event...");


                Patient contentPhaseCompletedPatient = e.getPatient();
                int serverInd = e.getAssignedServerInd();
                contentPhaseCompletedPatient.setLastArrivalTime(t);
                boolean didPatientGetIntoService = serversManager.contentPhaseEnd(serverInd, contentPhaseCompletedPatient);

                if (didPatientGetIntoService) { //Duplicate code!!.
                    if( t > ignoreUpToTime) {
                        results.registerWaitingTime(contentPhaseCompletedPatient, t);
                    }
                    contentPhaseCompletedPatient.addWaitingTime(t - contentPhaseCompletedPatient.getLastArrivalTime());
                    fes.addEvent(new Event(Event.SERVICE, t + serviceProcess.timeToNextEvent(t), contentPhaseCompletedPatient, serverInd));
                }

            }
            prevT = t;
        }
        if( DEBUG )
        {
            pw.write(logString.toString());
            pw.flush();
            pw.close();
        }
        System.out.println("There were total of " + totalNumArrivals + " arrivals to the system, out of which " + totalNumAddToHoldingQueue + " conversations were added to the holding queue.");
        return results;
    }

    private Patient getNextPatientFromHoldingQueue(LinkedList<Patient> holdingQueue, double currentTime) {
        do{
            Patient firstInLine = holdingQueue.peek();
            if( firstInLine == null ) //Empty queue
            {
                return null;
            }
           boolean hasAbandoned =  firstInLine.hasAbandoned( currentTime - firstInLine.getArrivalTime() );
           if( hasAbandoned )
           {
               holdingQueue.remove();
           }
           else
           {
               return firstInLine;
           }
        }
        while( true );
    }

    public double dec( double a, int i ){
        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
    }









//    @Override
//    public int getType(){
//        return 0;
//    }
    @SuppressWarnings("Duplicates")
    public static void main(String[] args) {

/*

        double lambda = 2;
        double mu = 2.5;
        double delta = 0.75;
        double p = 0; //No content phase - all serviced Patient leave after the first service period.
        int s = 1; //Single server.
        int n = 500000; //Unlimited server capacity.
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        ED_Simulation_ReturnToServer sim = null;
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {
            sim = new ED_Simulation_ReturnToServer(lambda,mu,delta,s,n,p, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
            SimResults results = sim.simulate(1000);
            double rho = lambda/mu;

            double theoreticalMeanNumberInsystem = rho/(1-rho);
            double empiricalMeanNumberInsystem = results.getMeanServiceQueueLength();
            assertEquals( 1, theoreticalMeanNumberInsystem/empiricalMeanNumberInsystem,   0.01);

            double empiricalMeanHolding = results.getMeanHoldingQueueLength(-1);
            assertEquals( 0, empiricalMeanHolding,   0.01);


            double theoreticalMeanWaitingTime = rho/((1-rho)*mu);
            double empiricalMeanWaitingTime = results.getMeanWaitingTime();
            assertEquals( 1, theoreticalMeanWaitingTime/empiricalMeanWaitingTime,   0.01);


            double theoreticalVarianceWaitingTime = (2-rho)*rho/((1-rho)*(1-rho)*mu*mu);
            double empiricalVarianceWaitingTime = results.getVarianceWaitingTime();
            assertEquals( 1, theoreticalVarianceWaitingTime/empiricalVarianceWaitingTime,   0.01);


            int x = 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

*/

//        double lambda = 179;
//        double mu = 81.52;
//        double delta = 61.0518;
//        double p = 0.93289;
//        double patienceTheta = 0;
//        int s = 4;
//        int perAgentMaxCapacity = 3; //per agent max capacity.
//        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
//        ED_Simulation_ReturnToServer sim = null;
//        //TODO: verify input is suitable to modes.
//        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
//        try {
//            sim = new ED_Simulation_ReturnToServer(lambda,mu,delta,s,perAgentMaxCapacity, p, patienceTheta, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        for( int i = 0; i < 1; i++ ){
//        SimResults results = sim.simulate(200000);
////        System.out.println(results.getCIdelayProbability()[0] + "\t"+results.getCIdelayProbability()[1] + "||\t"+  results.getCIWaitingTime()[0]+"\t"+  results.getCIWaitingTime()[1] +
////                "||\t" + results.getCISojournTime()[0]+"\t"+  results.getCISojournTime()[1]
////            );
//            System.out.println("\t"+ results.getMeanServiceQueueLength() + "\t"+results.getMeanHoldingTime() + "\t" + results.getMeanWaitingTime() + "\t"+ results.getHoldingProbability()
//                    + "\t"+ results.getWaitingProbability() + "\t"+ results.getMeanTotalInSystem() +  "\t"+ results.getMeanAllInSystem() );
//        }
//
    }


}
