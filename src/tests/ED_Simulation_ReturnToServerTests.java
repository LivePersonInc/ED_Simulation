package tests;

import ed_simulation.ED_Simulation_ReturnToServer;
import ed_simulation.ServerAssignmentMode;
import ed_simulation.SimResults;
import org.junit.Test;
import statistics.TimeInhomogeneousPoissionProcess;
import statistics.commons;
//import org.apache.commons.math3.
import javafx.util.Pair;
import java.io.FileWriter;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import  java.util.Vector;

import java.io.FileOutputStream;
//import org.apache.commons.math
import static statistics.commons.*;


import java.util.HashMap;

import static ed_simulation.ServerAssignmentMode.*;
import static org.junit.Assert.*;

public class ED_Simulation_ReturnToServerTests {

/*
    @SuppressWarnings("Duplicates")
    //Reduce the system to a standard single server queue M/M/1 (content and holding queues are constantly of 0 size)
    @Test
    public void _testSingleServer() {
        double lambda = 2;
        double mu = 2.5;
        double delta = 0.75;
        double p = 0; //No content phase - all serviced Patient leave after the first service period.
        double patienceTheta = 0;
        int s = 1; //Single server.
        int maxTotalCapacity = 5000000; //'Unlimited' server capacity.
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        ED_Simulation_ReturnToServer sim = null;
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {
            sim = new ED_Simulation_ReturnToServer(lambda,mu,delta,s,maxTotalCapacity,p, patienceTheta,  new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
            SimResults results = sim.simulate(10000000);
            double rho = lambda/mu;

            double theoreticalMeanNumberInsystem = rho/(1-rho);
            double empiricalMeanNumberInsystem = results.getMeanServiceQueueLength();
            assertEquals( 1, theoreticalMeanNumberInsystem/empiricalMeanNumberInsystem,   0.01);

            double empiricalMeanHolding = results.getMeanHoldingQueueLength(-1);
            assertEquals( 0, empiricalMeanHolding,   0.01);


            double theoreticalMeanWaitingTime = rho/((1-rho)*mu);
            double empiricalMeanWaitingTime = results.getMeanWaitingTime();
            assertEquals( 1, theoreticalMeanWaitingTime/empiricalMeanWaitingTime,   0.01);


            double theoreticalVarianceWaitingTime = (2-rho)*rho/((1-rho)*(1-rho)*mu*mu);
            double empiricalVarianceWaitingTime = results.getVarianceWaitingTime();
            assertEquals( 1, theoreticalVarianceWaitingTime/empiricalVarianceWaitingTime,   0.01);


          } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //Reduce the system to a standard multiple server queue M/M/s(content queue is constantly of size 0, service queues of each server is of size 1,
    // holding queue keeps all pending messages. )
    @Test
    public void _testMMs() {
        double lambda = 5;
        double mu = 1.05;
        double delta = 0.75;
        double p = 0; //No content phase - all serviced Patient leave after the first service period.
        double patienceTheta = 0;
        int s = 5; //multiple servers.
        int maxTotalCapacity = 1; //each server has a single job capacity.
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        ED_Simulation_ReturnToServer sim = null;
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {
            sim = new ED_Simulation_ReturnToServer(lambda, mu, delta, s, maxTotalCapacity, p, patienceTheta, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
            SimResults results = sim.simulate(10000000);

            double rho = lambda/mu;
            double c = commons.erlangCFormula(s, rho);


            double theoreticalMeanNumberOfBusyServers = rho;
            double empiricalMeanNumberOfBusyServers = results.getMeanServiceQueueLength();
            assertEquals( 1, theoreticalMeanNumberOfBusyServers/empiricalMeanNumberOfBusyServers,   0.01);



            double theoreticalMeanNumberInsystem = (rho)*(1+c/(s-rho));
            double empiricalMeanNumberInsystem = results.getMeanAllInSystem();
            assertEquals( 1, theoreticalMeanNumberInsystem/empiricalMeanNumberInsystem,   0.01);



            double theoreticalMeanWaitingTime = c/(mu*(s-rho));
            double empiricalMeanWaitingTime = results.getMeanHoldingTime(); //In this scenario, jobs wait only in the holding queue (which is the buffer of the M/M/s system)
            assertEquals( 1, theoreticalMeanWaitingTime/empiricalMeanWaitingTime,   0.01);




        } catch (Exception e) {
            e.printStackTrace();
        }

    }




    //Reduce the system to an Erlang-R system: holding queue size is constantly 0, infinite capacity per server.
    @Test
    public void _testErlangRSingleServer() {
        double lambda = 2;
        double mu = 4.1;
        double delta = 0.52;
        int s = 1; //Single server.
        int maxTotalCapacity = 5000000; //each server has 'unlimited' capacity.
        double p = 0.5;
        double patienceTheta = 0;
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        ED_Simulation_ReturnToServer sim = null;
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {

            sim = new ED_Simulation_ReturnToServer(lambda, mu, delta, s, maxTotalCapacity, p, patienceTheta, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
            SimResults results = sim.simulate(10000000);

            double rho = lambda/mu;

            //Expected figures are obtained by running YomTov_Simulation.

            double expectedWaitingProbability = 0.9760875813386678;
            double empiricalWaitingProbability = results.getWaitingProbability();
            assertEquals( 1, expectedWaitingProbability/empiricalWaitingProbability,   0.01);


            double expectedMeanWaitingTime = 9.918469302409778;
            double empiricalMeanWaitingTime = results.getMeanWaitingTime(); //In this scenario, jobs wait only in the holding queue (which is the buffer of the M/M/s system)
            assertEquals( 1, expectedMeanWaitingTime/empiricalMeanWaitingTime,   0.02);





            double expectedMeanAllInSystem = 44.495306301196464;
            double empiricalExpectedMeanAllInSystem = results.getMeanAllInSystem();
            assertEquals( 1, expectedMeanAllInSystem/empiricalExpectedMeanAllInSystem,   0.02);







        } catch (Exception e) {
            e.printStackTrace();
        }

    }





    //Expected results of the model, assuming Fixed Agent capacity.
    @Test
    public void _testFixedAgentCapacity() {
        double lambda = 2;
        double mu = 1.3;
        double delta = 0.52;
        int s = 5; //multiple servers.
        int maxTotalCapacity = 5000000; //each server has 'unlimited' capacity.
        double p = 0.5; //No content phase - all serviced Patient leave after the first service period.
        double patienceTheta = 0;
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {

            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(lambda, mu, delta, s, maxTotalCapacity, p, patienceTheta, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
            SimResults results = sim.simulate(10000000);

            double rho = lambda/mu;

            //Expected figures are obtained by running YomTov_Simulation.

            double expectedWaitingProbability = 0.4129379637284885;
            double empiricalWaitingProbability = results.getWaitingProbability();
            assertEquals( 1, expectedWaitingProbability/empiricalWaitingProbability,   0.03);


            double expectedMeanWaitingTime = 0.4066492933586345;
            double empiricalMeanWaitingTime = results.getMeanWaitingTime(); //In this scenario, jobs wait only in the holding queue (which is the buffer of the M/M/s system)
            assertEquals( 1, expectedMeanWaitingTime/empiricalMeanWaitingTime,   0.01);



            double expectedMeanAllInSystem = 8.549126936529918;
            double empiricalExpectedMeanAllInSystem = results.getMeanAllInSystem();
            assertEquals( 1, expectedMeanAllInSystem/empiricalExpectedMeanAllInSystem,   0.01);







        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    //
    @Test
    public void testTimeInhomogeneousPoissonProcess() {

        int[] times = {0,1,2,3};
        double[] lambdas = {5500,1000, 20000};
        int timeToRun = 600;
        double currTime = 0;
        int numSamples = 0;
        int[] empiricalNumSamples = new int[lambdas.length];
        double[] empiricalAccumNumSamples = new double[lambdas.length];
        //        Vector<Pair<Integer, Double>> empiricalRates = new Vector<>(lambdas.length);
//        double interArrivalsAccum = 0;

        Vector<Double> interArrivals = new Vector<Double>();
        //Test that an inhomogeneous reduces to a homogeneous when the rates are equal
        try {

            TimeInhomogeneousPoissionProcess ihpp = new TimeInhomogeneousPoissionProcess(times, lambdas);
            while( currTime <= timeToRun )
            {
                double curr = ihpp.timeToNextEvent(currTime);
                interArrivals.add(curr);
                numSamples += 1;
                currTime += curr;
                int currTimebinIndex = ihpp.findNextEventTimeBin( currTime );
                empiricalNumSamples[currTimebinIndex] += 1;
//                empiricalAccumNumSamples[currTimebinIndex] +=
//                System.out.println("Curr Time: " + currTime);
            }

            int totalPerIntervalNumSamples = 0;
            for( int i = 0 ; i < empiricalNumSamples.length ; i++ )
            {
                System.out.println("The average events rate at interval " + i + " is: " + empiricalNumSamples[i]/(timeToRun/times[times.length - 1]));
                totalPerIntervalNumSamples += empiricalNumSamples[i];
                assertEquals( 1, lambdas[i]/(empiricalNumSamples[i]/(timeToRun/times[times.length - 1])), 0.01);
            }

            System.out.println("There were total of " + numSamples);
            System.out.println("the sum of per-interval events is: " + totalPerIntervalNumSamples);

//            //Global total rate
//            double empiricalRate = numSamples/currTime;
//            FileWriter writer = new FileWriter("interArrivalsInhomPoiss.csv");
//            for (int j = 0; j < interArrivals.size(); j++) {
//                writer.append(String.valueOf(interArrivals.elementAt(j)));
//                writer.append("\maxTotalCapacity");
//            }
//            writer.close();
//
////            ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream("interArrivalsInhomPoiss.csv"));
////            outputStream.writeObject(ArrayUtils.join(interArrivals.toArray(), ","));
//
//            assertEquals( lambdas[0], empiricalRate, 0.01);


        }catch (Exception e)
        {
            e.printStackTrace();
        }


    }

*/

}