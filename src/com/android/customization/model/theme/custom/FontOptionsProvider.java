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
package com.android.customization.model.theme.custom;

import static com.android.customization.model.ResourceConstants.OVERLAY_CATEGORY_FONT;

import android.content.Context;
import android.content.FontInfo;
import android.content.IFontService;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.Typeface;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.customization.model.ResourceConstants;
import com.android.customization.model.theme.OverlayManagerCompat;
import com.android.customization.model.theme.custom.ThemeComponentOption.FontOption;
import com.android.wallpaper.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation of {@link ThemeComponentOptionProvider} that reads {@link FontOption}s from
 * font overlays.
 */
public class FontOptionsProvider extends ThemeComponentOptionProvider<FontOption> {

    private static final String TAG = "FontOptionsProvider";
    private IFontService mFontService;
    private List<FontInfo> mFontInfo = new ArrayList<FontInfo>();

    public FontOptionsProvider(Context context, OverlayManagerCompat manager) {
        super(context, manager, OVERLAY_CATEGORY_FONT);
        mFontService = IFontService.Stub.asInterface(
            ServiceManager.getService("dufont"));
    }

    @Override
    protected void loadOptions() {
        mFontInfo.clear();
        addDefault();
        try {
            Map<String, List<FontInfo>> fontMap = mFontService.getAllFonts();
            for (Map.Entry<String, List<FontInfo>> entry : fontMap.entrySet()) {
                String packageName = entry.getKey();
                List<FontInfo> fonts = entry.getValue();
                // manually add system font after we sort
                if (TextUtils.equals(packageName, FontInfo.DEFAULT_FONT_PACKAGE)) {
                    continue;
                }
                for (FontInfo font : fonts) {
                    mFontInfo.add(new FontInfo(font));
                }
            }
            Collections.sort(mFontInfo);
        } catch (RemoteException e) {
            Log.e(TAG, "Error in populating list");
        }
        for (FontInfo availableFont : mFontInfo) {
            try {
                Log.w(TAG, "FONT INFO:");
                Log.w(TAG, "font name:" + availableFont.previewPath);
                Log.w(TAG, "font path:" + availableFont.fontName);
                Log.w(TAG, "font package:" + availableFont.packageName);
                Typeface.Builder fontBuilder = new Typeface.Builder(availableFont.previewPath);
                Typeface tf = fontBuilder.build();
                mOptions.add(new FontOption(availableFont.fontName.toLowerCase(), availableFont.fontName.replace("_", " "),
                        tf));
            } catch (NotFoundException e) {
                Log.w(TAG, String.format("Couldn't load font %s, will skip it",
                        availableFont.fontName.toLowerCase()), e);
            }
        }
    }

    private void addDefault() {
        FontInfo defaultFont = FontInfo.getDefaultFontInfo();
        Typeface.Builder fontBuilder = new Typeface.Builder(defaultFont.previewPath);
        Typeface tf = fontBuilder.build();
        mOptions.add(new FontOption(defaultFont.fontName.toLowerCase(), mContext.getString(R.string.default_theme_title),
                tf));
    }
}
