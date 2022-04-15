/*
 * MMDBot - https://github.com/MinecraftModDevelopment/MMDBot
 * Copyright (C) 2016-2022 <MMD - MinecraftModDevelopment>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation;
 * Specifically version 2.1 of the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 * USA
 * https://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 */
package com.mcmoddev.updatinglauncher;

import com.mcmoddev.updatinglauncher.agent.ProcessConnector;
import com.mcmoddev.updatinglauncher.cli.CLIConnector;
import com.mcmoddev.updatinglauncher.cli.DataStore;
import com.mcmoddev.updatinglauncher.cli.SharedConstants;
import com.mcmoddev.updatinglauncher.discord.DiscordIntegration;
import com.mcmoddev.updatinglauncher.github.UpdateChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.configurate.ConfigurateException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public class Main {
    // Keep a "strong" reference to the connector and its registry, to avoid GC picking it up
    private static Registry registry;
    private static CLIConnector cli;

    private static final ThreadGroup THREAD_GROUP = new ThreadGroup("UpdatingLauncher");

    public static final String RMI_NAME = ProcessConnector.BASE_NAME + "#" + (int) ProcessHandle.current().pid();
    public static final Logger LOG = LoggerFactory.getLogger("UpdatingLauncher");
    public static final Path UL_DIRECTORY = Path.of(SharedConstants.UL_DIRECTORY_NAME);
    public static final Path CONFIG_PATH = UL_DIRECTORY.resolve("config.conf");
    public static final Path AGENT_PATH = UL_DIRECTORY.resolve("agent.jar");
    public static final ScheduledThreadPoolExecutor SERVICE;

    static {
        final var service = (ScheduledThreadPoolExecutor) Executors.newScheduledThreadPool(1, r -> new Thread(THREAD_GROUP, r, "UpdatingLauncher"));
        service.setKeepAliveTime(1, TimeUnit.HOURS);
        SERVICE = service;
    }

    private static final ExecutorService HTTP_CLIENT_EXECUTOR = Executors.newSingleThreadExecutor(r -> {
        final var thread = new Thread(THREAD_GROUP, r, "UpdatingLauncherHttpClient");
        thread.setDaemon(true);
        return thread;
    });

    private static DiscordIntegration discordIntegration;

    public static void main(String[] args) throws IOException {
        if (!Files.exists(UL_DIRECTORY)) {
            Files.createDirectories(UL_DIRECTORY);
        }

        final var data = new DataStore();
        // TODO make the port configurable, and the host
        data.port = CLIConnector.DEFAULT_PORT;
        // TODO build the command
        data.command = "";
        data.adminPass = getAlphaNumericString(ThreadLocalRandom.current(), 30);
        dumpData(data);

        try {
            copyAgent();
        } catch (IOException e) {
            LOG.error("Exception copying agent JAR: ", e);
            throw new RuntimeException(e);
        }

        final var cfgExists = Files.exists(CONFIG_PATH);
        Config config;
        try {
            config = Config.load(CONFIG_PATH);
        } catch (ConfigurateException e) {
            throw new RuntimeException(e);
        }
        if (!cfgExists) {
            throw new RuntimeException("A new configuration file was created! Please configure it.");
        }

        final var updateChecker = new UpdateChecker(config.gitHub.owner, config.gitHub.repo, HttpClient.newBuilder()
            .executor(HTTP_CLIENT_EXECUTOR)
            .build());
        final var updater = new JarUpdater(Paths.get(config.jarPath), updateChecker, Pattern.compile(config.checkingInfo.filePattern), config.jvmArgs);
        setupCli(() -> updater, CLIConnector.NAME, CLIConnector.DEFAULT_PORT, "127.0.0.1");
        if (config.checkingInfo.rate > -1) {
            SERVICE.scheduleAtFixedRate(updater, 0, config.checkingInfo.rate, TimeUnit.MINUTES);
            LOG.warn("Scheduled updater. Will run every {} minutes.", config.checkingInfo.rate);
        } else {
            updater.tryFirstStart();
            SERVICE.allowCoreThreadTimeOut(true);
        }

        if (config.discord.enabled) {
            discordIntegration = new DiscordIntegration(Paths.get(""), config.discord, updater);
            LOG.warn("Discord integration is active!");
        }
    }

    public static DiscordIntegration getDiscordIntegration() {
        return discordIntegration;
    }

    public static void copyAgent() throws IOException {
        var agent = Main.class.getResourceAsStream("/agent.jar");
        if (agent == null) {
            // If it isn't a .jar, try finding a .zip
            agent = Main.class.getResourceAsStream("/agent.zip");
        }
        Files.copy(Objects.requireNonNull(agent), AGENT_PATH, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * @return the current public IP address of the machine.
     */
    public static String getPublicIPAddress() throws IOException {
        final var url = new URL("https://api.ipify.org");
        try (final var sr = new InputStreamReader(url.openStream());
             final var sc = new BufferedReader(sr)) {
            return sc.readLine().trim();
        }
    }

    private static void dumpData(DataStore data) throws IOException {
        final var path = UL_DIRECTORY.resolve(DataStore.FILE_NAME);
        try (final var os = Files.newOutputStream(path, StandardOpenOption.CREATE);
             final var out = new ObjectOutputStream(os)) {
            out.writeObject(data);
        }
    }

    private static void setupCli(Supplier<JarUpdater> updater, String name, int port, String host) throws IOException {
        System.setProperty("java.rmi.server.hostname", host);
        try {
            registry = LocateRegistry.createRegistry(port);
        } catch (RemoteException ignored) {
            registry = LocateRegistry.getRegistry(port);
        }
        cli = new CliImpl(updater);

        final CLIConnector stub = (CLIConnector) UnicastRemoteObject.exportObject(cli, port);
        registry.rebind(name, stub);

        LOG.warn("RMI connection successfully established at port {}, with name '{}'", port, name);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                registry.unbind(name);
            } catch (RemoteException | NotBoundException e) {
                e.printStackTrace();
            }
        }));
    }

    private static String getAlphaNumericString(Random random, int n) {
        final var array = new byte[256];
        random.nextBytes(array);
        final var randomString = new String(array, StandardCharsets.UTF_8);
        final var r = new StringBuilder();
        for (int k = 0; k < randomString.length(); k++) {
            char ch = randomString.charAt(k);
            if (((ch >= 'a' && ch <= 'z')
                || (ch >= 'A' && ch <= 'Z')
                || (ch >= '0' && ch <= '9'))
                && (n > 0)) {
                r.append(ch);
                n--;
            }
        }
        return r.toString();
    }
}
