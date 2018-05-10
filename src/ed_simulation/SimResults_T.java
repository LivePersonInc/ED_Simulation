package ed_simulation;


public class SimResults_T{
    
    
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
    protected double sumTotalW;
    protected double sumTotalW2;
        
    static final int MAX_QUEUE = 1000;
    protected double oldT = 0;
    
    protected int n;
    protected int nIntervals;
    
    protected double utilizationNurses;
    protected double utilizationBeds;
    
    protected double prevUtilizationNurses;
    protected double prevUtilizationBeds;

      
    public SimResults_T( int n, int nIntervals){
        this.nIntervals = nIntervals;
        this.probHoldingQueueLength = new double[nIntervals][MAX_QUEUE];
        this.probAllInSystem = new double[nIntervals][MAX_QUEUE];
        this.sumH =  new double[nIntervals];
        this.sumH2 = new double[nIntervals];
        this.sumHcond = new double[nIntervals];
        this.sumW2cond = new double[nIntervals];
        this.counterH = new int[nIntervals];
        this.counterHcond = new int[nIntervals];
        
        this.probServiceQueueLength = new double[nIntervals][n+1];
        this.probTotalInSystem = new double[nIntervals][n+1];
        this.sumW = new double[nIntervals];
        this.sumW2 = new double[nIntervals];
        this.sumWcond = new double[nIntervals];
        this.sumW2cond = new double[nIntervals];
        this.counterW = new int[nIntervals];
        this.counterWcond = new int[nIntervals];
        
        this.sumS = new double[nIntervals];
        this.sumS2 = new double[nIntervals];
        this.counterS = new int[nIntervals];
        this.sumTotalW = 0;
        this.sumTotalW2 = 0;   
        
        this.oldT = 0;
        this.n = n;
        
        this.utilizationBeds = 0;
        this.utilizationNurses = 0;
        
        this.prevUtilizationBeds = 0;
        this.prevUtilizationNurses = 0;
    } 
      
    public void registerQueueLengths(int hQueue, int sQueue, int cQueue, int s, int n, double t, int interval){
        if( hQueue >= MAX_QUEUE ) hQueue = MAX_QUEUE-1;
        int all = sQueue+cQueue+hQueue;
        if( all >= MAX_QUEUE ) all = MAX_QUEUE-1;
        
        probHoldingQueueLength[interval][hQueue] += (t - oldT);
        probServiceQueueLength[interval][sQueue] += (t - oldT);
        probTotalInSystem[interval][sQueue+cQueue] += (t - oldT);
        probAllInSystem[interval][all] += (t - oldT);
        
        utilizationNurses += prevUtilizationNurses * (t-oldT);
        utilizationBeds += prevUtilizationBeds * (t-oldT);
        
        prevUtilizationNurses = 1.0*Math.min(s,sQueue) / s;
        prevUtilizationBeds = 1.0*(sQueue + cQueue) / n;
                
        oldT = t;
    } 
    
    public void registerHoldingTime(Patient p, double t){
        double w = t-p.getArrivalTime();
        int i = p.getArrivalInterval();
        p.setHoldingTime(w);
        sumH[i] += w;
        sumH2[i] += w*w;
        counterH[i] ++ ;
        
        if( w > 0 ){
            sumHcond[i] += w;
            //sumH2cond[i] += w*w;
            counterHcond[i]++;
            
        }  
    }
    
    
    public void registerWaitingTime(Patient p, double t, int i){
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
        if ( held) counterHcond[i]++;
    }
    
    
    public void registerDeparture(Patient p, double t){
        double s = t - p.getArrivalTime();
        double tw = s - p.getWaitingTime();
        int i = p.getArrivalInterval();
        sumS[i] += s;
        sumS2[i] += s*s;
        counterS[i]++;
        sumTotalW += tw;
        sumTotalW2 += tw*tw;
    }
    
    public double[] getMeanHoldingQueueLengths(){
        double[] out = new double[nIntervals];
        double time_in_interval = oldT/nIntervals;
        
        for( int i = 0; i < nIntervals; i++ ){
            for( int j = 0; j < probHoldingQueueLength[i].length; j++ ){
                out[i] += j*probHoldingQueueLength[i][j] / time_in_interval;
            }
        }
        
        return out;  
    }
    
    public double[] getMeanServiceQueueLengths(){
        double[] out = new double[nIntervals];
        double time_in_interval = oldT/nIntervals;
        
        for( int i = 0; i < nIntervals; i++ ){
            for( int j = 0; j < probServiceQueueLength[i].length; j++ ){
                out[i] += j*probServiceQueueLength[i][j] / time_in_interval;
            }
        }   
        return out;  
    }

    
    public double[] getMeanTotalInSystem(){
        double[] out = new double[nIntervals];
        double time_in_interval = oldT/nIntervals;
        
        for( int i = 0; i < nIntervals; i++ ){
            for( int j = 0; j < probTotalInSystem[i].length; j++ ){
                out[i] += j*probTotalInSystem[i][j] / time_in_interval;
            }
        }   
        return out;  
    }
    
    public double[] getMeanAllInSystem(){
        double[] out = new double[nIntervals];
        double time_in_interval = oldT/nIntervals;
        
        for( int i = 0; i < nIntervals; i++ ){
            for( int j = 0; j < probAllInSystem[i].length; j++ ){
                out[i] += j*probAllInSystem[i][j] / time_in_interval;
            }
        }   
        return out;  
    }
    
    public double[] getNurseUtilizations( int[] svalues ) {
        
        double[] out = new double[nIntervals];
        double time_in_interval = oldT/nIntervals;
        
        for( int i = 0; i < nIntervals; i++ ){
            for( int j = 0; j < probServiceQueueLength[i].length; j++ ){
                out[i] += Math.min(j,svalues[i])*probServiceQueueLength[i][j] / ( time_in_interval * svalues[i] );
            }
        }   
        return out;  
    }

    public double[] getMeanHoldingTimes(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = sumH[i] / counterH[i];
        }
        return out;
    }
    
    public double[] getVarianceHoldingTimes(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = ( sumH2[i] / counterH[i] - (sumH[i] / counterH[i])*(sumH[i] / counterH[i]) );
        }
        return out;
    }
    
    public double getTotalMeanHoldingTime(){
        double s = 0;
        int cS = 0;
        for( int i = 0; i < nIntervals; i++ ){
            s += sumH[i];
            cS += counterH[i];
        }
        return s / cS;
    }
    
    public double getTotalMeanHoldingTimeConditional(){
        double s = 0;
        int cS = 0;
        for( int i = 0; i < nIntervals; i++ ){
            s += sumHcond[i];
            cS += counterHcond[i];
        }
        return s / cS;
    }
    
    public double[] getMeanWaitingTimes(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = sumW[i] / counterW[i];
        }
        return out;
    }
    
    public double[] getMeanWaitingTimesConditional(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = sumWcond[i] / counterWcond[i];
        }
        return out;
    }
    
    public double[] getVarianceWaitingTimes(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = ( sumW2[i] / counterW[i] - (sumW[i] / counterW[i])*(sumW[i] / counterW[i]) );
        }
        return out;
    }
        
    public double getTotalMeanWaitingTime(){
        double s = 0;
        int cS = 0;
        for( int i = 0; i < nIntervals; i++ ){
            s += sumW[i];
            cS += counterW[i];
        }
        return 1.0*s / cS;
    }
    
    
    public double getTotalMeanWaitingTimeConditional(){
        double s = 0;
        int cS = 0;
        for( int i = 0; i < nIntervals; i++ ){
            s += sumWcond[i];
            cS += counterWcond[i];
        }
        return s / cS;
    }
    
//    public double[] getCITotalWaitingTime(){
//        double mean = getMeanTotalWaitingTime();
//        double var = getVarianceTotalWaitingTime();
//        double[] out = { mean - 1.96*Math.sqrt(var / counterS) , mean + 1.96*Math.sqrt(var / counterS) };
//        return out;
//    }
    
    
    public double[] getMeanSojournTimes(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = sumS[i] / counterS[i];
        }
        return out;
    }
    
    public double getTotalMeanSojournTime(){
        double s = 0;
        int cS = 0;
        for( int i = 0; i < nIntervals; i++ ){
            s += sumS[i];
            cS += counterS[i];
        }
        return 1.0* s / cS; 
    }
    
    
    public double[] getVarianceSojournTime(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = ( sumS2[i] / counterS[i] - (sumS[i] / counterS[i])*(sumS[i] / counterS[i]) );
        }
        return out;
    }
    
//    public double[] getCISojournTime(){
//        double mean = getMeanSojournTime();
//        double var = getVarianceSojournTime();
//        double[] out = { mean - 1.96*Math.sqrt(var / counterS) , mean + 1.96*Math.sqrt(var / counterS) };
//        return out;
//    }
    
    
//    public double[] getCIdelayProbability(){
//        double mean = getWaitingProbability();
//        double var = mean - mean*mean;
//        double[] out =  {( mean - Math.sqrt(var / counterW )), (mean + Math.sqrt( var / counterW )) };
//        return out;
//    }
       
    
    public double getHoldingProbability(){
        int h = 0;
        int hCond = 0;
        for( int i = 0; i < nIntervals; i++ ){
            h += counterH[i];
            hCond += counterHcond[i];
        }
        return (1.0*hCond / h);
    }
    
    public double[] getHoldingProbabilities(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = 1.0 *  counterHcond[i] / counterH[i];
        }
        return out;
    }
    
    public double getWaitingProbability(){
        int w = 0;
        int wCond = 0;
        for( int i = 0; i < nIntervals; i++ ){
            w += counterW[i];
            wCond += counterWcond[i];
        }
        return (1.0*wCond / w);
    }
    
    public double[] getWaitingProbabilities(){
        double[] out = new double[nIntervals];
        for( int i = 0; i < nIntervals; i++ ){
            out[i] = 1.0 *  counterWcond[i] / counterW[i];
        }
        return out;
    }
    
//    public double getHoldingProbability2(){
//        return (getTotalInSystemProbabilities()[n]);
//    }
//    
//    public double getWaitingProbability2(int s){
//        double[] pr = getServiceQueueLengthProbabilities();
//        double prob = 0;
//        for( int i = s; i <= n ; i++ ){
//            prob += pr[i];
//        }
//        return prob;
//    }
//    
    
    
    
    public double getUtilNurses(){
        return utilizationNurses / oldT;
    }
    
    public double getUtilBeds(){
        return utilizationBeds / oldT;
    }
    
    
    
       
    
    
    
}