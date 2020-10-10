/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.uiautomator.actions;

import com.android.uiautomator.UiAutomatorView;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;

public class ToggleNafAction extends Action {
    private UiAutomatorView mView;

    public ToggleNafAction(UiAutomatorView view) {
        super("&Toggle NAF Nodes", IAction.AS_CHECK_BOX);
        setChecked(view.shouldShowNafNodes());

        mView = view;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageHelper.loadImageDescriptorFromResource("images/warning.png");
    }

    @Override
    public void run() {
        mView.toggleShowNaf();
        mView.redrawScreenshot();
        setChecked(mView.shouldShowNafNodes());
    }
}
