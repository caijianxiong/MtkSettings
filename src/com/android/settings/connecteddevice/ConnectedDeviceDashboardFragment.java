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
package com.android.settings.connecteddevice;

import android.os.Bundle;
import android.app.settings.SettingsEnums;
import android.content.Context;
import android.net.Uri;
import android.provider.DeviceConfig;
import android.provider.SearchIndexableResource;
import android.text.TextUtils;
import android.util.Log;
import android.content.Intent;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.widget.SwitchBarController;
import com.android.settings.core.SettingsUIDeviceConfig;
import com.android.settings.dashboard.DashboardFragment;
import com.android.settings.password.PasswordUtils;
import com.android.settings.search.BaseSearchIndexProvider;
import com.android.settings.slices.SlicePreferenceController;
import com.android.settingslib.core.AbstractPreferenceController;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.search.SearchIndexable;
import com.android.settings.bluetooth.BluetoothSwitchPreferenceController;
import com.android.settings.widget.SwitchBar;
import com.android.settingslib.widget.FooterPreference;
import androidx.preference.Preference;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SearchIndexable(forTarget = SearchIndexable.ALL & ~SearchIndexable.ARC)
public class ConnectedDeviceDashboardFragment extends DashboardFragment {

    private static final String TAG = "ConnectedDeviceFrag";
    private static final String SETTINGS_PACKAGE_NAME = "com.android.settings";
    private static final String SYSTEMUI_PACKAGE_NAME = "com.android.systemui";
    private static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);

    @VisibleForTesting
    static final String KEY_CONNECTED_DEVICES = "connected_device_list";
    @VisibleForTesting
    static final String KEY_AVAILABLE_DEVICES = "available_device_list";

    //add by gjw start
    private FooterPreference mFooterPreference;
    private SwitchBar mSwitchBar;
    private BluetoothSwitchPreferenceController mController;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        mFooterPreference = mFooterPreferenceMixin.createFooterPreference();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SettingsActivity activity = (SettingsActivity) getActivity();
        mSwitchBar = activity.getSwitchBar();
        String onText = getString(R.string.bluetooth) + " " + getString(R.string.switch_on_text);
        String offText = getString(R.string.bluetooth) + " " + getString(R.string.switch_off_text);
        mSwitchBar.setSwitchBarText(onText, offText);
        mController = new BluetoothSwitchPreferenceController(activity,
                new SwitchBarController(mSwitchBar), mFooterPreference);
        Lifecycle lifecycle = getSettingsLifecycle();
        if (lifecycle != null) {
            lifecycle.addObserver(mController);
        }
    }

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        // if ("kandao_controller_preference".equals(preference.getKey())) {
        //     Intent intent = new Intent("com.kandaovr.meeting.KANDAO_CONTROLLER_GUIDE");
        //     startActivity(intent);
        //     return true;
        // }
        return super.onPreferenceTreeClick(preference);
    }

    @Override
    public void onResume() {
        super.onResume();
        String key1 = "OK";
        String key2 = "VOL-";
        SpannableStringBuilder builder = new SpannableStringBuilder();
        builder.append(getString(R.string.how_to_connect_kandao_controller1))
                .append("\n")
                .append(getString(R.string.how_to_connect_kandao_controller2, key1, key2));
        builder.setSpan(new ForegroundColorSpan(Color.parseColor("#008080")),
                builder.toString().indexOf(key1),
                builder.toString().indexOf(key1) + key1.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        builder.setSpan(new ForegroundColorSpan(Color.parseColor("#008080")),
                builder.toString().indexOf(key2),
                builder.toString().indexOf(key2) + key2.length(),
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final Preference pref = getPreferenceScreen().findPreference("kandao_controller_preference");
        if (pref != null) {
            pref.setTitle(builder);
        }
    }
    //add by gjw end

    @Override
    public int getMetricsCategory() {
        return SettingsEnums.SETTINGS_CONNECTED_DEVICE_CATEGORY;
    }

    @Override
    protected String getLogTag() {
        return TAG;
    }

    @Override
    public int getHelpResource() {
        return R.string.help_url_connected_devices;
    }

    @Override
    protected int getPreferenceScreenResId() {
        return R.xml.connected_devices;
    }

    @Override
    protected List<AbstractPreferenceController> createPreferenceControllers(Context context) {
        return buildPreferenceControllers(context, getSettingsLifecycle());
    }

    private static List<AbstractPreferenceController> buildPreferenceControllers(Context context,
            Lifecycle lifecycle) {
        final List<AbstractPreferenceController> controllers = new ArrayList<>();
        final DiscoverableFooterPreferenceController discoverableFooterPreferenceController =
                new DiscoverableFooterPreferenceController(context);
        controllers.add(discoverableFooterPreferenceController);

        if (lifecycle != null) {
            lifecycle.addObserver(discoverableFooterPreferenceController);
        }

        return controllers;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        final boolean nearbyEnabled = DeviceConfig.getBoolean(DeviceConfig.NAMESPACE_SETTINGS_UI,
                SettingsUIDeviceConfig.BT_NEAR_BY_SUGGESTION_ENABLED, true);
        String callingAppPackageName = PasswordUtils.getCallingAppPackageName(
                getActivity().getActivityToken());
        if (DEBUG) {
            Log.d(TAG, "onAttach() calling package name is : " + callingAppPackageName);
        }
        use(AvailableMediaDeviceGroupController.class).init(this);
        use(ConnectedDeviceGroupController.class).init(this);
        use(PreviouslyConnectedDevicePreferenceController.class).init(this);
        use(DiscoverableFooterPreferenceController.class).init(this);
        use(SlicePreferenceController.class).setSliceUri(nearbyEnabled
                ? Uri.parse(getString(R.string.config_nearby_devices_slice_uri))
                : null);
        use(DiscoverableFooterPreferenceController.class).setAlwaysDiscoverable(
                TextUtils.equals(SETTINGS_PACKAGE_NAME, callingAppPackageName)
                        || TextUtils.equals(SYSTEMUI_PACKAGE_NAME, callingAppPackageName));
    }

    /**
     * For Search.
     */
    public static final SearchIndexProvider SEARCH_INDEX_DATA_PROVIDER =
            new BaseSearchIndexProvider() {
                @Override
                public List<SearchIndexableResource> getXmlResourcesToIndex(
                        Context context, boolean enabled) {
                    final SearchIndexableResource sir = new SearchIndexableResource(context);
                    sir.xmlResId = R.xml.connected_devices;
                    return Arrays.asList(sir);
                }

                @Override
                public List<AbstractPreferenceController> createPreferenceControllers(Context
                        context) {
                    return buildPreferenceControllers(context, null /* lifecycle */);
                }
            };
}
