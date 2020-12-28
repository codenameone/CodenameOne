/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.codename1.io;

import com.codename1.testing.AbstractTest;

/**
 *
 * @author shannah
 */
public class ConnectionRequestTests extends AbstractTest {

    @Override
    public boolean runTest() throws Exception {
        testSecureConnectionToInsecureServer();
        testInSecureConnectionToInsecureServer();
        return true;
    }
    
    private void testSecureConnectionToInsecureServer() throws Exception {
        ConnectionRequest req = new ConnectionRequest("https://xataface.com");
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueueAndWait(req);
        assertNotEqual(200, req.getResponseCode(), "Expected response code to not be 200 because the server certificate is invalid");
    }
    private void testInSecureConnectionToInsecureServer() throws Exception {
        ConnectionRequest req = new ConnectionRequest("https://xataface.com");
        req.setInsecure(true);
        req.setFailSilently(true);
        NetworkManager.getInstance().addToQueueAndWait(req);
        assertEqual(200, req.getResponseCode(), "Expected response code to not be 200 because the server certificate is invalid");
    }
    
}
