package statistics;
import org.apache.commons.math3.util.*;

/**
 * Encapsulates formulas that can be used throughout the simulations.
 */
public class commons {

    /**
     *
     * @param numServers
     * @param rho - lambda/mu, where mu is the service rate of a single server.
     * @return
     */
    public static double erlangCFormula( int numServers, double rho)
    {
//        double rhoM = rho/numServers;

        double dnom = 0;
        for( int i = 0 ; i < numServers ; i++ )
        {
            dnom += Math.pow(rho, i)/CombinatoricsUtils.factorial(i);
        }
        dnom += (1/(1-rho/numServers))*Math.pow(rho, numServers)/CombinatoricsUtils.factorial(numServers);
        return (1/(1-rho/numServers)*Math.pow(rho, numServers)/CombinatoricsUtils.factorial(numServers) )/dnom;
    }

    public static double coefficientOfVariation( double[] x ){
        double mean = 0;
        double unbiasedStd = 0;

        for(int i = 0 ; i < x.length ; i++ )
        {
            mean += x[i];
        }
        mean = mean/x.length;
        for( int i = 0 ; i < x.length ; i++ )
        {
            unbiasedStd += (x[i] - mean) * (x[i] - mean);
        }
        unbiasedStd = Math.sqrt(unbiasedStd/(x.length - 1));
        return unbiasedStd/mean;
    }

}
