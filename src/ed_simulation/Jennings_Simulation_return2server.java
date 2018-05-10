//package ed_simulation;
//
//import statistics.ExponentialDistribution;
//
//import java.util.HashMap;
//import java.util.Random;
//
///**
// *
// * @author bmathijs
// */
//public class Jennings_Simulation_return2server extends Sim {
//
//    protected ExponentialDistribution arrivalDist;
//    protected ExponentialDistribution serviceDist;
//    protected ExponentialDistribution contentDist;
//    protected ExponentialDistribution abandonmentDist;
//    protected ExponentialDistribution spontaneousavDist;
//    protected int s;
//    protected int n;
//    protected double p;
//    Random rng = new Random();
//
//    public Jennings_Simulation_return2server(double lambda, double mu, double delta, int s, int n, double p) {
//        this.arrivalDist = new ExponentialDistribution(lambda, rng);
//        this.serviceDist = new ExponentialDistribution(mu, rng);
//        this.abandonmentDist = new ExponentialDistribution( .00001, rng);
//        this.contentDist = new ExponentialDistribution(delta, rng);
//        this.spontaneousavDist = new ExponentialDistribution(.00001, rng);
//        this.s = s;
//        this.n = n;
//        this.p = p;
//    }
//
//    public SimResults simulate(double maxTime) {
//        SimResults results = new SimResults(n);
//        FES fes = new FES();
//        Queue holdingQueue = new Queue();
//        HashMap<Integer, Queue> serviceQueues = new HashMap<>(s);
//        for(int i = 1 ; i <= s ; i++)
//        {
//            serviceQueues.put(new Integer(i), new Queue());
//        }
//        Queue contentQueue = new Queue();
//        double t = 0;
//        double p1 =   .9;
//        double p2 =  .99999;
//        int x = 0;
//
//        // Schedule first event
//        fes.addEvent(new Event(Event.ARRIVAL, arrivalDist.nextRandom()));
//
//        //Schedule First Spontaneous Service
//        fes.addEvent(new Event(Event.SPONTANEOUSSERV, spontaneousavDist.nextRandom()));
//
//        //Schedule First Abandonment
//        fes.addEvent(new Event(Event.ABANDONMENT, abandonmentDist.nextRandom()));
//
//        while (t < maxTime) {
//
//            Event e = fes.nextEvent();
//            t = e.getTime();
//            results.registerQueueLengths(holdingQueue.getSize(), serviceQueue.getSize(), contentQueue.getSize(),t);
//            //calculating p1
////            x = serviceQueue.getSize()+ contentQueue.getSize();
//            //p1 = 1/(1+Math.exp(-(-0.084-(.265*x)+(0.039*serviceQueue.getSize())+(0.023*s))));
//            //p2 = 1/(1+Math.exp(-(-8.010+(.206*x)+(0.069*serviceQueue.getSize())+(0.166*s))));
//
//            // System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
//            //!!!! - what's n?
//            if (e.getType() == Event.ARRIVAL) { //Arrival of a new patient to the system.
//                Patient newPatient = new Patient(t);
//                if (serviceQueue.getSize() + contentQueue.getSize() < n) { //!!! Do these two queues keep only up to n patients? So what's 's' for? So if there are, say, n jobs in the content queue, then all the servers are idle, and no job is inserted to service?
//                    //p1 prob of enter service
//                    double K = rng.nextDouble();
//                    if (K < p1) {
//                        //!!! p1 is supposed to determine agent's availability, no? Why is this arrival becomes content without going through any queue?
//                    results.registerHoldingTime(newPatient, t); //It doesn't wait but gets immediately into service.
//                    fes.addEvent(new Event(Event.CONTENT, t, newPatient)); // This is just a future event - it's getting into service queue when taking care of this event.
//                    }
//                } else {
//                    //Add this arrial to the holding queue only if there are n or more jobs currently in the system (content + service) !!! Why?
//                    holdingQueue.addPatient(newPatient);
//                }
//                // Add next arrival to the Futuere Events Set.
//                fes.addEvent(new Event(Event.ARRIVAL, t + arrivalDist.nextRandom()));
//
//
//            } else if (e.getType() == Event.SERVICE) { //Getting into service (from the service queue). The time of this event is the service end time.
//
//                Patient pat = e.getPatient();
//                serviceQueue.removePatient(pat); //!!!! Seems that the service queue is not a queue but keeps the set of patience that are being served now. What does he do with the removed patient?
//
//                // check service queue
//                if (serviceQueue.getSize() >= s) { //!!! why? what's s? I think he's trying add the next  service of this patient.
//
//                        Patient pp = serviceQueue.getPatientAt(s - 1); //!! Why taking the patient at s-1, and not the last one in the queue? If there are s+17 patients in queue?
//                        results.registerWaitingTime(pp, t);
//                        pp.addWaitingTime(t - pp.getLastArrivalTime());
//                        fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pp));
//                }
//                //w.p. p - this patient stays in the system (goes into the content queue). w.p. 1-p it leaves the system
//                double U = rng.nextDouble();
//                if (U < p) {
//                    fes.addEvent(new Event(Event.CONTENT, t + contentDist.nextRandom(), pat));
//                    contentQueue.addPatient(pat);
//                } else { //The patient leaves the system.
//                    results.registerDeparture(pat, t);
//
//                // check holding queue
//                    if (holdingQueue.getSize() > 0) {
//                        double Z = rng.nextDouble();
//                        if (Z < p2) {  //After a job has ended, w.p. p2 its agent remains active, thus receives another job.
//                            Patient pt = holdingQueue.removeFirstPatient();
//                            results.registerHoldingTime(pt, t);
//                            fes.addEvent(new Event(Event.CONTENT, t, pt)); //!! I don't get this. Why is it going to content?
//                        }
//                    }
//                }
//
//            } else if (e.getType() == Event.CONTENT) {
//                //Inidicates the end of a content phase of a patient (the time in which he leaves the content queue and gets back into the service queue).
//
//                Patient pat = e.getPatient();
//                pat.setLastArrivalTime(t);
//                contentQueue.removePatient(pat);// If it's a new arrival, it's not there.
//                serviceQueue.addPatient(pat);
//
//                if (serviceQueue.getSize() <= s) { //^^missing - if only s1 < s servers are available, this is missing.
//                    results.registerWaitingTime(pat, t); //registerWaitingTiime - only in service queue (by Antonio's explanation). So if all s are avaialble - it gets immediately to service. My Question: if s == 5 and n == 10,
//                    pat.addWaitingTime(t - pat.getLastArrivalTime());
//                    fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pat)); //Adding a service event indicating the instant of the service end.
//                }
//            }else if (e.getType() == Event.SPONTANEOUSSERV){ //Spontaneous transition from the holding queue (external queue) to the service queue. I think this represents  an assignment to one of the agents.
//               if (holdingQueue.getSize() > 0) {
//                        Patient ps = holdingQueue.removeFirstPatient();
//                        results.registerHoldingTime(ps, t);
//                        serviceQueue.addPatient(ps);
//                        results.registerWaitingTime(ps, t);
//                        ps.addWaitingTime(t - ps.getLastArrivalTime()); //!!!If it wasn't content before, lastArrivalTime == 0!!
//                        fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), ps)); //!! Not 100% clear about it. If it gets into the service queue, does it mean it's now starting service? Antonio: simulates VIP??
//                        //Schedule Next Spontaneous Service
//                        fes.addEvent(new Event(Event.SPONTANEOUSSERV, t + spontaneousavDist.nextRandom()));
//                    }
//            } else if (e.getType() == Event.ABANDONMENT){
//                if (holdingQueue.getSize() > 0) {
//                    holdingQueue.removeFirstPatient();
//                    //schedule next abadonment
//                    }
//                fes.addEvent(new Event(Event.ABANDONMENT, t + abandonmentDist.nextRandom()));
//
//            }
//
//
//        }
//
//        return results;
//    }
//    public double dec( double a, int i ){
//        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
//    }
//
//    @Override
//    public int getType(){
//        return 2;
//    }
//
//    public static void main(String[] args) {
//        double lambda = 2;
//        double mu = 1;
//        double delta = 0.75;
//        double p = 0.25;
//        int s = 9;
//        int n = 100;
//        Jennings_Simulation_return2server jv = new Jennings_Simulation_return2server(lambda,mu,delta,s,n,p);
//  for( int i = 0; i < 1; i++ ){
//        SimResults results = jv.simulate(10000000);
//        System.out.println(results.getCIdelayProbability()[0] + "\t"+results.getCIdelayProbability()[1] + "||\t"+  results.getCIWaitingTime()[0]+"\t"+  results.getCIWaitingTime()[1] +
//                "||\t" + results.getCISojournTime()[0]+"\t"+  results.getCISojournTime()[1]
//            );
//        }
//    }
//
//
//}