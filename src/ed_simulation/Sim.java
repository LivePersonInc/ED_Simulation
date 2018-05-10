/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package ed_simulation;

/**
 *
 * @author bmathijs
 */
abstract class Sim {
    
    public abstract SimResults simulate(double maxTime);
    
    public abstract int getType();
    
}
