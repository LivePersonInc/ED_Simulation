package ed_simulation;

import statistics.ExponentialDistribution;
import statistics.TimeInhomogeneousPoissionProcess;
import java.util.Iterator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import static ed_simulation.StaffingOptimizer.timeBinToPrint;


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
    //The probability that a conversation that exceeded its patience has actually left the system prior to assignment
    //TODO: we may need to have it wait-time dependent if results are unsatisfactory.
    private double knownAbanOutOfAllAbanRatio;
    private int binSize;
    private int numBins;
    private ServerWaitingQueueMode serverWaitingQueueMode;


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

    //Have I mentioned some opinion about java?
    public double myMax(double[] arr) throws Exception {
        if(  arr == null )
        {
            throw  new Exception("Got a null array to myMax");
        }
        if( arr.length == 0 )
        {
            return 0;
        }
        double currMax = -Double.MAX_VALUE;
        for( int i = 0 ; i < arr.length ; i++ )
        {
            currMax = Math.max(currMax, arr[i]);
        }
        return currMax;
    }


    public ED_Simulation_ReturnToServer( SimParams simParams,
                                        HashMap<Integer,Double> loadsToAssignmentMap, double loadsToAssignmentGran,
                                         ServerAssignmentMode serverAssignmentMode, ServerWaitingQueueMode serverWaitingQueueMode) throws Exception {

        this.arrivalProcess = new TimeInhomogeneousPoissionProcess( simParams.getTimeBins(), simParams.arrivalRates);
        this.serviceProcess = new TimeInhomogeneousPoissionProcess(simParams.getTimeBins(), simParams.singleConsumerNeedServiceRate);
        this.contentProcess = new TimeInhomogeneousPoissionProcess( simParams.getTimeBins(), simParams.contentDepartureRates);
        this.patienceProcess = new TimeInhomogeneousPoissionProcess( simParams.getTimeBins(), simParams.patienceTheta);
        this.knownAbanOutOfAllAbanRatio = simParams.knownAbanOutOfAllAbanRatio;
        this.serverWaitingQueueMode = serverWaitingQueueMode;


        int maxNumServers = myMax(simParams.numAgents);
        if( maxNumServers < 1 )
        {
            throw new Exception("There need to be at least 1 server in the system. Got " + maxNumServers + " servers instead. Aborting...");
        }
        int maxAgentCapacity = myMax(simParams.singleAgentCapacity);
        int[] tmp = simParams.getNumBinsAndSize();
        this.serversManager = new ServersManager(maxNumServers,  maxAgentCapacity, simParams.numAgents, tmp[0], tmp[1], simParams.singleAgentCapacity,
                    loadsToAssignmentMap, loadsToAssignmentGran, serverAssignmentMode );
//        if( perAgentMaxCapacity < 1 )
//        {
//            throw new Exception("Each server in the system should have at least 1 conversation capacity. Got " + perAgentMaxCapacity + " capacity instead. Aborting...");
//        }
        this.perAgentMaxCapacity = perAgentMaxCapacity; //Per agent max capacity (num conversations)
        this.convEndProbs = simParams.convEndProbs;
        this.patienceTheta = simParams.patienceTheta;
        int[] numBinsAndBinSize = simParams.getNumBinsAndSize();
        this.binSize = numBinsAndBinSize[1];
        this.numBins = numBinsAndBinSize[0];

    }


    //Non-Dynamic (old) version
    /*
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
*/
    private int getPeriodDuration() { return this.binSize * this.numBins; }
    private int getCurrTimeBin(double currTime) {
        return (int) Math.floor((currTime % getPeriodDuration()) / binSize);
    }

    private boolean hasPatientAbandoned(Patient examinedPatient,  double currentTime,
                                        BinnedProbFunction silentAbanDeterminator, BinnedProbFunction knownAbanDeterminator,
                                        AbandonmentModelingScheme abandonmentModelingScheme)
    {
        boolean hasAbandoned;
        boolean isSingleExchange;

        if( abandonmentModelingScheme == AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA)
        {
            hasAbandoned = !knownAbanDeterminator.isTrue(currentTime - examinedPatient.getArrivalTime());
            if(!hasAbandoned)
            {
                isSingleExchange = silentAbanDeterminator.isTrue(currentTime - examinedPatient.getArrivalTime());
                examinedPatient.setIsSingleExchange( isSingleExchange );
                return false;
            }
            else
            {
                return true;
//                holdingQueue.remove();
//                if(shouldRegisterAban ) {
//                    results.registerAbandonment(examinedPatient, currentTime);
//                }
            }

        }
        else
        {
            hasAbandoned =  examinedPatient.hasAbandoned( currentTime  );
            isSingleExchange = false;
            // Here we don't distinguish between silent abandonment and single-exchange. So a conversation is either abandoned before entering service,
            // with some probability knownAbanOutOfAllAbanRatio, or enters service, and then we forget about the fact that its wait time exceeded its
            //patience, and allow it to enter service as usual, with the probability of a single exchange being determined based on the statistics of all conversations
            //that enetered service.
            //TODO: we may need to extract the single exchange probability separately (as opposed to having a single p for conversation leaving).
            //In some of the AbandonmentModelingSchemes we use a single abandonment model for both known and silent abandonment. It these schemes, once
            //the conversation was set to abandoned, we need to determine whether it's silent or known abandonment.
            if( hasAbandoned )
            {
                double u = rng.nextDouble();
                if( u < knownAbanOutOfAllAbanRatio) {
                    return true;
//                    holdingQueue.remove();
//                    if(shouldRegisterAban ) {
//                        results.registerAbandonment(examinedPatient, currentTime);
//                    }
                }
                else{
                    //We regard it as a non-abandoned  or single Exchange conversation.
//                    System.out.println("I shouldn't be here!!!!!!!");
                    if(abandonmentModelingScheme == AbandonmentModelingScheme.EXPONENTIAL_SILENT_MARKED)
                    {
                        isSingleExchange = true;
                    }
                    examinedPatient.setIsSingleExchange( isSingleExchange );
                    return false;
                }
            }
            else
            {
                examinedPatient.setIsSingleExchange( isSingleExchange );
                return false;
            }
        }
    }

    public TimeDependentSimResults simulate(double singlePeriodDuration, int numPeriodsToIgnore , double timeToRunSim, SimParams simParams,
                                            AbandonmentModelingScheme abandonmentModelingScheme, boolean fastMode, double referenceWaitTime) throws Exception {
        double ignoreUpToTime = singlePeriodDuration*numPeriodsToIgnore;

//        SimResults results = new SimResults(perAgentMaxCapacity * serversManager.getNumServers());
        int[] numBinsAndBinSize = simParams.getNumBinsAndSize();
        TimeDependentSimResults results = new TimeDependentSimResults( numBinsAndBinSize[1],  numBinsAndBinSize[0], numPeriodsToIgnore,
                Math.min(1000, myMax(simParams.numAgents))*myMax(simParams.singleAgentCapacity), timeToRunSim, referenceWaitTime);
        FES fes = new FES();
        LinkedList<Patient> holdingQueue = new LinkedList<Patient>();
        StringBuilder logString;
        BinnedProbFunction binnedIsSingleExchange = new BinnedProbFunction(simParams.singleExchangeHistTimeBinSize, simParams.singleExchangeHist);
        BinnedProbFunction binnedIsKnownAban = new BinnedProbFunction(simParams.knownAbanHazardTimeBinSize, simParams.knownAbanSurvivalFunction);
        BinnedProbFunction convEndDeterminator = new BinnedProbFunction( 1, simParams.convEndHazard);



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
        int totalNumArrivalsToEmptyQueue = 0;
        int totalNumAddToHoldingQueue = 0;
        int periodDurationInSecs = simParams.getPeriodDurationInSecs();

        int totalNumArrivalsToBin = 0;
        int totalNumArrilalsToBinToEmpty = 0;
        int j = -1;

        while (t < timeToRunSim) {
//            System.out.println("Holding queue size is: " + holdingQueue.size());
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
                serversManager.updateActiveServers(t);
                int reported_serviced_size = serversManager.getOnlineContentQueueSize() + serversManager.getOnlineServiceQueueSize();
//                if( !fastMode ){
//                    int actual_num_serviced = serversManager.getAllInService(true);
//                    if( actual_num_serviced != reported_serviced_size)
//                    {
//                        System.out.println("actual_num_serviced != reported_serviced_size !!!!!");
//                    }
//                }

                //TODO!!! Need to remove known abandoned from the holding queue before registering its size.
                int holdingQueueSize = fastMode ? holdingQueue.size() : getHoldingQueueSizeNoAban( holdingQueue, t, binnedIsKnownAban, abandonmentModelingScheme);
                results.registerQueueLengths( holdingQueueSize, serversManager.getServiceQueueSize(), serversManager.getContentQueueSize(),
                        serversManager.getOnlineServiceQueueSize(), serversManager.getOnlineContentQueueSize(),
                        t, serversManager.getActualCurrNumServers(), serversManager.getCurrAgentMaxLoad(t)); //TODO: do we want to register the per-agent queues sizes?
            }
            if (e.getType() == Event.ARRIVAL) {
                Patient newPatient = new Patient(t);
                newPatient.setPatience(this.patienceProcess.timeToNextEvent(t));

                //*********************************************************************
                if( DEBUG ) {
                    //!! For Debogging only!!!
//                    int totalNumArrivalsToBin = 0;
//                    int totalNumArrilalsToBinToEmpty = 0;
                    int currTimeBin = getCurrTimeBin(t);
                    if (currTimeBin == timeBinToPrint) {
                        totalNumArrivalsToBin += 1;
                        if (holdingQueue.size() == 0) {
                            totalNumArrilalsToBinToEmpty += 1;
                        }
                    }
                }

                //*********************************************************************

                holdingQueue.add( newPatient );





                //Generate the next arrival
                fes.addEvent(new Event(Event.ARRIVAL, t + arrivalProcess.timeToNextEvent(t)));
                results.registerArrival(t);
                //Assign the next one from the holding queue (if the queue was empty when this arrival took place, it is taken out from the queue now)
                assignFromHoldingQueue( holdingQueue, results, t, t > ignoreUpToTime, binnedIsSingleExchange, binnedIsKnownAban, abandonmentModelingScheme,  serversManager, fes);

            } else if (e.getType() == Event.SERVICE) { //Represents the service completion time of a Patient.


//                System.out.println("Now processing a SERVICE event...");
                Patient serviceCompletedPatient = e.getPatient();
                int serverInd = e.getAssignedServerInd();
                if( t > ignoreUpToTime) {
                    results.registerExchange(t - serviceCompletedPatient.getLastArrivalTime());
                }
                //nextPatientToService is the Patient waiting in the server's internal queue, and gets into service after the current one has finished his service.
                //TODO: Implement Silent abandonment in this case!!
                Patient nextPatientToService = serversManager.serviceCompleted(serverInd, t);
                handleNextCandidatePatient( nextPatientToService, t,  ignoreUpToTime,  serverInd, results,
                        binnedIsSingleExchange, binnedIsKnownAban, abandonmentModelingScheme, fes );
                boolean patientDeparts = serviceCompletedPatient.isSingleExchange();

                if( !patientDeparts )
                {
                    //In AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA we fully model single exchanges using the data extracted function.
                    //In other schemes we differentiate between silent abandoned and single-exchange resolved, thus allow also an non-silent abandoned to have a single exchange.
                    double U = rng.nextDouble();
                    if(abandonmentModelingScheme == AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA)
                    {
                        //In these modes we allow spontaneous departure only as of the second visit (the first ones are determined as abandoned)
                        if( serviceCompletedPatient.getNrVisits() > 1)
                        {
                            patientDeparts = convEndDeterminator.isTrue(serviceCompletedPatient.nrVisits);
                        }

                    }
                    else
                    {
                        patientDeparts = U < convEndProbs[getCurrTimeBin(t)];
                    }

                }

                if( !patientDeparts)  {
                    fes.addEvent(new Event(Event.CONTENT, t + contentProcess.timeToNextEvent(t), serviceCompletedPatient, serverInd));
                    serversManager.contentPhaseStart(serverInd, serviceCompletedPatient);
                    serviceCompletedPatient.setLastExchangeEndTime(t);

                } else { //Patient departs the system.
                    if( t > ignoreUpToTime) {
                        results.registerDeparture(serviceCompletedPatient, t);
                    }
//                    System.out.println("Patient departs!!");
                // check holding queue !!! TODO: in the dynamic concurrency mode - I think we need to attempt assignment not only upon departures. That is, it's possible for an agent to become available/unavailable not only upon departures.
//                    assignFromHoldingQueue( holdingQueue, results, t, t > ignoreUpToTime, binnedIsSingleExchange, binnedIsKnownAban, abandonmentModelingScheme,  serversManager, fes);

                }
                assignFromHoldingQueue( holdingQueue, results, t, t > ignoreUpToTime, binnedIsSingleExchange, binnedIsKnownAban, abandonmentModelingScheme,  serversManager, fes);


            } else if (e.getType() == Event.CONTENT) { //Content phase end.
//                System.out.println("Now processing a CONTENT event...");


                Patient contentPhaseCompletedPatient = e.getPatient();
                int serverInd = e.getAssignedServerInd();
                contentPhaseCompletedPatient.setLastArrivalTime(t);
                //First content phase is artificial (when the patient first enters service it's considered as ending its content phase).
                if( contentPhaseCompletedPatient.getNrVisits() >= 1 &&  t > ignoreUpToTime ) {

                    results.registerInterExchange(t - contentPhaseCompletedPatient.getLastExchangeEndTime());
                }
                boolean didPatientGetIntoService = serversManager.contentPhaseEnd(serverInd, contentPhaseCompletedPatient);

                if (didPatientGetIntoService) {
                    handleNextCandidatePatient( contentPhaseCompletedPatient, t,  ignoreUpToTime,  serverInd, results,
                            binnedIsSingleExchange, binnedIsKnownAban, abandonmentModelingScheme, fes );
                }
                assignFromHoldingQueue( holdingQueue, results, t, t > ignoreUpToTime, binnedIsSingleExchange, binnedIsKnownAban, abandonmentModelingScheme,  serversManager, fes);


            }
            prevT = t;
        }
//        if( DEBUG )
//        {
//            pw.write(logString.toString());
//            pw.flush();
//            pw.close();
//        }
//        System.out.println("There were total of " + totalNumArrivals + " arrivals to the system, out of which " + totalNumAddToHoldingQueue + " conversations were added to the holding queue.");
//        System.out.println("There were " + totalNumArrivalsToEmptyQueue + " Arrivals to empty queue");
        if( DEBUG ){
            System.out.println("Directly calculating - there was a probability of  " + (1 - 1.0*totalNumArrilalsToBinToEmpty/totalNumArrivalsToBin) + " to arrive at a non empty queue");
        }

        return results;
    }



    private void assignFromHoldingQueue(LinkedList<Patient> holdingQueue, TimeDependentSimResults results, double t, boolean registerStatistics, BinnedProbFunction binnedIsSingleExchange,
                                        BinnedProbFunction binnedIsKnownAban, AbandonmentModelingScheme abandonmentModelingScheme, ServersManager serversManager, FES fes) {
        //Important! Notice that the below invocation may empty the holding Queue, in case of abandonment.
        Patient patToAssign = getNextPatientFromHoldingQueue( holdingQueue, results, t, registerStatistics,
                binnedIsSingleExchange, binnedIsKnownAban, abandonmentModelingScheme);
        if( patToAssign != null )
        {
            int assignedServerInd = serversManager.assignPatientToAgent( patToAssign, t );
            if( assignedServerInd != ServersManager.ASSIGNMENT_FAILED )
            {
                if( registerStatistics) {
                    results.registerHoldingTime(patToAssign, t);
                }
                fes.addEvent(new Event(Event.CONTENT, t, patToAssign, assignedServerInd));
                holdingQueue.remove();
            }
        }

    }

    private int getHoldingQueueSizeNoAban(LinkedList<Patient> holdingQueue, double currTime, BinnedProbFunction knownAbanDeterminator, AbandonmentModelingScheme abandonmentModelingScheme) {
        int res = 0;
        for( Iterator<Patient> it = holdingQueue.iterator(); it.hasNext(); )
        {
            boolean hasAbandoned;
            Patient currPatient = it.next();
            if( abandonmentModelingScheme == AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA )
            {
                hasAbandoned = !knownAbanDeterminator.isTrue(currTime - currPatient.getArrivalTime());
            }
            else
            {
                hasAbandoned = currPatient.hasAbandoned(currTime);
            }
            if(!hasAbandoned )
            {
                res += 1;
            }
        }
        return res;
    }

    private void handleNextCandidatePatient(Patient nextPatientToService, double currentTime, double ignoreUpToTime, int serverInd, TimeDependentSimResults results,
                                            BinnedProbFunction silentAbanDeterminator, BinnedProbFunction knownAbanDeterminator,
                                            AbandonmentModelingScheme abandonmentModelingScheme, FES fes ) throws Exception {
        while( nextPatientToService != null )
        {
            Patient nextCandidate = null;
            //Currently we implement abandonment w.r.t. first agent response. This means we don't simulate abandonment taking place at successive exchanges.
            if( nextPatientToService.nrVisits == 0 )
            {
                boolean hasAbandoned = hasPatientAbandoned(nextPatientToService, currentTime, silentAbanDeterminator, knownAbanDeterminator, abandonmentModelingScheme);
                if( hasAbandoned )
                {
                    nextCandidate = serversManager.patientAbandoned(nextPatientToService, serverInd, currentTime);
                    if( currentTime > ignoreUpToTime ) {
                        results.registerAbandonment(nextPatientToService, currentTime);
                    }
                    nextPatientToService = nextCandidate;
                    continue;
                }
            }
            if( currentTime > ignoreUpToTime) {
                results.registerWaitingTime(nextPatientToService, currentTime);
            }
            nextPatientToService.addWaitingTime(currentTime - nextPatientToService.getLastArrivalTime());
            fes.addEvent(new Event(Event.SERVICE, currentTime + serviceProcess.timeToNextEvent(currentTime), nextPatientToService, serverInd));
            break;

        }

    }

    //Returns the next non-abandoned Patient or null if no such Patient exists. Removes abandoned Patient, but not the returned,
    //non-null one, in case it exists, since it is removed only if its assignment to an agent succeeds.
    //TODO: Abandonment should be checked at First Agent Response time, not assign time.
    private Patient getNextPatientFromHoldingQueue(LinkedList<Patient> holdingQueue, TimeDependentSimResults results, double currentTime, boolean shouldRegisterAban,
                                                   BinnedProbFunction silentAbanDeterminator, BinnedProbFunction knownAbanDeterminator,
                                                   AbandonmentModelingScheme abandonmentModelingScheme) {
//        System.out.println("Just enetered getNextPatientFromHoldingQueue");
        return holdingQueue.peek();
//        Patient firstInLine = holdingQueue.peek();
//        if( firstInLine == null ) //Empty queue
//        {
//            return null;
//        }
//        else
//        {
//            holdingQueue.remove();
//            return firstInLine;
//        }
        /*
        do{
            Patient firstInLine = holdingQueue.peek();
            if( firstInLine == null ) //Empty queue
            {
                return null;
            }

            boolean hasAbandoned;
            boolean isSingleExchange;

            if( abandonmentModelingScheme == AbandonmentModelingScheme.SINGLE_KNOWN_AND_CONV_END_FROM_DATA)
            {
                hasAbandoned = !knownAbanDeterminator.isTrue(currentTime - firstInLine.getArrivalTime());
                if(!hasAbandoned)
                {
                    isSingleExchange = silentAbanDeterminator.isTrue(currentTime - firstInLine.getArrivalTime());
                    firstInLine.setIsSingleExchange( isSingleExchange );
                    return firstInLine;
                }
                else
                {
                    holdingQueue.remove();
                    if(shouldRegisterAban ) {
                        results.registerAbandonment(firstInLine, currentTime);
                    }
                }

            }
            else
            {
                hasAbandoned =  firstInLine.hasAbandoned( currentTime  );
                isSingleExchange = false;
                // Here we don't distinguish between silent abandonment and single-exchange. So a conversation is either abandoned before entering service,
                // with some probability knownAbanOutOfAllAbanRatio, or enters service, and then we forget about the fact that its wait time exceeded its
                //patience, and allow it to enter service as usual, with the probability of a single exchange being determined based on the statistics of all conversations
                //that enetered service.
                //TODO: we may need to extract the single exchange probability separately (as opposed to having a single p for conversation leaving).
                //In some of the AbandonmentModelingSchemes we use a single abandonment model for both known and silent abandonment. It these schemes, once
                //the conversation was set to abandoned, we need to determine whether it's silent or known abandonment.
                if( hasAbandoned )
                {
                    double u = rng.nextDouble();
                    if( u < knownAbanOutOfAllAbanRatio) {
                        holdingQueue.remove();
                        if(shouldRegisterAban ) {
                            results.registerAbandonment(firstInLine, currentTime);
                        }
                    }
                    else{
                        //We regard it as a non-abandoned  or single Exchange conversation.
//                    System.out.println("I shouldn't be here!!!!!!!");
                        if(abandonmentModelingScheme == AbandonmentModelingScheme.EXPONENTIAL_SILENT_MARKED)
                        {
                            isSingleExchange = true;
                        }
                        firstInLine.setIsSingleExchange( isSingleExchange );
                        return firstInLine;
                    }
                }
                else
                {
                    firstInLine.setIsSingleExchange( isSingleExchange );
                    return firstInLine;
                }
            }


        }
        while( true );
        */
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
        int maxTotalCapacity = 500000; //Unlimited server capacity.
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        ED_Simulation_ReturnToServer sim = null;
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {
            sim = new ED_Simulation_ReturnToServer(lambda,mu,delta,s,maxTotalCapacity,p, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
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
//            System.out.println("\t"+ results.getMeanServiceQueueLength() + "\t"+results.getMeanHoldingTime() + "\t" + results.getMeanWaitingTime() + "\t"+ results.getHoldingProbabilities()
//                    + "\t"+ results.getWaitingProbability() + "\t"+ results.getMeanTotalInSystem() +  "\t"+ results.getMeanAllInSystem() );
//        }
//
    }


}
