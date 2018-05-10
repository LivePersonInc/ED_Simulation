package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class ED_Simulation_TimeVarying_Randomized {

    protected ExponentialDistribution expDist;
    protected double[] s;
    protected double[] n;
    protected double p;
    protected double[] lambda;
    protected double mu;
    protected double delta;
    double cycleLength;
    double intervalLength;
    int nPeriods;
    Random rng = new Random();
    int nBeds;
    int servers;

    public ED_Simulation_TimeVarying_Randomized(double[] lambda, double cycleLength, double mu, double delta, double[] s, double[] n, double p) {
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
    }

    public SimResults_TimeVarying simulate(double maxTime) {
        SimResults_TimeVarying results = new SimResults_TimeVarying(10000, nPeriods);
        int holdingQueue = 0;
        int serviceQueue = 0;
        int contentQueue = 0;
        double t = 0;

        double nextIntervalStart = intervalLength;
        int interval = 0;
        nBeds = choose_nBeds(n[0]);
        servers = choose_nBeds(s[0]);
        
        // Schedule first event
        while (t < maxTime) {

            if( t > nextIntervalStart ){
                nextIntervalStart += intervalLength;
                interval = (interval+1)%nPeriods;
                int newBeds = choose_nBeds(n[interval]);
                servers = choose_nBeds(s[interval]);
                int addedBeds = newBeds - nBeds;
                while( addedBeds > 0 && holdingQueue > 0 ){
                    addedBeds--;
                    holdingQueue--;
                    serviceQueue++;
                    results.registerOneWait(serviceQueue > servers, interval);
                } 
                nBeds = newBeds;
                //System.out.println(interval +"\t"+ nBeds);
            }
            
            Event e = nextEvent(serviceQueue, contentQueue, interval);
            t += e.getTime();
            results.registerQueueLengths(holdingQueue, serviceQueue, contentQueue, t, interval);
            //System.out.println(t+"\t"+ e.getType()+"\t"+holdingQueue+"\t"+serviceQueue+"\t"+contentQueue +"\t"+s[interval]+"\t"+n[interval]);
            //System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {

                if (serviceQueue + contentQueue < nBeds) {
                    serviceQueue++;
                    results.registerOneHold(false, interval);
                    results.registerOneWait(serviceQueue > servers, interval);
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
                    if (holdingQueue > 0 && serviceQueue + contentQueue < nBeds) {
                        holdingQueue--;
                        serviceQueue++;
                        results.registerOneWait(serviceQueue > servers, interval);
                    }
                }

            } else if (e.getType() == Event.CONTENT) {

                contentQueue--;
                serviceQueue++;
                results.registerOneWait(serviceQueue > servers, interval);
            } 

        }

        return results;
    }

    public Event nextEvent(int sQ, int cQ, int i) {
        double U = rng.nextDouble();
        double time = expDist.nextRandom() / (lambda[i] + mu * Math.min(servers, sQ) + delta * cQ);
        //System.out.println(((int)((t + time) / intervalLength)) +"\t"+ ((int)(t / intervalLength)));
            if (U <= lambda[i] / (lambda[i] + mu * Math.min(servers, sQ) + delta * cQ)) {
                return new Event(Event.ARRIVAL, time);
            } else if (U <= (lambda[i] + mu * Math.min(servers, sQ)) / (lambda[i] + mu * Math.min(servers, sQ) + delta * cQ)) {
                return new Event(Event.SERVICE, time);
            } else {
                return new Event(Event.CONTENT, time);
            }
    }
    
    public int choose_nBeds( double n ){
        int floor = (int)(n);
        double prob = n - floor;
        if( rng.nextDouble() > prob ){
            return floor;
        } else{
            return floor+1;
        }  
    }

    public double dec(double a, int i) {
        return (int) (Math.pow(10, i) * a) / Math.pow(10, i);
    }

    public int getType() {
        return 0;
    }

    public static void main(String[] args) {

        double[] lambda = {39.779,36.8202,33.9883,31.3998,29.1713,28.1962,27.5035,26.8987,26.1878,24.3413,22.3343,20.3065,18.3978,17.1171,16.0877,15.3017,14.7514,14.5714,14.5546,14.6361,14.7514,14.6672,14.5546,14.416,14.2541,13.9097,13.6119,13.428,13.4254,13.4643,13.9019,14.8886,16.5746,19.0193,22.5,27.203,33.3149,42.9515,53.5981,64.6694,75.5801,85.104,93.5532,100.599,105.912,106.942,106.471,105.057,103.26,103.147,103.166,103.274,103.425,103.596,103.715,103.731,103.591,102.746,101.84,101.023,100.442,100.872,101.581,102.467,103.425,104.89,106.005,106.452,105.912,102.532,98.1423,93.0378,87.5138,81.7813,76.2535,71.2591,67.1271,65.8115,65.366,65.4696,65.8011,65.2948,64.672,63.9093,62.9834,61.7287,60.3211,58.7945,57.1823,55.732,54.1782,52.4689,50.5525,48.0792,45.4144,42.6252};
        
        double delta = 1.5;
        double p = 0.75;
        double mu= 6;
        double r = delta / (delta + p * mu);

        double[] s = {46.936,45.4782,43.9691,42.4402,40.9238,39.5294,38.2709,37.0983,35.9769,34.7882,33.5136,32.2,30.8744,29.6009,28.4011,27.2695,26.2115,25.2476,24.3791,23.5935,22.8801,22.2089,21.5701,20.9662,20.3945,19.8358,19.2928,18.7835,18.3235,17.9098,17.5701,17.3537,17.3086,17.4779,17.9207,18.7077,19.9101,21.791,24.3535,27.433,30.907,34.5817,38.3301,42.0618,45.6578,48.7805,51.3683,53.5405,55.3721,57.0916,58.7581,60.3244,61.788,63.1533,64.4211,65.5878,66.6482,67.5486,68.2957,68.9362,69.5069,70.105,70.7539,71.4285,72.1182,72.8597,73.6186,74.311,74.8574,75.0292,74.7786,74.1773,73.2633,72.0725,70.6627,69.1108,67.5031,66.0874,64.9187,63.9242,63.0658,62.2296,61.3823,60.5408,59.6978,58.8287,57.9257,56.9926,56.0307,55.0622,54.0838,53.0756,52.0223,50.8804,49.6422,48.324};
        
        double[] n = {175.484, 170.531, 165.346, 159.989, 154.538, 149.184, 144.076, 
139.223, 134.599, 130.039, 125.403, 120.686, 115.909, 111.164, 
106.54, 102.078, 97.8137, 93.799, 90.0695, 86.6291, 83.4685, 80.5447, 
77.8159, 75.2631, 72.8682, 70.5949, 68.421, 66.3589, 64.4331, 
62.6551, 61.0638, 59.7509, 58.8323, 58.4353, 58.7157, 59.8668, 
62.1051, 65.9063, 71.6151, 79.2005, 88.5209, 99.2474, 111., 123.435, 
136.166, 148.499, 159.874, 170.144, 179.272, 187.522, 195.154, 
202.226, 208.791, 214.895, 220.567, 225.824, 230.668, 235.033, 
238.877, 242.251, 245.227, 247.979, 250.656, 253.304, 255.946, 
258.651, 261.423, 264.141, 266.63, 268.479, 269.344, 269.138, 
267.834, 265.456, 262.092, 257.9, 253.093, 248.133, 243.407, 239.026, 
235.034, 231.318, 227.756, 224.315, 220.954, 217.615, 214.246, 
210.822, 207.328, 203.781, 200.191, 196.531, 192.767, 188.821, 
184.631, 180.185};   
        ED_Simulation_TimeVarying_Randomized ed = new ED_Simulation_TimeVarying_Randomized(lambda, 24, mu, delta, s,  n,  p);

        
        SimResults_TimeVarying sr = ed.simulate(100000);
        double[] waitProb = sr.getWaitingProbability();
        for (int i = 0; i < waitProb.length; i++) {
            System.out.println(i + "\t" + waitProb[i]);
        }

    }

}
