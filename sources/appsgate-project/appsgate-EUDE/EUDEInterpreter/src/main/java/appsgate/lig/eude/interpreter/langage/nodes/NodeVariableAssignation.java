/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.eude.interpreter.langage.nodes;

import appsgate.lig.eude.interpreter.langage.components.EndEvent;
import appsgate.lig.eude.interpreter.langage.components.SpokObject;
import appsgate.lig.eude.interpreter.langage.components.SpokVariable;
import appsgate.lig.eude.interpreter.langage.exceptions.SpokException;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author jr
 */
public class NodeVariableAssignation extends Node {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeReturn.class.getName());

    private String name;
    private Node evalNode = null;
    private SpokObject value = null;

    /**
     * Default Constructor
     *
     * @param p
     */
    public NodeVariableAssignation(Node p) {
        super(p);
    }

    public NodeVariableAssignation(JSONObject obj, Node p) throws SpokException {
        super(p);
        if (obj.has("select")) {
            evalNode = new NodeSelect(obj.optJSONObject("select"), this);
        } else if (obj.has("function")) {
            evalNode = new NodeFunction(obj.optJSONObject("function"), this);
        }

        if (obj.has("value")) {
            value = new SpokVariable(obj.optJSONObject("value"));
        }
        name = getJSONString(obj, "name");

    }

    @Override
    protected void specificStop() throws SpokException {
    }

    @Override
    public JSONObject getJSONDescription() {
        JSONObject o = new JSONObject();
        try {
            if (evalNode != null) {
                o.put("function", evalNode);
            }
            if (value != null) {
                o.put("value", value);
            }
            o.put("name", name);

        } catch (JSONException ex) {
            // Do nothing since 'JSONObject.put(key,val)' would raise an exception
            // only if the key is null, which will never be the case
        }
        return o;

    }

    @Override
    public String getExpertProgramScript() {
        if (this.evalNode != null) {
            return this.name + " = " + this.evalNode.getExpertProgramScript() + ";";
        } else {
            return this.name + "=" + this.value.toString() + ";";
        }
    }

    @Override
    protected Node copy(Node parent) {
        NodeVariableAssignation ret = new NodeVariableAssignation(parent);
        if (evalNode != null) {
            ret.evalNode = this.evalNode.copy(parent);
        }
        if (value != null) {
            try {
                ret.value = new SpokVariable(value.getJSONDescription());
            } catch (SpokException ex) {
            }
        }
        return ret;
    }

    @Override
    public JSONObject call() {
        setStarted(true);
        if (evalNode != null) {
            evalNode.addEndEventListener(this);
            evalNode.call();
        } else {
            addEndEventListener(this);
            fireEndEvent(new EndEvent(this));
        }
        return null;
    }

    @Override
    public void endEventFired(EndEvent e) {
        Node source = (Node) e.getSource();
        try {
            value = source.getResult();
            if (value != null) {
                setVariable(new SpokVariable(value.getJSONDescription()));
            } else {
                setVariable(null);
            }
        } catch (SpokException ex) {
            LOGGER.error("Exception raised during evaluation" + ex);
        }
    }

    /**
     * Method that set the variable to its value
     *
     * @param v the variable to set
     */
    private void setVariable(SpokVariable v) {
        Node findNode = findNode(NodeFunction.class, this);
        if (findNode == null) {
            findNode = findNode(NodeProgram.class, this);
        }
        if (findNode == null) {
            LOGGER.warn("Unable to find a bloc to assign this variable ({})", this.name);
        } else {
            findNode.setVariable(this.name, v);
        }
    }

}
