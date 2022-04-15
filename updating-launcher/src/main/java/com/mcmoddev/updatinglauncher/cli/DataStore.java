package com.mcmoddev.updatinglauncher.cli;

import java.io.Serializable;

public class DataStore implements Serializable {

    public static final String FILE_NAME = "info.ul";

    public String command;
    public int port;
    public String adminPass;
}
