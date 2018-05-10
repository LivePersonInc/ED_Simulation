package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class ED_Simulation_diff  {

    protected ExponentialDistribution arrivalDist;
    protected ExponentialDistribution serviceDist;
    protected ExponentialDistribution contentDist;
    protected int s;
    protected int n;
    protected double p;
    Random rng = new Random();

    public ED_Simulation_diff(double lambda, double mu, double delta, int s, int n, double p) {
        this.arrivalDist = new ExponentialDistribution(lambda, rng);
        this.serviceDist = new ExponentialDistribution(mu, rng);
        this.contentDist = new ExponentialDistribution(delta, rng);
        this.s = s;
        this.n = n;
        this.p = p;
    }

    public SimResults_diff simulate(double maxTime, double warmup) {
        SimResults_diff results = new SimResults_diff(n);
        FES fes = new FES();
        Queue holdingQueue = new Queue();
        Queue serviceQueue = new Queue();
        Queue contentQueue = new Queue();
        double t = 0;
        boolean register = false;
        
        // Schedule first event
        fes.addEvent(new Event(Event.ARRIVAL, arrivalDist.nextRandom()));

        while (t < maxTime) {
            
            if ( t > warmup && !register) {
                register = true;
            }

            Event e = fes.nextEvent();
            t = e.getTime();
            results.registerQueueLengths(holdingQueue.getSize(), serviceQueue.getSize(), contentQueue.getSize(),t);

            //System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize()+ "\t C:"+contentQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {
                Patient newPatient = new Patient(t);
                if (serviceQueue.getSize() + contentQueue.getSize() < n) {
                    if (register) results.registerHoldingTime(newPatient, t);
                    fes.addEvent(new Event(Event.CONTENT, t, newPatient));
                } else {
                    holdingQueue.addPatient(newPatient);
                }

                fes.addEvent(new Event(Event.ARRIVAL, t + arrivalDist.nextRandom()));

            } else if (e.getType() == Event.SERVICE) {

                Patient pat = e.getPatient();
                serviceQueue.removePatient(pat);

                // check service queue 
                if (serviceQueue.getSize() >= s) {
                    Patient pp = serviceQueue.getPatientAt(s - 1);
                    if (register) results.registerWaitingTime(pp, t);
                    pp.addWaitingTime(t);
                    fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pp));
                }

                double U = rng.nextDouble();
                if (U < p) {
                    fes.addEvent(new Event(Event.CONTENT, t + contentDist.nextRandom(), pat));
                    contentQueue.addPatient(pat);
                } else {
                    results.registerDeparture(pat, t);

                // check holding queue
                    if (holdingQueue.getSize() > 0) {
                        Patient pt = holdingQueue.removeFirstPatient();
                        if (register) results.registerHoldingTime(pt, t);
                        fes.addEvent(new Event(Event.CONTENT, t, pt));
                    }
                }

            } else if (e.getType() == Event.CONTENT) {

                Patient pat = e.getPatient();
                pat.setLastArrivalTime(t);
                contentQueue.removePatient(pat);
                serviceQueue.addPatient(pat);

                if (serviceQueue.getSize() <= s) {
                    if (register) results.registerWaitingTime(pat, t);
                    pat.addWaitingTime(t);
                    fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), pat));
                }

            }

        }

        return results;
    }
    
    public double dec( double a, int i ){
        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
    }
    

    public static void main(String[] args) {
        
        double lambda = 2;
        double mu = 1;
        double delta = 0.25;
        double p = 0.75;
        int s = 9;
        int nmin = 48;
        int nmax = 51;
        
        double[] w = new double[nmax];
        double[] h = new double[nmax];
        
        for(int n = nmin; n < nmax; n++ ){
            ED_Simulation_diff sim = new ED_Simulation_diff(lambda,mu,delta,s,n,p);
            SimResults_diff results = sim.simulate(10000000,1000);
            w[n] = results.getMeanWaitingTime();
            h[n] =  results.getMeanHoldingTime();
            System.out.println(n+"\t"+h[n]+"\t"+w[n]);
        }
        
        
        
        
    }


}
