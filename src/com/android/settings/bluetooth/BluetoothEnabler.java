/*
 * Copyright (C) 2010 The Android Open Source Project
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

package com.android.settings.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.UserManager;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.VisibleForTesting;

import com.android.settings.R;
import com.android.settings.widget.SwitchWidgetController;
import com.android.settingslib.RestrictedLockUtils.EnforcedAdmin;
import com.android.settingslib.WirelessUtils;
import com.android.settingslib.core.instrumentation.MetricsFeatureProvider;
import android.util.Log;
import android.app.AlertDialog;

/**
 * BluetoothEnabler is a helper to manage the Bluetooth on/off checkbox
 * preference. It turns on/off Bluetooth and ensures the summary of the
 * preference reflects the current state.
 */
public final class BluetoothEnabler implements SwitchWidgetController.OnSwitchChangeListener {
    private static final String TAG = "BluetoothEnabler";
    private final SwitchWidgetController mSwitchController;
    private final MetricsFeatureProvider mMetricsFeatureProvider;
    private Context mContext;
    private boolean mValidListener;
    private final BluetoothAdapter mBluetoothAdapter;
    private final IntentFilter mIntentFilter;
    private final RestrictionUtils mRestrictionUtils;
    private SwitchWidgetController.OnSwitchChangeListener mCallback;

    private static final String EVENT_DATA_IS_BT_ON = "is_bluetooth_on";
    private static final int EVENT_UPDATE_INDEX = 0;
    private final int mMetricsEvent;

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Broadcast receiver is always running on the UI thread here,
            // so we don't need consider thread synchronization.
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
            Log.d(TAG, "BluetoothAdapter state changed to" + state);
            handleStateChanged(state);
        }
    };

    public BluetoothEnabler(Context context, SwitchWidgetController switchController,
            MetricsFeatureProvider metricsFeatureProvider, int metricsEvent) {
        this(context, switchController, metricsFeatureProvider, metricsEvent,
                new RestrictionUtils());
    }

    public BluetoothEnabler(Context context, SwitchWidgetController switchController,
            MetricsFeatureProvider metricsFeatureProvider, int metricsEvent,
            RestrictionUtils restrictionUtils) {
        mContext = context;
        mMetricsFeatureProvider = metricsFeatureProvider;
        mSwitchController = switchController;
        mSwitchController.setListener(this);
        mValidListener = false;
        mMetricsEvent = metricsEvent;

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            // Bluetooth is not supported
            mSwitchController.setEnabled(false);
        }
        mIntentFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        mRestrictionUtils = restrictionUtils;
    }

    public void setupSwitchController() {
        mSwitchController.setupView();
    }

    public void teardownSwitchController() {
        mSwitchController.teardownView();
    }

    public void resume(Context context) {
        if (mContext != context) {
            mContext = context;
        }

        final boolean restricted = maybeEnforceRestrictions();

        if (mBluetoothAdapter == null) {
            mSwitchController.setEnabled(false);
            return;
        }

        // Bluetooth state is not sticky, so set it manually
        if (!restricted) {
            handleStateChanged(mBluetoothAdapter.getState());
        }

        mSwitchController.startListening();
        mContext.registerReceiver(mReceiver, mIntentFilter);
        mValidListener = true;
    }

    public void pause() {
        if (mBluetoothAdapter == null) {
            return;
        }
        if (mValidListener) {
            mSwitchController.stopListening();
            mContext.unregisterReceiver(mReceiver);
            mValidListener = false;
        }
    }

    void handleStateChanged(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_TURNING_ON:
                mSwitchController.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_ON:
                setChecked(true);
                mSwitchController.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                mSwitchController.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_OFF:
                setChecked(false);
                mSwitchController.setEnabled(true);
                break;
            default:
                setChecked(false);
                mSwitchController.setEnabled(true);
        }
    }

    private void setChecked(boolean isChecked) {
        if (isChecked != mSwitchController.isChecked()) {
            // set listener to null, so onCheckedChanged won't be called
            // if the checked status on Switch isn't changed by user click
            if (mValidListener) {
                mSwitchController.stopListening();
            }
            mSwitchController.setChecked(isChecked);
            if (mValidListener) {
                mSwitchController.startListening();
            }
        }
    }

    @Override
    public boolean onSwitchToggled(boolean isChecked) {
        if (maybeEnforceRestrictions()) {
            triggerParentPreferenceCallback(isChecked);
            return true;
        }
        Log.d(TAG, "onSwitchChanged to " + isChecked);
        // Show toast message if Bluetooth is not allowed in airplane mode
        if (isChecked &&
                !WirelessUtils.isRadioAllowed(mContext, Settings.Global.RADIO_BLUETOOTH)) {
            Toast.makeText(mContext, R.string.wifi_in_airplane_mode, Toast.LENGTH_SHORT).show();
            // Reset switch to off
            mSwitchController.setChecked(false);
            triggerParentPreferenceCallback(false);
            return false;
        }

        mMetricsFeatureProvider.action(mContext, mMetricsEvent, isChecked);

        if (mBluetoothAdapter != null) {
            if (!isChecked) {
                mSwitchController.setChecked(true);
                mSwitchController.setEnabled(true);
                mSwitchController.updateTitle(true);
                triggerParentPreferenceCallback(true);
                new AlertDialog.Builder(mContext)
                        .setMessage(R.string.disable_bluetooth_step_1)
                        .setNegativeButton(android.R.string.cancel, null)
                        .setPositiveButton(R.string.next_step, (dialog, which) -> {
                            showDisableBluetoothConfirmDialog();
                        }).show();
                return true;
            }
            boolean status = setBluetoothEnabled(isChecked);
            // If we cannot toggle it ON then reset the UI assets:
            // a) The switch should be OFF but it should still be togglable (enabled = True)
            // b) The switch bar should have OFF text.
            if (isChecked && !status) {
                mSwitchController.setChecked(false);
                mSwitchController.setEnabled(true);
                mSwitchController.updateTitle(false);
                triggerParentPreferenceCallback(false);
                return false;
            }
        }
        mSwitchController.setEnabled(false);
        triggerParentPreferenceCallback(isChecked);
        return true;
    }

    private void showDisableBluetoothConfirmDialog() {
        new AlertDialog.Builder(mContext)
                .setMessage(R.string.disable_bluetooth_step_2)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.disable_bluetooth, (dialog, which) -> {
                    mSwitchController.setEnabled(false);
                    setBluetoothEnabled(false);
                    triggerParentPreferenceCallback(false);
                }).show();
    }

    /**
     * Sets a callback back that this enabler will trigger in case the preference using the enabler
     * still needed the callback on the SwitchController (which we now use).
     * @param listener The listener with a callback to trigger.
     */
    public void setToggleCallback(SwitchWidgetController.OnSwitchChangeListener listener) {
        mCallback = listener;
    }

    /**
     * Enforces user restrictions disallowing Bluetooth (or its configuration) if there are any.
     *
     * @return if there was any user restriction to enforce.
     */
    @VisibleForTesting
    boolean maybeEnforceRestrictions() {
        EnforcedAdmin admin = getEnforcedAdmin(mRestrictionUtils, mContext);
        mSwitchController.setDisabledByAdmin(admin);
        if (admin != null) {
            mSwitchController.setChecked(false);
            mSwitchController.setEnabled(false);
        }
        return admin != null;
    }

    public static EnforcedAdmin getEnforcedAdmin(RestrictionUtils mRestrictionUtils,
            Context mContext) {
        EnforcedAdmin admin = mRestrictionUtils.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_BLUETOOTH);
        if (admin == null) {
            admin = mRestrictionUtils.checkIfRestrictionEnforced(
                    mContext, UserManager.DISALLOW_CONFIG_BLUETOOTH);
        }
        return admin;
    }

    // This triggers the callback which was manually set for this enabler since the enabler will
    // take over the switch controller callback
    private void triggerParentPreferenceCallback(boolean isChecked) {
        if (mCallback != null) {
            mCallback.onSwitchToggled(isChecked);
        }
    }

    private boolean setBluetoothEnabled(boolean isEnabled) {
        return isEnabled ? mBluetoothAdapter.enable() : mBluetoothAdapter.disable();
    }
}
