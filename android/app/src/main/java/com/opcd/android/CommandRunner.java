package com.opcd.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Utility class to run commands inside the Alpine Linux runtime.
 */
public class CommandRunner {

    private final RuntimeManager runtimeManager;

    public CommandRunner(RuntimeManager runtimeManager) {
        this.runtimeManager = runtimeManager;
    }

    /**
     * Executes a command inside the runtime and returns its output.
     *
     * @param command the shell command to execute
     * @return CommandResult containing exit code and output
     * @throws IOException if the runtime is not ready
     */
    public CommandResult execute(String command) throws IOException, InterruptedException {
        Process process = runtimeManager.runInAlpine(command);

        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        int exitCode = process.waitFor();
        return new CommandResult(exitCode, output.toString());
    }

    /**
     * Executes a command and returns only stdout lines, ignoring exit code.
     */
    public String executeForOutput(String command) throws IOException, InterruptedException {
        Process process = runtimeManager.runInAlpine(command);
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }
        process.waitFor();
        return output.toString();
    }

    public static class CommandResult {
        public final int exitCode;
        public final String output;

        public CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output;
        }

        public boolean isSuccess() {
            return exitCode == 0;
        }
    }
}
