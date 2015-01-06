package se.saar.protostats;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseExpandableListAdapter;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class ProtoStatsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_protostats);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        final Scenarios scenarios = new Scenarios();

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_start, container, false);

            Button runTestsButton = (Button) rootView.findViewById(R.id.runTestButton);
            System.out.println("Adding onclick listener to button that has listeners: " + runTestsButton.hasOnClickListeners());
            runTestsButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    System.out.println("Button clicked");
                    String[] excludedScenarios = checkExcludedScenarios();
//                    if (showCostWarningDialog()) {
//                        for (int i = 0; i < scenarios.getCategories().length; i++) {
//                            for (String scenario : scenarios.getScenariosForCategoryIdx(i)) {
//                                runScenario(scenarios.getScenarioInfo(scenario));
//                            }
//                        }
//                    }
                }
            });

            // System.out.println(view);
            final ExpandableListView testScenariosView = (ExpandableListView) rootView.findViewById(R.id.testScenarioListView);
            TextView testScenariosHeader = new TextView(getActivity());
            testScenariosHeader.setText("Network Tests");
            testScenariosView.addHeaderView(testScenariosHeader);

            // testScenariosView.setGroupIndicator(null);
            testScenariosView.setChildIndicator(null);
            testScenariosView.setAdapter(new BaseExpandableListAdapter() {

                @Override
                public int getGroupCount() {
                    return scenarios.getCategories().length;
                }

                @Override
                public int getChildrenCount(int groupPosition) {
                    return scenarios.getScenariosForCategoryIdx(groupPosition).length;
                }

                @Override
                public Object getGroup(int groupPosition) {
                    return scenarios.getCategories()[groupPosition];
                }

                @Override
                public Object getChild(int groupPosition, int childPosition) {
                    return scenarios.getScenariosForCategoryIdx(groupPosition)[childPosition];
                }

                @Override
                public long getGroupId(int groupPosition) {
                    return groupPosition + 1 << 32;
                }

                @Override
                public long getChildId(int groupPosition, int childPosition) {
                    return groupPosition << 16 | childPosition; // Allows for 16k groups and children
                }

                @Override
                public boolean hasStableIds() {
                    return false;
                }

                @Override
                public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
                    View itemView = null;
                    TextView labelView = null;
                    View progressView;
                    if ((itemView = convertView) == null ||
                            (labelView = (TextView) convertView.findViewById(R.id.scenarioLabel)) == null ||
                            (progressView = itemView.findViewById(R.id.scenarioProgress)) == null) {
                        itemView = inflater.inflate(R.layout.scenario_list_item, parent, false);
                        labelView = (TextView) itemView.findViewById(R.id.scenarioLabel);
                        progressView = itemView.findViewById(R.id.scenarioProgress);
                    }
                    labelView.setText(scenarios.getCategories()[groupPosition]);
                    labelView.setTypeface(null, Typeface.BOLD);
                    progressView.setVisibility(View.INVISIBLE);
                    return itemView;
                }

                @Override
                public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
                    View itemView = null;
                    TextView labelView = null;
                    if ((itemView = convertView) == null || (labelView = (TextView) convertView.findViewById(R.id.scenarioLabel)) == null) {
                        itemView = inflater.inflate(R.layout.scenario_list_item, parent, false);
                        labelView = (TextView) itemView.findViewById(R.id.scenarioLabel);
                    }
                    labelView.setText(scenarios.getScenariosForCategoryIdx(groupPosition)[childPosition]);
                    return itemView;
                }

                @Override
                public boolean isChildSelectable(int groupPosition, int childPosition) {
                    return false;
                }
            });
            // for (int i = 0; i < scenarios.getCategories().length; i++) {
            //     testScenariosView.expandGroup(i);
            // }

            return rootView;
        }

        private String[] checkExcludedScenarios() {
            TelephonyManager telephonyManager = (TelephonyManager) getActivity().
                    getSystemService(Context.TELEPHONY_SERVICE);
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().
                    getSystemService(Context.CONNECTIVITY_SERVICE);
            System.out.println("Managers received: " + telephonyManager);
            NetworkInfo activeNetwork = ensureConnectedOverMobile(connectivityManager);

            return new String[0];
        }

        private NetworkInfo ensureConnectedOverMobile(ConnectivityManager connectivityManager) {
            NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            String title = "Approve running test";
            String message = "Running the tests may incur bandwidth costs. Continue?";
            if (activeNetwork.getType() != ConnectivityManager.TYPE_MOBILE || !activeNetwork.isConnected()) {
                activeNetwork = null;
                NetworkInfo[] networkInfos = connectivityManager.getAllNetworkInfo();
                title = "Not connected on mobile network";
                message = "Device does not have mobile any mobile networks to test.";
                for (NetworkInfo info : networkInfos) {
                    if (info.getType() == ConnectivityManager.TYPE_MOBILE) {
                        connectivityManager.setNetworkPreference(info.getType());
                        message = "Connect with the mobile network to run tests.";
                        break;
                    }
                }
            }
            final NetworkInfo finalActiveNet = activeNetwork;
            AlertDialog dialog = new AlertDialog.Builder(getActivity()).
                    setTitle(title).setMessage(message).
                    setIcon(android.R.drawable.ic_dialog_alert).setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    TelephonyManager telephonyManager = (TelephonyManager) getActivity().
                            getSystemService(Context.TELEPHONY_SERVICE);
                    ScenarioRunner runner = new ScenarioRunner(scenarios);
                    runner.runScenarios(telephonyManager, finalActiveNet);
                }
            }).create();

            dialog.show();

            return activeNetwork;
        }

        private boolean showCostWarningDialog() {
            // Show dialog that warns of bandwidth costs
            return true;
        }

        private void runScenario(Scenario scenarioInfo) {
            AsyncTask<Runnable, Integer, Exception> task = new AsyncTask<Runnable, Integer, Exception>() {

                @Override
                protected Exception doInBackground(Runnable... params) {
                    try {
                        params[0].run();
                    } catch (Exception e) {
                        return e;
                    }
                    return null;
                }
            };
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, scenarioInfo.scenarioLogic);
        }
    }
}
