/*
 * Copyright (C) 2013 The Android Open Source Project
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

import com.android.uiautomator.UiAutomatorViewer;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;

import java.io.File;

public class SaveScreenShotAction extends Action {
    private static final String PNG_TYPE = ".png";
    private static final String UIX_TYPE = ".uix";
    private UiAutomatorViewer mViewer;
    public SaveScreenShotAction(UiAutomatorViewer viewer) {
        super("&Save");
        mViewer = viewer;
    }

    @Override
    public ImageDescriptor getImageDescriptor() {
        return ImageHelper.loadImageDescriptorFromResource("images/save.png");
    }

    @Override
    public void run() {
        final Image screenshot = mViewer.getScreenShot();
        final File model = mViewer.getModelFile();
        if (model == null || screenshot == null) {
            return;
        }
        DirectoryDialog dd = new DirectoryDialog(Display.getDefault().getActiveShell());
        dd.setText("Save Screenshot and UiX Files");
        final String path = dd.open();
        if (path == null) {
            return;
        }

        // to prevent blocking the ui thread, we do the saving in the other thread.
        new Thread(){
            String filepath;
            @Override
            public void run() {
                filepath = new File(path, model.getName()).toString();
                filepath = filepath.substring(0,filepath.lastIndexOf("."));
                ImageLoader imageLoader = new ImageLoader();
                imageLoader.data = new ImageData[] {
                        screenshot.getImageData() };
                try {
                    imageLoader.save(filepath + PNG_TYPE, SWT.IMAGE_PNG);
                    FileUtils.copyFile(model, new File(filepath + UIX_TYPE));
                } catch (final Exception e) {
                    Display.getDefault().syncExec(new Runnable() {
                        @Override
                        public void run() {
                            Status status = new Status(IStatus.ERROR,
                                    "Error writing file", e.getLocalizedMessage());
                            ErrorDialog.openError(Display.getDefault().getActiveShell(),
                                    String.format("Error writing %s.uix", filepath),
                                    e.getLocalizedMessage(), status);
                        }
                    });
                }
            };
        }.start();
    }
}
