/*
 * Copyright (C) 2021 The Android Open Source Project
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

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.android.customization.model.CustomizationManager.Callback;
import com.android.customization.model.CustomizationManager.OptionsFetchedListener;
import com.android.customization.model.CustomizationOption;
import com.android.customization.picker.uistyle.UIStyleFragment;
import com.android.customization.picker.uistyle.UIStyleSectionView;
import com.android.customization.widget.OptionSelectorController;
import com.android.customization.widget.OptionSelectorController.OptionSelectedListener;

import com.android.wallpaper.R;
import com.android.wallpaper.model.CustomizationSectionController;
import com.android.wallpaper.util.LaunchUtils;

import java.util.List;

/** A {@link CustomizationSectionController} for system ui styles. */

public class UIStyleSectionController implements CustomizationSectionController<UIStyleSectionView> {

    private static final String TAG = "UIStyleSectionController";

    private final UIStyleManager mUIStyleOptionsManager;
    private final CustomizationSectionNavigationController mSectionNavigationController;
    private final Callback mApplyUIStyleCallback = new Callback() {
        @Override
        public void onSuccess() {
        }

        @Override
        public void onError(@Nullable Throwable throwable) {
        }
    };

    public UIStyleSectionController(UIStyleManager uiStyleOptionsManager,
            CustomizationSectionNavigationController sectionNavigationController) {
        mUIStyleOptionsManager = uiStyleOptionsManager;
        mSectionNavigationController = sectionNavigationController;
    }

    @Override
    public boolean isAvailable(Context context) {
        return mUIStyleOptionsManager.isAvailable();
    }

    @Override
    public UIStyleSectionView createView(Context context) {
        UIStyleSectionView uiStylesSectionView = (UIStyleSectionView) LayoutInflater.from(context)
                .inflate(R.layout.ui_style_section_view, /* root= */ null);

        TextView sectionDescription = uiStylesSectionView.findViewById(R.id.ui_style_section_description);
        View sectionTile = uiStylesSectionView.findViewById(R.id.ui_style_section_tile);

        mUIStyleOptionsManager.fetchOptions(new OptionsFetchedListener<UIStyleOption>() {
            @Override
            public void onOptionsLoaded(List<UIStyleOption> options) {
                UIStyleOption activeOption = getActiveOption(options);
                sectionDescription.setText(activeOption.getTitle());
                activeOption.bindThumbnailTile(sectionTile);
            }

            @Override
            public void onError(@Nullable Throwable throwable) {
                if (throwable != null) {
                    Log.e(TAG, "Error loading UI style options", throwable);
                }
                sectionDescription.setText(R.string.something_went_wrong);
                sectionTile.setVisibility(View.GONE);
            }
        }, /* reload= */ true);

        uiStylesSectionView.setOnClickListener(v -> mSectionNavigationController.navigateTo(
                UIStyleFragment.newInstance(context.getString(R.string.preview_name_ui_style))));

        return uiStylesSectionView;
    }

    private UIStyleOption getActiveOption(List<UIStyleOption> options) {
        return options.stream()
                .filter(option -> option.isActive(mUIStyleOptionsManager))
                .findAny()
                // For development only, as there should always be a grid set.
                .orElse(options.get(0));
    }
}
