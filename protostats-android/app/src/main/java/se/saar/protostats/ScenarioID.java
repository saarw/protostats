package se.saar.protostats;

/**
 * Created by william on 2015-01-06.
 */
public enum ScenarioID {
    HTTP_POST_JSON_RPC(0, "HTTP scenarios", "HTTP POST JSON-RPC");

    public final int id;
    public final String category;
    public final String title;

    ScenarioID(int id, String category, String title) {
        this.id = id;
        this.category = category;
        this.title = title;
    }
}
