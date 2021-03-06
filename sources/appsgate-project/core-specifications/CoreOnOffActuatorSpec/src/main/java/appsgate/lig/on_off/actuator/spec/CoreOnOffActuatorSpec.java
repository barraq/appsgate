package appsgate.lig.on_off.actuator.spec;

/**
 * This java interface is an ApAM specification shared by all ApAM
 * AppsGate application to handle On/Off actuator actions.
 * 
 * @author Cédric Gérard
 * @version 1.0.0
 * @since March 3, 2013
 *
 */
public interface CoreOnOffActuatorSpec {
	
	/**
	 * Get the virtual state of this actuator.
	 * Nothing is sure that the real device is in this corresponding
	 * state.
	 * 
	 * @return true if the device is on and false otherwise.
	 */
	public boolean getTargetState();
	
	/**
	 * Set the device ON state
	 */
	public void on();
	
	/**
	 * Set the device OFF state
	 */
	public void off();

}
