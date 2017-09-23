/*
 * ****************************************************************************
 * Copyright VMware, Inc. 2010-2016.  All Rights Reserved.
 * ****************************************************************************
 *
 * This software is made available for use under the terms of the BSD
 * 3-Clause license:
 *
 * Redistribution and use in source and binary forms, with or without 
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright 
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in 
 *    the documentation and/or other materials provided with the 
 *    distribution.
 * 
 * 3. Neither the name of the copyright holder nor the names of its 
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE 
 * COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR
 * TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.vmware.connection;

import com.vmware.common.annotations.After;
import com.vmware.common.annotations.Before;
import com.vmware.common.annotations.Option;
import com.vmware.connection.helpers.GetMOREF;
import com.vmware.connection.helpers.WaitForValues;
import com.vmware.vim25.ManagedObjectReference;
import com.vmware.vim25.ServiceContent;
import com.vmware.vim25.VimPortType;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

/**
 * An abstract base class, extend this class if your common needs to
 * open a connection to the virtual center server before it can do anything useful.
 * <p/>
 * Example: The simplest possible extension class merely forms a connection and
 * specifies it's own common action.
 * <p/>
 * <pre>
 *     &#064;Sample(name = "connect")
 *     public class Connect extends ConnectedVimServiceBase {
 *          &#064;Action
 *          public void action() {
 *               System.out.println("currently connected: " + this.isConnected());
 *          }
 *     }
 * </pre>
 * <p/>
 * This is provided as an alternative to extending the Connection class directly.
 * <p/>
 * For a very simple connected sample:
 *
 * @see com.vmware.general.GetCurrentTime
 */
public abstract class ConnectedVimServiceBase {
    public static final String PROP_ME_NAME = "name";
    public static final String SVC_INST_NAME = "ServiceInstance";
    protected Connection connection;
    protected VimPortType vimPort;
    protected ServiceContent serviceContent;
    protected ManagedObjectReference rootRef;
    @SuppressWarnings("rawtypes")
    protected Map headers;
    protected WaitForValues waitForValues;
    protected GetMOREF getMOREFs;

    // By default assume we are talking to a vCenter
    Boolean hostConnection = Boolean.FALSE;

    @Option(
            name = "basic-connection",
            required = false,
            description =
                    "Turn off the use of SSO for connections. Useful for connecting to ESX or ESXi hosts.",
            parameter = false
    )
    public void setHostConnection(final Boolean value) {
        // NOTE: the common framework will insert a "Boolean.TRUE" object on
        // options that have parameter = false set. This indicates they
        // are boolean flag options not string parameter options.
        this.hostConnection = value;
    }

    /**
     * Use this method to get a reference to the service instance itself.
     * <p/>
     *
     * @return a managed object reference referring to the service instance itself.
     */
    public ManagedObjectReference getServiceInstanceReference() {
        return connection.getServiceInstanceReference();
    }

    /**
     * A method for dependency injection of the connection object.
     * <p/>
     *
     * @param connect the connection object to use for this POJO
     * @see com.vmware.connection.Connection
     */
    @Option(name = "connection", type = Connection.class)
    public void setConnection(Connection connect) {
        this.connection = connect;
    }

    /**
     * connects this object, returns itself to allow for method chaining
     *
     * @return a connected reference to itself.
     * @throws Exception
     */
    @Before
    public Connection connect() {

        if(hostConnection) {
            // construct a BasicConnection object to use for
            connection = basicConnectionFromConnection(connection);
        }

        try {
            connection.connect();
            this.waitForValues = new WaitForValues(connection);
            this.getMOREFs = new GetMOREF(connection);
            this.headers = connection.getHeaders();
            this.vimPort = connection.getVimPort();
            this.serviceContent = connection.getServiceContent();
            this.rootRef = serviceContent.getRootFolder();
        }
        catch (ConnectionException e) {
            // SSO or Basic connection exception has occurred
            e.printStackTrace();
            // not the best form, but without a connection these samples are pointless.
            System.err.println("No valid connection available. Exiting now.");
            System.exit(0);
        }
        return connection;
    }

    public BasicConnection basicConnectionFromConnection(final Connection original) {
        BasicConnection connection = new BasicConnection();
        connection.setUrl(original.getUrl());
        connection.setUsername(original.getUsername());
        connection.setPassword(original.getPassword());
        return connection;
    }

    /**
     * disconnects this object and returns a reference to itself for method chaining
     *
     * @return a disconnected reference to itself
     * @throws Exception
     */
    @After
    public Connection disconnect() {
        this.waitForValues = null;
        try {
            connection.disconnect();
        } catch (Throwable t) {
            throw new ConnectionException(t);
        }
        return connection;
    }

    public class ConnectionException extends RuntimeException {
        public ConnectionException(Throwable cause) {
            super(cause);
        }

        public ConnectionException(String message) {
            super(message);
        }
    }
}
