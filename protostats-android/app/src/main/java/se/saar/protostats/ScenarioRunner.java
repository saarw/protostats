package se.saar.protostats;

import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by william on 2015-01-06.
 */
public class ScenarioRunner {

    private final String protostatsHost;
    private final int protostatsPort;
    private final String initEndpointUrl;
    private final AtomicInteger requestId = new AtomicInteger();
    private final AtomicBoolean isFinished = new AtomicBoolean();
    private final ConcurrentLinkedQueue<RunnableScenario> scenarioQueue;
    private final Set<Integer> excludedScenarioIds;
    private HttpClient httpClient;

    public ScenarioRunner(Scenarios scenarios, Set<Integer> excludedScenarioIds) {
        scenarioQueue = new ConcurrentLinkedQueue<RunnableScenario>();
        this.excludedScenarioIds = excludedScenarioIds;
        scenarioQueue.add(scenarios.getRunnableScenario(ScenarioID.HTTP_POST_JSON_RPC));
        protostatsHost = "10.0.2.2";
        protostatsPort = 8080;
        initEndpointUrl = "http://" + protostatsHost + ":" + protostatsPort + "/rpc";
    }

    public void runScenarios(final TelephonyManager telephonyManager, final NetworkInfo networkInfo) {
        isFinished.set(false);
        initRun(telephonyManager.getNetworkOperatorName());
    }

    private AsyncTask<Runnable, Integer, Exception> initRun(final String operatorName) {
        isFinished.set(false);
        AsyncTask<Runnable, Integer, Exception> task = new AsyncTask<Runnable, Integer, Exception>() {
            @Override
            protected Exception doInBackground(Runnable... params) {
                try {
                    String token = null;
                    try {
                        httpClient = new DefaultHttpClient();
                        String rsp = sendGet("http://" + protostatsHost + ":" + protostatsPort + "/excluded.txt");
                        if (rsp != null && rsp.trim().length() > 0) {
                            String[] ids = rsp.split(",");
                            for (String id : ids) {
                                excludedScenarioIds.add(Integer.valueOf(id));
                            }
                        }
                        token = sendRpcPost(initEndpointUrl, "InitTests", operatorName);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return e;
                    }
                    while (!scenarioQueue.isEmpty()) {
                        RunnableScenario scenario = scenarioQueue.remove();
                        try {
                            if (!excludedScenarioIds.contains(Integer.valueOf(scenario.getScenarioId().id))) {
                                scenario.run(token, ScenarioRunner.this);
                            }
                        } catch (Exception e) {
                            return e;
                        }
                    }
                    return null;
                } finally {
                    isFinished.set(true);
                }
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        return task;
    }

    private String sendGet(String url) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        HttpResponse rsp = httpClient.execute(httpGet);
        if (rsp.getStatusLine().getStatusCode() == 404) {
            return null;
        } else if (rsp.getStatusLine().getStatusCode() == 200) {
            return readResponse(rsp);
        } else {
            throw new Exception("Failed to GET " + url + " with error " +
                    rsp.getStatusLine().getStatusCode() + ": " +
                    rsp.getStatusLine().getReasonPhrase());
        }
    }

    public String sendRpcPost(String url, String method, Object... params) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        JSONObject json = new JSONObject();
        json.put("id", requestId.incrementAndGet());
        json.put("method", method);
        JSONArray paramArray = new JSONArray();
        for (int i = 0; i < params.length; i++) {
            paramArray.put(i, params[i].toString()); // Server expects all params to be strings
        }
        json.put("params", paramArray);

        String body = json.toString();
        System.out.println("Posting body: " + body);
        httpPost.setEntity(new StringEntity(body));
        HttpResponse rsp = httpClient.execute(httpPost);
        if (rsp.getStatusLine().getStatusCode() != 200) {
            rsp.getEntity().consumeContent();
            throw new Exception("RPC call with body " + body + " returned status " +
                    rsp.getStatusLine().getStatusCode() + ": " +
                    rsp.getStatusLine().getReasonPhrase());
        }
        String responseString = readResponse(rsp);
        System.out.println("Received response: " + responseString);
        JSONObject responseJson = new JSONObject(responseString);
        if (!responseJson.isNull("error")) {
            throw new Exception("RPC call returned error: " + responseJson.getString("error"));
        }
        return responseJson.getString("result");
    }

    private String readResponse(HttpResponse rsp) throws Exception {
        String str = EntityUtils.toString(rsp.getEntity());
        if (str.length() == 0) {
            System.out.println("Got empty response from server.");
            return "";
        }
        return str;
    }

    public void trackScenarioTiming(String token, ScenarioID scenario, int step, float timingMs) throws Exception {
        sendRpcPost("http://10.0.2.2:8080/rpc", "TrackScenarioTime", token, scenario.id, step, timingMs);
    }

    public void trackScenarioException(String token, ScenarioID scenario, int step, Exception e) throws Exception {
        sendRpcPost("http://10.0.2.2:8080/rpc", "TrackScenarioError", token, scenario.id, step,
                e.getClass().getSimpleName() + ": " + e.getMessage());
    }

    public boolean isFinished() {
        return isFinished.get();
    }
}
