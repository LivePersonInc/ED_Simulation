package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class Jennings_Simulation_Efficient extends Sim {

    protected ExponentialDistribution expDist;
    protected int s;
    protected int n;
    protected double p;
    protected double mu;
    protected double delta;
    Random rng = new Random();

    public Jennings_Simulation_Efficient(double mu, double delta, int s, int n, double p) {
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
        int serviceQueue = n;
        int contentQueue = 0;
        double t = 0;

        // Schedule first event

        while (t < maxTime) {

            Event e = nextEvent(serviceQueue,contentQueue);
            t += e.getTime();
            results.registerQueueLengths(holdingQueue, serviceQueue, contentQueue,t);

            // System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
            if (e.getType() == Event.SERVICE) {

                serviceQueue--;

                // check service queue 

                double U = rng.nextDouble();
                if (U < p) {
                    contentQueue++;
                } else {
                    serviceQueue++;
                    results.registerOneWait(serviceQueue > s);
                }

            } else{          
                contentQueue--;
                serviceQueue++;
                results.registerOneWait(serviceQueue > s);
            }

        
        }
        return results;
    }
    
    public Event nextEvent(int sQ, int  cQ){
        double U = rng.nextDouble();
        double time = expDist.nextRandom() / (mu*Math.min(s,sQ)+ delta* cQ) ;
        if ( U <= (mu*Math.min(s,sQ)) / (mu*Math.min(s,sQ)+ delta* cQ)){
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
        return 2;
    }

    public static void main(String[] args) {
        Jennings_Simulation_Efficient ed = new Jennings_Simulation_Efficient( 1, 0.2, 5, 20, 0.5);
        SimResults sr = ed.simulate(10000000);

        System.out.println("Mean hold:" + sr.getMeanHoldingTime());
        System.out.println("Mean sojourn:" + sr.getMeanSojournTime());
        System.out.println("Mean wait:" + sr.getMeanWaitingTime());
        
        System.out.println("Wait probability: "+sr.getHoldingProbability());
        
        double[] probs = sr.getServiceQueueLengthProbabilities();
        for( int i=0; i < probs.length; i++ ){
            System.out.println(i+"\t"+ed.dec(probs[i],5));
            System.out.println((i+0.99)+"\t"+ed.dec(probs[i],5));
        }
    }

}
