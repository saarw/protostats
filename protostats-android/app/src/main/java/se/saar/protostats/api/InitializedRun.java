package se.saar.protostats.api;

/**
 * Received from the backend when a test run is initialized.
 */
public class InitializedRun {
    public String token;
    public String[] excludedScenarios;
}
