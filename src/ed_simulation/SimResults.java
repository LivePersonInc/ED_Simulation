package ed_simulation;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class SimResults{
    
    
    protected double[] probHoldingQueueLength;
    protected double sumH;
    protected double sumH2;
    protected double sumHcond;
    protected double sumH2cond;
    protected int counterH;
    protected int counterHcond;
    
    protected double[] probServiceQueueLength;
    protected double sumW;
    protected double sumW2;
    protected double sumWcond;
    protected double sumW2cond;
    protected int counterW;
    protected int counterWcond;
    
    protected double[] probTotalInSystem; //Service + content
    protected double[] probAllInSystem; // Holding + Service + content
    protected double[] probAllInSystemOnline; //Holding + Service of online agents + content of online agents.
    protected int numSamplesToNumsInSystem;
    
    protected double sumS;
    protected double sumS2;
    protected int counterS;
    protected double sumTotalW;
    protected double sumTotalW2;
        
    static final int MAX_QUEUE = 10000;
    protected double oldT = 0;
    protected long[] averageQueueSizePerIteraton;
    protected int[] numSamplesPerIterationQueueSize;
    protected double[] averageHoldingTimePerIteration;
    protected int[] numSamplesPerIterationWaitTime;
//    protected int[] numSamplesPerIterationWaitTime;
    int[]  numArrivalsPerIteration;
    int[] numAbandonedPerIteration;
    //Number of new conversations assigned to agents from the holding queue per unit time.
    int[] numAssignmentsPerIteration;
    //Number of concurrent conversations per agent.
    int[] allAgentLoadPerIteration;
    int[] onlineAgentLoadPerIteration;
    int[] numSamplesForAgentLoad;
    int[] staffing;
    int[] numSamplesForStaffing;
    int[] agentMaxCapacity;
    int[] numExchangesPerConv;
    int[] numSingleExchangeConvs;
    double[] exchangeDuration;
    double[] interExchangeDuration;
    int[] numConvs;
    protected int maxTotalCapacity;
    private double referenceWaitTime;
    private int counterAboveReferenceWaitTime;


    //A SimResults object is assumed to accumulate results pertaining to a given time bin in a time-varying period. The
    //simulation is assumed to repeat over a number of such periods.
    public SimResults( int maxTotalCapacity, int numPeriodsInSimulation, double referenceWaitTime){
        this.probHoldingQueueLength = new double[MAX_QUEUE];
        this.probAllInSystem = new double[MAX_QUEUE];
        this.probAllInSystemOnline = new double[MAX_QUEUE];
        this.sumH = 0;
        this.sumH2 = 0;
        this.sumHcond = 0;
        this.sumW2cond = 0;
        this.counterH = 0;
        this.counterHcond = 0;
        this.counterAboveReferenceWaitTime = 0;
        
        this.probServiceQueueLength = new double[maxTotalCapacity+1];
        this.probTotalInSystem = new double[maxTotalCapacity+1];
        this.sumW = 0;
        this.sumW2 = 0;
        this.sumWcond = 0;
        this.sumW2cond = 0;
        this.counterW = 0;
        this.counterWcond = 0;
        
        this.sumS = 0;
        this.sumS2 = 0;
        this.counterS = 0;
        this.sumTotalW = 0;
        this.sumTotalW2 = 0;   
        
        this.oldT = 0;
        this.maxTotalCapacity = maxTotalCapacity;
        //Per each time iteration of the time bin associated with this result - register the average queueSize encountered by conversations
        //arriving at this timebin, and the average wait time experienced by conversations that arrived at this timebin.
        averageQueueSizePerIteraton = new long[numPeriodsInSimulation];
        numSamplesPerIterationQueueSize = new int[numPeriodsInSimulation];
        averageHoldingTimePerIteration = new double[numPeriodsInSimulation];
        numSamplesPerIterationWaitTime = new int[numPeriodsInSimulation];
        numArrivalsPerIteration = new int[numPeriodsInSimulation];
        numAbandonedPerIteration = new int[numPeriodsInSimulation];
        numAssignmentsPerIteration = new int[numPeriodsInSimulation];
        allAgentLoadPerIteration = new int[numPeriodsInSimulation];
        onlineAgentLoadPerIteration = new int[numPeriodsInSimulation];
        numSamplesForAgentLoad = new int[numPeriodsInSimulation];
        staffing = new int[numPeriodsInSimulation];
        numSamplesForStaffing = new int[numPeriodsInSimulation];
        agentMaxCapacity = new int[numPeriodsInSimulation];
        numExchangesPerConv = new int[numPeriodsInSimulation];
        numSingleExchangeConvs = new int[numPeriodsInSimulation];
        exchangeDuration = new double[numPeriodsInSimulation];
        interExchangeDuration = new double[numPeriodsInSimulation];
        numConvs = new int[numPeriodsInSimulation];
        this.referenceWaitTime = referenceWaitTime;




    }

    public SimResults( int maxTotalCapacity){
        this(maxTotalCapacity, 1, 0);
    }

    public void registerQueueLengths(int holdingQueueSize, int serviceQueueSize, int contentQueueSize, double currentTime ){
        if( holdingQueueSize >= MAX_QUEUE ) holdingQueueSize = MAX_QUEUE-1;
        int all = serviceQueueSize+contentQueueSize+holdingQueueSize;
        if( all >= MAX_QUEUE ) all = MAX_QUEUE-1;

        probHoldingQueueLength[holdingQueueSize] += (currentTime - oldT);
        probServiceQueueLength[serviceQueueSize] += (currentTime - oldT);
        //Service + holding
        probTotalInSystem[serviceQueueSize+contentQueueSize] += (currentTime - oldT);
        //Holding + Service + content.
        probAllInSystem[all] += (currentTime - oldT);
        oldT = currentTime;

    }


    //HoldingQueue, ServiceQueue, ContentQueue, current time
    //Adds the current time interval spent in the corresponding queue states.
    public void registerQueueLengths(int holdingQueueSize, int serviceQueueSize, int contentQueueSize, int serviceQueueSizeOnlineServers, int contentQueueSizeOnlineAgents,
                                     double currentTime, int currentTimePeriodIndex, int numOnlineAgents, int agentsMaxCapacity){
        if( holdingQueueSize >= MAX_QUEUE ) holdingQueueSize = MAX_QUEUE-1;
        int all = serviceQueueSize+contentQueueSize+holdingQueueSize;
        int onlineAll = serviceQueueSizeOnlineServers + contentQueueSizeOnlineAgents + holdingQueueSize;
        if( all >= MAX_QUEUE ) all = MAX_QUEUE-1;
        if( onlineAll >= MAX_QUEUE ) onlineAll = MAX_QUEUE - 1;
        //Replaced the time-based statistics, since in the time-dependent simulation each SimResults stores the result of a single time bin, so
        //oldT is completely biased when moving from one period to another.
        probHoldingQueueLength[holdingQueueSize] +=  1; //(currentTime - oldT);
        probServiceQueueLength[serviceQueueSize] += 1; //(currentTime - oldT);

        //Service + content
        probTotalInSystem[serviceQueueSize+contentQueueSize] += 1; //(currentTime - oldT);
        //Holding + Service + content.
        probAllInSystem[all] += 1;//(currentTime - oldT);
        probAllInSystemOnline[onlineAll] += 1;
        numSamplesToNumsInSystem += 1;
//        oldT = currentTime;
        allAgentLoadPerIteration[currentTimePeriodIndex] += (serviceQueueSize + contentQueueSize); //The overall number of conversations at service in the system.
        onlineAgentLoadPerIteration[currentTimePeriodIndex] += (serviceQueueSizeOnlineServers + contentQueueSizeOnlineAgents);
        numSamplesForAgentLoad[currentTimePeriodIndex] += 1;
        staffing[currentTimePeriodIndex] += numOnlineAgents;
        numSamplesForStaffing[currentTimePeriodIndex] += 1;
        agentMaxCapacity[currentTimePeriodIndex] += agentsMaxCapacity;
        averageQueueSizePerIteraton[currentTimePeriodIndex] += holdingQueueSize;
        if( holdingQueueSize < 2 )
        {
            int x = 0;
        }
//        System.out.println("Registering the queues lengths. The holding queue size is:  " + holdingQueueSize);

        numSamplesPerIterationQueueSize[currentTimePeriodIndex] += 1;
        if( onlineAgentLoadPerIteration[currentTimePeriodIndex]/((double)numSamplesForAgentLoad[currentTimePeriodIndex]*numOnlineAgents ) > agentsMaxCapacity )
        {
            int x = 0;
        }



    } 
    
    public void registerHoldingTime(Patient p,double t, int arrivalTimePeriodIndex){
        double w = t-p.getArrivalTime();
        p.setHoldingTime(w);
        sumH += w;
        sumH2 += w*w;
        counterH ++ ;
        
        if( w > 0 ){
            sumHcond += w;
            sumH2cond += w*w;
            counterHcond++;
            
        }
        if( w > referenceWaitTime )
        {
            counterAboveReferenceWaitTime += 1;
        }
        //The time in queue is associated with the arrival time (i.e. conversations arriving at timebin j waited on average Wj)
        averageHoldingTimePerIteration[arrivalTimePeriodIndex] += w;
        numSamplesPerIterationWaitTime[arrivalTimePeriodIndex] += 1;
        //The assignRate is associated with the current timebin (in which the assingment to agent took place).

    }


    public void registerAssignmentToAgent( int currTimePeriodIndex ){

        numAssignmentsPerIteration[ currTimePeriodIndex ] += 1;


    }


    public void registerHoldingTime(Patient p,double t){
        double w = t-p.getArrivalTime();
        p.setHoldingTime(w);
        sumH += w;
        sumH2 += w*w;
        counterH ++ ;

        if( w > 0 ){
            sumHcond += w;
            sumH2cond += w*w;
            counterHcond++;

        }

    }
    
    //Register the current waiting time in the service queue (internal agent queue).
    public void registerWaitingTime(Patient p,double t){
        double w = t-p.getLastArrivalTime();
        sumW += w;
        sumW2 += w*w;
        counterW ++ ;
        
        if( w > 0 ){
            sumWcond += w;
            sumW2cond += w*w;
            counterWcond ++;
        }    
    }
    
    public void registerOneWait(boolean waited){
        System.out.println("!!!! Reached registerOneWait!!!! Shouldn't be here!!!");
        counterW++;
        if ( waited ) counterWcond++;
    }
    
    public void registerOneHold(boolean held){
        counterH++;
        if ( held ) counterHcond++;
    }
    
    //Register a Patient departure that took place at departure time. The arrival of the patient to the system took place
    // at period arrivalTimePeriodIndex.
    public void registerDeparture(Patient p, double departureTime, int arrivalTimePeriodIndex){
        double s = departureTime - p.getArrivalTime();
        double tw = s - p.getWaitingTime();
        if(p==null)
        {
            int x = 0;
        }
        numExchangesPerConv[arrivalTimePeriodIndex] += p.getNrVisits();
        numSingleExchangeConvs[arrivalTimePeriodIndex] += (p.getNrVisits() == 1 ? 1 : 0);
        numConvs[arrivalTimePeriodIndex] += 1;
        sumS += s;
        sumS2 += s*s;
        counterS++;
        sumTotalW += tw;
        sumTotalW2 += tw*tw;
    }


    public void registerDeparture(Patient p, double departureTime){
        double s = departureTime - p.getArrivalTime();
        double tw = s - p.getWaitingTime();
        sumS += s;
        sumS2 += s*s;
        counterS++;
        sumTotalW += tw;
        sumTotalW2 += tw*tw;
    }


    public double[] getHoldingQueueLengthProbabilities( int max ){
        double[] out = new double[max+1];
        for( int i = 0; i < max+1 ; i++ ){
            out[i] = probHoldingQueueLength[i]/oldT;
        }
        return out;
    }
    
    public double[] getServiceQueueLengthProbabilities(){
        double[] out = new double[probServiceQueueLength.length];
        for( int i = 0; i < out.length ; i++ ){
            out[i] = probServiceQueueLength[i]/oldT;
        }
        return out;
    }
    
    public double[] getTotalInSystemProbabilities(){
        double[] out = new double[probTotalInSystem.length];
        for( int i = 0; i < out.length ; i++ ){
            out[i] = probTotalInSystem[i]/numSamplesToNumsInSystem;
        }
        return out;
    }


    public double[] getAllInSystemProbabilities(int max){
        return getAllInSystemProbabilities(max, false);
    }


    public double[] getAllInSystemProbabilities(boolean onlineAgentsOnly){
        return getAllInSystemProbabilities(probAllInSystem.length-1, onlineAgentsOnly);
    }


    public double[] getAllInSystemProbabilities(int max, boolean onlyOnline){
        double[] out = new double[max+1];
        double[] relevantArr = onlyOnline ? probAllInSystemOnline : probAllInSystem;
        for( int i = 0; i < out.length ; i++ ){
            out[i] = relevantArr[i]/numSamplesToNumsInSystem;
        }
        return out;
    }
    
    
    public double getMeanHoldingQueueLength(int max ){
        if( max < 0 )  max = MAX_QUEUE;      
        double mean = 0;
        
        for( int i = 0; i < max ; i++ ){
            mean += i*probHoldingQueueLength[i]/oldT;
        }
        
        return mean;
    }
    
    public double getMeanServiceQueueLength(){     
        double mean = 0;
        
        for( int i = 0; i < probServiceQueueLength.length ; i++ ){
            mean += i*probServiceQueueLength[i]/oldT;
        }
        
        return mean;
    }
    
    public double getMeanHoldingTime(){
        return sumH / counterH;
    }
    
    public double getVarianceHoldingTime(){
        return ( sumH2 / counterH - (sumH / counterH)*(sumH / counterH) );
    }
    
    public double[] getCIHoldingTime(){
        double mean = getMeanHoldingTime();
        double var = getVarianceHoldingTime();
        double[] out = { mean - 1.96*Math.sqrt(var / counterH) , mean + 1.96*Math.sqrt(var / counterH) };
        return out;
    }
    
    public double getMeanWaitingTime(){
        return sumW / counterW;
    }
    
    public double getVarianceWaitingTime(){
        return ( sumW2 / counterW - (sumW / counterW)*(sumW / counterW) );
    }
    
    public double[] getCIWaitingTime(){
        double mean = getMeanWaitingTime();
        double var = getVarianceWaitingTime();
        double[] out = { mean - 1.96*Math.sqrt(var / counterW) , mean + 1.96*Math.sqrt(var / counterW) };
        return out;
    }
    
    
    public double getMeanTotalWaitingTime(){
        return sumTotalW / counterS;
    }
    
    public double getVarianceTotalWaitingTime(){
        return ( sumTotalW2 / counterS - (sumTotalW / counterS)*(sumTotalW / counterS) );
    }
    
    public double[] getCITotalWaitingTime(){
        double mean = getMeanTotalWaitingTime();
        double var = getVarianceTotalWaitingTime();
        double[] out = { mean - 1.96*Math.sqrt(var / counterS) , mean + 1.96*Math.sqrt(var / counterS) };
        return out;
    }
    
    
    public double getMeanSojournTime(){
        return sumS / counterS;
    }
    
    public double getVarianceSojournTime(){
        return ( sumS2 / counterS - (sumS / counterS)*(sumS / counterS) );
    }
    
    public double[] getCISojournTime(){
        double mean = getMeanSojournTime();
        double var = getVarianceSojournTime();
        double[] out = { mean - 1.96*Math.sqrt(var / counterS) , mean + 1.96*Math.sqrt(var / counterS) };
        return out;
    }
    
    public double getNurseOccupancy(int s){
        double sum = 0;
        for(int i = 0 ; i < probServiceQueueLength.length; i++ ){
            sum += ( probServiceQueueLength[i] / oldT ) * Math.min(s,i) / s;
        }
        return sum;
    }
    
    public double[] getCIdelayProbability(){
        double mean = getWaitingProbability();
        double var = mean - mean*mean;
        double[] out =  {( mean - Math.sqrt(var / counterW )), (mean + Math.sqrt( var / counterW )) };
        return out;
    }
       
    
    public double getHoldingProbability(){
        return (1.0*counterHcond / counterH);
    }

    public double getHoldingProbabilityBasedOnAllInSystem(){
        int numPeriods = this.getNumPeriods();
        int staffing = this.staffing[numPeriods-1]/this.numSamplesForStaffing[numPeriods-1];
        int agentCapacity = this.agentMaxCapacity[numPeriods-1]/this.numSamplesForAgentLoad[numPeriods-1];
        int totalNumSlots = staffing * agentCapacity;
        double accumProb = 0;
        for( int i = 0 ; i <= totalNumSlots ; i++ )
        {
            accumProb += probAllInSystemOnline[i];
        }
        return 1-(accumProb/numSamplesToNumsInSystem);
    }

    public double getWaitingProbability(){
        return (1.0*counterWcond / counterW);
    }
    
    public double getHoldingProbability2(){
        return (getTotalInSystemProbabilities()[maxTotalCapacity]);
    }

    public double getWaitingProbability2(int s){
        double[] pr = getServiceQueueLengthProbabilities();
        double prob = 0;
        for(int i = s; i <= maxTotalCapacity; i++ ){
            prob += pr[i];
        }
        return prob;
    }


    /**
     *
     * @return The mean number of non-held jobs in the system: service queue + content queue;
     */
    public double getMeanTotalInSystem(){
        double out = 0;
        double[] probs = getTotalInSystemProbabilities();
        
        for( int i = 0; i < probs.length; i++ ){
            out += i*probs[i];
        }
        
        return out;
    }


    public double getMeanAllInSystem(){
        return getMeanAllInSystem(false);
    }

    /**
     *
     * @return The mean number of jobs in the system: holding queue + service queue + content queue;
     */
    public double getMeanAllInSystem(boolean onlineAgentsOnly){
        double out = 0;
        double[] probs = getAllInSystemProbabilities(MAX_QUEUE-1, onlineAgentsOnly);
        
        for( int i = 0; i < probs.length; i++ ){
//            if(probs[i] != 0 )
//            {
//                int x = 9;
//            }
            out += i*probs[i];
        }
        
        return out;
    }
    
    public String getQueueSizeRealizationAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numSamplesPerIterationQueueSize.length - numRepetitionsToTruncate ; i++)
        {
            res += "," + /*this.numSamplesPerIterationQueueSize[i] + "," + */ (this.numSamplesPerIterationQueueSize[i] != 0 ? this.averageQueueSizePerIteraton[i]/(double)this.numSamplesPerIterationQueueSize[i] : Double.NaN) ;
        }
        return res;
    }

    public String getQueueSizeRealizationNumSamplesAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numSamplesPerIterationQueueSize.length - numRepetitionsToTruncate ; i++)
        {
            res += "," + this.numSamplesPerIterationQueueSize[i];
        }
        return res;
    }

    public String getStaffingRealizationNumSamplesAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numSamplesForStaffing.length - numRepetitionsToTruncate ; i++)
        {
            res += "," + this.numSamplesForStaffing[i];
        }
        return res;
    }




    public String getAllAgentLoadNumSamplesAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numSamplesForAgentLoad.length - numRepetitionsToTruncate ; i++)
        {
            res += "," + this.numSamplesForAgentLoad[i];
        }
        return res;
    }





    public String getArrivalRateRealizationAsCsv(int binSizeInSec, int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numArrivalsPerIteration.length - numRepetitionsToTruncate ; i++)
        {
            res += "," + this.numArrivalsPerIteration[i]/(double)binSizeInSec  ;
        }
        return res;
    }

    public String getAssignRateRealizationAsCsv(int binSizeInSec, int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numAssignmentsPerIteration.length - numRepetitionsToTruncate ; i++)
        {
            res += "," + this.numAssignmentsPerIteration[i]/(double)binSizeInSec  ;
        }
        return res;
    }


    public String getTimeInQueueRealizationAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numSamplesPerIterationWaitTime.length - numRepetitionsToTruncate ; i++)
        {
            res += "," + /*this.numSamplesPerIterationWaitTime[i] + "," + */ (this.numSamplesPerIterationWaitTime[i] != 0 ? this.averageHoldingTimePerIteration[i]/this.numSamplesPerIterationWaitTime[i] : 0 );

        }
        //For dubugging....
//        System.out.println("NumAssignmentsPerIteration: ");
//        for( int i = 0 ; i < this.numAssignmentsPerIteration.length - numRepetitionsToTruncate ; i++)
//        {
//             System.out.println(this.numAssignmentsPerIteration[i]);
//        }
//
//        System.out.println("AverageHoldingTimePerIteration: ");
//        for( int i = 0 ; i < this.numAssignmentsPerIteration.length - numRepetitionsToTruncate ; i++)
//        {
//            System.out.println(this.averageHoldingTimePerIteration[i]);
//        }

        return res;
    }


    public String getTimeInQueueRealizationNumSamplesAsCsv(int numRepetitionsToTruncate) {

        String res = "";
        for( int i = 0 ; i < this.numSamplesPerIterationWaitTime.length - numRepetitionsToTruncate ; i++)
        {
            res += "," + this.numSamplesPerIterationWaitTime[i] ;

        }

        return res;
    }



    //This is inaccurate, since the agentLoadPerIteration counts the overall load over the entire call center, including offline agents which are still working on their existing jobs.
    public String getOnlineAgentLoadRealizationAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.onlineAgentLoadPerIteration.length - numRepetitionsToTruncate ; i++)
        {
            double currLoad = (this.numSamplesForAgentLoad[i] != 0 ? this.onlineAgentLoadPerIteration[i]/(double)this.numSamplesForAgentLoad[i]/(this.staffing[i]/numSamplesForStaffing[i])  : 0) ;

            res += "," + currLoad ;
        }
        return res;
    }



    public String getAllAgentLoadRealizationAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.allAgentLoadPerIteration.length - numRepetitionsToTruncate; i++)
        {
//            double currLoadAllSystemAgents = (this.numSamplesForAgentLoad[i] != 0 ? (this.agentLoadPerIteration[i]/(double)(this.maxTotalCapacity*this.numSamplesForAgentLoad[i]))  : -1 );
            //TODO!! Need to verify this, since here are also included agents that are 'away' but still working (e.g. after reducing the number of agents per hour). Need to differenciate
            //here between active agents and non-active ones.
//            double average_num_in_service = this.numSamplesForAgentLoad[i] != 0 ? this.allAgentLoadPerIteration[i]/(double)(this.numSamplesForAgentLoad[i]) : 0 ;
//            double average_num_servers = numSamplesForStaffing[i] != 0 ?  (this.staffing[i]/(double)numSamplesForStaffing[i]) : 0 ;
//            double currLoadAllSystemAgents = average_num_servers != 0 ? average_num_in_service/average_num_servers : Double.NaN;
////            double currLoadAllSystemAgents = (this.numSamplesForAgentLoad[i] != 0 ? this.allAgentLoadPerIteration[i]/(double)(this.numSamplesForAgentLoad[i])/(this.staffing[i]/numSamplesForStaffing[i])  : 0 );

            //Just write OnlineAgentLoad/OnlineAgentMaxCapacity
            double currLoad = (this.numSamplesForAgentLoad[i] != 0 ? this.onlineAgentLoadPerIteration[i]/(double)this.numSamplesForAgentLoad[i]/(this.staffing[i]/numSamplesForStaffing[i])  : 0) ;
            double currCapacity = (this.numSamplesForAgentLoad[i] != 0 ? this.agentMaxCapacity[i]/(double)(this.numSamplesForAgentLoad[i])  : 0 );
            double currLoadAllSystemAgents = currCapacity != 0 ? currLoad/currCapacity : Double.NaN;
            res += ","  + currLoadAllSystemAgents;
        }
        return res;
    }


    //TODO: Verify: is this really the online agents? Not all of them?
    public String getOnlineAgentMaxCapacityRealizationAsCsv(int numRepetitionsToTruncate) {
        String res = "";
        for( int i = 0 ; i < this.agentMaxCapacity.length - numRepetitionsToTruncate ; i++)
        {
            double currCapacity = (this.numSamplesForAgentLoad[i] != 0 ? this.agentMaxCapacity[i]/(double)(this.numSamplesForAgentLoad[i]) /* /(this.staffing[i]/numSamplesForStaffing[i]) */ : Double.NaN );
            res += ","  + currCapacity;
        }
        return res;
    }


    public String getStaffingRealizationAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.staffing.length - numRepetitionsToTruncate; i++)
        {
            double currSaffing = (this.numSamplesForStaffing[i] != 0 ? this.staffing[i]/(double)(this.numSamplesForStaffing[i])  : 0 );
            res += ","  + currSaffing;
        }
        return res;
    }

    public String getNumExchangesPerConvRealizationAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numConvs.length - numRepetitionsToTruncate ; i++)
        {
            double avgNumExchangesPerConv = (this.numConvs[i] != 0 ? this.numExchangesPerConv[i]/(double)(this.numConvs[i])  : 0 );
            res += ","  + avgNumExchangesPerConv;
        }
        return res;
    }

    public String getSingleExchangeRatioAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numConvs.length - numRepetitionsToTruncate ; i++)
        {
            double singleExchangeRatio = (this.numConvs[i] != 0 ? this.numSingleExchangeConvs[i]/(double)(this.numConvs[i])  : 0 );
            res += ","  + singleExchangeRatio;
        }
        return res;
    }

    public String getAbanBeforeAgentRatioAsCsv(int numRepetitionsToTruncate)
    {
        String res = "";
        for( int i = 0 ; i < this.numArrivalsPerIteration.length - numRepetitionsToTruncate ; i++)
        {
            double avgAbanRate = (this.numArrivalsPerIteration[i] != 0 ? this.numAbandonedPerIteration[i]/(double)(this.numArrivalsPerIteration[i])  : 0 );
            res += ","  + avgAbanRate;
        }
        return res;
    }


    public String getAvgExchangeDuration() {
        String res = "";
        for( int i = 0 ; i < this.numConvs.length ; i++)
        {
            res += ","  +  (this.numConvs[i] != 0 ? this.exchangeDuration[i]/(double)(this.numConvs[i])  : 0 );
        }
        return res;

    }


    public String getAvgInterExchangeDuration() {
        String res = "";
        for( int i = 0 ; i < this.numConvs.length ; i++)
        {
            res += ","  +  (this.numConvs[i] != 0 ? this.interExchangeDuration[i]/(double)(this.numConvs[i])  : 0 );
        }
        return res;
    }




    //Returns the number of periods simulated in this simulation (e.g. if we're simulating a single week as the base period, and
    //the simulation consisted of 100 repetitions of this single week, then this method returns 100.
    public int getNumPeriods()
    {
        return this.numSamplesPerIterationQueueSize.length;
    }


    public void registerArrival(int currentTimePeriodIndex) {
        numArrivalsPerIteration[currentTimePeriodIndex] += 1;
    }

    public void registerArrivals( int currentTimePeriodIndex, int numArrivals) {
        numArrivalsPerIteration[currentTimePeriodIndex] += numArrivals;
    }

    //TODO: add the wait time distribution as well (i.e. how long abandoned patient waited - both for silent and known abandonment.)
    //Abdndonment is registered 'backward', i.e. is associated with the arrival time of the abandoned conversation.
    public void registerAbandonment(int currTimePeriod) {
        this.numAbandonedPerIteration[currTimePeriod] += 1;
    }

    //In general, the treatment is as follows:
    //If there's a valid iteration, we use it. Otherwise there were no assignments from the holding queue in all the iterations,
    //hence the holding time is assumed to be Infinity.

    public double getAvgHoldingTime() {
        double res = 0;
        int numValidSamples = 0;
        for( int i = 0 ; i < this.numAssignmentsPerIteration.length ; i++)
        {
            boolean isValidIteration = this.numAssignmentsPerIteration[i] != 0;
            res += ( isValidIteration ? this.averageHoldingTimePerIteration[i]/this.numSamplesPerIterationWaitTime[i] : 0 );
            numValidSamples +=  ( isValidIteration ? 1 : 0 );
        }
        return  numValidSamples > 0 ? res/numValidSamples : Double.POSITIVE_INFINITY;
    }

    public double getExcessWaitProbabilities() {
        //counterH == 0 iff no assignment took place during this time bin (which happens, basically, when there are 0 agents, or
        //when the service time is much longer than the bin size and the servers are all loaded.)
        return counterH > 0 ?  1.0*counterAboveReferenceWaitTime/counterH : 1;
    }

    //!!! TODO - remove initial and terminal iterations!!!
    public void append(SimResults otherResults, int numPeriodsRepetitionsTillSteadyState) {

        for(int i = 0 ; i < probHoldingQueueLength.length ; i++){
            probHoldingQueueLength[i] += otherResults.probHoldingQueueLength[i];
        }
        sumH += otherResults.sumH;
        sumH2 += otherResults.sumH2;
        sumHcond += otherResults.sumHcond;
        sumH2cond += otherResults.sumH2cond;
        counterH += otherResults.counterH;
        counterHcond += otherResults.counterHcond;
        for(int i = 0 ; i < probServiceQueueLength.length ; i++){
            probServiceQueueLength[i] += otherResults.probHoldingQueueLength[i];
        }
        sumW += otherResults.sumW;
        sumW2 += otherResults.sumW2;
        sumWcond += otherResults.sumWcond;
        sumW2cond += otherResults.sumW2cond;
        counterW += otherResults.counterW;
        counterWcond += otherResults.counterWcond;

        for(int i = 0 ; i < probTotalInSystem.length ; i++){
            probTotalInSystem[i] += otherResults.probTotalInSystem[i];
        }
        for(int i = 0 ; i < probAllInSystem.length ; i++){
            probAllInSystem[i] += otherResults.probAllInSystem[i];
        }

        for(int i = 0 ; i < probAllInSystemOnline.length ; i++){
            probAllInSystemOnline[i] += otherResults.probAllInSystemOnline[i];
        }
        numSamplesToNumsInSystem += otherResults.numSamplesToNumsInSystem;

        sumS += otherResults.sumS;
        sumS2 += otherResults.sumS2;
        counterS += otherResults.counterS;
        sumTotalW += otherResults.sumTotalW;
        sumTotalW2 += otherResults.sumTotalW2;

//        for(int i = 0 ; i < averageQueueSizePerIteraton.length ; i++){
//            averageQueueSizePerIteraton[i] += otherResults.averageQueueSizePerIteraton[i];
//        }
//        for(int i = 0 ; i < numSamplesPerIterationQueueSize.length ; i++){
//            numSamplesPerIterationQueueSize[i] += otherResults.numSamplesPerIterationQueueSize[i];
//        }
//        for(int i = 0 ; i < averageHoldingTimePerIteration.length ; i++){
//            averageHoldingTimePerIteration[i] += otherResults.averageHoldingTimePerIteration[i];
//        }


        averageQueueSizePerIteraton = appendPeriods(averageQueueSizePerIteraton, otherResults.averageQueueSizePerIteraton, numPeriodsRepetitionsTillSteadyState);
        numSamplesPerIterationQueueSize = appendPeriods(numSamplesPerIterationQueueSize, otherResults.numSamplesPerIterationQueueSize, numPeriodsRepetitionsTillSteadyState);
        numSamplesPerIterationWaitTime = appendPeriods(numSamplesPerIterationWaitTime, otherResults.numSamplesPerIterationWaitTime, numPeriodsRepetitionsTillSteadyState);
        averageHoldingTimePerIteration = appendPeriods(averageHoldingTimePerIteration, otherResults.averageHoldingTimePerIteration, numPeriodsRepetitionsTillSteadyState);
        numArrivalsPerIteration = appendPeriods(numArrivalsPerIteration, otherResults.numArrivalsPerIteration, numPeriodsRepetitionsTillSteadyState);
        numAbandonedPerIteration = appendPeriods( numAbandonedPerIteration, otherResults.numAbandonedPerIteration, numPeriodsRepetitionsTillSteadyState);
        numAssignmentsPerIteration = appendPeriods( numAssignmentsPerIteration, otherResults.numAssignmentsPerIteration, numPeriodsRepetitionsTillSteadyState);
        allAgentLoadPerIteration = appendPeriods( allAgentLoadPerIteration, otherResults.allAgentLoadPerIteration, numPeriodsRepetitionsTillSteadyState);
        onlineAgentLoadPerIteration = appendPeriods( onlineAgentLoadPerIteration, otherResults.onlineAgentLoadPerIteration, numPeriodsRepetitionsTillSteadyState);
        numSamplesForAgentLoad = appendPeriods( numSamplesForAgentLoad, otherResults.numSamplesForAgentLoad, numPeriodsRepetitionsTillSteadyState);
        staffing = appendPeriods( staffing, otherResults.staffing, numPeriodsRepetitionsTillSteadyState);
        numSamplesForStaffing = appendPeriods( numSamplesForStaffing, otherResults.numSamplesForStaffing, numPeriodsRepetitionsTillSteadyState);
        agentMaxCapacity = appendPeriods( agentMaxCapacity, otherResults.agentMaxCapacity, numPeriodsRepetitionsTillSteadyState);
        numExchangesPerConv = appendPeriods( numExchangesPerConv, otherResults.numExchangesPerConv, numPeriodsRepetitionsTillSteadyState);
        numSingleExchangeConvs = appendPeriods( numSingleExchangeConvs, otherResults.numSingleExchangeConvs, numPeriodsRepetitionsTillSteadyState);
        exchangeDuration = appendPeriods( exchangeDuration, otherResults.exchangeDuration, numPeriodsRepetitionsTillSteadyState);
        interExchangeDuration = appendPeriods( interExchangeDuration, otherResults.interExchangeDuration, numPeriodsRepetitionsTillSteadyState);
        numConvs = appendPeriods( numConvs, otherResults.numConvs, numPeriodsRepetitionsTillSteadyState);


//        for(int i = 0 ; i < numArrivalsPerIteration.length ; i++){
//            numArrivalsPerIteration[i] += otherResults.numArrivalsPerIteration[i];
//            numAbandonedPerIteration[i] += otherResults.numAbandonedPerIteration[i];
//            numAssignmentsPerIteration[i] += otherResults.numAssignmentsPerIteration[i];
//            allAgentLoadPerIteration[i] += otherResults.allAgentLoadPerIteration[i];
//            onlineAgentLoadPerIteration[i] += otherResults.onlineAgentLoadPerIteration[i];
//            numSamplesForAgentLoad[i] += otherResults.numSamplesForAgentLoad[i];
//            staffing[i] += otherResults.staffing[i];
//            numSamplesForStaffing[i] += otherResults.numSamplesForStaffing[i];
//            agentMaxCapacity[i] += otherResults.agentMaxCapacity[i];
//            numExchangesPerConv[i] += otherResults.numExchangesPerConv[i];
//            numSingleExchangeConvs[i] += otherResults.numSingleExchangeConvs[i];
//            exchangeDuration[i] += otherResults.exchangeDuration[i];
//            interExchangeDuration[i] += otherResults.interExchangeDuration[i];
//            numConvs[i] += otherResults.numConvs[i];
//        }

        counterAboveReferenceWaitTime += otherResults.counterAboveReferenceWaitTime;

    }

    //Java, Java, Java. Could it get more cumbersome than that?
    private int[] appendPeriods(int[] numArrivalsPerIteration, int[] otherumArrivalsPerIteration1, int numPeriodsRepetitionsTillSteadyState) {
        List<Integer> currNumArrivals = Arrays.stream(numArrivalsPerIteration).boxed().collect(Collectors.toList());
        List<Integer> otherNumArrivals = Arrays.stream(otherumArrivalsPerIteration1, numPeriodsRepetitionsTillSteadyState, otherumArrivalsPerIteration1.length ).boxed().collect(Collectors.toList());
        currNumArrivals.addAll(otherNumArrivals);
        return currNumArrivals.stream().mapToInt(i->i).toArray();
    }

    private double[] appendPeriods(double[] numArrivalsPerIteration, double[] otherumArrivalsPerIteration1, int numPeriodsRepetitionsTillSteadyState) {
        List<Double> currNumArrivals = Arrays.stream(numArrivalsPerIteration).boxed().collect(Collectors.toList());
        List<Double> otherNumArrivals = Arrays.stream(otherumArrivalsPerIteration1, numPeriodsRepetitionsTillSteadyState, otherumArrivalsPerIteration1.length ).boxed().collect(Collectors.toList());
        currNumArrivals.addAll(otherNumArrivals);
        return currNumArrivals.stream().mapToDouble(i->i).toArray();
    }

    private long[] appendPeriods(long[] numArrivalsPerIteration, long[] otherumArrivalsPerIteration1, int numPeriodsRepetitionsTillSteadyState) {
        List<Long> currNumArrivals = Arrays.stream(numArrivalsPerIteration).boxed().collect(Collectors.toList());
        List<Long> otherNumArrivals = Arrays.stream(otherumArrivalsPerIteration1, numPeriodsRepetitionsTillSteadyState, otherumArrivalsPerIteration1.length ).boxed().collect(Collectors.toList());
        currNumArrivals.addAll(otherNumArrivals);
        return currNumArrivals.stream().mapToLong(i->i).toArray();
    }


//    public Vector<Double> getAllInSystemDistribution() {
//        return totalNumInSystemDistribution;
//    }
}