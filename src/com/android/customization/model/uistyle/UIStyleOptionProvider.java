/*
 * Copyright (C) 2019 The Android Open Source Project
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
package com.android.customization.model.uistyle;

import static com.android.customization.model.ResourceConstants.ANDROID_PACKAGE;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_UI_STYLE_ANDROID;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_UI_STYLE_SETTINGS;
import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_UI_STYLE_SYSUI;
import static com.android.customization.model.ResourceConstants.UI_STYLE_BACKGROUND_COLOR_LIGHT_NAME;
import static com.android.customization.model.ResourceConstants.UI_STYLE_BACKGROUND_COLOR_DARK_NAME;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.UserHandle;
import android.util.Log;

import com.android.customization.model.ResourceConstants;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIStyleOptionProvider {

    private static final String TAG = "UIStyleOptionProvider";

    private Context mContext;
    private PackageManager mPm;
    private final List<String> mOverlayPackages;
    private final List<UIStyleOption> mOptions = new ArrayList<>();
    private final List<String> mSysUiStylesOverlayPackages = new ArrayList<>();
    private final List<String> mSettingsStylesOverlayPackages = new ArrayList<>();

    public UIStyleOptionProvider(Context context, OverlayManagerCompat manager) {
        mContext = context;
        mPm = context.getPackageManager();
        String[] targetPackages = ResourceConstants.getPackagesToOverlay(context);
        mSysUiStylesOverlayPackages.addAll(manager.getOverlayPackagesForCategory(
                OVERLAY_CATEGORY_UI_STYLE_SYSUI, UserHandle.myUserId(), targetPackages));
        mSettingsStylesOverlayPackages.addAll(manager.getOverlayPackagesForCategory(
                OVERLAY_CATEGORY_UI_STYLE_SETTINGS, UserHandle.myUserId(), targetPackages));
        mOverlayPackages = new ArrayList<>();
        mOverlayPackages.addAll(manager.getOverlayPackagesForCategory(OVERLAY_CATEGORY_UI_STYLE_ANDROID,
                UserHandle.myUserId(), ResourceConstants.getPackagesToOverlay(mContext)));
    }

    public List<UIStyleOption> getOptions() {
        if (mOptions.isEmpty()) loadOptions();
        return mOptions;
    }

    private void loadOptions() {
        addDefault();

        Map<String, UIStyleOption> optionsByPrefix = new HashMap<>();
        for (String overlayPackage : mOverlayPackages) {
            UIStyleOption option = addOrUpdateOption(optionsByPrefix, overlayPackage,
                    OVERLAY_CATEGORY_UI_STYLE_ANDROID);
            try{
                Resources overlayRes = mPm.getResourcesForApplication(overlayPackage);
                int lightColor = overlayRes.getColor(
                        overlayRes.getIdentifier(UI_STYLE_BACKGROUND_COLOR_LIGHT_NAME, "color", overlayPackage),
                        null);
                int darkColor = overlayRes.getColor(
                        overlayRes.getIdentifier(UI_STYLE_BACKGROUND_COLOR_DARK_NAME, "color", overlayPackage),
                        null);
                PackageManager pm = mContext.getPackageManager();
                String label = pm.getApplicationInfo(overlayPackage, 0).loadLabel(pm).toString();
                option.addStyleInfo(lightColor, darkColor);
            } catch (NotFoundException | NameNotFoundException e) {
                Log.w(TAG, String.format("Couldn't load UI style overlay details for %s, will skip it",
                        overlayPackage), e);
            }
        }

        for (String overlayPackage : mSysUiStylesOverlayPackages) {
            addOrUpdateOption(optionsByPrefix, overlayPackage, OVERLAY_CATEGORY_UI_STYLE_SYSUI);
        }

        for (String overlayPackage : mSettingsStylesOverlayPackages) {
            addOrUpdateOption(optionsByPrefix, overlayPackage, OVERLAY_CATEGORY_UI_STYLE_SETTINGS);
        }

        for (UIStyleOption option : optionsByPrefix.values()) {
            if (option.isValid(mContext)) {
                mOptions.add(option);
            }
        }
    }

    private UIStyleOption addOrUpdateOption(Map<String, UIStyleOption> optionsByPrefix,
            String overlayPackage, String category) {
        String prefix = overlayPackage.substring(0, overlayPackage.lastIndexOf("."));
        UIStyleOption option = null;
        try {
            if (!optionsByPrefix.containsKey(prefix)) {
                option = new UIStyleOption(mPm.getApplicationInfo(overlayPackage, 0).loadLabel(mPm).toString());
                optionsByPrefix.put(prefix, option);
            } else {
                option = optionsByPrefix.get(prefix);
            }
            option.addOverlayPackage(category, overlayPackage);
        } catch (NameNotFoundException e) {
            Log.e(TAG, String.format("Package %s not found", overlayPackage), e);
        }
        return option;
    }

    private void addDefault() {
        int lightColor, darkColor;
        Resources system = Resources.getSystem();
        UIStyleOption option = new UIStyleOption(mContext.getString(R.string.default_theme_title), true);
        try {
            lightColor = system.getColor(
                    system.getIdentifier(UI_STYLE_BACKGROUND_COLOR_LIGHT_NAME, "color", ANDROID_PACKAGE), null);

            darkColor = system.getColor(
                    system.getIdentifier(UI_STYLE_BACKGROUND_COLOR_DARK_NAME, "color", ANDROID_PACKAGE), null);

            option.addStyleInfo(lightColor, darkColor);
        } catch (NotFoundException e) {
            Log.w(TAG, "Didn't find system default ui style package, will skip option", e);
        }
        option.addOverlayPackage(OVERLAY_CATEGORY_UI_STYLE_ANDROID, null);
        option.addOverlayPackage(OVERLAY_CATEGORY_UI_STYLE_SYSUI, null);
        option.addOverlayPackage(OVERLAY_CATEGORY_UI_STYLE_SETTINGS, null);
        mOptions.add(option);
    }

}
