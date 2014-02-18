/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package appsgate.lig.context.agregator;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author jr
 */
public class ContextAgregatorTest {

    public ContextAgregatorTest() {
    }

    @Test
    public void testAgregator() throws Exception{
        ContextAgregatorMock m = new ContextAgregatorMock("src/test/resources/jsonLibs/testMock.json");
        Assert.assertNotNull(m);
        Assert.assertNotNull(m.getEventsFromState("test", "testState"));
    }
    
    @Test
    public void testNewInst() throws Exception{
        ContextAgregatorImpl m = new ContextAgregatorImpl();
        m.newInst();
        m.deleteInst();
    }

}