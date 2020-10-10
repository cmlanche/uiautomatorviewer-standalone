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

import com.android.uiautomator.OpenDialog;
import com.android.uiautomator.UiAutomatorModel;
import com.android.uiautomator.UiAutomatorViewer;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.Display;

import java.io.File;

public class OpenFilesAction extends Action {
    private UiAutomatorViewer mViewer;

    public OpenFilesAction(UiAutomatorViewer viewer) {
        super("&Open");

        mViewer = viewer;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageHelper.loadImageDescriptorFromResource("images/open-folder.png");
    }

    @Override
    public void run() {
        OpenDialog d = new OpenDialog(Display.getDefault().getActiveShell());
        if (d.open() != OpenDialog.OK) {
            return;
        }

        UiAutomatorModel model;
        try {
            model = new UiAutomatorModel(d.getXmlDumpFile());
        } catch (Exception e) {
            // FIXME: show error
            return;
        }

        Image img = null;
        File screenshot = d.getScreenshotFile();
        if (screenshot != null) {
            try {
                ImageData[] data = new ImageLoader().load(screenshot.getAbsolutePath());

                // "data" is an array, probably used to handle images that has multiple frames
                // i.e. gifs or icons, we just care if it has at least one here
                if (data.length < 1) {
                    throw new RuntimeException("Unable to load image: "
                            + screenshot.getAbsolutePath());
                }

                img = new Image(Display.getDefault(), data[0]);
            } catch (Exception e) {
                // FIXME: show error
                return;
            }
        }

        mViewer.setModel(model, d.getXmlDumpFile(), img);
    }
}
