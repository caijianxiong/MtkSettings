/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.network;

import android.app.Dialog;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.provider.SearchIndexableResource;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.Preference.OnPreferenceClickListener;
import androidx.preference.PreferenceScreen;
import com.android.internal.logging.nano.MetricsProto.MetricsEvent;
import com.android.settings.R;
import com.android.settings.core.FeatureFlags;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.development.featureflags.FeatureFlagPersistent;
import com.android.settings.network.MobilePlanPreferenceController.MobilePlanPreferenceHost;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.wifi.WifiMasterSwitchPreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.android.settings.network.MobilePlanPreferenceController.MANAGE_MOBILE_PLAN_DIALOG_ID;


@SearchIndexable
public class NetworkDashboardFragment extends DashboardFragment implements
        MobilePlanPreferenceHost, OnPreferenceClickListener {
    private static final String TAG = "NetworkDashboardFrag";
    private Context mContext;

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_NETWORK_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    protected int getPreferenceScreenResId() {
        if (FeatureFlagPersistent.isEnabled(getContext(), FeatureFlags.NETWORK_INTERNET_V2)) {
            return R.xml.network_and_internet_v2;
        } else {
            return R.xml.network_and_internet;
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mContext = context;
        if (FeatureFlagPersistent.isEnabled(context, FeatureFlags.NETWORK_INTERNET_V2)) {
            use(MultiNetworkHeaderController.class).init(getSettingsLifecycle());
        }
        use(AirplaneModePreferenceController.class).setFragment(this);
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_network_dashboard;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle(), mMetricsFeatureProvider,
                this /* fragment */, this /* mobilePlanHost */);
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
                                                                                 Lifecycle lifecycle, MetricsFeatureProvider metricsFeatureProvider, Fragment fragment,
                                                                                 MobilePlanPreferenceHost mobilePlanHost) {
        final MobilePlanPreferenceController mobilePlanPreferenceController =
                new MobilePlanPreferenceController(context, mobilePlanHost);
        final WifiMasterSwitchPreferenceController wifiPreferenceController =
                new WifiMasterSwitchPreferenceController(context, metricsFeatureProvider);
        MobileNetworkPreferenceController mobileNetworkPreferenceController = null;
        if (!FeatureFlagPersistent.isEnabled(context, FeatureFlags.NETWORK_INTERNET_V2)) {
            mobileNetworkPreferenceController = new MobileNetworkPreferenceController(context);
        }

        final VpnPreferenceController vpnPreferenceController =
                new VpnPreferenceController(context);
        final PrivateDnsPreferenceController privateDnsPreferenceController =
                new PrivateDnsPreferenceController(context);

        if (lifecycle != null) {
            lifecycle.addObserver(mobilePlanPreferenceController);
            lifecycle.addObserver(wifiPreferenceController);
            if (mobileNetworkPreferenceController != null) {
                lifecycle.addObserver(mobileNetworkPreferenceController);
            }
            lifecycle.addObserver(vpnPreferenceController);
            lifecycle.addObserver(privateDnsPreferenceController);
        }

        final List<AbstractPreferenceController> controllers = new ArrayList<>();

        if (FeatureFlagPersistent.isEnabled(context, FeatureFlags.NETWORK_INTERNET_V2)) {
            controllers.add(new MobileNetworkSummaryController(context, lifecycle));
        }
        if (mobileNetworkPreferenceController != null) {
            controllers.add(mobileNetworkPreferenceController);
        }
        controllers.add(new TetherPreferenceController(context, lifecycle));
        controllers.add(vpnPreferenceController);
        controllers.add(new ProxyPreferenceController(context));
        controllers.add(mobilePlanPreferenceController);
        controllers.add(wifiPreferenceController);
        controllers.add(privateDnsPreferenceController);
        return controllers;
    }

    @Override
    public void onResume() {
        super.onResume();
        final PreferenceScreen screen = getPreferenceScreen();
        if (screen != null) {
            int size = screen.getPreferenceCount();
            List<Preference> list = new ArrayList();
            for (int i = 0; i < size; i++) {
                Preference preference = screen.getPreference(i);
                Log.d(TAG, "preference title:" + preference.getTitle().toString());
                String key = preference.getKey();
                if (key != null) {
                    Log.d(TAG, "preference key:" + key);
                    if (key.contains("Settings$DataUsageSummaryActivity")
                            || key.contains("Settings$SimSettingsActivity")) {
                        list.add(preference);
                    }
                }
            }
            for (Preference p : list) {
                screen.removePreference(p);
            }
        }
    }

    @Override
    public void showMobilePlanMessageDialog() {
        showDialog(MANAGE_MOBILE_PLAN_DIALOG_ID);
    }

    @Override
    public Dialog onCreateDialog(int dialogId) {
        Log.d(TAG, "onCreateDialog: dialogId=" + dialogId);
        switch (dialogId) {
            case MANAGE_MOBILE_PLAN_DIALOG_ID:
                final MobilePlanPreferenceController controller =
                        use(MobilePlanPreferenceController.class);
                return new AlertDialog.Builder(getActivity())
                        .setMessage(controller.getMobilePlanDialogMessage())
                        .setCancelable(false)
                        .setPositiveButton(com.android.internal.R.string.ok,
                                (dialog, id) -> controller.setMobilePlanDialogMessage(null))
                        .create();
            //add by gjw start
            case DIALOG_PROXY_KD:
                View view = getActivity().getLayoutInflater().inflate(R.layout.proxy_kd_config, null);
                String proxyStr = Settings.Global.getString(mContext.getContentResolver(), Settings.Global.HTTP_PROXY);
                String host = "", port = "";
                if (proxyStr != null && !proxyStr.equals(":0")) {
                    String[] splitStr = proxyStr.split(":");
                    if (splitStr.length == 2) {
                        host = splitStr[0];
                        port = splitStr[1];
                    }
                }
                EditText etHost = view.findViewById(R.id.et_proxy_host);
                // if (System.getProperty("http.proxyHost") != null)
                etHost.setText(host);
                EditText etPort = view.findViewById(R.id.et_proxy_port);
                // if (System.getProperty("http.proxyPort") != null)
                etPort.setText(port);
                AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                builder.setView(view);
                builder.setTitle(mContext.getString(R.string.proxy_settings_title));
                builder.setNegativeButton(mContext.getString(R.string.cancel), null);
                builder.setPositiveButton(mContext.getString(R.string.ok), new OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setProxy(etHost.getText().toString(), etPort.getText().toString());
                    }
                });
                return builder.create();
            //add by gjw end
        }
        return super.onCreateDialog(dialogId);
    }

    @Override
    public int getDialogMetricsCategory(int dialogId) {
        if (MANAGE_MOBILE_PLAN_DIALOG_ID == dialogId) {
            return SettingsEnums.DIALOG_MANAGE_MOBILE_PLAN;
        } else if (dialogId == DIALOG_PROXY_KD) {
            return MetricsEvent.METRICS_ETHERNET;
        }
        return 0;
    }

    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    if (FeatureFlagPersistent.isEnabled(context,
                            FeatureFlags.NETWORK_INTERNET_V2)) {
                        sir.xmlResId = R.xml.network_and_internet_v2;
                    } else {
                        sir.xmlResId = R.xml.network_and_internet;
                    }
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                                                                                              context) {
                    return buildPreferenceControllers(context, null /* lifecycle */,
                            null /* metricsFeatureProvider */, null /* fragment */,
                            null /* mobilePlanHost */);
                }

                @Override
                public List<String> getNonIndexableKeys(Context context) {
                    List<String> keys = super.getNonIndexableKeys(context);
                    // Remove master switch as a result
                    keys.add(WifiMasterSwitchPreferenceController.KEY_TOGGLE_WIFI);
                    return keys;
                }
            };

    /// M: Add for handling activity result. @{
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        AirplaneModePreferenceController airplaneModePreferenceController =
                use(AirplaneModePreferenceController.class);
        if (airplaneModePreferenceController.onActivityResult(requestCode, resultCode, data)) {
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
    /// @}

    //add by gjw start
    private static final String PROXY_KD_KEY = "proxy_settings_kandao";
    private static final int DIALOG_PROXY_KD = 1001;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        Preference proxyPc = (Preference) findPreference(PROXY_KD_KEY);
        proxyPc.setOnPreferenceClickListener(this);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference.getKey().equals(PROXY_KD_KEY)) {
            showDialog(DIALOG_PROXY_KD);
        }
        return true;
    }

    private void setProxy(String host, String port) {
        if (host.length() == 0 || port.length() == 0) {
            Settings.Global.putString(mContext.getContentResolver(), Settings.Global.HTTP_PROXY, ":0");
        } else {
            Settings.Global.putString(mContext.getContentResolver(), Settings.Global.HTTP_PROXY, host + ":" + port);
        }
    }
    //add by gjw end
}
