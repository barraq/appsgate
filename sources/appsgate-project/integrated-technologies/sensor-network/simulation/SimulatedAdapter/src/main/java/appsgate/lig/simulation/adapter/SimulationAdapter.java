package appsgate.lig.simulation.adapter;

import java.util.HashMap;

import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;
import org.apache.felix.ipojo.annotations.Requires;
import org.json.JSONArray;
import org.json.JSONException;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.impl.ComponentBrokerImpl;

import appsGate.lig.manager.client.communication.service.send.SendWebsocketsService;
import appsgate.lig.core.object.spec.CoreObjectSpec;
import appsgate.lig.simulation.adapter.services.SimulatedObjectManagementService;

@Component(publicFactory=false)
@Instantiate(name="AppsgateSensorSimulatedAdapter")
@Provides(specifications= {SimulatedObjectManagementService.class})
public class SimulationAdapter implements SimulatedObjectManagementService {
	
	/**
	 * Service to communicate with clients
	 */
	@Requires
	private SendWebsocketsService sendToClientService;
	
	private HashMap<String, Instance> objectList = new HashMap<String, Instance>();
	
	@Override
	public boolean addSimulateObject(String type, HashMap<String, String> properties) {
		
		String implName; 
		if(type.contentEquals(SIMULATED_TEMPERATURE)) {
			implName = "SimulatedTemperatureSensorImpl";
		}else {
			return false;
		}
		
		Implementation impl = CST.apamResolver.findImplByName(null, implName);
		Instance inst = impl.createInstance(null, properties);

		if(objectList.put(properties.get("deviceId"), inst) != null) {
			return true;
		}
		
		return false;
		
	}

	@Override
	public boolean removeSimulatedObject(String objectId) {
		Instance inst = objectList.get(objectId);
		ComponentBrokerImpl.disappearedComponent(inst.getName());
		objectList.remove(objectId);
		return true;
	}

	@Override
	public JSONArray getSimulateObjectlist() {
		JSONArray list = new JSONArray();
		Instance inst;
		for(String key : objectList.keySet()) {
			inst = objectList.get(key);
			try {
				list.put(((CoreObjectSpec)inst.getServiceObject()).getDescription());
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		return list;
	}

	public void sendResponse(int clientId, String respType, JSONArray list) {
		sendToClientService.send(clientId, respType, list);
	}
	
	//TODO add simulate object persistence management

}