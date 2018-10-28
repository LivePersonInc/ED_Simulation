package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class YomTov_Simulation extends Sim{

    protected ExponentialDistribution arrivalDist;
    protected ExponentialDistribution serviceDist;
    protected ExponentialDistribution contentDist;
    protected int s;
    protected int n;
    protected double p;
    Random rng = new Random();

    public YomTov_Simulation(double lambda, double mu, double delta, int s, int n, double p) {
        this.arrivalDist = new ExponentialDistribution(lambda, rng);
        this.serviceDist = new ExponentialDistribution(mu, rng);
        this.contentDist = new ExponentialDistribution(delta, rng);
        this.s = s;
        this.n = n;
        this.p = p;
    }

    int totalNumArrivals = 0;
    int totalNumRejected = 0;

    public SimResults simulate(double maxTime) {
        SimResults results = new SimResults(n);
        FES fes = new FES();
        Queue serviceQueue = new Queue();
        Queue contentQueue = new Queue();
        double t = 0;

        // Schedule first event
        fes.addEvent(new Event(Event.ARRIVAL, arrivalDist.nextRandom()));

        while (t < maxTime) {

            Event e = fes.nextEvent();
            t = e.getTime();
            results.registerQueueLengths(0, serviceQueue.getSize(), contentQueue.getSize(),t);

//             System.out.println( "S: " + serviceQueue.getSize() + " \t C: " + contentQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {
                totalNumArrivals += 1;
                Patient newPatient = new Patient(t);
                if (serviceQueue.getSize() + contentQueue.getSize() < n) {
                    results.registerHoldingTime(newPatient, t);
                    fes.addEvent(new Event(Event.CONTENT, t, newPatient));
                }
                else
                {
                    System.out.println("A Petient was just rejected!!");
                    totalNumRejected += 1;

                }

                fes.addEvent(new Event(Event.ARRIVAL, t + arrivalDist.nextRandom()));

            } else if (e.getType() == Event.SERVICE) {

                Patient pat = e.getPatient();
                serviceQueue.removePatient(pat);

                // check service queue 
                if (serviceQueue.getSize() >= s) {
                    Patient pp = serviceQueue.getPatientAt(s - 1);
                    results.registerWaitingTime(pp, t);
                    pp.addWaitingTime(t - pp.getLastArrivalTime());
                    fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pp));
                }

                double U = rng.nextDouble();
                if (U < p) {
                    fes.addEvent(new Event(Event.CONTENT, t + contentDist.nextRandom(), pat));
                    contentQueue.addPatient(pat);
                } else {
                    results.registerDeparture(pat, t);

                }

            } else if (e.getType() == Event.CONTENT) {

                Patient pat = e.getPatient();
                pat.setLastArrivalTime(t);
                contentQueue.removePatient(pat);
                serviceQueue.addPatient(pat);

                if (serviceQueue.getSize() <= s) {
                    results.registerWaitingTime(pat, t);
                    pat.addWaitingTime(t - pat.getLastArrivalTime());
                    fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pat));
                }

            }

        }
        System.out.println("There were total of " + totalNumArrivals + " arrivals to the system, out of which " + totalNumRejected + "conversations were rejected.");
        return results;
    }

    
    public double dec( double a, int i ){
        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
    }
        
    
    @Override
    public int getType(){
        return 1;
    }
    
    
    public static void main(String[] args) {

        double lambda = 179;
        double mu = 81.52;
        double delta = 61.0518;
        int s = 4;
        int n = s*3;
        double p = 0.93289;
        YomTov_Simulation ed = new YomTov_Simulation(lambda, mu, delta, s, n, p);
        SimResults sr = ed.simulate(10000);
//        System.out.println(lambda + "\t" + sr.getWaitingProbability() + "\t" + sr.getMeanWaitingTime() + "\t" + sr.getMeanAllInSystem());
        System.out.println("\t"+ sr.getMeanServiceQueueLength() + "\t"+sr.getMeanHoldingTime() + "\t" + sr.getMeanWaitingTime() + "\t"+ sr.getHoldingProbability()
                + "\t"+ sr.getWaitingProbability() + "\t"+ sr.getMeanTotalInSystem() +  "\t"+ sr.getMeanAllInSystem() );
        int x = 0;
    }

        //
//                double mu = 1;
//        double delta = 0.2;
//        double p = 0.8;
//
//        double[] lambda = {0.609612,
//4,
//8.53893,
//22.6219,
//46.5873,
//70.7931,
//95.1234,
//119.533,
//144,
//168.509,
//193.053
//    };
//
//        int[] s = {4,
//22,
//46,
//118,
//241,
//363,
//487,
//610,
//733,
//857,
//981};
//
//        int[] n = {
//            17,
//105,
//221,
//577,
//1182,
//1791,
//2402,
//3016,
//3630,
//4245,
//4861};
//
//
//
//
//        for( int i = 8; i < s.length; i++ ){
//            YomTov_Simulation ed = new YomTov_Simulation(lambda[i], mu, delta, s[i], n[i], p);
//            SimResults sr = ed.simulate(10000);
//
//            System.out.println(lambda[i]+"\t"+ sr.getWaitingProbability()+"\t"+ sr.getMeanWaitingTime() + "\t"+ sr.getHoldingProbabilities()+ "\t"+ sr.getMeanHoldingTime() + "\t" + sr.getNurseOccupancy(s[i]));
//        }
//    }

}
