package com.hazelcast.session;

import org.apache.catalina.Context;
import org.apache.catalina.connector.Connector;
import org.apache.catalina.startup.Tomcat;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;

public class Tomcat8Configurator extends WebContainerConfigurator<Tomcat> {

    private Tomcat tomcat;
    private SessionManager manager;

    private String p2pConfigLocation;
    private String clientServerConfigLocation;

    public Tomcat8Configurator(String p2pConfigLocation, String clientServerConfigLocation) {
        super();
        this.p2pConfigLocation = p2pConfigLocation;
        this.clientServerConfigLocation = clientServerConfigLocation;
    }

    public Tomcat8Configurator() {
    }

    @Override
    public Tomcat configure() throws Exception {
        final URL root = new URL(TestServlet.class.getResource("/"), "../../../tomcat-core/target/test-classes");
        final String cleanedRoot = URLDecoder.decode(root.getFile(), "UTF-8");

        final String fileSeparator = File.separator.equals("\\") ? "\\\\" : File.separator;
        final String docBase = cleanedRoot + File.separator + TestServlet.class.getPackage().getName().replaceAll("\\.", fileSeparator);

        Tomcat tomcat = new Tomcat();
        if (!clientOnly) {
            P2PLifecycleListener p2PLifecycleListener = new P2PLifecycleListener();
            p2PLifecycleListener.setConfigLocation(p2pConfigLocation);
            tomcat.getServer().addLifecycleListener(p2PLifecycleListener);
        } else {
            ClientServerLifecycleListener clientServerLifecycleListener = new ClientServerLifecycleListener();
            clientServerLifecycleListener.setConfigLocation(clientServerConfigLocation);
            tomcat.getServer().addLifecycleListener(clientServerLifecycleListener);
        }
        tomcat.getEngine().setJvmRoute("tomcat-" + port);
        tomcat.setBaseDir(docBase);

        tomcat.getEngine().setName("engine-" + port);

        final Connector connector = tomcat.getConnector();
        connector.setPort(port);
        connector.setProperty("bindOnInit", "false");

        Context context;
        try {
            context = tomcat.addWebapp(tomcat.getHost(), "/", docBase + fileSeparator + "webapp");
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }

        this.manager = new HazelcastSessionManager();
        context.setManager((HazelcastSessionManager) manager);
        updateManager((HazelcastSessionManager) manager);
        context.setCookies(true);
        context.setBackgroundProcessorDelay(1);
        context.setReloadable(true);

        return tomcat;
    }

    @Override
    public void start() throws Exception {
        tomcat = configure();
        tomcat.start();
    }

    @Override
    public void stop() throws Exception {
        tomcat.stop();
    }

    @Override
    public void reload() {
        Context context = (Context) tomcat.getHost().findChild("/");
        context.reload();
    }

    @Override
    public SessionManager getManager() {
        return manager;
    }

    private void updateManager(HazelcastSessionManager manager) {
        manager.setSticky(sticky);
        manager.setClientOnly(clientOnly);
        manager.setMapName(mapName);
        manager.setMaxInactiveInterval(sessionTimeout);
        manager.setDeferredWrite(deferredWrite);
    }
}
