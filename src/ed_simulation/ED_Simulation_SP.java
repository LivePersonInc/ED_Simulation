package ed_simulation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class ED_Simulation_SP {

    protected ExponentialDistribution arrivalDist;
    protected ExponentialDistribution serviceDist;
    protected ExponentialDistribution contentDist;
    protected int s;
    protected int n;
    protected double p;
    Random rng = new Random();

    public ED_Simulation_SP(double lambda, double mu, double delta, int s, int n, double p) {
        this.arrivalDist = new ExponentialDistribution(lambda, rng);
        this.serviceDist = new ExponentialDistribution(mu, rng);
        this.contentDist = new ExponentialDistribution(delta, rng);
        this.s = s;
        this.n = n;
        this.p = p;
    }

    public SimResults simulate(double maxTime, String name) throws IOException {
        SimResults results = new SimResults(n);
        FES fes = new FES();
        Queue holdingQueue = new Queue();
        Queue serviceQueue = new Queue();
        Queue contentQueue = new Queue();
        double t = 0;

        // Schedule first event
        fes.addEvent(new Event(Event.ARRIVAL, arrivalDist.nextRandom()));

        while (t < maxTime) {

            Event e = fes.nextEvent();
            t = e.getTime();
            results.registerQueueLengths(holdingQueue.getSize(), serviceQueue.getSize(), contentQueue.getSize(),t);

            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(name  + ".txt", true)))) {            
                out.println( dec(t,3) + "\t "+holdingQueue.getSize()+ "\t " + serviceQueue.getSize()+ "\t "+(serviceQueue.getSize()+contentQueue.getSize()));
            }
            
            if (e.getType() == Event.ARRIVAL) {
                Patient newPatient = new Patient(t);
                if (serviceQueue.getSize() + contentQueue.getSize() < n) {
                    results.registerHoldingTime(newPatient, t);
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

                // check holding queue
                    if (holdingQueue.getSize() > 0) {
                        Patient pt = holdingQueue.removeFirstPatient();
                        results.registerHoldingTime(pt, t);
                        fes.addEvent(new Event(Event.CONTENT, t, pt));
                    }
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
            
            try (PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(name  + ".txt", true)))) {            
                out.println( dec(t,3) + "\t "+holdingQueue.getSize()+ "\t " + serviceQueue.getSize()+ "\t "+(serviceQueue.getSize()+contentQueue.getSize()));
            }
            
            //System.out.println("{" + dec(t,3) + ", "+holdingQueue.getSize()+ ", " + serviceQueue.getSize()+ ", "+contentQueue.getSize()+ "},");

        }

        return results;
    }
    
    public double dec( double a, int i ){
        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
    }

    public static void main(String[] args) throws IOException {
        ED_Simulation_SP ed = new ED_Simulation_SP(1.25, 1, 0.25, 7, 24, 0.75);
        SimResults sr = ed.simulate(1000,"Lambda125");

        
        
    }

}
