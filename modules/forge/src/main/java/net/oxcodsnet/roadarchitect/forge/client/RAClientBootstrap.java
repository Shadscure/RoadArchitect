package net.oxcodsnet.roadarchitect.forge.client;

import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.oxcodsnet.roadarchitect.RoadArchitect;
import net.oxcodsnet.roadarchitect.forge.config.RAConfigForgeBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public final class RAClientBootstrap {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoadArchitect.MOD_ID + "/" + RAClientBootstrap.class.getSimpleName());

    private RAClientBootstrap() {
    }

    public static void init() {
        LOGGER.info("Registering configuration screen");
        try {
            ConfigScreenHandler.ConfigScreenFactory factory = (ConfigScreenHandler.ConfigScreenFactory) Proxy.newProxyInstance(
                    ConfigScreenHandler.ConfigScreenFactory.class.getClassLoader(),
                    new Class[]{ConfigScreenHandler.ConfigScreenFactory.class},
                    new FactoryInvocationHandler());

            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> factory);
            LOGGER.info("Configuration screen registered");
        } catch (Exception e) {
            LOGGER.warn("Failed to register configuration screen", e);
        }
    }

    private static final class FactoryInvocationHandler implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (("create".equals(name) || "createScreen".equals(name)) && args != null && args.length == 2) {
                return RAConfigForgeBridge.createScreen(args[1]);
            }
            if ("equals".equals(name) && args != null && args.length == 1) {
                return proxy == args[0];
            }
            if ("hashCode".equals(name) && (args == null || args.length == 0)) {
                return System.identityHashCode(proxy);
            }
            if ("toString".equals(name) && (args == null || args.length == 0)) {
                return FactoryInvocationHandler.class.getName();
            }
            throw new UnsupportedOperationException("Unsupported method: " + method);
        }
    }
}
