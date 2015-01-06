package se.saar.protostats;

import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.telephony.TelephonyManager;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by william on 2015-01-06.
 */
public class ScenarioRunner {

    private AtomicInteger requestId = new AtomicInteger();
    private ConcurrentLinkedQueue<ScenarioLogic> scenarioQueue;

    public ScenarioRunner(Scenarios scenarios) {
        scenarioQueue = new ConcurrentLinkedQueue<ScenarioLogic>();
        scenarioQueue.add(new JsonRpcScenario());
    }

    public void runScenarios(final TelephonyManager telephonyManager, final NetworkInfo networkInfo) {
        initRun(telephonyManager.getNetworkOperatorName());
    }

    private void initRun(final String operatorName) {
        AsyncTask<Runnable, Integer, Exception> task = new AsyncTask<Runnable, Integer, Exception>() {
            @Override
            protected Exception doInBackground(Runnable... params) {
                HttpClient client = new DefaultHttpClient();
                HttpPost httpPost = new HttpPost();

                try {
                    String token = sendPost("http://10.0.2.2:8080/rpc", "InitTests", operatorName);
                    while (!scenarioQueue.isEmpty()) {
                        scenarioQueue.poll().run(token);
                    }
                    return null;
                } catch(Exception e) {
                    e.printStackTrace();
                    return e;
                }
            }
        };
        task.execute();
    }

    byte[] buffer = new byte[10000];
    private String sendPost(String url, String method, Object... params) throws Exception {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(url);

            JSONObject json = new JSONObject();
            json.put("id", requestId.incrementAndGet());
            json.put("method", method);
            json.put("params", params);

            httpPost.setEntity(new StringEntity(json.toString()));
            HttpResponse rsp = client.execute(httpPost);

            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            rsp.getEntity().writeTo(buf);
            JSONObject responseJson = new JSONObject(buf.toString("UTF-8"));
            if (responseJson.get("error") != null) {
                throw new Exception("RPC call returned error: " + responseJson.getString("error"));
            }
            return responseJson.getString("result");
        } catch (Exception e) {
            throw e;
        }
    }

    interface ScenarioLogic {
        public void run(String token);
    }

    class JsonRpcScenario implements ScenarioLogic {

        @Override
        public void run(String token) {

        }
    }
}
