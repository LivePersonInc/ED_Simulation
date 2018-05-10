package ed_simulation;


public class SimResults_TimeVarying{
    
    
    protected double[][] probHoldingQueueLength;
    protected double[] sumH;
    protected double[] sumH2;
    protected double[] sumHcond;
    protected double[] sumH2cond;
    protected int[] counterH;
    protected int[] counterHcond;
    
    protected double[][] probServiceQueueLength;
    protected double[] sumW;
    protected double[] sumW2;
    protected double[] sumWcond;
    protected double[] sumW2cond;
    protected int[] counterW;
    protected int[] counterWcond;
    
    protected double[][] probTotalInSystem;
    protected double[][] probAllInSystem;
    
    protected double[] sumS;
    protected double[] sumS2;
    protected int[] counterS;
    protected double[] sumTotalW;
    protected double[] sumTotalW2;
        
    static final int MAX_QUEUE = 10000;
    protected double oldT = 0;
    
    protected int n;

      
    public SimResults_TimeVarying( int n, int intervals){
        this.probHoldingQueueLength = new double[intervals][MAX_QUEUE];
        this.probAllInSystem = new double[intervals][MAX_QUEUE];
        this.sumH = new double[intervals];
        this.sumH2 = new double[intervals];
        this.sumHcond = new double[intervals];
        this.sumW2cond = new double[intervals];
        this.counterH = new int[intervals];
        this.counterHcond = new int[intervals];
        
        this.probServiceQueueLength = new double[intervals][n+1];
        this.probTotalInSystem = new double[intervals][n+1];
        this.sumW = new double[intervals];
        this.sumW2 = new double[intervals];
        this.sumWcond = new double[intervals];
        this.sumW2cond = new double[intervals];
        this.counterW = new int[intervals];
        this.counterWcond = new int[intervals];
        
        this.sumS = new double[intervals];
        this.sumS2 = new double[intervals];
        this.counterS = new int[intervals];
        this.sumTotalW = new double[intervals];
        this.sumTotalW2 = new double[intervals];   
        
        this.oldT = 0;
        this.n = n;
    } 
    
    
    public void registerQueueLengths(int hQueue, int sQueue, int cQueue, double t, int i){
        if( hQueue >= MAX_QUEUE ) hQueue = MAX_QUEUE-1;
        int all = sQueue+cQueue+hQueue;
        if( all >= MAX_QUEUE ) all = MAX_QUEUE-1;
        
        probHoldingQueueLength[i][hQueue] += (t - oldT);
        probServiceQueueLength[i][sQueue] += (t - oldT);
        probTotalInSystem[i][sQueue+cQueue] += (t - oldT);
        probAllInSystem[i][all] += (t - oldT);
        oldT = t;
    } 
    
    public void registerHoldingTime(Patient p,double t,int i){
        double w = t-p.getArrivalTime();
        p.setHoldingTime(w);
        sumH[i] += w;
        sumH2[i] += w*w;
        counterH[i] ++ ;
        
        if( w > 0 ){
            sumHcond[i] += w;
            sumH2cond[i] += w*w;
            counterHcond[i]++;
            
        }  
    }
    
    
    public void registerWaitingTime(Patient p,double t, int i){
        double w = t-p.getLastArrivalTime();
        sumW[i] += w;
        sumW2[i] += w*w;
        counterW[i] ++ ;
        
        if( w > 0 ){
            sumWcond[i] += w;
            sumW2cond[i] += w*w;
            counterWcond[i] ++;
        }    
    }
    
    public void registerOneWait(boolean waited, int i){
        counterW[i]++;
        if ( waited ) counterWcond[i]++;
    }
    
    public void registerOneHold(boolean held, int i){
        counterH[i]++;
        if ( held ) counterHcond[i]++;
    }
    
    
    public void registerDeparture(Patient p, double t,int i){
        double s = t - p.getArrivalTime();
        double tw = s - p.getWaitingTime();
        sumS[i] += s;
        sumS2[i] += s*s;
        counterS[i]++;
        sumTotalW[i] += tw;
        sumTotalW2[i] += tw*tw;
    }
    
    public double[] getHoldingQueueLengthProbabilities( int max, int j ){
        double[] out = new double[max+1];
        for( int i = 0; i < max+1 ; i++ ){
            out[i] = probHoldingQueueLength[j][i]/oldT;
        }
        return out;
    }
    
    public double[] getServiceQueueLengthProbabilities(int j){
        double[] out = new double[probServiceQueueLength.length];
        for( int i = 0; i < out.length ; i++ ){
            out[i] = probServiceQueueLength[j][i]/oldT;
        }
        return out;
    }
    
    public double[] getTotalInSystemProbabilities(int j){
        double[] out = new double[probTotalInSystem.length];
        for( int i = 0; i < out.length ; i++ ){
            out[i] = probTotalInSystem[j][i]/oldT;
        }
        return out;
    }
    
    public double[] getAllInSystemProbabilities(int max, int j){
        double[] out = new double[max+1];
        for( int i = 0; i < out.length ; i++ ){
            out[i] = probAllInSystem[j][i]/oldT;
        }
        return out;
    }
    
    
    public double getMeanHoldingQueueLength(int max,int j ){
        if( max < 0 )  max = MAX_QUEUE;      
        double mean = 0;
        
        for( int i = 0; i < max ; i++ ){
            mean += i*probHoldingQueueLength[j][i]/oldT;
        }
        
        return mean;
    }
    
    public double getMeanServiceQueueLength(int j){     
        double mean = 0;
        
        for( int i = 0; i < probServiceQueueLength.length ; i++ ){
            mean += i*probServiceQueueLength[j][i]/oldT;
        }
        
        return mean;
    }
    
    public double[] getMeanHoldingTime(){
        double[] out = new double[sumH.length];
        for( int i = 0 ; i < sumH.length; i++ ){
            out[i] = sumH[i] / counterH[i];
        }
        return out;
    }
    
    
    
    public double[] getMeanWaitingTime(){
        double[] out = new double[sumW.length];
        for( int i = 0 ; i < sumW.length; i++ ){
            out[i] = sumW[i] / counterW[i];
        }
        return out;
    }
    
    
    
    public double[] getMeanTotalWaitingTime(){
        double[] out = new double[sumW.length];
        for( int i = 0 ; i < sumW.length; i++ ){
            out[i] = sumTotalW[i] / counterS[i];
        }
        return out;
    }
   
    
    
    public double[] getNurseOccupancy(int s){
        double[] sum = new double[sumW.length];
        for( int j = 0; j < probServiceQueueLength.length; j++ ){
        for(int i = 0 ; i < probServiceQueueLength[j].length; i++ ){
            sum[j] += ( probServiceQueueLength[j][i] / oldT ) * Math.min(s,i) / s;
        }
        }
        return sum;
    }
    
//    public double[] getCIdelayProbability(){
//        double mean = getWaitingProbability();
//        double var = mean - mean*mean;
//        double[] out =  {( mean - Math.sqrt(var / counterW )), (mean + Math.sqrt( var / counterW )) };
//        return out;
//    }
       
    
    public double[] getHoldingProbability(){
        double[] out = new double[sumW.length];
        for( int i = 0 ; i < sumW.length; i++ ){
            out[i] = (1.0*counterHcond[i] / counterH[i]);
        }
        return out;
    }
    
    public double[] getWaitingProbability(){
        double[] out = new double[sumW.length];
        for( int i = 0 ; i < sumW.length; i++ ){
            out[i] = (1.0*counterWcond[i] / counterW[i]);
        }
        return out;
    }
    
    
    public double[] getMeanTotalInSystem(){
        double[] out = new double[sumW.length];
        for( int j = 0 ; j < sumW.length; j++ ){
        double[] probs = getTotalInSystemProbabilities(j);
        
        for( int i = 0; i < probs.length; i++ ){
            out[j] += i*probs[i];
        }
        }
        return out;
    }
    
    public double[] getMeanAllInSystem(){
        double[] out = new double[sumW.length];
        for( int j = 0 ; j < sumW.length; j++ ){
        double[] probs = getAllInSystemProbabilities(MAX_QUEUE-1,j);
        
        for( int i = 0; i < probs.length; i++ ){
            out[j] += i*probs[i];
        }
        }
        return out;
    }
    
    
    
       
    
    
    
}