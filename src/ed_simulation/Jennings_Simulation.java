package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class Jennings_Simulation extends Sim {

    protected ExponentialDistribution arrivalDist;
    protected ExponentialDistribution serviceDist;
    protected ExponentialDistribution contentDist;
    protected ExponentialDistribution abandonmentDist;
    protected ExponentialDistribution spontaneousavDist;
    protected int s;
    protected int n;
    protected double p;
    Random rng = new Random();

    public Jennings_Simulation(double lambda, double mu, double delta, int s, int n, double p) {
        this.arrivalDist = new ExponentialDistribution(lambda, rng);
        this.serviceDist = new ExponentialDistribution(mu, rng);
        this.abandonmentDist = new ExponentialDistribution( .00001, rng);
        this.contentDist = new ExponentialDistribution(delta, rng);
        this.spontaneousavDist = new ExponentialDistribution(.00001, rng);
        this.s = s;
        this.n = n;
        this.p = p;
    }

    public SimResults simulate(double maxTime) {
        SimResults results = new SimResults(n);
        FES fes = new FES();
        Queue holdingQueue = new Queue();
        Queue serviceQueue = new Queue();
        Queue contentQueue = new Queue();
        double t = 0;
        double p1 =   .9;
        double p2 =  .99999;
        int x = 0;     
        
        // Schedule first event
        fes.addEvent(new Event(Event.ARRIVAL, arrivalDist.nextRandom()));
        
        //Schedule First Spontaneous Service 
        fes.addEvent(new Event(Event.SPONTANEOUSSERV, spontaneousavDist.nextRandom()));
        
        //Schedule First Abandonment 
        fes.addEvent(new Event(Event.ABANDONMENT, abandonmentDist.nextRandom()));
        
        while (t < maxTime) {

            Event e = fes.nextEvent();
            t = e.getTime();
            results.registerQueueLengths(holdingQueue.getSize(), serviceQueue.getSize(), contentQueue.getSize(),t);
            //calculating p1
            x = serviceQueue.getSize()+ contentQueue.getSize();
            //p1 = 1/(1+Math.exp(-(-0.084-(.265*x)+(0.039*serviceQueue.getSize())+(0.023*s))));
            //p2 = 1/(1+Math.exp(-(-8.010+(.206*x)+(0.069*serviceQueue.getSize())+(0.166*s))));
            
            // System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {
                Patient newPatient = new Patient(t);
                if (serviceQueue.getSize() + contentQueue.getSize() < n) {
                    //p1 prob of enter service 
                    double K = rng.nextDouble();
                    if (K < p1) {
                    results.registerHoldingTime(newPatient, t);
                    fes.addEvent(new Event(Event.CONTENT, t, newPatient));
                    }
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
                        double Z = rng.nextDouble();
                        if (Z < p2) {
                            Patient pt = holdingQueue.removeFirstPatient();
                            results.registerHoldingTime(pt, t);
                            fes.addEvent(new Event(Event.CONTENT, t, pt));
                        }
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
            }else if (e.getType() == Event.SPONTANEOUSSERV){
               if (holdingQueue.getSize() > 0) {
                        Patient ps = holdingQueue.removeFirstPatient();
                        results.registerHoldingTime(ps, t);
                        serviceQueue.addPatient(ps);
                        results.registerWaitingTime(ps, t);
                        ps.addWaitingTime(t - ps.getLastArrivalTime());
                        fes.addEvent(new Event(Event.SERVICE, t + serviceDist.nextRandom(), ps));
                        //Schedule Next Spontaneous Service
                        fes.addEvent(new Event(Event.SPONTANEOUSSERV, t + spontaneousavDist.nextRandom()));
                    } 
            } else if (e.getType() == Event.ABANDONMENT){
                if (holdingQueue.getSize() > 0) {
                    holdingQueue.removeFirstPatient();
                    //schedule next abadonment
                    }
                fes.addEvent(new Event(Event.ABANDONMENT, t + abandonmentDist.nextRandom())); 
                
            }
            

        }

        return results;
    }
    public double dec( double a, int i ){
        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
    }
    
    @Override
    public int getType(){
        return 2;
    }

    public static void main(String[] args) {
        double lambda = 2;
        double mu = 1;
        double delta = 0.75;
        double p = 0.25;
        int s = 9;
        int n = 100;
        Jennings_Simulation jv = new Jennings_Simulation(lambda,mu,delta,s,n,p);
  for( int i = 0; i < 1; i++ ){
        SimResults results = jv.simulate(10000000);
        System.out.println(results.getCIdelayProbability()[0] + "\t"+results.getCIdelayProbability()[1] + "||\t"+  results.getCIWaitingTime()[0]+"\t"+  results.getCIWaitingTime()[1] +
                "||\t" + results.getCISojournTime()[0]+"\t"+  results.getCISojournTime()[1] 
            );
        }
    }


}