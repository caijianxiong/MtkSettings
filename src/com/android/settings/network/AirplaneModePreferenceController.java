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

import static android.provider.SettingsSlicesContract.KEY_AIRPLANE_MODE;

/// M: Add for checking result code.
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.SystemProperties;
/// M: Add for checking current user.
import android.os.UserManager;
import android.util.Log;

import androidx.fragment.app.Fragment;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;
import androidx.preference.SwitchPreference;

import com.android.internal.telephony.TelephonyIntents;
import com.android.internal.telephony.TelephonyProperties;
import com.android.settings.AirplaneModeEnabler;
import com.android.settings.R;
import com.android.settings.core.TogglePreferenceController;
import com.android.settings.overlay.FeatureFactory;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnPause;
import com.android.settingslib.core.lifecycle.events.OnResume;

public class AirplaneModePreferenceController extends TogglePreferenceController
        implements LifecycleObserver, OnResume, OnPause,
        AirplaneModeEnabler.OnAirplaneModeChangedListener {

    private static final String TAG = "AirplaneModePreferenceController";

    public static final int REQUEST_CODE_EXIT_ECM = 1;

    private static final String EXIT_ECM_RESULT = "exit_ecm_result";

    private Fragment mFragment;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private AirplaneModeEnabler mAirplaneModeEnabler;
    private SwitchPreference mAirplaneModePreference;

    public AirplaneModePreferenceController(Context context, String key) {
        super(context, key);
        mMetricsFeatureProvider = FeatureFactory.getFactory(context).getMetricsFeatureProvider();
        mAirplaneModeEnabler = new AirplaneModeEnabler(mContext, mMetricsFeatureProvider, this);
    }

    public void setFragment(Fragment hostFragment) {
        mFragment = hostFragment;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        /// M: The property may be a conbination string of "true" and "false". @{
        if (getPreferenceKey().equals(preference.getKey())) {
            String ecbMode = SystemProperties.get(
                    TelephonyProperties.PROPERTY_INECM_MODE, "false");
            /// M: Launch ECM app dialog in ECM mode for host owner only.
            boolean isAdmin = UserManager.get(mContext).isAdminUser();
            Log.d(TAG, "Click airplane mode, ECM=" + ecbMode
                    + ", isAdmin=" + isAdmin
                    + ", fragment=" + (mFragment == null ? "null" : mFragment));
            if (ecbMode != null && ecbMode.contains("true") && isAdmin) {
                // In ECM mode launch ECM app dialog
                if (mFragment != null) {
                    mFragment.startActivityForResult(
                            new Intent(TelephonyIntents.ACTION_SHOW_NOTICE_ECM_BLOCK_OTHERS, null),
                            REQUEST_CODE_EXIT_ECM);
                }
                return true;
            }
        }
        /// @}

        return false;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
        if (isAvailable()) {
            mAirplaneModePreference = screen.findPreference(getPreferenceKey());
        }
    }

    public static boolean isAvailable(Context context) {
    	return false;
        // return context.getResources().getBoolean(R.bool.config_show_toggle_airplane)
        //         && !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_LEANBACK);
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    @AvailabilityStatus
    public int getAvailabilityStatus() {
        return isAvailable(mContext) ? AVAILABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public void onResume() {
        if (isAvailable()) {
            mAirplaneModeEnabler.resume();
        }
    }

    @Override
    public void onPause() {
        if (isAvailable()) {
            mAirplaneModeEnabler.pause();
        }
    }

    /// M: Add return value to handle the activity result.
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_EXIT_ECM) {
            /// M: Check the result code directly.
            // There will be no intent returned from EmergencyCallbackModeExitDialog.
            boolean isChoiceYes = (resultCode == Activity.RESULT_OK);
            Log.d(TAG, "Exit ECM, result=" + isChoiceYes + ", resultCode=" + resultCode);
            if (mAirplaneModePreference != null && mAirplaneModeEnabler != null) {
                Log.d(TAG, "Exit ECM, checked=" + mAirplaneModePreference.isChecked());
                // Set Airplane mode based on the return value and checkbox state
                mAirplaneModeEnabler.setAirplaneModeInECM(isChoiceYes,
                        mAirplaneModePreference.isChecked());
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean isChecked() {
        return mAirplaneModeEnabler.isAirplaneModeOn();
    }

    @Override
    public boolean setChecked(boolean isChecked) {
        if (isChecked() == isChecked) {
            return false;
        }
        mAirplaneModeEnabler.setAirplaneMode(isChecked);
        return true;
    }

    @Override
    public void onAirplaneModeChanged(boolean isAirplaneModeOn) {
        if (mAirplaneModePreference != null) {
            mAirplaneModePreference.setChecked(isAirplaneModeOn);
        }
    }
}
