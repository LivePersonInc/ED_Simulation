package ed_simulation;

import com.sun.corba.se.spi.activation.ServerManager;

import java.rmi.server.ExportException;
import java.util.*;

class ServersManager {



    class Server{

        private int id;
        private int maxLoad; //Max number of conversations this server can handle.
        private LinkedList<Patient> serviceQueue;
        private HashSet<Patient> contentQueue;
        private Random rng = new Random();
        //Currently not supported.
        private HashMap<Integer,Double> loadsToAssignProbMap;
        private double loadsToAssignmentGran;


        public Server( int id, int maxLoad, HashMap<Integer,Double> loadsToAssignProbMap, double loadsToAssignmentGran)
        {
            this.id = id;
            serviceQueue = new LinkedList<Patient>();
            contentQueue = new HashSet<Patient>();
            this.maxLoad = maxLoad;
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
        public boolean assignPatient(Patient pt, ServerAssignmentMode assignmentMode) throws  IllegalArgumentException {

            switch (assignmentMode) {
                case FIXED_SERVER_CAPACITY:
                    if( getLoad() + 1 <= maxLoad )
                    {
                        contentQueue.add(pt);
                        return true;
                    }
                    else
                    {
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
    private TreeSet<Server> serversByLoad;
    private int serviceQueueSize;
    private int contentQueueSize;
    private HashMap<Integer,Double> loadsToAssignmentMap;
    private double loadsToAssignmentGran;



    public ServersManager(int numServers, int serverFixedMaxLoad,
                          HashMap<Integer,Double> loadsToAssignmentMap, double loadsToAssignmentGran,  ServerAssignmentMode serverAssignmentMode)
    {


        servers = new Server[numServers];
        serversByLoad = new TreeSet<Server>(new ServersComparator());
        boolean b;
        for( int i = 0 ; i < numServers ; i++)
        {
            Server currServ = new Server(i, serverFixedMaxLoad, loadsToAssignmentMap, loadsToAssignmentGran);
            servers[i]  = currServ;
            b =  serversByLoad.add(currServ);
        }

        this.serverAssignmentMode = serverAssignmentMode;
        this.loadsToAssignmentGran = loadsToAssignmentGran;
        this.loadsToAssignmentMap = loadsToAssignmentMap;

        serviceQueueSize = 0;
        contentQueueSize = 0;

    }

    public int getServiceQueueSize() {return serviceQueueSize; }
    public int getContentQueueSize() {return contentQueueSize; }


    //Attemps to assign an agent to a conversation. Returns the index of the assigned agent in case of success, or an indicator in case of failure.
    public int assignPatientToAgent( Patient pt)
    {
        Server currCandServ = null;
        boolean assigned = false;
        for( Iterator<Server> it = serversByLoad.iterator() ; it.hasNext() ; )
        {
            currCandServ = it.next();
            if(currCandServ.assignPatient(pt, serverAssignmentMode))
            {
                it.remove( );
                serversByLoad.add( currCandServ );
                assert( serversByLoad.size() == servers.length);
                contentQueueSize += 1;
                return currCandServ.getId();

            }

        }
        assert( serversByLoad.size() == servers.length);
        return ASSIGNMENT_FAILED;

    }


    /**
     * Applies a serivce completion of the server of serverInd.
     * @param serverInd
     * @return the Patient getting into service due to the service completion of the current job (i.e. the one getting into service
     * just after the one which has finished service now), null if no patient is waiting in queue.
     */
    Patient serviceCompleted(int serverInd)
    {
        //Important! - remove the server before modifying it, otherwise it's not properly removed from the TreeSet.
        boolean tmp = serversByLoad.remove( servers[serverInd]);
        assert( tmp );

        Patient nextPatientToService =  servers[serverInd].serviceCompleted();
        serviceQueueSize -= 1;
        //Update its load.

        serversByLoad.add( servers[serverInd]);
        assert( serversByLoad.size() == servers.length);
        return nextPatientToService;
    }

    void contentPhaseStart(int serverInd, Patient contentStartedPatient)
    {
        serversByLoad.remove( servers[serverInd]);
        servers[serverInd].contentPhaseStart(contentStartedPatient);
        serversByLoad.add( servers[serverInd]);
        assert( serversByLoad.size() == servers.length);

        contentQueueSize += 1;
    }

    //Returns true if the patient which has just finished its content phase immediately got into service.
    boolean contentPhaseEnd(int serverInd, Patient contentEndPatient)
    {
        serversByLoad.remove( servers[serverInd]);
        boolean anotherGotToService =  servers[serverInd].contentPhaseEnd( contentEndPatient );
        contentQueueSize -= 1;
        serviceQueueSize += 1;
        serversByLoad.add( servers[serverInd]);
        assert( serversByLoad.size() == servers.length);
        return anotherGotToService;
    }


    public int getNumServers() { return servers.length ;   }






}
