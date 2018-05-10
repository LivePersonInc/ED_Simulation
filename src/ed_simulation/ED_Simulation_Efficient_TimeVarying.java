package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class ED_Simulation_Efficient_TimeVarying {

    protected ExponentialDistribution expDist;
    protected int[] s;
    protected int[] n;
    protected double p;
    protected double[] lambda;
    protected double mu;
    protected double delta;
    double cycleLength;
    double intervalLength;
    int nPeriods;
    Random rng = new Random();

    public ED_Simulation_Efficient_TimeVarying(double[] lambda, double cycleLength, double mu, double delta, int[] s, int[] n, double p) {
        this.lambda = lambda;
        this.cycleLength = cycleLength;
        this.intervalLength = cycleLength / lambda.length;
        this.nPeriods = lambda.length;
        this.mu = mu;
        this.delta = delta;
        this.s = s;
        this.n = n;
        this.p = p;
        this.expDist = new ExponentialDistribution(1, rng);
        System.out.println(intervalLength);
    }

    public SimResults_TimeVarying simulate(double maxTime) {
        SimResults_TimeVarying results = new SimResults_TimeVarying(10000, nPeriods);
        int holdingQueue = 0;
        int serviceQueue = 0;
        int contentQueue = 0;
        double t = 0;

        int print = 100000;
        // Schedule first event
        while (t < maxTime) {

            if( t > print ){
                System.out.println(print);
                print += 100000;
            }
            
            int interval = (int) ((t % cycleLength) / intervalLength);
            Event e = nextEvent(serviceQueue, contentQueue, interval, t);
            t += e.getTime();
            results.registerQueueLengths(holdingQueue, serviceQueue, contentQueue, t, interval);
            System.out.println(t+"\t"+interval+"\t"+ e.getType()+"\t"+holdingQueue+"\t"+serviceQueue+"\t"+contentQueue +"\t"+s[interval]+"\t"+n[interval]);
            //System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {

                if (serviceQueue + contentQueue < n[interval]) {
                    serviceQueue++;
                    results.registerOneHold(false, interval);
                    results.registerOneWait(serviceQueue > s[interval], interval);
                } else {
                    holdingQueue++;
                    results.registerOneHold(true, interval);
                }

            } else if (e.getType() == Event.SERVICE) {

                serviceQueue--;

                // check service queue 
                double U = rng.nextDouble();
                if (U < p) {
                    contentQueue++;
                } else {
                    if (holdingQueue > 0 && serviceQueue + contentQueue < n[interval]) {
                        holdingQueue--;
                        serviceQueue++;
                        results.registerOneWait(serviceQueue > s[interval], interval);
                    }
                }

            } else if (e.getType() == Event.CONTENT) {

                contentQueue--;
                serviceQueue++;
                results.registerOneWait(serviceQueue > s[interval], interval);
            } else if (e.getType() == -1) {
                // Reevaluate number of beds 
                int newInterval = (interval + 1) % nPeriods;
                int addedBeds = n[newInterval] - n[interval];
                while( addedBeds > 0 && holdingQueue > 0 ){
                    addedBeds--;
                    holdingQueue--;
                    serviceQueue++;
                    results.registerOneWait(serviceQueue > s[newInterval], newInterval);
                }

            }

        }

        return results;
    }

    public Event nextEvent(int sQ, int cQ, int i, double t) {
        double U = rng.nextDouble();
        double time = expDist.nextRandom() / (lambda[i] + mu * Math.min(s[i], sQ) + delta * cQ);
        //System.out.println(((int)((t + time) / intervalLength)) +"\t"+ ((int)(t / intervalLength)));
        if ((int) ((t + time)  / intervalLength) > (int) (t / intervalLength)) {
            //System.out.println("++++++++++++++++++++++++++++++++++++++++++++++++++++");
            return new Event(-1, Math.ceil(t / intervalLength) * intervalLength - t + 0.000000000001);
        } else {
            if (U <= lambda[i] / (lambda[i] + mu * Math.min(s[i], sQ) + delta * cQ)) {
                return new Event(Event.ARRIVAL, time);
            } else if (U <= (lambda[i] + mu * Math.min(s[i], sQ)) / (lambda[i] + mu * Math.min(s[i], sQ) + delta * cQ)) {
                return new Event(Event.SERVICE, time);
            } else {
                return new Event(Event.CONTENT, time);
            }
        }
    }

    public double dec(double a, int i) {
        return (int) (Math.pow(10, i) * a) / Math.pow(10, i);
    }

    public int getType() {
        return 0;
    }

    public static void main(String[] args) {

        double[] lambda = 
                {   119,
            92,
            62,
            45,
            31,
            23,
            32,
            22,
            15,
            17,
            32,
            63,
            109,
            141,
            148,
            157,
            124,
            124,
            117,
            125,
            157,
            156,
            140,
            122};
//        {   11.9,
//            9.2,
//            6.2,
//            4.5,
//            3.1,
//            2.3,
//            3.2,
//            2.2,
//            1.5,
//            1.7,
//            3.2,
//            6.3,
//            10.9,
//            14.1,
//            14.8,
//            15.7,
//            12.4,
//            12.4,
//            11.7,
//            12.5,
//            15.7,
//            15.6,
//            14,
//            12.2};
        
        
                
                
        double mu = 10.9;
        double delta = 2.3;
        double p = 0.69697;
        double r = delta / (delta + p * mu);

        int[] s = {47, 45, 40, 33, 26, 20, 15, 14, 12, 10, 8, 10, 16, 25, 36, 43, 48, 
47, 45, 44, 43, 48, 51, 50};
        int[] n = {1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000,1000};
                
                
                
        ED_Simulation_Efficient_TimeVarying ed = new ED_Simulation_Efficient_TimeVarying(lambda, 24, mu, delta, s,  n,  p);

        
        SimResults_TimeVarying sr = ed.simulate(200000);
        double[] waitProb = sr.getWaitingProbability();
        for (int i = 0; i < waitProb.length; i++) {
            System.out.println(i + "\t" + waitProb[i]);
        }

    }

}
