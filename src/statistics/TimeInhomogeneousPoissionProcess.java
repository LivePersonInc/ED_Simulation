package statistics;

import java.security.InvalidParameterException;
import java.util.*;
import java.lang.Math;


// Represents a time inhomogeneous poisson process. Implementation based on thinning a time homogeneous process.
// Params are assumed periodic (e.g. daily with values specified per hour)
public class TimeInhomogeneousPoissionProcess {

    private int[] bins;
    private ExponentialDistribution maxLambdaExp;
    private double[] diluteProbs;
    private UniformDistribution unf;
    /**
     *
     * @param lambdas - n-vector representing the time-varying rates of the process.
     * @param times - n+1-array representing the time intervals corresponding to the lambdas. Entry i represents the left edge of the timebin.
     *             The last entry represents the rightmost bin edge, and is assumed to be equal (mod itself) to the first entry, which must be 0.
     *              The bins are assumed left closed and right opened. For example, if times = <0,1,2> this means that in the interval [0, 1) the rate is
     *              lamndas[0], and in [1,2) the rate is lambda[1]
     * @param - randSeedExp, randSeedUnif - optionally specify a random seed so as to be able to reproduce the run
     *
     * For example, if times = <0,15,20,24>, and lambda = <0.2,0.3,0.5> this means that in the time interval [0,15) the rate is 0.2,
     * in the interval [15,20) - 0.3, and in [20,24] - 0.5
     */
    public TimeInhomogeneousPoissionProcess( int[] times, double[] lambdas, Long randSeedExp, Long randSeedUnif) throws Exception {
        if( times.length != lambdas.length + 1 )
        {
            throw new Exception(" times.length != lambdas.size() + 1!! Aborting...");
        }
        if(times[0] != 0)
        {
            throw new Exception("times[0] should equal 0, since we're working mod(times[-1]), so exceeding the last entry we get back to 0. ");
        }
        this.bins = times;
        double lambda  = findMax(lambdas);

        this.diluteProbs = new double[lambdas.length];
        for( int i = 0 ; i < lambdas.length ; i++ )
        {
            diluteProbs[i] = lambdas[i]/lambda;
        }
        Random rndExp = randSeedExp != null ? new Random( randSeedExp.longValue() ) : new Random();
        this.unf = new  UniformDistribution(0, 1, rndExp);
        Random rndUnif = randSeedUnif != null ? new Random( randSeedExp.longValue() ) : new Random();
        this.maxLambdaExp = new ExponentialDistribution(lambda, rndUnif);

    }

    public TimeInhomogeneousPoissionProcess( int[] times, double[] lambdas ) throws Exception {
        this(times, lambdas, null, null);
    }

    // Assumes this.bins represents a single period of a periodic function (e.g. 24 hours)
    private int findNextEventTimeBin( double eventTime ) throws Exception
    {
        //Avoid infinite loops in case of negative time
        if( eventTime < 0 )
        {
            throw new Exception("Current implementation doesn't support negative times!");
        }
        double eventTimeModPeriod = eventTime % bins[bins.length - 1];
        int i = 0;
        while( i <= bins.length - 2 && bins[i] <= eventTimeModPeriod ){
            i += 1;
        }
        return i-1;
        //The only inaccuracy here is when eventTimeModPeriod == bins[bins.length - 1] (e.g. 24), in which case we return 23,
        // whereas we should have returned 0, since the intervals are right-open, so 24 is in fact 0.
    }

    public double timeToNextEvent( double currEventTime ) throws Exception
    {
        double nextEventTime = currEventTime;
        boolean hasEventHappened;
        do{
            nextEventTime = nextEventTime + this.maxLambdaExp.nextRandom();
            if( Double.isInfinite(nextEventTime))
            {
                //This may happen, for example, as the abandonment time of a Patient with infinite patience.
                return nextEventTime;
            }
            int i = findNextEventTimeBin( nextEventTime );
//            System.out.println("currEventTime: " + currEventTime +   " nextEventTime: " + nextEventTime + " i: " + i);
            hasEventHappened = unf.nextRandom() <= diluteProbs[i];
        }while( ! hasEventHappened );
        return nextEventTime - currEventTime;

    }

    private double findMax(double[] vals) throws Exception
    {
        if( vals == null || vals.length == 0 )
        {
            throw new Exception("Can't find the max of a null or empty array!");
        }
        double ret = -Double.MAX_VALUE;
        for( int i = 0 ; i < vals.length; i++ )
        {
            ret = Math.max(ret, vals[i]);
        }
        return ret;
    }


}
