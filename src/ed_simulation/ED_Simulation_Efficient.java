package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class ED_Simulation_Efficient extends Sim {

    protected ExponentialDistribution expDist;
    protected int s;
    protected int n;
    protected double p;
    protected double lambda;
    protected double mu;
    protected double delta;
    Random rng = new Random();
    

    public ED_Simulation_Efficient(double lambda, double mu, double delta, int s, int n, double p) {
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
                    holdingQueue++;
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
        return 0;
    }

    public static void main(String[] args) {
        
        double lambda =  2;
        double mu = 1;
        double delta = 0.25;
        double p = 0.75;
        double r = delta / (delta + p*mu);
        
        int n = 36;
        
        for( int s = 1; s <= 16; s ++ ){
        ED_Simulation_Efficient ed = new ED_Simulation_Efficient(lambda, mu,delta,s,n,p);
        SimResults sr = ed.simulate(1000000);
        double[] ci = sr.getCIdelayProbability();
        System.out.print(" { "+s+ " , " + ed.dec(0.5*(ci[0]+ci[1]),5) + " , ");
        System.out.println(sr.getHoldingProbability2() + " }");
        }
    }

        
    }


