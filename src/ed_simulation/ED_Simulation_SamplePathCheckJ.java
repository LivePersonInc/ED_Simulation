package ed_simulation;

import statistics.*;
import java.util.*;

/**
 *
 * @author bmathijs
 */
public class ED_Simulation_SamplePathCheckJ {

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

    public ED_Simulation_SamplePathCheckJ(double lambda, double mu, double delta, int s, int n, double p) {
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
        int nurseH = 5;
        int nurseJ = 5;
        int contentH = 5;
        int contentJ = 5;

        double t = 0;

        int hold;
        int nQ;
        int qQ;
        int cQ;

        boolean[][][] b0 = new boolean[2][3][3];
        boolean[][][] b1 = new boolean[2][3][3];

        boolean[][][] a0 = new boolean[2][3][3];
        boolean[][][] a1 = new boolean[2][3][3];

        boolean[][][] c0 = new boolean[2][3][3];
        boolean[][][] c1 = new boolean[2][3][3];

        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 3; j++) {
                for (int k = 0; k < 3; k++) {
                    b0[i][j][k] = false;
                    b1[i][j][k] = false;
                    a0[i][j][k] = false;
                    a1[i][j][k] = false;
                    c0[i][j][k] = false;
                    c1[i][j][k] = false;
                }
            }
        }

        while (t < maxTime) {

            Event e = getNextEvent(nurseH, contentH, nurseJ, contentJ, t);
            //System.out.println(e.getType());
            t = e.getTime();

            // System.out.println("H: " + holdingQueue.getSize() + " \t S: " + serviceQueue.getSize());
            if (e.getType() == Event.ARRIVAL) {

                if (nurseH + contentH == n) {
                    holding++;
                } else {
                    nurseH++;
                }

            } else if (e.getType() == Event.SERVICE) {

                nurseH--;
                nurseJ--;

                if (rng.nextDouble() < p) {
                    contentH++;
                    contentJ++;
                } else {
                    nurseJ++;
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

                nurseJ--;
                if (rng.nextDouble() < p) {
                    contentJ++;
                }else{
                    nurseJ++;
                }

            } else if (e.getType() == Event.CONTENT) {

                contentH--;
                contentJ--;
                nurseH++;
                nurseJ++;
                

            } else if (e.getType() == Event.CONTENTH) {
                contentH--;
                nurseH++;
            } else if (e.getType() == Event.CONTENTY) {
                contentJ--;
                nurseJ++;
            }

            //System.out.println("t: " + t + " type: " + e.getType() + "\t " + holding + " " + nurseH + " " + contentH + " || " + nurseJ + " " + contentJ + "\t|||\t" + (nurseH + contentH) + "\t" + (nurseJ + contentJ) + "\t" );

            //System.out.print("{" + holding + ","+ (nurseH - nurseJ)+"},");
            if ( (nurseH - nurseJ) > 5 ){ 
                System.out.print("{" + holding + ","+ (nurseH - nurseJ)+"},"); 
                System.out.println( "<==============");
            }
            //System.out.println();
            //System.out.print("t: "+t+" type: "+e.getType()+"\t "+holding+" "+nurseH+" "+contentH +" || "+nurseY+" "+contentY + "\t|||\t" + (nurseH+contentH)+ "\t"+ (nurseY+contentY) );

           
            
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
        System.out.println(c0[0][0][0] + "\t" + c0[0][0][1] + "\t" + c0[0][0][2]);
        System.out.println(c0[0][1][0] + "\t" + c0[0][1][1] + "\t" + c0[0][1][2]);
        System.out.println(c0[0][2][0] + "\t" + c0[0][2][1] + "\t" + c0[0][2][2]);

        System.out.println();

        System.out.println(c0[1][0][0] + "\t" + c0[1][0][1] + "\t" + c0[1][0][2]);
        System.out.println(c0[1][1][0] + "\t" + c0[1][1][1] + "\t" + c0[1][1][2]);
        System.out.println(c0[1][2][0] + "\t" + c0[1][2][1] + "\t" + c0[1][2][2]);

        System.out.println();
        System.out.println();

        System.out.println(c1[0][0][0] + "\t" + c1[0][0][1] + "\t" + c1[0][0][2]);
        System.out.println(c1[0][1][0] + "\t" + c1[0][1][1] + "\t" + c1[0][1][2]);
        System.out.println(c1[0][2][0] + "\t" + c1[0][2][1] + "\t" + c1[0][2][2]);

        System.out.println();

        System.out.println(c1[1][0][0] + "\t" + c1[1][0][1] + "\t" + c1[1][0][2]);
        System.out.println(c1[1][1][0] + "\t" + c1[1][1][1] + "\t" + c1[1][1][2]);
        System.out.println(c1[1][2][0] + "\t" + c1[1][2][1] + "\t" + c1[1][2][2]);

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
        ED_Simulation_SamplePathCheckJ ed = new ED_Simulation_SamplePathCheckJ(1, 1, 0.5, 5, 10, 0.7);
        ed.simulate(100000000);

    }

}
