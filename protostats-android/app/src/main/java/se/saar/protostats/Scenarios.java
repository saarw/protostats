package se.saar.protostats;

import android.os.RecoverySystem;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by william on 2014-12-29.
 */
public class Scenarios {
    private Map<String, String[]> categoriesToScenarios;
    private Map<ScenarioID, RunnableScenario> runnableScenarios;
    private String[] categories;

    public Scenarios() {
        categoriesToScenarios = new LinkedHashMap<String, String[]>();
        categoriesToScenarios.put(ScenarioID.HTTP_POST_JSON_RPC.category,
                new String[] {ScenarioID.HTTP_POST_JSON_RPC.title, "HTTP long-polling"});
        categoriesToScenarios.put("HTTPS scenarios",
                new String[] {"HTTPS POST JSON-RPC", "HTTPS long-polling"});
        categoriesToScenarios.put("Socket scenarios",
                new String[] {"Socket send RPCs", "Socket subscribe-receive broadcasts"});
        categoriesToScenarios.put("Websocket scenarios",
                new String[] {"Websocket RPCs", "Websocket subscribe-receive broadcasts"});
        categories = categoriesToScenarios.keySet().toArray(new String[categoriesToScenarios.size()]);
        runnableScenarios = new ConcurrentHashMap<ScenarioID, RunnableScenario>();
        runnableScenarios.put(ScenarioID.HTTP_POST_JSON_RPC, new JsonRpcScenario());
    }


    public String[] getCategories() {
        return categories;
    }

    public String[] getScenariosForCategoryIdx(int categoryIdx) {
        String[] scenarios = categoriesToScenarios.get(categories[categoryIdx]);
        return scenarios == null ? new String[0] : scenarios;
    }

    public int getProgress(int categoryIdx, String scenarioTitle) {
        for (Map.Entry<ScenarioID, RunnableScenario> entry : runnableScenarios.entrySet()) {
            if (entry.getKey().title.equals(scenarioTitle)) {
                return entry.getValue().getProgress();
            }
        }
        return 0;
    }

    public RunnableScenario getRunnableScenario(ScenarioID scenarioId) {
        return runnableScenarios.get(scenarioId);
    }

    static class JsonRpcScenario extends RunnableScenario {

        public JsonRpcScenario() {
            super(ScenarioID.HTTP_POST_JSON_RPC, new JsonRpcScenarioLogic(ScenarioID.HTTP_POST_JSON_RPC));
        }

        static class JsonRpcScenarioLogic implements ScenarioLogic {
            private ScenarioID scenarioId;

            public JsonRpcScenarioLogic(ScenarioID scenarioId) {
                this.scenarioId = scenarioId;
            }

            @Override
            public void run(String token, ScenarioRunner context, RecoverySystem.ProgressListener progressListener) throws Exception {
                int reqs = 5;
                for (int i = 0; i < reqs; i++) {
                    if (i > 0) {
                        Thread.sleep(5000);
                    }
                    long nanoTime = System.nanoTime();
                    try {
                        String result = context.sendRpcPost("http://10.0.2.2:8080/rpc", "RpcScenarioCall", token);
                        if (result == null || result.length() < 10 || !result.endsWith("end an end")) {
                            throw new Exception("Returned unexpected result: " + result);
                        }
                        float timing = ((float) (System.nanoTime() - nanoTime)) / 1000000f;
                        context.trackScenarioTiming(token, scenarioId, i, timing);
                    } catch(Exception e) {
                        context.trackScenarioException(token, scenarioId, i, e);
                    }
                    progressListener.onProgress((i + 1) * (100 / reqs));
                }
            }
        }
    }
}
