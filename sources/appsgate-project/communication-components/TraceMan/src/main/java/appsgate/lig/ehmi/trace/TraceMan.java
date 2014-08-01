package appsgate.lig.ehmi.trace;

import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import appsgate.lig.context.device.properties.table.spec.DevicePropertiesTableSpec;
import appsgate.lig.ehmi.spec.GrammarDescription;
import appsgate.lig.ehmi.spec.EHMIProxySpec;
import appsgate.lig.ehmi.spec.messages.NotificationMsg;
import appsgate.lig.ehmi.spec.trace.TraceManSpec;
import appsgate.lig.eude.interpreter.spec.ProgramLineNotification;
import appsgate.lig.eude.interpreter.spec.ProgramNotification;
import appsgate.lig.manager.place.spec.PlaceManagerSpec;
import appsgate.lig.manager.place.spec.SymbolicPlace;
import appsgate.lig.persistence.MongoDBConfiguration;

/**
 * This component get CHMI from the EHMI proxy and got notifications for each
 * event in the EHMI layer to merge them into a JSON stream.
 *
 * @author Cedric Gerard
 * @since July 13, 2014
 * @version 1.0.0
 */
public class TraceMan implements TraceManSpec {

    /**
     * The logger
     */
    private final static Logger LOGGER = LoggerFactory.getLogger(TraceMan.class);

    /**
     * Dependence to the main EHMI component
     */
    private EHMIProxySpec EHMIProxy;

    /**
     * Dependence to the device property table
     */
    private DevicePropertiesTableSpec devicePropTable;

    /**
     * Dependencies to the place manager
     */
    private PlaceManagerSpec placeManager;

    /*
     * The collection containing the links (wires) created, and deleted
     */
    private MongoDBConfiguration myConfiguration;

    /**
     * Map for device id to their user friendly name
     */
    private final HashMap<String, String> deviceTypeName = new HashMap<String, String>();
    /**
     * Map for device id to their user friendly name
     */
    private final HashMap<String, GrammarDescription> deviceGrammar = new HashMap<String, GrammarDescription>();

    /**
     *
     */
    private TraceHistory fileTracer;
    /**
     *
     */
    private TraceHistory dbTracer;

    /**
     * Called by APAM when an instance of this implementation is created
     */
    public void newInst() {
        fileTracer = new TraceFile();
        if (!fileTracer.init()) {
            LOGGER.warn("Unable to start the tracer");
        }
        dbTracer = new TraceMongo(myConfiguration);
        if (!dbTracer.init()) {
            LOGGER.warn("Unable to start the tracer");
        }
    }

    /**
     * Called by APAM when an instance of this implementation is removed
     */
    public void deleteInst() {
        fileTracer.close();
        dbTracer.close();
    }

    private void trace(JSONObject o) {
        dbTracer.trace(o);
        fileTracer.trace(o);
        //liveTracer.trace(o);
    }

    @Override
    public synchronized void commandHasBeenPassed(String objectID, String command, String caller) {
        if (deviceTypeName.get(objectID) != null) { //if the equipment has been instantiated from ApAM spec before
            //Create the event description device entry
            JSONObject event = new JSONObject();
            JSONObject deviceJson = getJSONDevice(objectID, event, Trace.getJSONDecoration("write", "user", null, objectID, "User trigger this action using HMI"));
            //Create the notification JSON object
            JSONObject coreNotif = getCoreNotif(deviceJson, null);
            //Trace the notification JSON object in the trace file
            trace(coreNotif);
        }
    }

    @Override
    public synchronized void coreEventNotify(long timeStamp, String srcId, String varName, String value) {
        if (deviceTypeName.get(srcId) != null) { //if the equipment has been instantiated from ApAM spec before
            //Create the event description device entry
            JSONObject event = new JSONObject();
            try {
                event.put("type", "update");
                event.put("state", getDeviceState(srcId, varName, value));
            } catch (JSONException e) {
            }
            JSONObject deviceJson = getJSONDevice(srcId, event, null);
            //Create the notification JSON object
            JSONObject coreNotif = getCoreNotif(deviceJson, null);
            //Trace the notification JSON object in the trace file
            trace(coreNotif);
        }
    }

    private JSONObject getCoreNotif(JSONObject device, JSONObject program) {
        JSONObject coreNotif = new JSONObject();
        try {
            coreNotif.put("timestamp", EHMIProxy.getCurrentTimeInMillis());
            //Create the device tab JSON entry
            JSONArray deviceTab = new JSONArray();
            {
                if (device != null) {
                    deviceTab.put(device);
                }
                coreNotif.put("devices", deviceTab);
            }
            //Create the device tab JSON entry
            JSONArray pgmTab = new JSONArray();
            {
                if (program != null) {
                    pgmTab.put(program);
                }
                coreNotif.put("programs", pgmTab);
            }
        } catch (JSONException e) {

        }
        return coreNotif;
    }

    private JSONObject getJSONDevice(String srcId, JSONObject event, JSONObject cause) {
        JSONObject objectNotif = new JSONObject();
        try {
            objectNotif.put("id", srcId);
            objectNotif.put("name", devicePropTable.getName(srcId, ""));
            objectNotif.put("type", deviceTypeName.get(srcId));
            JSONObject location = new JSONObject();
            location.put("id", placeManager.getCoreObjectPlaceId(srcId));
            SymbolicPlace place = placeManager.getPlaceWithDevice(srcId);
            if (place != null) {
                location.put("name", place.getName());
            }
            objectNotif.put("location", location);
            objectNotif.put("decoration", cause);
            objectNotif.put("event", event);

        } catch (JSONException e) {

        }
        return objectNotif;

    }

    @Override
    public synchronized void coreUpdateNotify(long timeStamp, String srcId, String coreType,
            String userType, String name, JSONObject description, String eventType) {

        //Put the user friendly core type in the map
        GrammarDescription grammar = EHMIProxy.getGrammarFromType(userType);

        addUserType(srcId, userType);
        deviceGrammar.put(srcId, grammar);

        JSONObject event = new JSONObject();
        JSONObject cause = new JSONObject();
        try {
            if (eventType.contentEquals("new")) {
                event.put("type", "appear");
                cause = Trace.getJSONDecoration("", "technical", null, null, "Equipment appear");
                event.put("state", getDeviceState(srcId, "", ""));

            } else if (eventType.contentEquals("remove")) {
                event.put("type", "disappear");
                cause = Trace.getJSONDecoration("", "technical", null, null, "Equipment disappear");
            }

        } catch (JSONException e) {

        }

        JSONObject jsonDevice = getJSONDevice(srcId, event, cause);
        JSONObject coreNotif = getCoreNotif(jsonDevice, null);
        //Trace the notification JSON object in the trace file
        trace(coreNotif);

    }

    /**
     *
     * @param n
     */
    public synchronized void gotNotification(NotificationMsg n) {
        if (!(n instanceof ProgramNotification)) {
            return;
        }
        if (n instanceof ProgramLineNotification) {
            JSONObject o = getDecorationNotification((ProgramLineNotification) n);
            trace(o);
            return;
        }
        ProgramNotification notif = (ProgramNotification) n;
        //Create the notification JSON object
        //Create a device trace entry
        //Trace the notification JSON object in the trace file
        JSONObject jsonProgram = getJSONProgram(notif.getProgramId(), notif.getProgramName(), notif.getVarName(), notif.getRunningState(), null);

        trace(getCoreNotif(null, jsonProgram));
    }

    @Override
    public JSONArray getTraces(Long timestamp, Integer number) {
        return dbTracer.get(timestamp, number);
    }

    @Override
    public JSONArray getTracesBetweenInterval(Long start, Long end) {
        return dbTracer.getInterval(start, end);
    }

    private JSONObject getJSONProgram(String id, String name, String change, String state, String iid) {
        JSONObject progNotif = new JSONObject();
        try {
            progNotif.put("id", id);
            progNotif.put("name", name);

            //Create the event description device entry
            JSONObject event = new JSONObject();
            JSONObject cause = new JSONObject();
            {
                JSONObject s = new JSONObject();
                s.put("name", state);
                s.put("instruction_id", iid);
                event.put("state", s);
                if (change != null) {
                    if (change.contentEquals("newProgram")) {
                        event.put("type", "appear");
                        cause = Trace.getJSONDecoration("", "user", null, null, "Program has been added");
                    } else if (change.contentEquals("removeProgram")) {
                        event.put("type", "disappaear");
                        cause = Trace.getJSONDecoration("", "user", null, null, "Program has been deleted");

                    } else { //change == "updateProgram"
                        event.put("type", "update");
                    }
                    //Create causality event entry
                    if (change.contentEquals("updateProgram")) {
                        cause = Trace.getJSONDecoration("", "user", null, null, "Program has been updated");
                    }
                }

            }
            progNotif.put("event", event);
            progNotif.put("decoration", cause);
        } catch (JSONException e) {

        }
        return progNotif;
    }

    private JSONObject getDeviceState(String srcId, String varName, String value) {
        try {
            JSONObject deviceState = new JSONObject();

            String deviceFriendlyType = deviceTypeName.get(srcId);

            if (deviceFriendlyType.contentEquals("Temperature")) {
                if (varName.contentEquals("currentTemperature")) {
                    deviceState.put("value", value);
                    deviceState.put("status", "2");
                } else if (varName.contentEquals("status")) {
                    deviceState.put("status", value);
                    deviceState.put("value", EHMIProxy.getDevice(srcId).getString("value"));
                } else {
                    deviceState.put("status", "2");
                    deviceState.put("value", EHMIProxy.getDevice(srcId).getString("value"));
                }

            } else if (deviceFriendlyType.contentEquals("Illumination")
                    || deviceFriendlyType.contentEquals("Co2")) {
                if (varName.contains("current")) {
                    deviceState.put("value", value);
                    deviceState.put("status", "2");
                } else if (varName.contentEquals("status")) {
                    deviceState.put("status", value);
                    deviceState.put("value", EHMIProxy.getDevice(srcId).getString("value"));
                } else {
                    deviceState.put("status", "2");
                    deviceState.put("value", EHMIProxy.getDevice(srcId).getString("value"));
                }

            } else if (deviceFriendlyType.contentEquals("Switch")) {
                if (varName.contains("switchNumber")) {
                    deviceState.put("switchNumber", value);
                    deviceState.put("buttonStatus", "true");
                    deviceState.put("status", "2");
                } else if (varName.contentEquals("status")) {
                    deviceState.put("status", value);
                    JSONObject descr = EHMIProxy.getDevice(srcId);
                    deviceState.put("switchNumber", descr.getString("switchNumber"));
                    deviceState.put("buttonStatus", descr.getInt("buttonStatus"));
                } else {
                    JSONObject descr = EHMIProxy.getDevice(srcId);
                    deviceState.put("status", descr.getString("status"));
                    deviceState.put("switchNumber", descr.getString("switchNumber"));
                    deviceState.put("buttonStatus", descr.getInt("buttonStatus"));
                }

            } else if (deviceFriendlyType.contentEquals("Contact")
                    || deviceFriendlyType.contentEquals("KeyCardSwitch")
                    || deviceFriendlyType.contentEquals("Occupancy")
                    || deviceFriendlyType.contentEquals("OnOffActuator")) {

                if (varName.contains("current") || varName.contains("occupied")
                        || varName.contains("isOn")) {

                    deviceState.put("status", "2");
                    deviceState.put("value", value);

                } else if (varName.contentEquals("status")) {

                    deviceState.put("status", value);
                    if (deviceFriendlyType.contentEquals("Contact")) {
                        deviceState.put("value", EHMIProxy.getDevice(srcId).getString("contact"));
                    } else if (deviceFriendlyType.contentEquals("KeyCardSwitch")) {
                        deviceState.put("value", EHMIProxy.getDevice(srcId).getString("inserted"));
                    } else if (deviceFriendlyType.contentEquals("Occupancy")) {
                        deviceState.put("value", EHMIProxy.getDevice(srcId).getString("occupied"));
                    } else if (deviceFriendlyType.contentEquals("OnOffActuator")) {
                        deviceState.put("value", EHMIProxy.getDevice(srcId).getString("isOn"));
                    }
                } else {
                    deviceState.put("status", "2");
                    if (deviceFriendlyType.contentEquals("Contact")) {
                        deviceState.put("value", EHMIProxy.getDevice(srcId).getString("contact"));
                    } else if (deviceFriendlyType.contentEquals("KeyCardSwitch")) {
                        deviceState.put("value", EHMIProxy.getDevice(srcId).getString("inserted"));
                    } else if (deviceFriendlyType.contentEquals("Occupancy")) {
                        deviceState.put("value", EHMIProxy.getDevice(srcId).getString("occupied"));
                    } else if (deviceFriendlyType.contentEquals("OnOffActuator")) {
                        deviceState.put("value", EHMIProxy.getDevice(srcId).getString("isOn"));
                    }
                }

            } else if (deviceFriendlyType.contentEquals("SmartPlug")) {
                if (varName.contains("plugState")) {
                    deviceState.put("plugState", value);
                    deviceState.put("consumption", EHMIProxy.getDevice(srcId).getString("consumption"));
                    deviceState.put("status", "2");
                } else if (varName.contentEquals("consumption")) {
                    deviceState.put("status", "2");
                    deviceState.put("plugState", EHMIProxy.getDevice(srcId).getString("plugState"));
                    deviceState.put("consumption", value);
                } else if (varName.contentEquals("status")) {
                    deviceState.put("status", value);
                    JSONObject descr = EHMIProxy.getDevice(srcId);
                    deviceState.put("plugState", descr.getString("plugState"));
                    deviceState.put("consumption", descr.getString("consumption"));
                } else {
                    JSONObject descr = EHMIProxy.getDevice(srcId);
                    deviceState.put("status", descr.getString("status"));
                    deviceState.put("plugState", descr.getString("plugState"));
                    deviceState.put("consumption", descr.getString("consumption"));
                }
            } else if (deviceFriendlyType.contentEquals("Colorlight")) {
                JSONObject descr = EHMIProxy.getDevice(srcId);
                if (varName.contains("state")) {
                    deviceState.put("state", value);
                    deviceState.put("color", descr.getInt("color"));
                    deviceState.put("saturation", descr.getInt("saturation"));
                    deviceState.put("brightness", descr.getInt("brightness"));
                    deviceState.put("status", descr.getString("status"));

                } else if (varName.contentEquals("hue")) {
                    deviceState.put("state", descr.getBoolean("value"));
                    deviceState.put("color", value);
                    deviceState.put("saturation", descr.getInt("saturation"));
                    deviceState.put("brightness", descr.getInt("brightness"));
                    deviceState.put("status", descr.getString("status"));

                } else if (varName.contentEquals("sat")) {
                    deviceState.put("state", descr.getBoolean("value"));
                    deviceState.put("color", descr.getInt("color"));
                    deviceState.put("saturation", value);
                    deviceState.put("brightness", descr.getInt("brightness"));
                    deviceState.put("status", descr.getString("status"));

                } else if (varName.contentEquals("bri")) {
                    deviceState.put("state", descr.getBoolean("value"));
                    deviceState.put("color", descr.getInt("color"));
                    deviceState.put("saturation", descr.getInt("saturation"));
                    deviceState.put("brightness", value);
                    deviceState.put("status", descr.getString("status"));

                } else if (varName.contentEquals("reachable")) {
                    deviceState.put("state", descr.getBoolean("value"));
                    deviceState.put("color", descr.getInt("color"));
                    deviceState.put("saturation", descr.getInt("saturation"));
                    deviceState.put("brightness", descr.getInt("brightness"));
                    if (value.contentEquals("true")) {
                        deviceState.put("status", "2");
                    } else {
                        deviceState.put("status", "0");
                    }
                } else {
                    deviceState.put("state", descr.getBoolean("value"));
                    deviceState.put("color", descr.getInt("color"));
                    deviceState.put("saturation", descr.getInt("saturation"));
                    deviceState.put("brightness", descr.getInt("brightness"));
                    deviceState.put("status", descr.getString("status"));
                }
            } else if (deviceFriendlyType.contentEquals("SystemClock")) {
                //TODO Add state profile in next version

            } else if (deviceFriendlyType.contentEquals("MediaPlayer")) {
                //TODO Add state profile in next version

            } else if (deviceFriendlyType.contentEquals("MediaBrowser")) {
                //TODO Add state profile in next version

            } else if (deviceFriendlyType.contentEquals("GoogleCalendar")) {
                //TODO Add state profile in next version

            } else if (deviceFriendlyType.contentEquals("Mail")) {
                //TODO Add state profile in next version

            } else if (deviceFriendlyType.contentEquals("Weather")) {
                //TODO Add state profile in next version

            } else if (deviceFriendlyType.contentEquals("MobileDevice")) {
                //TODO Add state profile in next version

            } else if (deviceFriendlyType.contentEquals("DomiCube")) {

                JSONObject descr = EHMIProxy.getDevice(srcId);
                if (varName.contains("newFace")) {
                    deviceState.put("activeFace", value);
                    deviceState.put("batteryLevel", descr.getString("batteryLevel"));
                    deviceState.put("dimValue", descr.getString("dimValue"));
                    deviceState.put("status", descr.getString("status"));

                } else if (varName.contentEquals("newBatteryLevel")) {
                    deviceState.put("activeFace", descr.getString("activeFace"));
                    deviceState.put("batteryLevel", value);
                    deviceState.put("dimValue", descr.getString("dimValue"));
                    deviceState.put("status", descr.getString("status"));

                } else if (varName.contentEquals("newDimValue")) {
                    deviceState.put("activeFace", descr.getString("activeFace"));
                    deviceState.put("batteryLevel", descr.getString("batteryLevel"));
                    deviceState.put("dimValue", value);
                    deviceState.put("status", descr.getString("status"));

                } else if (varName.contentEquals("status")) {
                    deviceState.put("activeFace", descr.getString("activeFace"));
                    deviceState.put("batteryLevel", descr.getString("batteryLevel"));
                    deviceState.put("dimValue", descr.getString("dimValue"));
                    deviceState.put("status", value);
                } else {
                    deviceState.put("activeFace", descr.getString("activeFace"));
                    deviceState.put("batteryLevel", descr.getString("batteryLevel"));
                    deviceState.put("dimValue", descr.getString("dimValue"));
                    deviceState.put("status", descr.getString("status"));
                }

            } else {
                if (varName.contentEquals("status")) {
                    deviceState.put("status", value);
                }
            }

            return deviceState;

        } catch (JSONException ex) {
            LOGGER.error("JSONException thrown: " + ex.getMessage());
        }
        return null;
    }

    private void addUserType(String srcId, String userType) {

        String typeFriendlyName;
        int i_userType = Integer.valueOf(userType);

        switch (i_userType) {
            case 0:
                typeFriendlyName = "Temperature";
                break;
            case 1:
                typeFriendlyName = "Illumination";
                break;
            case 2:
                typeFriendlyName = "Switch";
                break;
            case 3:
                typeFriendlyName = "Contact";
                break;
            case 4:
                typeFriendlyName = "KeyCardSwitch";
                break;
            case 5:
                typeFriendlyName = "Occupancy";
                break;
            case 6:
                typeFriendlyName = "SmartPlug";
                break;
            case 7:
                typeFriendlyName = "Colorlight";
                break;
            case 8:
                typeFriendlyName = "OnOffActuator";
                break;
            case 9:
                typeFriendlyName = "Co2";
                break;
            case 21:
                typeFriendlyName = "SystemClock";
                break;
            case 31:
                typeFriendlyName = "MediaPlayer";
                break;
            case 36:
                typeFriendlyName = "MediaBrowser";
                break;
            case 101:
                typeFriendlyName = "GoogleCalendar";
                break;
            case 102:
                typeFriendlyName = "Mail";
                break;
            case 103:
                typeFriendlyName = "Weather";
                break;
            case 200:
                typeFriendlyName = "MobileDevice";
                break;
            case 210:
                typeFriendlyName = "DomiCube";
                break;
            case 794225618:
                typeFriendlyName = "ContentDirectory";
                break;
//    		case -532540516:
//    			typeFriendlyName = "Unknown";
//				break;
            case 2052964255:
                typeFriendlyName = "ConnectionManager";
                break;
            case 415992004:
                typeFriendlyName = "AvTransport";
                break;
            case -164696113:
                typeFriendlyName = "RenderingControl";
                break;
//    		case -1943939940:
//    			typeFriendlyName = "Unknown";
//				break;
            default:
                typeFriendlyName = "Undefined";
                break;
        }

        deviceTypeName.put(srcId, typeFriendlyName);
    }

    private JSONObject getDecorationNotification(ProgramLineNotification n) {
        JSONObject p = getJSONProgram(n.getProgramId(), n.getProgramName(), null, n.getRunningState(), n.getInstructionId());
        JSONObject d = getJSONDevice(n.getTargetId(), null, Trace.getJSONDecoration(n.getType(), "Program", n.getSourceId(), null, n.getDescription()));
        try {
            p.put("decoration", Trace.getJSONDecoration(n.getType(), "Program", null, n.getTargetId(), n.getDescription()));
        } catch (JSONException ex) {
        }
        return getCoreNotif(d, p);
    }

}