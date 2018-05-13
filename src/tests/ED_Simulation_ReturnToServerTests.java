package tests;

import ed_simulation.ED_Simulation_ReturnToServer;
import ed_simulation.ServerAssignmentMode;
import ed_simulation.SimResults;
import org.junit.Test;
import statistics.commons;

//import static org.apache.commons.math3.TestUtils.assertEquals;
import static statistics.commons.*;


import java.util.HashMap;

import static ed_simulation.ServerAssignmentMode.*;
import static org.junit.Assert.*;

public class ED_Simulation_ReturnToServerTests {

    @SuppressWarnings("Duplicates")
    //Reduce the system to a standard single server queue M/M/1 (content and holding queues are constantly of 0 size)
    @Test
    public void testSingleServer() {
        double lambda = 2;
        double mu = 2.5;
        double delta = 0.75;
        double p = 0; //No content phase - all serviced Patient leave after the first service period.
        int s = 1; //Single server.
        int n = 5000000; //'Unlimited' server capacity.
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        ED_Simulation_ReturnToServer sim = null;
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {
            sim = new ED_Simulation_ReturnToServer(lambda,mu,delta,s,n,p, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
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
    public void testMMs() {
        double lambda = 5;
        double mu = 1.05;
        double delta = 0.75;
        double p = 0; //No content phase - all serviced Patient leave after the first service period.
        int s = 5; //multiple servers.
        int n = 1; //each server has a single job capacity.
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        ED_Simulation_ReturnToServer sim = null;
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {
            sim = new ED_Simulation_ReturnToServer(lambda, mu, delta, s, n, p, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
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
    public void testErlangRSingleServer() {
        double lambda = 2;
        double mu = 4.1;
        double delta = 0.52;
        int s = 1; //Single server.
        int n = 5000000; //each server has 'unlimited' capacity.
        double p = 0.5;
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        ED_Simulation_ReturnToServer sim = null;
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {

            sim = new ED_Simulation_ReturnToServer(lambda, mu, delta, s, n, p, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
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
    public void testFixedAgentCapacity() {
        double lambda = 2;
        double mu = 1.3;
        double delta = 0.52;
        int s = 5; //multiple servers.
        int n = 5000000; //each server has 'unlimited' capacity.
        double p = 0.5; //No content phase - all serviced Patient leave after the first service period.
        //TODO: generate the loads to assignment prob hashmap by parsing an input file.
        //TODO: verify input is suitable to modes.
        ServerAssignmentMode serverAssignmemtMode = FIXED_SERVER_CAPACITY;
        try {

            ED_Simulation_ReturnToServer sim = new ED_Simulation_ReturnToServer(lambda, mu, delta, s, n, p, new HashMap<Integer, Double>(), 0.2, serverAssignmemtMode);
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


}