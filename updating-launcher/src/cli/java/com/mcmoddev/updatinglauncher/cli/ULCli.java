package com.mcmoddev.updatinglauncher.cli;

import com.mcmoddev.updatinglauncher.cli.command.StartCommand;
import picocli.CommandLine;

import java.util.List;
import java.util.Scanner;

@CommandLine.Command(
    name = "updatinglauncher",
    aliases = {"ul", "updating-launcher"},
    description = "Management tool for UpdatingLauncher",
    subcommands = {
        CommandLine.HelpCommand.class, StartCommand.class
    }
)
public class ULCli {
    @CommandLine.Spec
    CommandLine.Model.CommandSpec spec;

    private ULCli() {}

    public static void main(String[] args) {
        final var cmdLine = new CommandLine(new ULCli());
        final var argsAsList = List.of(args);
        if (argsAsList.contains("--devDynamicArgs")) {
            System.out.println();
            System.out.println("Please input the program arguments: ");
            try (final var scanner = new Scanner(System.in)) {
                final var actualArgs = scanner.nextLine().split(" ");
                int exitCode = cmdLine.execute(actualArgs);
                System.exit(exitCode);
            }
        } else {
            int exitCode = cmdLine.execute(args);
            System.exit(exitCode);
        }
    }
}
