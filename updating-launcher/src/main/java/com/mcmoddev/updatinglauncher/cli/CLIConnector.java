package com.mcmoddev.updatinglauncher.cli;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface CLIConnector extends Remote {
    int DEFAULT_PORT = 6290;
    String NAME = "UpdatingLauncher";

    boolean checkCredentials(String username, String password) throws RemoteException;
    void startProcess(String username, String password) throws RemoteException;
}
