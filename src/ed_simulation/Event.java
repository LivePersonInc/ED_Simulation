package ed_simulation;

public class Event {

  /* Arrival Event */
  public static final int ARRIVAL = 1;
  /* Departure Event */
  public static final int SERVICE = 2;
  
  public static final int SERVICEH = 22;
  
  public static final int SERVICEY = 222;
  
  public static final int CONTENT = 3;
  
  public static final int CONTENTH = 33;
  
  public static final int CONTENTY = 333;
  /* Event type */
  public static final int NEWINTERVAL = 4;
  
  //spontaneous service event
  public static final int SPONTANEOUSSERV = 5;
  
  public static final int ABANDONMENT = 6;
  
  protected int type;
  /* Event time */
  protected double time;
  /* Event customer */
  protected Patient patient;
  /* Assigned server */
  protected int server_ind;

  
  /**
   * Constructs an Event
   * @param type the Event type (ARRIVAL or DEPARTURE)
   * @param time the Event time
   */
   
  public Event(int type, double time) {
    this.type = type;
    this.time = time;
  }
  
  public Event(int type, double time, Patient p){
    this.type = type;
    this.time = time; 
    this.patient = p;
  }


  public Event(int type, double time, Patient p, int server_ind){
    this(type,  time, p);
    this.server_ind = server_ind;
  }

 
  /**
   * Returns the event type.
   * @return event type
   */
  
  public int getType() {
    return type;
  }
  
  /**
   * Returns the event time.
   * @return event time
   */

  public double getTime() {
    return time;
  }
  
  
  public Patient getPatient(){
      return patient;
  }

  public int getAssignedServerInd() {return server_ind; }
  
} 