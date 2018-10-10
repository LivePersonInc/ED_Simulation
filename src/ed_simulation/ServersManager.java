package ed_simulation;




import java.util.*;
import java.util.stream.*;

class ServersManager {


    public int getAllInService(boolean onlyOnline) {
        int allInService = 0;
        for(Server s : this.activeServersByLoad)
        {
            allInService += s.getServiceQueueSize() + s.getContentQueueSize();
        }
        if( !onlyOnline )
        {
            for(Server s : this.inactiveServers)
            {
                allInService += s.getServiceQueueSize() + s.getContentQueueSize();
            }
        }
        return allInService;
    }



    class Server{

        private int id;
//        private int maxLoad; //Max number of conversations this server can handle.
        private LinkedList<Patient> serviceQueue;
        private HashSet<Patient> contentQueue;
        private Random rng = new Random();
        //Currently not supported.
        private HashMap<Integer,Double> loadsToAssignProbMap;
        private double loadsToAssignmentGran;
        boolean isActive;


        public Server( int id, int maxLoad, HashMap<Integer,Double> loadsToAssignProbMap, double loadsToAssignmentGran)
        {
            this.id = id;
            serviceQueue = new LinkedList<Patient>();
            contentQueue = new HashSet<Patient>();
//            this.maxLoad = maxLoad;
            this.loadsToAssignProbMap = loadsToAssignProbMap;
            this.loadsToAssignmentGran = loadsToAssignmentGran;
        }



        public float getLoad() { return serviceQueue.size() + contentQueue.size(); }
        public int getId() {return id;}


        /**
         * Assigns the given Patient to this server, if possible. Notice the Patient is added to the content queue, and
         * not to the service queue at this stage. This is so as to maintain compatibility with existing simulation Logic (ED_Simulation),
         * in which a newly assigned Patient is placed in the content queue (with an event dispatched indicating its content
         * end time equals to its arrival time.
         * @param pt
         * @param assignmentMode
         * @return true or false, based on the success or failure of the assignemt.
         */
        public boolean assignPatient(Patient pt, ServerAssignmentMode assignmentMode, int currentServerMaxCapacity) throws  IllegalArgumentException {

            switch (assignmentMode) {
                case FIXED_SERVER_CAPACITY:
//                    System.out.println(currentServerMaxCapacity);
                    if( getLoad() + 1 <= currentServerMaxCapacity )
                    {
                        contentQueue.add(pt);
//                        System.out.println("Just accepted a new Patient. Now my load is: " + getLoad());

                        return true;
                    }
                    else
                    {
//                        System.out.println("Just refused accepting a new Patient, since my load is: " + getLoad());
                        return false;
                    }

                case UNBOUNDED_SERVER_CAPACITY:
                    double rnd = rng.nextDouble();
                    double assignmentProbGivenLoad = getAssignmentProbGivenLoad( getLoad() );
                    if (rnd < assignmentProbGivenLoad)
                    {
                        contentQueue.add(pt);
                        return true;
                    }
                    else
                    {
                        return false;
                    }

                default:
                   throw new IllegalArgumentException("Received an unsupported assignment mode: " + assignmentMode + "!!");

            }


        } //End of assignPatient.

        //Returns the Patient getting into service following the one that finished service, provided there's such a Patient waiting
        //in the service queue.
        public Patient serviceCompleted()
        {
            serviceQueue.remove();
//            System.out.println( "serviceCompleted. My queue size is: " + serviceQueue.size());
            return serviceQueue.peek(); //It'll be null if the queue is empty.

        }

        public void contentPhaseStart(Patient contentStartedPatient)
        {
            contentQueue.add(contentStartedPatient);
        }


        //Returns true if the patient which has just finished its content phase immediately got into service.
        boolean contentPhaseEnd( Patient contentEndPatient)
        {
            contentQueue.remove( contentEndPatient );
            serviceQueue.add( contentEndPatient );
//            System.out.println( "contentPhaseEnd. My queue size is: " + serviceQueue.size());

            return serviceQueue.size() == 1;
        }

        private double getAssignmentProbGivenLoad(float load)
        {
            return  loadsToAssignProbMap.get( Math.round(load/this.loadsToAssignmentGran));
        }


        public int getContentQueueSize() {
            return contentQueue.size();
        }

        public  int getServiceQueueSize() {
            return serviceQueue.size();
        }

        //returns the next Patient waiting in the internal queue, if such Patient exists.
        public Patient patientAbandoned(Patient abandonedPatient) throws Exception {
            if( !serviceQueue.contains(abandonedPatient))
            {
                throw new Exception("I'm a server. Abandoned Patient not found in my ServiceQueue...");
            }
            else
            {
                serviceQueue.remove(abandonedPatient);
                return serviceQueue.peek();
            }
        }
    } //End of internal class Server



        class ServersComparator implements Comparator<Server> {
        public int compare(Server a, Server b) {
            if(a.equals(b))
            {
                return 0;
            }
            int compVal =  Float.compare(a.getLoad(), b.getLoad());
            if(compVal == 0 )
            {
                return Integer.compare( a.hashCode(), b.hashCode() );
            }
            return compVal;
        }

    }



    public static int ASSIGNMENT_FAILED = -1;
    private ServerAssignmentMode serverAssignmentMode;
    private Server[] servers;
    private TreeSet<Server> activeServersByLoad;
    //Servers (agents) that can still process existing jobs (conversations), but aren't assigned new one.s

    private Vector<Server> inactiveServers;

    //The aggregated number of conversations currently waiting for service over all the agents.
    private int serviceQueueSize;
    //The aggregated number of conversations currently at content phase over all the agents.
    private int contentQueueSize;
    //The aggregated number of conversations currently waiting for service over all the online agents.
    private int onlineServiceQueueSize;
    //The aggregated number of conversations currently at content phase over all the online agents.
    private int onlineContentQueueSize;


    private HashMap<Integer,Double> loadsToAssignmentMap;
    private double loadsToAssignmentGran;
    private int binSize;
    private int numBins;
    private int[] numServersPerTimeBinInAPeriod;
    private int[] singleAgentAverageMaxLoad;


    public ServersManager(int maxNumServers, int serverFixedMaxLoad, int[] numSeversPerTimeBin, int numBins, int binSize, int[] singleAgentAverageCapacity,
                          HashMap<Integer, Double> loadsToAssignmentMap, double loadsToAssignmentGran, ServerAssignmentMode serverAssignmentMode)
    {


        servers = new Server[maxNumServers];
        activeServersByLoad = new TreeSet<Server>(new ServersComparator());
        inactiveServers = new Vector<>(maxNumServers);
        boolean additionSucceeded;
        for( int i = 0 ; i < maxNumServers ; i++)
        {
            Server currServ = new Server(i, serverFixedMaxLoad, loadsToAssignmentMap, loadsToAssignmentGran);
            servers[i]  = currServ;
            additionSucceeded =  activeServersByLoad.add(currServ);
            if(additionSucceeded)
            {
                currServ.isActive = true;
            }
        }

        this.serverAssignmentMode = serverAssignmentMode;
        this.loadsToAssignmentGran = loadsToAssignmentGran;
        this.loadsToAssignmentMap = loadsToAssignmentMap;
        this.numBins = numBins;
        this.binSize = binSize;
        this.numServersPerTimeBinInAPeriod = numSeversPerTimeBin;
        this.singleAgentAverageMaxLoad = singleAgentAverageCapacity;
        serviceQueueSize = 0;
        contentQueueSize = 0;
        onlineServiceQueueSize = 0;
        onlineContentQueueSize = 0;

    }



    public int getServiceQueueSize() {return serviceQueueSize; }
    public int getContentQueueSize() {return contentQueueSize; }
    public int getOnlineServiceQueueSize() {return onlineServiceQueueSize; }
    public int getOnlineContentQueueSize() {return onlineContentQueueSize; }

    private int getPeriodDuration() { return numBins * binSize; }

    public int getCurrNumServers( double currTime )
    {
        return numServersPerTimeBinInAPeriod[(int)Math.floor(currTime % getPeriodDuration())/binSize];

    }

    public int getActualCurrNumServers(  )
    {
        return this.activeServersByLoad.size();

    }

    public int getCurrAgentMaxLoad(double currTime) {
        return this.singleAgentAverageMaxLoad[(int)Math.floor(currTime % getPeriodDuration())/binSize];
    }

    public void updateActiveServers(double currTime) {
        int numCurrActiveServers = getCurrNumServers( currTime );
        int numAgentsToConvertToInactive = this.activeServersByLoad.size() - numCurrActiveServers ;
        int prevActiveServers = activeServersByLoad.size();
        if( numAgentsToConvertToInactive == 0 )
        {
            return;
        }
        int numAgentsToConvert = Math.abs(numAgentsToConvertToInactive);
        boolean fromActiveToInactive = numAgentsToConvertToInactive > 0;
        Iterator<Server> from;
        int fromSize;
        int reduceOrIncrease;
        AbstractCollection<Server> to;
        if( fromActiveToInactive )
        {
            from = activeServersByLoad.iterator();
            fromSize = activeServersByLoad.size();
            to = inactiveServers;
            reduceOrIncrease = -1;
        }
        else
        {
            from = inactiveServers.iterator();
            fromSize = inactiveServers.size();
            to = activeServersByLoad;
            reduceOrIncrease = 1;
        }
        List<Integer> range = IntStream.rangeClosed(1, fromSize)
                .boxed().collect(Collectors.toList());
        Collections.shuffle(range);
        List<Integer> indicesToConvert = range.stream().limit(numAgentsToConvert).collect(Collectors.toList());
        Collections.sort(indicesToConvert);
        int i = 1;
        int j = 0;
        Server currConvertedServer;


        boolean additionSucceeded;
        if( numAgentsToConvert > indicesToConvert.size())
        {
            int x = 5;
        }
        for(Iterator<Server> it = from; it.hasNext() && j < numAgentsToConvert ; i++ )
        {
            currConvertedServer = it.next();
            if( i == indicesToConvert.get(j))
            {
                it.remove();
                additionSucceeded =  to.add(currConvertedServer);
                if(additionSucceeded)
                {
                    currConvertedServer.isActive = !fromActiveToInactive;
                    this.onlineContentQueueSize += reduceOrIncrease*currConvertedServer.getContentQueueSize();
                    this.onlineServiceQueueSize += reduceOrIncrease*currConvertedServer.getServiceQueueSize();
                }
                j++;
            }

        }
        assert( inactiveServers.size() + activeServersByLoad.size() == servers.length);
        assert( prevActiveServers == activeServersByLoad.size() + numAgentsToConvertToInactive);
//        System.out.println( "After updateActiveServers(). There are now: " + activeServersByLoad.size() + " Active servers, and: " + inactiveServers.size() + " inactive servers. " + (activeServersByLoad.size() + inactiveServers.size()) + " altogether" );
    }

    //Attemps to assign an agent to a conversation. Returns the index of the assigned agent in case of success, or an indicator in case of failure, which takes place when there are no available agents to receive the conversation.
    public int assignPatientToAgent( Patient pt, double currTime)
    {

        updateActiveServers(currTime);
        Server currCandServ = null;
        for(Iterator<Server> it = activeServersByLoad.iterator(); it.hasNext() ; )
        {
            currCandServ = it.next();
            if(currCandServ.assignPatient(pt, serverAssignmentMode, getCurrAgentMaxLoad(currTime)))
            {
                it.remove( );
                activeServersByLoad.add( currCandServ );
                assert( activeServersByLoad.size() == servers.length);
                contentQueueSize += 1;
                onlineContentQueueSize += 1;
                return currCandServ.getId();

            }

        }
        assert( activeServersByLoad.size() == servers.length);
        return ASSIGNMENT_FAILED;

    }




    /**
     * Applies a serivce completion of the server of serverInd.
     * @param serverInd
     * @return the Patient getting into service due to the service completion of the current job (i.e. the one getting into service
     * just after the one which has finished service now), null if no patient is waiting in queue.
     */
    Patient serviceCompleted(int serverInd, double currTime)
    {
        //When working in dynamic mode, the server may have shifted between active/inactive states
        updateActiveServers(currTime);
        AbstractCollection<Server> currServerCollection = servers[serverInd].isActive ? activeServersByLoad : inactiveServers;
        //Important! - remove the server before modifying it, otherwise it's not properly removed from the TreeSet.
        boolean tmp = currServerCollection.remove( servers[serverInd]);
        assert( tmp );

        Patient nextPatientToService =  servers[serverInd].serviceCompleted();
        serviceQueueSize -= 1;
        if( servers[serverInd].isActive )
        {
            onlineServiceQueueSize -= 1;
        }
        //Update its load.

        currServerCollection.add( servers[serverInd]);
        assert( activeServersByLoad.size() + inactiveServers.size() == servers.length);
        return nextPatientToService;
    }


    public Patient patientAbandoned(Patient abandonedPatient, int serverInd,  double currTime) throws Exception {
        //When working in dynamic mode, the server may have shifted between active/inactive states
        updateActiveServers(currTime);
        AbstractCollection<Server> currServerCollection = servers[serverInd].isActive ? activeServersByLoad : inactiveServers;
        //Important! - remove the server before modifying it, otherwise it's not properly removed from the TreeSet.
        boolean tmp = currServerCollection.remove( servers[serverInd]);
        assert( tmp );

        Patient nextInLine = servers[serverInd].patientAbandoned(abandonedPatient);
        serviceQueueSize -= 1;
        if( servers[serverInd].isActive )
        {
            onlineServiceQueueSize -= 1;
        }
        //Update its load.

        currServerCollection.add( servers[serverInd]);
        assert( activeServersByLoad.size() + inactiveServers.size() == servers.length);
        return nextInLine;

    }


    void contentPhaseStart(int serverInd, Patient contentStartedPatient)
    {
        Server currServer = servers[serverInd];
        if(currServer.isActive) {
           boolean isInActiveServers =  activeServersByLoad.remove(servers[serverInd]);
           assert(isInActiveServers);
        }
        currServer.contentPhaseStart(contentStartedPatient);
        if(currServer.isActive) {
            activeServersByLoad.add(servers[serverInd]);
        }
        assert( activeServersByLoad.size() + inactiveServers.size() == servers.length);

        contentQueueSize += 1;
        if(currServer.isActive){
            onlineContentQueueSize += 1;
        }
    }

    //Returns true if the patient which has just finished its content phase immediately got into service.
    boolean contentPhaseEnd(int serverInd, Patient contentEndPatient)
    {
        Server currServer = servers[serverInd];
        if(currServer.isActive) {
            activeServersByLoad.remove(currServer);
        }
        boolean anotherGotToService =  currServer.contentPhaseEnd( contentEndPatient );
        contentQueueSize -= 1;
        serviceQueueSize += 1;
        if(currServer.isActive)
        {
            onlineContentQueueSize -= 1;
            if( onlineContentQueueSize < 0 )
            {
                int x = 9;
            }
            onlineServiceQueueSize += 1;
        }
        if(currServer.isActive) {
            activeServersByLoad.add(servers[serverInd]);
        }
        assert( activeServersByLoad.size() + inactiveServers.size() == servers.length);
//        assert( activeServersByLoad.size() != servers.length);
        return anotherGotToService;
    }


    public int getNumServers() { return servers.length ;   }






}
