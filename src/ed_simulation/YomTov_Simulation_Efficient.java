package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class YomTov_Simulation_Efficient extends Sim{

    protected ExponentialDistribution expDist;
    protected int s;
    protected int n;
    protected double p;
    protected double lambda;
    protected double mu;
    protected double delta;
    Random rng = new Random();

    public YomTov_Simulation_Efficient(double lambda, double mu, double delta, int s, int n, double p) {
        this.lambda = lambda;
        this.mu = mu;
        this.delta = delta;
        this.s = s;
        this.n = n;
        this.p = p;
        this.expDist = new ExponentialDistribution(1,rng);
    }

    public SimResults simulate(double maxTime) {
        SimResults results = new SimResults(n);
        int holdingQueue = 0;
        int serviceQueue = 0;
        int contentQueue = 0;
        double t = 0;

        // Schedule first event

        while (t < maxTime) {

            Event e = nextEvent(serviceQueue,contentQueue);
            t += e.getTime();
            results.registerQueueLengths(holdingQueue, serviceQueue, contentQueue,t);

            // System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {
                
                if (serviceQueue + contentQueue < n) {
                    serviceQueue++;
                    results.registerOneHold(false);
                    results.registerOneWait(serviceQueue > s);
                } else {
                    results.registerOneHold(true);
                }

            } else if (e.getType() == Event.SERVICE) {

                serviceQueue--;

                // check service queue 

                double U = rng.nextDouble();
                if (U < p) {
                    contentQueue++;
                } else {
                    if (holdingQueue > 0) {
                        holdingQueue--;
                        serviceQueue++;
                        results.registerOneWait(serviceQueue > s);
                    }
                }

            } else if (e.getType() == Event.CONTENT) {
                
                contentQueue--;
                serviceQueue++;
                results.registerOneWait(serviceQueue > s);
            }

        }

        return results;
    }
    
    public Event nextEvent(int sQ, int  cQ){
        double U = rng.nextDouble();
        double time = expDist.nextRandom() / (lambda + mu*Math.min(s,sQ)+ delta* cQ) ;
        if( U <= lambda / (lambda + mu*Math.min(s,sQ)+ delta* cQ) ){
            return new Event(Event.ARRIVAL, time);
        }else if ( U <= (lambda+mu*Math.min(s,sQ)) / (lambda + mu*Math.min(s,sQ)+ delta* cQ)){
            return new Event(Event.SERVICE,time);
        }else{
            return new Event(Event.CONTENT,time);
        }
    }
    
    public double dec( double a, int i ){
        return (int)(Math.pow(10,i)*a)/Math.pow(10,i);
    }
        
    
    @Override
    public int getType(){
        return 1;
    }
    
    
    public static void main(String[] args) {
        
        double b = 0.5;
        double g = 1.5;
        double mu = 1;
        double delta = 0.2;
        double p = 0.5;
        double r = delta / (delta + p*mu);
        
//        for(double l = 1; l <= 100; l++ ){
//            
//            double R = l / ((1-p)*mu);
//            int s = (int)(Math.round(R + b*Math.sqrt(R)));
//            int n = (int)(Math.round(R/r+g*Math.sqrt(R/r)));
//            YomTov_Simulation ed = new YomTov_Simulation(l, mu, delta, s, n, p);
//            SimResults sr = ed.simulate(100000);
//            System.out.println(l+"\t"+sr.getWaitingProbability());
//        }
//        
        YomTov_Simulation_Efficient ed = new YomTov_Simulation_Efficient(1.95, 1, 0.2, 5, 15, 0.5);
        SimResults sr = ed.simulate(1000000);

        System.out.println("Mean hold:" + sr.getMeanHoldingTime());
        System.out.println("Mean sojourn:" + sr.getMeanSojournTime());
        System.out.println("Mean wait:" + sr.getMeanWaitingTime());
        
        System.out.println("Wait probability: "+sr.getHoldingProbability());
        
        double[] probs = sr.getServiceQueueLengthProbabilities();
        for( int i=0; i < probs.length; i++ ){
            System.out.println("{"+i+","+ed.dec(probs[i],4)+"},");
        }
    }

}
