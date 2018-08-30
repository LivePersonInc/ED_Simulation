//package ed_simulation;
//
//import statistics.*;
//import java.util.*;
//
///**
// *
// * @author bmathijs
// */
//public class ED_Simulation_1 extends Sim {
//
//    protected ExponentialDistribution arrivalDist;
//    protected ExponentialDistribution serviceDist;
//    protected ExponentialDistribution contentDist;
//    protected ExponentialDistribution abandonmentDist;
//    protected ExponentialDistribution spontaneousavDist;
//    protected int s;
//    protected int n;
//    protected double p;
//    //protected double p1;
//    //protected double p2;
//    Random rng = new Random();
//
//    public ED_Simulation_1(double lambda, double mu, double delta, int s, int n, double p) {
//        this.arrivalDist = new ExponentialDistribution(lambda, rng);
//        this.serviceDist = new ExponentialDistribution(mu, rng);
//        this.contentDist = new ExponentialDistribution(delta, rng);
//        this.abandonmentDist = new ExponentialDistribution(.2*lambda, rng);
//        this.spontaneousavDist = new ExponentialDistribution(.0001, rng);
//        this.s = s;
//        this.maxTotalCapacity = maxTotalCapacity;
//        this.p = p;
//        //this.p1 = p1;
//        //this.p2 = p2;
//
//    }
//
//    public SimResults simulate(double maxTime) {
//        SimResults results = new SimResults(n);
//        FES fes = new FES();
//        Queue holdingQueue = new Queue();
//        Queue serviceQueue = new Queue();
//        Queue contentQueue = new Queue();
//        double t = 0;
//        double p1 = 0.5;
//        double p2 = 0.001;
//
//        // Schedule first event
//        fes.addEvent(new Event(Event.ARRIVAL, arrivalDist.nextRandom()));
//
//        //Schedule First Spontaneous Service
//        fes.addEvent(new Event(Event.SPONTANEOUSSERV, spontaneousavDist.nextRandom()));
//
//
//        while (t < maxTime) {
//
//            Event e = fes.nextEvent();
//            t = e.getTime();
//            results.registerQueueLengths(holdingQueue.getSize(), serviceQueue.getSize(), contentQueue.getSize(),t);
//
//            //Abandonment of the holding Queue
//            for( int i = 0; i < holdingQueue.getSize() ; i++ ){
//                Patient PatientHolding = holdingQueue.getPatientAt(i);
//                if(PatientHolding.getHoldingTime()>= abandonmentDist.nextRandom()){
//                    holdingQueue.removePatient(PatientHolding);
//                    }
//            }
//            // System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
//            if (e.getType() == Event.ARRIVAL) {
//                Patient newPatient = new Patient(t);
//                if (serviceQueue.getSize() + contentQueue.getSize() < n) {
//                    //p1 prob of enter service
//                    double K = rng.nextDouble();
//                    if (K < p1) {
//                    results.registerHoldingTime(newPatient, t);
//                    fes.addEvent(new Event(Event.CONTENT, t, newPatient));
//                    }
//                } else {
//                    holdingQueue.addPatient(newPatient);
//                }
//                fes.addEvent(new Event(Event.ARRIVAL, t + arrivalDist.nextRandom()));
//
//            } else if (e.getType() == Event.SERVICE) {
//
//                Patient pat = e.getPatient();
//                serviceQueue.removePatient(pat);
//
//                // check service queue
//                //p2 server continuation probability
//                if (serviceQueue.getSize() >= s) {
//                    double Z = rng.nextDouble();
//                    if (Z < p2) {
//                        Patient pp = serviceQueue.getPatientAt(s - 1);
//                        results.registerWaitingTime(pp, t);
//                        pp.addWaitingTime(t - pp.getLastArrivalTime());
//                        fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pp));
//                    }
//                }
//
//                double U = rng.nextDouble();
//                if (U < p) {
//                    fes.addEvent(new Event(Event.CONTENT, t + contentDist.nextRandom(), pat));
//                    contentQueue.addPatient(pat);
//                } else {
//                    results.registerDeparture(pat, t);
//
//                // check holding queue
//                    if (holdingQueue.getSize() > 0) {
//                        Patient pt = holdingQueue.removeFirstPatient();
//                        results.registerHoldingTime(pt, t);
//                        fes.addEvent(new Event(Event.CONTENT, t, pt));
//                    }
//                }
//
//            } else if (e.getType() == Event.CONTENT) {
//
//                Patient pat = e.getPatient();
//                pat.setLastArrivalTime(t);
//                contentQueue.removePatient(pat);
//                serviceQueue.addPatient(pat);
//
//                if (serviceQueue.getSize() <= s) {
//                    results.registerWaitingTime(pat, t);
//                    pat.addWaitingTime(t - pat.getLastArrivalTime());
//                    fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pat));
//                }
//
//            }else if (e.getType() == Event.SPONTANEOUSSERV){
//               if (holdingQueue.getSize() > 0) {
//                        Patient ps = holdingQueue.removeFirstPatient();
//                        results.registerHoldingTime(ps, t);
//                        serviceQueue.addPatient(ps);
//                        results.registerWaitingTime(ps, t);
//                        ps.addWaitingTime(t - ps.getLastArrivalTime());
//                        fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), ps));
//                        //Schedule Next Spontaneous Service
//                        fes.addEvent(new Event(Event.SPONTANEOUSSERV, t + spontaneousavDist.nextRandom()));
//                    }
//            }
//
//        }
//    return results;
//    }
//
//    public double dec( double a, int i ){
//        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
//    }
//
//    //@Override
//    //public int getType(){
//        //return 2;
//    //}
//
//    public static void main(String[] args) {
//
//        double lambda = 2;
//        double mu = 1;
//        double delta = 0.75;
//        double p = 0.25;
//        int s = 9;
//        int n = 100;
//        //double theta = .2;
//        //double p1 = .026;
//        //double p2 = .040;
//        //double xi = .001;
//        ED_Simulation_1 sim = new ED_Simulation_1(lambda,mu,delta,s,n,p);
//        for( int i = 0; i < 1; i++ ){
//        SimResults results = sim.simulate(10000000);
//        System.out.println(results.getCIdelayProbability()[0] + "\t"+results.getCIdelayProbability()[1] + "||\t"+  results.getCIWaitingTime()[0]+"\t"+  results.getCIWaitingTime()[1] +
//                "||\t" + results.getCISojournTime()[0]+"\t"+  results.getCISojournTime()[1]
//            );
//        }
//    }
//
//
//}
