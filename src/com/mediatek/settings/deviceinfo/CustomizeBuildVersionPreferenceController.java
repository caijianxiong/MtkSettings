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

package com.mediatek.settings.deviceinfo;

import android.content.Context;
import android.os.SystemProperties;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.core.PreferenceControllerMixin;
import com.android.settingslib.core.AbstractPreferenceController;
// mid add for 5 hits @{
import android.app.Activity;
import android.content.Intent;
import android.text.TextUtils;
import com.android.settingslib.core.lifecycle.Lifecycle;
import com.android.settingslib.core.lifecycle.LifecycleObserver;
import com.android.settingslib.core.lifecycle.events.OnResume;
// @}
import com.mediatek.settings.FeatureOption;

public class CustomizeBuildVersionPreferenceController extends AbstractPreferenceController
        implements PreferenceControllerMixin, LifecycleObserver, OnResume  {

    private static final String KEY_CUSTOM_BUILD_VERSION = "custom_build_version";
    private static final String CUSTOM_BUILD_VERSION_PROPERTY
            = "ro.vendor.mediatek.version.release";
    private int mHitCountdown;  // mid modify for 5 hits @{
    private Activity mActivity; 
    public CustomizeBuildVersionPreferenceController(Context context, 
        Activity activity ,Lifecycle lifecycle) {
        super(context);
        mActivity = activity;
        if (lifecycle != null) {
            lifecycle.addObserver(this);
        }
    }  
	// @}

    @Override
    public String getPreferenceKey() {
        return KEY_CUSTOM_BUILD_VERSION;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public void updateState(Preference preference) {
        super.updateState(preference);
        preference.setSummary(SystemProperties.get(CUSTOM_BUILD_VERSION_PROPERTY,
                mContext.getResources().getString(R.string.device_info_default)));
    }
// mid add for 5 hits @{
    @Override
    public void onResume() {
        mHitCountdown = 5;
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        return TextUtils.equals(preference.getKey(), KEY_CUSTOM_BUILD_VERSION);
    }
// @}
}
