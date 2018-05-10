/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package statistics;
import java.util.Random;
/**
 *
 * @author mboon
 */
public class GammaDistribution extends Distribution {

    protected double alpha, beta;
    
    public GammaDistribution(double alpha, double beta, Random random) {
        this.alpha = alpha;
        this.beta = beta;
        this.random = random;
    }
    
    @Override
    public double expectation() {
        return alpha/beta;
    }

    @Override
    public double variance() {
        return alpha/beta/beta;
    }
    
    @Override
    public double nextRandom() {
        int k = (int) Math.floor(alpha);
        double a = alpha - k;
        double y = 0;
        double e = Math.E;
        if (a > 0) {
            while (y == 0) {
                double u = random.nextDouble();
                double p = u * (e + a) / e;
                if (p <= 1) {
                    double x = Math.pow(p, 1/a);
                    double u1 = random.nextDouble();
                    if (u1 <= Math.exp(-x)) { // Accept
                        y = x;
                    }
                }
                else {
                    double x = -Math.log(p/a);
                    double u1 = random.nextDouble();
                    if (u1 <= Math.pow(x, a-1)) { // Accept
                        y = x;
                    }   
                }    
            }
        }
        double product = 1;
        for (int i = 0; i < k; i++) {
            product = product * random.nextDouble();
        }
        return ((y - Math.log(product))/beta);
    }
    
    public static void main(String[] arg) {
        double sumX = 0;
        double sumX2 = 0;
        GammaDistribution dist = new GammaDistribution(3, 4, new Random());
        int n = 100000;
        for (int i = 0; i < n; i++) {
            double r = dist.nextRandom();
            sumX += r;
            sumX2 += r*r;
        }
        double EX = sumX/n;
        double EX2 = sumX2/n;
        System.out.println("E[X] = "+EX);
        System.out.println("Var[X] = "+(EX2-EX*EX));
    }
    
}
