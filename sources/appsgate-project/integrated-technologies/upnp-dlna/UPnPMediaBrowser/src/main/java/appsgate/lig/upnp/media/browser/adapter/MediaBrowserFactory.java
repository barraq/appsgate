

package appsgate.lig.upnp.media.browser.adapter;

import java.util.Hashtable;
import java.util.Map;

import org.osgi.service.upnp.UPnPDevice;

import fr.imag.adele.apam.CST;
import fr.imag.adele.apam.Implementation;
import fr.imag.adele.apam.Instance;
import fr.imag.adele.apam.impl.ComponentImpl.InvalidConfiguration;
import fr.imag.adele.apam.impl.InstanceImpl;

public class MediaBrowserFactory {
	
	public void mediaServerBound(Instance device) {
		

		try {
			Implementation adapterImplementtation = CST.apamResolver.findImplByName(null,"MediaBrowser");

			String deviceId = device.getProperty(UPnPDevice.ID);

			Map<String,Object> configuration = new Hashtable<String,Object>();
			configuration.put(UPnPDevice.ID,deviceId);
			adapterImplementtation.getApformImpl().addDiscoveredInstance(configuration);

		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public void mediaServerUnbound(Instance device) {

		Implementation adapterImplementtation = CST.apamResolver.findImplByName(null,"MediaBrowser");
		for (Instance player : adapterImplementtation.getInsts()) {
			if (player.getProperty(UPnPDevice.ID).equals(device.getPropertyObject(UPnPDevice.ID)))
				((InstanceImpl)player).unregister();
		}
	}

}
