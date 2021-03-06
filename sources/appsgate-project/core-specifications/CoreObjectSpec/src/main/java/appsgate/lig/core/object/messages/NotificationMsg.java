package appsgate.lig.core.object.messages;

import org.json.JSONObject;

import appsgate.lig.core.object.spec.CoreObjectSpec;

/**
 * 
 * Java interface for all ApAM message with AppsGate.
 * 
 * @author Cédric Gérard
 * @since February 13, 2013
 * @version 1.0.0
 *
 */
public interface NotificationMsg {
	
	/**
	 * Get the source of this notification
	 * @return the AbstractObject view of the source object or service
	 */
	public CoreObjectSpec getSource();
	
	/**
	 * Get the new value.
	 * 
	 * @return a string that represent the new value.
	 */
	public String getNewValue();

    /**
     * Get the old value if the device is stateful and if it make sense
     *
     * @return a string that represent the old value or an empty string.
     */
    public String getOldValue();

	/**
	 * Get the variable that changed
	 * @return a string that contains the variable name.
	 */
	public String getVarName();
	
	/**
	 * transform the NotifcationMsg object to JSONSObject.
	 * 
	 * @return a JSONObject that represent the NotificationMsg java object
	 */
	public JSONObject JSONize();

}
