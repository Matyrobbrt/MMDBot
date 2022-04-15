package com.mcmoddev.updatinglauncher;

import com.mcmoddev.updatinglauncher.cli.CLIConnector;

import java.rmi.RemoteException;
import java.util.function.Supplier;

public class CliImpl implements CLIConnector {
    private final Supplier<JarUpdater> updater;

    public CliImpl(final Supplier<JarUpdater> updater) {
        this.updater = updater;
    }

    @Override
    public void startProcess(final String username, final String password) throws RemoteException {
        updater.get().runProcess();
    }

    @Override
    public boolean checkCredentials(final String username, final String password) throws RemoteException {
        return true;
    }
}
