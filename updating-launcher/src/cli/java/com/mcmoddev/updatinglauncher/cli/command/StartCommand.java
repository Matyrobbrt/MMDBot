package com.mcmoddev.updatinglauncher.cli.command;

import com.mcmoddev.updatinglauncher.cli.CLIConnector;
import com.mcmoddev.updatinglauncher.cli.ExitCodes;
import picocli.CommandLine;

@CommandLine.Command(
    name = "start",
    aliases = "s",
    description = "Starts a process."
)
public class StartCommand extends BaseCommand {
    @Override
    protected Integer run(final CLIConnector connector) throws Exception {
        connector.startProcess(username, password);
        return ExitCodes.SUCCESS;
    }
}
