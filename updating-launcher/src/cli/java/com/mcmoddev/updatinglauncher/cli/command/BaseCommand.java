package com.mcmoddev.updatinglauncher.cli.command;

import com.mcmoddev.updatinglauncher.agent.ProcessConnector;
import com.mcmoddev.updatinglauncher.cli.CLIConnector;
import com.mcmoddev.updatinglauncher.cli.DataStore;
import com.mcmoddev.updatinglauncher.cli.ExitCodes;
import com.mcmoddev.updatinglauncher.cli.SharedConstants;
import picocli.CommandLine.*;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.rmi.NotBoundException;
import java.rmi.registry.LocateRegistry;
import java.util.concurrent.Callable;

public abstract class BaseCommand implements Callable<Integer> {

    @Option(names = {"-h", "--host"}, description = """
    The host to connect to. Format: ip:port
    If this option is omitted, a connection will be established with a launcher that is running in the same directory as the one in which the command is run.""")
    protected String host = "";

    @Option(names = {"-u", "--username", "--user"}, description = """
    The username to use for connecting to the launcher.
    This option can be omitted if the connection is established with a local launcher running in the directory the command is run in.""")
    protected String username = "admin";
    @Option(
        names = {"-p", "--password", "--pass"},
        interactive = true,
        arity = "0..1",
        description = """
        The username to use for connecting to the launcher.
        This option can be omitted if the connection is established with a local launcher running in the directory the command is run in."""
    )
    protected String password;

    @Override
    public Integer call() {
        try {
            final var connector = resolveConnector();
            return run(connector);
        } catch (ConnectionException e) {
            System.out.println(colouredError("Exception trying to connect to launcher: " + e.getLocalizedMessage()));
            e.printStackTrace();
            return ExitCodes.ERROR;
        } catch (Exception e) {
            System.out.println(colouredError("Exception executing command: " + e.getLocalizedMessage()));
            e.printStackTrace();
            return ExitCodes.ERROR;
        }
    }

    protected abstract Integer run(CLIConnector connector) throws Exception;

    protected CLIConnector resolveConnector() throws ConnectionException {
        if (host == null || host.isBlank()) {
            final var ulDir = Path.of(SharedConstants.UL_DIRECTORY_NAME);
            final var dataFile = ulDir.resolve(DataStore.FILE_NAME);
            if (Files.exists(dataFile)) {
                try (final var is = Files.newInputStream(dataFile);
                     final var in = new ObjectInputStream(is)) {
                    final var data = (DataStore) in.readObject();
                    password = data.adminPass;
                    final var registry = LocateRegistry.getRegistry("127.0.0.1", data.port);
                    System.out.println(coloured("bold,green", "Successfully connected to port %s on localhost.".formatted(data.port)));
                    return (CLIConnector) registry.lookup(CLIConnector.NAME);
                } catch (IOException | ClassNotFoundException | NotBoundException e) {
                    throw new ConnectionException(e);
                }
            }
        }
        // TODO implement external host connection
        return null;
    }

    public static String colouredError(String text) {
        return Help.Ansi.ON.string("@|bold,red %s|@".formatted(text));
    }

    public static String coloured(String style, String text) {
        return Help.Ansi.ON.string("@|blue === |@@|%s %s |@@|blue ===|@".formatted(style, text));
    }

    public static final class ConnectionException extends Exception {
        public ConnectionException(Throwable t) {
            super(t);
        }
        public ConnectionException(String msg) {
            super(msg);
        }
    }
}
