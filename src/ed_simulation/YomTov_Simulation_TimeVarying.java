package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class YomTov_Simulation_TimeVarying {

    protected ExponentialDistribution arrivalDist;
    protected ExponentialDistribution serviceDist;
    protected ExponentialDistribution contentDist;
    protected int[] s;
    protected int[] n;
    protected double cycleLength;
    protected double p;
    protected double[] lambda;
    protected double intervalLength;
    protected int nPeriods;
    Random rng = new Random();

    public YomTov_Simulation_TimeVarying(double[] lambda, double cycleLength, double mu, double delta, int[] s, int[] n, double p) {
        this.arrivalDist = new ExponentialDistribution(1, rng);
        this.serviceDist = new ExponentialDistribution(mu, rng);
        this.contentDist = new ExponentialDistribution(delta, rng);
        this.s = s;
        this.n = n;
        this.p = p;
        this.lambda = lambda;
        this.cycleLength = cycleLength;
        this.intervalLength = cycleLength / lambda.length;
        this.nPeriods = lambda.length;
    }
    
        public int maxN( int[] a ){
        int m = -100;
        for ( int i =0; i < a.length; i++ ) if ( a[i] > m ) m = a[i];
        return m;
        
    }

    public SimResults_T simulate(double maxTime) {
        SimResults_T results = new SimResults_T(maxN(n),nPeriods);
        FES fes = new FES();
        Queue serviceQueue = new Queue();
        Queue contentQueue = new Queue();
        double t = 0;
        int interval = 0;

        // Schedule first event
        fes.addEvent(new Event(Event.ARRIVAL, arrivalDist.nextRandom()/lambda[0]));
        fes.addEvent(new Event(Event.NEWINTERVAL,intervalLength));

        while (t < maxTime) {

            Event e = fes.nextEvent();
            t = e.getTime();
            
            //System.out.println("Event:" + e.getType()+ "\t  H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize() + "\t C:"+ contentQueue.getSize() + "\t Total: "+ (serviceQueue.getSize()+contentQueue.getSize()));
  
                       
            results.registerQueueLengths(0, serviceQueue.getSize(), contentQueue.getSize(),s[interval],n[interval],t,interval);

            if (e.getType() == Event.ARRIVAL) {
                Patient newPatient = new Patient(t);
                if (serviceQueue.getSize() + contentQueue.getSize() < n[interval]) {
                    results.registerHoldingTime(newPatient, t);
                    fes.addEvent(new Event(Event.CONTENT, t, newPatient));
                    results.registerOneHold(false, interval);
                } else {
                    results.registerOneHold(true, interval);
                }

                fes.addEvent(new Event(Event.ARRIVAL, t + arrivalDist.nextRandom()/lambda[interval]));

            } else if (e.getType() == Event.SERVICE) {

                Patient pat = e.getPatient();
                pat.setInService(false);
                boolean b = serviceQueue.removePatient(pat);
                
                if (serviceQueue.getSize() >= s[interval]) {
                    Patient pp = serviceQueue.getPatientAt(s[interval] - 1);
                    if ( !pp.getInService() ){
                        pp.setInService(true);
                        results.registerWaitingTime(pp, t,interval);
                        pp.addWaitingTime(t - pp.getLastArrivalTime());
                        fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pp));
                    }
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

                if (serviceQueue.getSize() <= s[interval]) {
                    results.registerWaitingTime(pat, t,interval);
                    pat.setInService(true);
                    pat.addWaitingTime(t - pat.getLastArrivalTime());
                    fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pat));
                }

            } else if ( e.getType() == Event.NEWINTERVAL ){
                
                int oldS = s[interval];
                interval++;
                if (interval == nPeriods) interval = 0;
                int newS = s[interval];
                
                for ( int i = oldS; i < newS; i++ ){
                    if ( serviceQueue.getSize() > i ){
                        Patient patt = serviceQueue.getPatientAt(i);
                        if( !patt.getInService() ){
                            patt.setInService(true);
                            results.registerWaitingTime(patt, t,interval);
                            patt.addWaitingTime(t - patt.getLastArrivalTime());
                            fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), patt));
                        }
                    }
                }
                
                fes.addEvent(new Event(Event.NEWINTERVAL,t+intervalLength));
                
            }

        }

        return results;
    }
    
    public double dec( double a, int i ){
        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
    }
    

    public static void main(String[] args) {
        double[] l = {7.471264, 5.275862, 3.873563, 3.206897, 2.425287,
  2.735632, 2.689655, 5.850575, 13.90805, 23.26437,
  25.54023, 21.90805, 19.57471, 16.34483, 14.85057,
  14.29885, 14.24138, 14.83908, 16.06897, 16.87356,
  17.44828, 15.48276, 12.68966, 9.609195, 7.436782,
  5.137931, 4.034483, 3.068966, 2.62069, 2.655172,
  2.793103, 5.54023, 13.27586, 23.18391, 25.10345,
  22.29885, 18.29885, 16.31034, 12.93103, 12.86207,
  13.18391, 16.70115, 19.25287, 18.87356, 17.17241,
  15.03448, 13.10345, 9.563218, 6.643678, 4.678161,
  3.735632, 2.83908, 2.62069, 2.609195, 2.988506,
  6.195402, 13.2069, 22.57471, 24.34483, 22.45977,
  19.02299, 16.21839, 14.18391, 13.85057, 13.75862,
  13.68966, 15.70115, 16.14943, 16.01149, 13.94253,
  12.56322, 9.563218, 7.159091, 5.056818, 4.102273,
  3.170455, 2.397727, 2.363636, 3.045455, 5.704545,
  12.19318, 22.25, 24.56818, 23.68182, 19.89773,
  17.20455, 15.40909, 14.59091, 13.82955, 16,
  18.17045, 18, 17.76136, 16.32955, 13.40909,
  10.30682, 8.215909, 5.784091, 4.011364, 3.352273,
  3.136364, 2.784091, 2.670455, 5.090909, 12.21591,
  18.79545, 19.82955, 17.97727, 16.59091, 13.94318,
  13.17045, 12.80682, 13.22727, 13.55682, 12.19318,
  13.18182, 12.23864, 11.60227, 10.23864, 7.852273,
  5.977273, 5.590909, 3.795455, 3.829545, 3.090909,
  3.102273, 2.897727, 3.931818, 5.204545, 7.329545,
  9.420455, 10.85227, 10.82955, 9.204545, 8.943182,
  9.318182, 10.17045, 11.22727, 14.26136, 13.77273,
  15.90909, 17.42045, 15.22727, 11.06818, 8.693182,
  6.284091, 5.068182, 3.852273, 3.636364, 2.897727,
  3.25, 7.159091, 15.78409, 26.75, 30.81818,
  26.55682, 23.26136, 18.65909, 16.52273, 14.52273,
  13.71591, 17.69318, 20.55682, 20.97727, 19.39773,
  17.09091, 13.69318, 10.40909};
        double mu = 10.9;
        double cycleLength = 168;
        double delta = 2.3;
        double p = 0.69697;
        
        int[] s = {6, 5, 4, 3, 3, 2, 2, 3, 5, 7, 9, 7, 9, 9, 8, 8, 7, 7, 8, 8, 8, 8, 7, 
            7, 6, 5, 4, 3, 3, 2, 2, 3, 5, 7, 9, 9, 9, 9, 8, 7, 7, 7, 8, 8, 8, 8, 
            7, 7, 6, 4, 4, 3, 3, 2, 2, 3, 5, 7, 9, 9, 9, 9, 8, 8, 7, 7, 7, 8, 8, 
            7, 7, 6, 5, 5, 4, 3, 3, 2, 2, 3, 4, 7, 9, 9, 9, 9, 8, 8, 8, 8, 8, 8, 
            8, 8, 8, 7, 6, 5, 4, 3, 3, 3, 2, 3, 4, 6, 8, 8, 8, 8, 7, 7, 7, 7, 7, 
            7, 7, 6, 6, 5, 5, 4, 4, 3, 3, 3, 2, 3, 3, 3, 4, 5, 5, 5, 5, 5, 5, 6, 
            6, 7, 7, 8, 8, 7, 6, 5, 4, 4, 3, 3, 3, 3, 5, 8, 10, 10, 9, 9, 8, 8, 
            8, 8, 8, 9, 8, 8, 8, 7};
        int n = 25;
        
//        YomTov_Simulation_TimeVarying sim = new YomTov_Simulation_TimeVarying(l, cycleLength, mu, delta, s, n, p);
//        SimResults_T results = sim.simulate(1000000);
//        
//        System.out.println("Mean inner wait: "+ results.getMeanWaitingTime());
//        System.out.println("Mean LOS: "+results.getMeanSojournTime());
//        System.out.println("P(W>0): "+ results.getWaitingProbability());
//        System.out.println("Nurse occupancy: "+results.getUtilNurses());
//        System.out.println("Bed occupancy: "+results.getUtilBeds());
    }


}
