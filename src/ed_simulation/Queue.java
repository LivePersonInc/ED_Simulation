package ed_simulation;
import java.util.ArrayList;


public class Queue {

  /* List of customers in the queue */
  protected ArrayList<Patient> patients;  

  
  
  /**
   * Constructs an empty single-server Queue
   */
   
  public Queue() {
    this.patients = new ArrayList<>();
  }
  
  /** 
   * Returns the number of customers in the queue (including the person in service)
   * @return the number of customers
   */
   
  public int getSize() {
    return patients.size();
  }
  
  /**
   * Adds a patient to this queue
     * @param p
   */
   
  public void addPatient(Patient p) {
    patients.add(p);
  }
  
  /** 
   * Returns the first customer in the queue.
   * @return the first customer
   */
  
  public Patient getFirstPatient() {
      return patients.get(0);
  }
  
  
  public Patient getPatientAt(int index){
      return patients.get(index);
  }
  
  /**
   * @return the patient that is removed from the queue
   */
   
  public Patient removeFirstPatient() {
       return patients.remove(0);
  }
  
  public boolean removePatient(Patient p){
      return patients.remove(p);
  }
  
}
  
  
