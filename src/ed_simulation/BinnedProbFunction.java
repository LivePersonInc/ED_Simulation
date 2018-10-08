package ed_simulation;

import java.lang.Math;

import java.util.Random;

public class BinnedProbFunction {

    protected int timeBinSize;
    protected double[] vals;
    Random rng;


    public BinnedProbFunction( int timeBinSize, double[] vals)
    {
        this.timeBinSize = timeBinSize;
        this.vals = vals;
        this.rng = new Random();
    }

    public boolean isTrue( double time )
    {
        double U = rng.nextDouble();
        int bin = (int)(Math.floor(time/timeBinSize));
        int numVals = vals.length;
        double prob =  bin > numVals - 1 ? vals[numVals - 1] : vals[bin];
        return U < prob;
    }

}
