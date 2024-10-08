/*
 * Copyright (C) 2017 The Android Open Source Project
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
package com.android.settings.deviceinfo;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.preference.PreferenceScreen;

import com.android.settings.R;
import com.android.settings.core.BasePreferenceController;
import com.android.settingslib.DeviceInfoUtils;
import com.mediatek.settings.UtilsExt;
import com.mediatek.settings.ext.IDeviceInfoSettingsExt;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

import androidx.preference.Preference;
import android.text.TextUtils;
import android.content.Intent; // mid Add 

public class HardwareInfoPreferenceController extends BasePreferenceController {

    private static final String TAG = "DeviceModelPrefCtrl";
    private static IDeviceInfoSettingsExt mExt;
    private static final String KEY_DEVICE_MODEL = "device_model";
    private Context mContext;
    private int mHitCountdown=5;  // mid Add

    public HardwareInfoPreferenceController(Context context, String key) {
        super(context, key);
        mContext=context;
        mExt = UtilsExt.getDeviceInfoSettingsExt(context);

    }
    
   

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), KEY_DEVICE_MODEL)) {
            return false;
        }
        android.util.Log.d("zzzz","mHitCountdown:"+mHitCountdown);
        if(mHitCountdown > 0){
            mHitCountdown--;
        }else{
            Intent intent = new Intent("android.intent.action.MAIN");
            intent.setClassName("com.mediatek.engineermode", "com.mediatek.engineermode.EngineerMode");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            mContext.startActivity(intent);
            mHitCountdown=5;
        }
        return true;
    }

    @Override
    public void displayPreference(PreferenceScreen screen) {
        super.displayPreference(screen);
    }

    @Override
    public int getAvailabilityStatus() {
        return mContext.getResources().getBoolean(R.bool.config_show_device_model)
                ? AVAILABLE_UNSEARCHABLE : UNSUPPORTED_ON_DEVICE;
    }

    @Override
    public CharSequence getSummary() {
        return mContext.getResources().getString(R.string.model_summary,
                mExt.customeModelInfo(getDeviceModel()));
    }

    public static String getDeviceModel() {
        FutureTask<String> msvSuffixTask = new FutureTask<>(() -> DeviceInfoUtils.getMsvSuffix());

        msvSuffixTask.run();
        try {
            // Wait for msv suffix value.
            final String msvSuffix = msvSuffixTask.get();
            return Build.MODEL + msvSuffix;
        } catch (ExecutionException e) {
            Log.e(TAG, "Execution error, so we only show model name");
        } catch (InterruptedException e) {
            Log.e(TAG, "Interruption error, so we only show model name");
        }
        // If we can't get an msv suffix value successfully,
        // it's better to return model name.
        return Build.MODEL;
    }
}
