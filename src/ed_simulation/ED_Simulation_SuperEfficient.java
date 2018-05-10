package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class ED_Simulation_SuperEfficient  {

    protected ExponentialDistribution expDist;
    protected int s;
    protected int n;
    protected double p;
    protected double lambda;
    protected double mu;
    protected double delta;
    Random rng = new Random();
    

    public ED_Simulation_SuperEfficient(double lambda, double mu, double delta, int s, int n, double p) {
        this.lambda = lambda;
        this.mu = mu;
        this.delta = delta;
        this.s = s;
        this.n = n;
        this.p = p;
        this.expDist = new ExponentialDistribution(1,rng);
    }

    public double simulate(double maxTime) {
        int holdingQueue = 0;
        int serviceQueue = 0;
        int contentQueue = 0;
        double t = 0;
        int countHold = 0;
        int countWait = 0;
        int countAllWait = 0;
        int countAllHold = 0;

        // Schedule first event

        while (t < maxTime) {

            Event e = nextEvent(serviceQueue,contentQueue);
            t += e.getTime();
            

            // System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {
                countAllHold++;
                if (serviceQueue + contentQueue < n) {
                    serviceQueue++;
                    countAllWait++;
                    if( serviceQueue > s ) countWait++;
                    
                } else {
                    holdingQueue++;
                    countHold++;
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
                        countAllWait++;
                    if( serviceQueue > s ) countWait++;
                    }
                }

            } else if (e.getType() == Event.CONTENT) {
                
                contentQueue--;
                serviceQueue++;
                countAllWait++;
                if( serviceQueue > s ) countWait++;
            }
            
            //if ( countAllWait % 10000 == 0 ) System.out.println( 1.0*countWait/countAllWait);

        }
        

        return (1.0*countWait/countAllWait);
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
    

    public int getType(){
        return 0;
    }

    public static void main(String[] args) {
        
        double lambda = 250;
        double mu = 1;
        double delta = 0.5;
        double p = 0.5;
        double r = delta / (delta + p*mu);
        
        int s = 511;
        int[] n = {930,940,950,960,970,980,990,1000,1010,1020,1030,1040,1050,1060,1070};
  
        System.out.println(s);
        for( int  i = 0; i < n.length; i++ ){
            ED_Simulation_SuperEfficient ed = new ED_Simulation_SuperEfficient(lambda, mu,delta,s,n[i],p);
            System.out.println(n[i]+"\t \t"+ed.simulate(200000));
            //double[] ci = sr.getCIdelayProbability();
            //System.out.println(n[i] + "\t \t"+ sr.getWaitingProbability());
        }

        
    }


}
