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
        
    static final int MAX_QUEUE = 1000000;
    protected double oldT = 0;
    
    protected int n;

      
    public SimResults( int n){
        this.probHoldingQueueLength = new double[MAX_QUEUE];
        this.probAllInSystem = new double[MAX_QUEUE];
        this.sumH = 0;
        this.sumH2 = 0;
        this.sumHcond = 0;
        this.sumW2cond = 0;
        this.counterH = 0;
        this.counterHcond = 0;
        
        this.probServiceQueueLength = new double[n+1];
        this.probTotalInSystem = new double[n+1];
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
        this.n = n;
    } 
    
    //HoldingQueue, ServiceQueue, ContentQueue, current time
    //Adds the current time interval spent in the corresponding queue states.
    public void registerQueueLengths(int hQueue, int sQueue, int cQueue, double t){
        if( hQueue >= MAX_QUEUE ) hQueue = MAX_QUEUE-1;
        int all = sQueue+cQueue+hQueue;
        if( all >= MAX_QUEUE ) all = MAX_QUEUE-1;
        
        probHoldingQueueLength[hQueue] += (t - oldT);
        probServiceQueueLength[sQueue] += (t - oldT);
        //Service + holding
        probTotalInSystem[sQueue+cQueue] += (t - oldT);
        //Holding + Service + content.
        probAllInSystem[all] += (t - oldT);
        oldT = t;
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
    
    
    public void registerDeparture(Patient p, double t){
        double s = t - p.getArrivalTime();
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
        return (getTotalInSystemProbabilities()[n]);
    }
    
    public double getWaitingProbability2(int s){
        double[] pr = getServiceQueueLengthProbabilities();
        double prob = 0;
        for( int i = s; i <= n ; i++ ){
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
    
    
    
       
    
    
    
}