/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.components;

import appsgate.lig.eude.interpreter.langage.exceptions.SpokNodeException;
import appsgate.lig.eude.interpreter.langage.exceptions.SpokException;
import appsgate.lig.eude.interpreter.langage.exceptions.SpokSymbolTableException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jr
 */
public class SpokVariable implements SpokObject {

    // Logger
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(SpokVariable.class);

    private final String id;
    private final String type;
    private final JSONObject json;

    /**
     * Constructor
     *
     * @param i the id
     * @param t the type
     */
    public SpokVariable(String i, String t) {
        this.id = i;
        this.type = t;
        this.json = null;
    }

    /**
     *
     * @param obj
     * @throws SpokNodeException
     */
    public SpokVariable(JSONObject obj) throws SpokException {
        this.id = obj.optString("id");
        this.type = obj.optString("type");
        this.json = obj;
        checkVariable();
    }

    /**
     *
     * @param v_name
     * @param jsonVariable
     * @throws SpokException
     */
    public SpokVariable(String v_name, JSONObject jsonVariable) throws SpokException {
        this.id = v_name;
        this.type = jsonVariable.optString("type");
        this.json = jsonVariable;
        checkVariable();
    }

    /**
     *
     * @param t
     * @param value
     */
    public SpokVariable(String t, Object value) {
        this.id = null;
        this.type = t;
        this.json = new JSONObject(value);
    }

    /**
     * Return the text which is used to build a text program from a program tree
     *
     * @return
     */
    public String getExpertProgramDecl() {
        if (this.json == null) {
            return "{ type: " + this.type + ", id: " + this.id + "}";
        } else {
            return json.toString();
        }
    }

    /**
     * two variables are equals if they have the same id and type
     *
     * @param other
     * @return true if both variables are the same
     */
    public boolean equals(SpokVariable other) {
        return other.id.equals(this.id) && other.type.equals(this.type);
    }

    /**
     * Return the list of events of a variable
     *
     * By default a variable can not throw any events
     *
     * @return the events that can be generated by this variable
     */
    public Set<String> getEventList() {
        return null;
    }

    /**
     * Return the list of actions of a variable
     *
     * By default a variable can not do any action
     *
     * @return the actions that can be generated by this variable
     */
    public Set<String> getActionList() {
        return null;
    }

    /**
     * Return the list of states a variable can have
     *
     * By default a variable has no states
     *
     * @return the events that can be generated by this variable
     */
    public Set<String> getStateList() {
        return null;
    }

    /**
     * @return the type of this variable
     */
    @Override
    public String getType() {
        return this.type;
    }

    @Override
    public String getValue() {
        return json.optString("value");
    }

    /**
     * @return the name of the variable
     */
    public String getName() {
        return this.id;
    }

    /**
     * Method to get the variables of a list, if the variable is not a list, it
     * returns null
     *
     * @return a list of Variable or null
     */
    public List<SpokVariable> getElements() {
        try {
            ArrayList<SpokVariable> a = new ArrayList<SpokVariable>();
            JSONArray list = this.json.getJSONArray("list");
            for (int i = 0; i < list.length(); i++) {
                a.add(new SpokVariable(list.getJSONObject(i)));
            }
            return a;

        } catch (JSONException ex) {
            LOGGER.error("list without a list");
            return null;
        } catch (SpokException ex) {
            LOGGER.error("The variable was not well formed");
            return null;
        }
    }

    /**
     * Method that check the availability of the variable, which must have a
     * type and an id
     *
     * @throws SpokSymbolTableException
     */
    private void checkVariable() throws SpokSymbolTableException {
        if (type == null || type.isEmpty()) {
            throw new SpokSymbolTableException("Variable has no type", null);
        }
    }

    /**
     * @return the json description of the variable
     */
    @Override
    public JSONObject getJSONDescription() {
        JSONObject o = new JSONObject();
        try {
            o.put("id", id);
            o.put("type", type);
        } catch (JSONException ex) {
            // Do nothing since 'JSONObject.put(key,val)' would raise an exception
            // only if the key is null, which will never be the case
        }
        return o;
    }

    @Override
    public String toString() {
        return "[var " + this.id + ", " + type + ": + " + this.json.toString() + "]";

    }

}
