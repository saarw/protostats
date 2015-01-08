package se.saar.protostats;

import android.os.RecoverySystem;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by william on 2014-12-31.
 */
public class RunnableScenario {
    private final ScenarioID id;
    private final ScenarioLogic scenarioLogic;
    private final AtomicInteger runProgress = new AtomicInteger();

    public RunnableScenario(ScenarioID id, ScenarioLogic logic) {
        this.id = id;
        this.scenarioLogic = logic;
    }

    public void run(String token, ScenarioRunner context) throws Exception {
        scenarioLogic.run(token, context, new RecoverySystem.ProgressListener() {
            @Override
            public void onProgress(int progress) {
                runProgress.set(1);
                runProgress.set(progress);
            }
        });
    }

    public int getProgress() {
        return runProgress.get();
    }

    public ScenarioID getScenarioId() {
        return id;
    }

    interface ScenarioLogic {
        public void run(String token, ScenarioRunner context, RecoverySystem.ProgressListener progressListener) throws Exception;
    }
}
