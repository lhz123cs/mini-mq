package com.xcxcxcxcx.core;

import com.xcxcxcxcx.core.connector.MiniConnectionServer;
import com.xcxcxcxcx.mini.tools.config.MiniConfig;
import com.xcxcxcxcx.registry.abs.DiscoveryClient;
import com.xcxcxcxcx.registry.zookeeper.ZkDiscoveryClientFactory;

/**
 * @author XCXCXCXCX
 * @since 1.0
 */
public final class MiniServer {

    private static final String SERVER_HOST = MiniConfig.mini.registry.serverHost;
    private static final int SERVER_PORT = MiniConfig.mini.registry.serverPort;
    private static final String CONNECT_STRING = MiniConfig.mini.registry.connectString;

    private final MiniConnectionServer miniConnectionServer;

    private final DiscoveryClient discoveryClient;

    public MiniServer() {
        this.miniConnectionServer = new MiniConnectionServer(SERVER_HOST, SERVER_PORT);
        this.discoveryClient = ZkDiscoveryClientFactory.create(CONNECT_STRING);
    }

}