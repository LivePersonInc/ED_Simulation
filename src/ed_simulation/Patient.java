package ed_simulation;


public class Patient{
    
    protected double arrivalTime;
    protected double lastArrivalTime;
    protected double holdingTime;
    protected double totalWaitingTime;
    protected boolean inService;
    protected int arrivalInterval;
    protected int nrVisits;
    
    public Patient(double t){
        this.arrivalTime = t;
        this.totalWaitingTime = 0;
        this.inService = false;
        this.nrVisits = 0;
    }
    
    public Patient(double t, int i){
        this.arrivalTime = t;
        this.totalWaitingTime = 0;
        this.inService = false;
        this.arrivalInterval = i;
        this.nrVisits = 0;
    }
    
    public double getArrivalTime(){
        return arrivalTime;
    }
    
    public int getArrivalInterval(){
        return arrivalInterval;
    }
    
    public void setLastArrivalTime(double t){
        this.lastArrivalTime = t;
    }
    
    public double getLastArrivalTime(){
        return lastArrivalTime;
    }
    
    public void setHoldingTime(double t){
        this.holdingTime = t;
    }
    
    public double getHoldingTime(){
        return holdingTime;
    }
    
    public void addWaitingTime(double t){ //Accumulated wait time, not including holding queue.
        totalWaitingTime += (t-lastArrivalTime);
        //System.out.println(t-lastArrivalTime);
        nrVisits++;
    }
    
    public double getWaitingTime(){
        return totalWaitingTime;
    }
    
    public void setInService( boolean b ){
        inService = b;
    }
    
    public boolean getInService(){
        return inService;        
    }
    
    public int getNrVisits(){
        return nrVisits;
    }
    
}