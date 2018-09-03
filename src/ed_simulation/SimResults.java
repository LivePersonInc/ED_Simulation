package ed_simulation;


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
    
    protected double[] probTotalInSystem;
    protected double[] probAllInSystem;
    
    protected double sumS;
    protected double sumS2;
    protected int counterS;
    protected double sumTotalW;
    protected double sumTotalW2;
        
    static final int MAX_QUEUE = 10000;
    protected double oldT = 0;
    protected long[] averageQueueSizePerIteraton;
    protected int[] numSamplesPerIterationQueueSize;
    protected double[] averageWaitTimePerIteration;
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
    double[] exchangeDuration;
    double[] interExchangeDuration;
    int[] numConvs;
    protected int maxTotalCapacity;

    //A SimResults object is assumed to accumulate results pertaining to a given time bin in a time-varying period. The
    //simulation is assumed to repeat over a number of such periods.
    public SimResults( int maxTotalCapacity, int numPeriodsInSimulation){
        this.probHoldingQueueLength = new double[MAX_QUEUE];
        this.probAllInSystem = new double[MAX_QUEUE];
        this.sumH = 0;
        this.sumH2 = 0;
        this.sumHcond = 0;
        this.sumW2cond = 0;
        this.counterH = 0;
        this.counterHcond = 0;
        
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
        averageWaitTimePerIteration  = new double[numPeriodsInSimulation];
//        numSamplesPerIterationWaitTime = new int[numPeriodsInSimulation];
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
        exchangeDuration = new double[numPeriodsInSimulation];
        interExchangeDuration = new double[numPeriodsInSimulation];
        numConvs = new int[numPeriodsInSimulation];




    }

    public SimResults( int maxTotalCapacity){
        this(maxTotalCapacity, 1);
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
        if( all >= MAX_QUEUE ) all = MAX_QUEUE-1;
        
        probHoldingQueueLength[holdingQueueSize] += (currentTime - oldT);
        probServiceQueueLength[serviceQueueSize] += (currentTime - oldT);
        //Service + content
        probTotalInSystem[serviceQueueSize+contentQueueSize] += (currentTime - oldT);
        //Holding + Service + content.
        probAllInSystem[all] += (currentTime - oldT);
        oldT = currentTime;
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
    
    public void registerHoldingTime(Patient p,double t, int currTimePeriodIndex){
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
        averageWaitTimePerIteration[currTimePeriodIndex] += w;
//        numSamplesPerIterationWaitTime[currTimePeriodIndex] += 1;
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
            out[i] = probTotalInSystem[i]/oldT;
        }
        return out;
    }
    
    public double[] getAllInSystemProbabilities(int max){
        double[] out = new double[max+1];
        for( int i = 0; i < out.length ; i++ ){
            out[i] = probAllInSystem[i]/oldT;
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

    /**
     *
     * @return The mean number of jobs in the system: holding queue + service queue + content queue;
     */
    public double getMeanAllInSystem(){
        double out = 0;
        double[] probs = getAllInSystemProbabilities(MAX_QUEUE-1);
        
        for( int i = 0; i < probs.length; i++ ){
            out += i*probs[i];
        }
        
        return out;
    }
    
    public String getQueueSizeRealizationAsCsv()
    {
        String res = "";
        for( int i = 0 ; i < this.numSamplesPerIterationQueueSize.length ; i++)
        {
            res += "," + /*this.numSamplesPerIterationQueueSize[i] + "," + */ (this.numSamplesPerIterationQueueSize[i] != 0 ? this.averageQueueSizePerIteraton[i]/(double)this.numSamplesPerIterationQueueSize[i] : -1) ;
        }
        return res;
    }

    public String getArrivalRateRealizationAsCsv(int binSizeInSec)
    {
        String res = "";
        for( int i = 0 ; i < this.numArrivalsPerIteration.length ; i++)
        {
            res += "," + this.numArrivalsPerIteration[i]/(double)binSizeInSec  ;
        }
        return res;
    }

    public String getAssignRateRealizationAsCsv(int binSizeInSec)
    {
        String res = "";
        for( int i = 0 ; i < this.numAssignmentsPerIteration.length ; i++)
        {
            res += "," + this.numAssignmentsPerIteration[i]/(double)binSizeInSec  ;
        }
        return res;
    }


    public String getTimeInQueueRealizationAsCsv()
    {
        String res = "";
        for( int i = 0 ; i < this.numAssignmentsPerIteration.length ; i++)
        {
            res += "," + /*this.numSamplesPerIterationWaitTime[i] + "," + */ (this.numAssignmentsPerIteration[i] != 0 ? this.averageWaitTimePerIteration[i]/this.numAssignmentsPerIteration[i] : -1 );
        }
        return res;
    }

    //This is inaccurate, since the agentLoadPerIteration counts the overall load over the entire call center, including offline agents which are still working on their existing jobs.
    public String getOnlineAgentLoadRealizationAsCsv()
    {
        String res = "";
        for( int i = 0 ; i < this.onlineAgentLoadPerIteration.length ; i++)
        {
            double currLoad = (this.numSamplesForAgentLoad[i] != 0 ? this.onlineAgentLoadPerIteration[i]/(double)this.numSamplesForAgentLoad[i]/(this.staffing[i]/numSamplesForStaffing[i])  : -1) ;

            res += "," + currLoad ;
        }
        return res;
    }


    public String getAllAgentLoadRealizationAsCsv()
    {
        String res = "";
        for( int i = 0 ; i < this.allAgentLoadPerIteration.length ; i++)
        {
//            double currLoadAllSystemAgents = (this.numSamplesForAgentLoad[i] != 0 ? (this.agentLoadPerIteration[i]/(double)(this.maxTotalCapacity*this.numSamplesForAgentLoad[i]))  : -1 );
            double currLoadAllSystemAgents = (this.numSamplesForAgentLoad[i] != 0 ? this.allAgentLoadPerIteration[i]/(double)(this.numSamplesForAgentLoad[i])/(this.staffing[i]/numSamplesForStaffing[i])  : -1 );
            res += ","  + currLoadAllSystemAgents;
        }
        return res;
    }

    public String getStaffingRealizationAsCsv()
    {
        String res = "";
        for( int i = 0 ; i < this.staffing.length ; i++)
        {
            double currSaffing = (this.numSamplesForStaffing[i] != 0 ? this.staffing[i]/(double)(this.numSamplesForStaffing[i])  : -1 );
            res += ","  + currSaffing;
        }
        return res;
    }

    public String getNumExchangesPerConvRealizationAsCsv()
    {
        String res = "";
        for( int i = 0 ; i < this.numConvs.length ; i++)
        {
            double avgNumExchangesPerConv = (this.numConvs[i] != 0 ? this.numExchangesPerConv[i]/(double)(this.numConvs[i])  : -1 );
            res += ","  + avgNumExchangesPerConv;
        }
        return res;
    }

    public String getAbandonmentRealizationAsCsv()
    {
        String res = "";
        for( int i = 0 ; i < this.numArrivalsPerIteration.length ; i++)
        {
            double avgAbanRate = (this.numArrivalsPerIteration[i] != 0 ? this.numAbandonedPerIteration[i]/(double)(this.numArrivalsPerIteration[i])  : -1 );
            res += ","  + avgAbanRate;
        }
        return res;
    }


    public String getAvgExchangeDuration() {
        String res = "";
        for( int i = 0 ; i < this.numConvs.length ; i++)
        {
            res += ","  +  (this.numConvs[i] != 0 ? this.exchangeDuration[i]/(double)(this.numConvs[i])  : -1 );
        }
        return res;

    }


    public String getAvgInterExchangeDuration() {
        String res = "";
        for( int i = 0 ; i < this.numConvs.length ; i++)
        {
            res += ","  +  (this.numConvs[i] != 0 ? this.interExchangeDuration[i]/(double)(this.numConvs[i])  : -1 );
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

    //TODO: add the wait time distribution as well (i.e. how long abandoned patient waited - both for silent and known abandonment.)
    public void registerAbandonment(int currTimePeriod) {
        this.numAbandonedPerIteration[currTimePeriod] += 1;
    }
}