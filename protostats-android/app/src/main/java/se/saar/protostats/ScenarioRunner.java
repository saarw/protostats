package se.saar.protostats;

import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by william on 2015-01-06.
 */
public class ScenarioRunner {

    private final String initEndpointUrl;
    private final byte[] buffer = new byte[10000];
    private final AtomicInteger requestId = new AtomicInteger();
    private final AtomicBoolean isFinished = new AtomicBoolean();
    private final ConcurrentLinkedQueue<RunnableScenario> scenarioQueue;

    public ScenarioRunner(Scenarios scenarios) {
        scenarioQueue = new ConcurrentLinkedQueue<RunnableScenario>();
        scenarioQueue.add(scenarios.getRunnableScenario(ScenarioID.HTTP_POST_JSON_RPC));
        initEndpointUrl = "http://10.0.2.2:8080/rpc";
    }

    public void runScenarios(final TelephonyManager telephonyManager, final NetworkInfo networkInfo) {
        isFinished.set(false);
        initRun(telephonyManager.getNetworkOperatorName());
    }

    private void initRun(final String operatorName) {
        AsyncTask<Runnable, Integer, Exception> task = new AsyncTask<Runnable, Integer, Exception>() {
            @Override
            protected Exception doInBackground(Runnable... params) {
                HttpClient client = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost();

                try {
                    String token = sendPost(initEndpointUrl, "InitTests", operatorName);
                    while (!scenarioQueue.isEmpty()) {
                        scenarioQueue.remove().run(token, ScenarioRunner.this);
                    }
                    isFinished.set(true);
                    return null;
                } catch(Exception e) {
                    e.printStackTrace();
                    return e;
                }
            }
        };
        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, null);
        // task.execute();
    }


    public String sendPost(String url, String method, Object... params) throws Exception {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            JSONObject json = new JSONObject();
            json.put("id", requestId.incrementAndGet());
            json.put("method", method);
            JSONArray paramArray = new JSONArray();
            for (int i = 0; i < params.length; i++) {
                paramArray.put(i, params[i]);
            }
            json.put("params", paramArray);

            String body = json.toString();
            System.out.println("Posting body: " + body);
            httpPost.setEntity(new StringEntity(body));
            HttpResponse rsp = client.execute(httpPost);
            if (rsp.getEntity().getContentLength() == 0) {
                System.out.println("Got empty response from server.");
                return "";
            }
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            InputStream content = rsp.getEntity().getContent();
            int read = 0;
            while((read = content.read(buffer)) >= 0) {
                buf.write(buffer, 0, read);
            }
            String responseString = buf.toString("UTF-8");
            System.out.println("Received response: " + responseString);
            JSONObject responseJson = new JSONObject(responseString);
            if (!responseJson.isNull("error")) {
                throw new Exception("RPC call returned error: " + responseJson.getString("error"));
            }
            return responseJson.getString("result");
        } catch (Exception e) {
            throw e;
        }
    }

    public boolean isFinished() {
        return isFinished.get();
    }
}
