package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class ED_Simulation_SamplePathCheck {

    protected ExponentialDistribution arrivalDist;
    protected ExponentialDistribution serviceDist;
    protected ExponentialDistribution contentDist;
    protected int s;
    protected int n;
    protected double p;
    protected double lambda;
    protected double mu;
    protected double delta;
    Random rng = new Random();
    Distribution expDist = new ExponentialDistribution(1, rng);

    public ED_Simulation_SamplePathCheck(double lambda, double mu, double delta, int s, int n, double p) {
        this.arrivalDist = new ExponentialDistribution(lambda, rng);
        this.serviceDist = new ExponentialDistribution(mu, rng);
        this.contentDist = new ExponentialDistribution(delta, rng);
        this.expDist = new ExponentialDistribution(1, rng);
        this.s = s;
        this.n = n;
        this.p = p;
        this.lambda = lambda;
        this.mu = mu;
        this.delta = delta;
    }

    public void simulate(double maxTime) {
        int holding = 0;
        int nurseH = 0;
        int nurseY = 0;
        int contentH = 0;
        int contentY = 0;

        double t = 0;

        int hold;
        int nQ;
        int qQ;
        int cQ;

        boolean[] checkedH = new boolean[n+1];
        boolean[] checkedY = new boolean[n+1];
        boolean[] checkedDiff = new boolean[n+1];

        while (t < maxTime) {

            Event e = getNextEvent(nurseH, contentH, nurseY, contentY, t);
            //System.out.println(e.getType());
            t = e.getTime();

            // System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {

                if (nurseH + contentH == n) {
                    holding++;
                } else {
                    nurseH++;
                }

                if (nurseY + contentY < n) {
                    nurseY++;
                }

            } else if (e.getType() == Event.SERVICE) {

                nurseH--;
                nurseY--;

                if (rng.nextDouble() < p) {
                    contentH++;
                    contentY++;
                } else {
                    if (holding > 0) {
                        holding--;
                        nurseH++;
                    }
                }
            } else if (e.getType() == Event.SERVICEH) {

                nurseH--;

                if (rng.nextDouble() < p) {
                    contentH++;
                } else {
                    if (holding > 0) {
                        holding--;
                        nurseH++;
                    }
                }
            } else if (e.getType() == Event.SERVICEY) {

                nurseY--;
                if (rng.nextDouble() < p) {
                    contentY++;
                }

            } else if (e.getType() == Event.CONTENT) {

                contentH--;
                contentY--;
                nurseH++;
                nurseY++;

            } else if (e.getType() == Event.CONTENTH) {
                contentH--;
                nurseH++;
            } else if (e.getType() == Event.CONTENTY) {
                contentY--;
                nurseY++;
            }

            //System.out.println("t: " + t + " type: " + e.getType() + "\t " + holding + " " + nurseH + " " + contentH + " || " + nurseY + " " + contentY + "\t|||\t" + (nurseH + contentH) + "\t" + (nurseY + contentY) + "\t" + ((nurseH + contentH) >= (nurseY + contentY)));

            //System.out.print("t: " + t + " type: " + e.getType() + "\t " + holding + " " + nurseH + " " + contentH + " || " + nurseY + " " + contentY + "\t|||\t" + (nurseH + contentH) + "\t" + (nurseY + contentY) + "\t" + ((nurseH + contentH) >= (nurseY + contentY)));
            //System.out.println(" \t" + (nurseY-nurseH));
                
            if ( nurseH  < nurseY ) {
               
                checkedH[ nurseH ] = true;
                checkedY[ nurseY ] = true;
                checkedDiff[ nurseY - nurseH ] = true;
                
            }
            
            
            
            
            //System.out.println(" _____________________ "+ (contentH - contentY) + "   " + holding + "   " + ((holding < contentH-contentY)&&(holding>0)) );

            //if ( nurseH < nurseY-1 ) System.out.println(nurseH - nurseY);
        }

//        for(int i =0;i<2;i++){
//            for(int j = 0; j < 3; j++){
//                for(int k = 0; k < 3; k++){
//                    System.out.println("("+i+","+j+","+k+")  "+b[i][j][k]);
//                }
//            }
//        }
//        
        
        for(int i = 0; i < n+1; i++ ){
            System.out.println(i+ "\t"+checkedH[i]+"\t"+checkedY[i]+"\t"+checkedDiff[i]);
        }
        

    }

    public Event getNextEvent(int QNH, int QCH, int QNY, int QCY, double t) {
        double totalRate = Math.min(Math.max(QNH, QNY), s) * mu + Math.max(QCH, QCY) * delta + lambda;
        double time = t + expDist.nextRandom() / totalRate;
        double U = rng.nextDouble() * totalRate;
        int type;

        if (U < lambda) {
            type = Event.ARRIVAL;
        } else if (U < lambda + Math.min(Math.max(QNH, QNY), s) * mu) {

            if (QNH >= s && QNY >= s) {
                type = Event.SERVICE;
            } else if (QNH >= s && QNY < s) {
                if (rng.nextDouble() < QNY / s) {
                    type = Event.SERVICE;
                } else {
                    type = Event.SERVICEH;
                }
            } else if (QNH < s && QNY >= s) {
                if (rng.nextDouble() < QNH / s) {
                    type = Event.SERVICE;
                } else {
                    type = Event.SERVICEY;
                }
            } else {
                if (QNH == QNY) {
                    type = Event.SERVICE;
                } else if (QNH < QNY) {
                    if (rng.nextDouble() < QNH / QNY) {
                        type = Event.SERVICE;
                    } else {
                        type = Event.SERVICEY;
                    }
                } else {
                    if (rng.nextDouble() < QNY / QNH) {
                        type = Event.SERVICE;
                    } else {
                        type = Event.SERVICEH;
                    }
                }

            }
        } else {
            if (QCH == QCY) {
                type = Event.CONTENT;
            } else if (QCH < QCY) {
                if (rng.nextDouble() < QCH / QCY) {
                    type = Event.CONTENT;
                } else {
                    type = Event.CONTENTY;
                }
            } else {
                if (rng.nextDouble() < QCY / QCH) {
                    type = Event.CONTENT;
                } else {
                    type = Event.CONTENTH;
                }
            }

        }
        return new Event(type, time);

    }

    public static void main(String[] args) {
        ED_Simulation_SamplePathCheck ed = new ED_Simulation_SamplePathCheck(10, 1, 0.2, 24,75, 0.5);
        ed.simulate(1000000);

    }

}
