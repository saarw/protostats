package se.saar.protostats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by william on 2014-12-29.
 */
public class Scenarios {
    private Map<String, String[]> categoriesToScenarios;
    private Map<String, Scenario> nameToScenarioDescriptors;
    private String[] categories;

    public Scenarios() {
        categoriesToScenarios = new LinkedHashMap<String, String[]>();
        categoriesToScenarios.put("Init Test", new String[] {"HTTP - Init test RPC call"});
        categoriesToScenarios.put("HTTP/HTTPS request/response",
                new String[] {"HTTP Post JSON-RPC", "HTTPS Post JSON-RPC"});
        categoriesToScenarios.put("HTTP/HTTPS long-polling",
                new String[] {"HTTP Post long-polling", "HTTPS Post long-polling"});
        categories = categoriesToScenarios.keySet().toArray(new String[categoriesToScenarios.size()]);
    }


    public String[] getCategories() {
        return categories;
    }

    public String[] getScenariosForCategoryIdx(int categoryIdx) {
        String[] scenarios = categoriesToScenarios.get(categories[categoryIdx]);
        return scenarios == null ? new String[0] : scenarios;
    }

    public Scenario getScenarioInfo(String scenarioName) {
        return nameToScenarioDescriptors.get(scenarioName);
    }
}
