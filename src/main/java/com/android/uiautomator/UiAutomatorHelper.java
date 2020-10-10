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

package com.android.uiautomator;

import com.android.ddmlib.CollectingOutputReceiver;
import com.android.ddmlib.IDevice;
import com.android.ddmlib.RawImage;
import com.android.ddmlib.SyncService;
import com.android.uiautomator.tree.BasicTreeNode;
import com.android.uiautomator.tree.RootWindowNode;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.widgets.Display;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class UiAutomatorHelper {
    public static final int UIAUTOMATOR_MIN_API_LEVEL = 16;

    private static final String UIAUTOMATOR = "/system/bin/uiautomator";    //$NON-NLS-1$
    private static final String UIAUTOMATOR_DUMP_COMMAND = "dump";          //$NON-NLS-1$
    private static final String UIDUMP_DEVICE_PATH = "/data/local/tmp/uidump.xml";  //$NON-NLS-1$
    private static final int XML_CAPTURE_TIMEOUT_SEC = 40;

    private static boolean supportsUiAutomator(IDevice device) {
        String apiLevelString = device.getProperty(IDevice.PROP_BUILD_API_LEVEL);
        int apiLevel;
        try {
            apiLevel = Integer.parseInt(apiLevelString);
        } catch (NumberFormatException e) {
            apiLevel = UIAUTOMATOR_MIN_API_LEVEL;
        }

        return apiLevel >= UIAUTOMATOR_MIN_API_LEVEL;
    }

    private static void getUiHierarchyFile(IDevice device, File dst,
            IProgressMonitor monitor, boolean compressed) {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        monitor.subTask("Deleting old UI XML snapshot ...");
        String command = "rm " + UIDUMP_DEVICE_PATH;

        try {
            CountDownLatch commandCompleteLatch = new CountDownLatch(1);
            device.executeShellCommand(command,
                    new CollectingOutputReceiver(commandCompleteLatch));
            commandCompleteLatch.await(5, TimeUnit.SECONDS);
        } catch (Exception e1) {
            // ignore exceptions while deleting stale files
        }

        monitor.subTask("Taking UI XML snapshot...");
        if (compressed){
            command = String.format("%s %s --compressed %s", UIAUTOMATOR,
                UIAUTOMATOR_DUMP_COMMAND,
                UIDUMP_DEVICE_PATH);
        } else {
            command = String.format("%s %s %s", UIAUTOMATOR,
                    UIAUTOMATOR_DUMP_COMMAND,
                    UIDUMP_DEVICE_PATH);
        }
        CountDownLatch commandCompleteLatch = new CountDownLatch(1);

        try {
            device.executeShellCommand(
                    command,
                    new CollectingOutputReceiver(commandCompleteLatch),
                    XML_CAPTURE_TIMEOUT_SEC * 1000);
            commandCompleteLatch.await(XML_CAPTURE_TIMEOUT_SEC, TimeUnit.SECONDS);

            monitor.subTask("Pull UI XML snapshot from device...");
            device.getSyncService().pullFile(UIDUMP_DEVICE_PATH,
                    dst.getAbsolutePath(), SyncService.getNullProgressMonitor());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //to maintain a backward compatible api, use non-compressed as default snapshot type
    public static UiAutomatorResult takeSnapshot(IDevice device, IProgressMonitor monitor)
            throws UiAutomatorException {
        return takeSnapshot(device, monitor,false);
    }

    public static UiAutomatorResult takeSnapshot(IDevice device, IProgressMonitor monitor,
           boolean compressed) throws UiAutomatorException {
        if (monitor == null) {
            monitor = new NullProgressMonitor();
        }

        monitor.subTask("Checking if device support UI Automator");
        if (!supportsUiAutomator(device)) {
            String msg = "UI Automator requires a device with API Level "
                                + UIAUTOMATOR_MIN_API_LEVEL;
            throw new UiAutomatorException(msg, null);
        }

        monitor.subTask("Creating temporary files for uiautomator results.");
        File tmpDir = null;
        File xmlDumpFile = null;
        File screenshotFile = null;
        try {
            tmpDir = File.createTempFile("uiautomatorviewer_", "");
            tmpDir.delete();
            if (!tmpDir.mkdirs())
                throw new IOException("Failed to mkdir");
            xmlDumpFile = File.createTempFile("dump_", ".uix", tmpDir);
            screenshotFile = File.createTempFile("screenshot_", ".png", tmpDir);
        } catch (Exception e) {
            String msg = "Error while creating temporary file to save snapshot: "
                    + e.getMessage();
            throw new UiAutomatorException(msg, e);
        }

        tmpDir.deleteOnExit();
        xmlDumpFile.deleteOnExit();
        screenshotFile.deleteOnExit();

        monitor.subTask("Obtaining UI hierarchy");
        try {
            UiAutomatorHelper.getUiHierarchyFile(device, xmlDumpFile, monitor, compressed);
        } catch (Exception e) {
            String msg = "Error while obtaining UI hierarchy XML file: " + e.getMessage();
            throw new UiAutomatorException(msg, e);
        }

        UiAutomatorModel model;
        try {
            model = new UiAutomatorModel(xmlDumpFile);
        } catch (Exception e) {
            String msg = "Error while parsing UI hierarchy XML file: " + e.getMessage();
            throw new UiAutomatorException(msg, e);
        }

        monitor.subTask("Obtaining device screenshot");
        RawImage rawImage;
        try {
            rawImage = device.getScreenshot();
        } catch (Exception e) {
            String msg = "Error taking device screenshot: " + e.getMessage();
            throw new UiAutomatorException(msg, e);
        }

        // rotate the screen shot per device rotation
        // 不需要翻转，翻转反而错了
//        BasicTreeNode root = model.getXmlRootNode();
//        if (root instanceof RootWindowNode) {
//            for (int i = 0; i < ((RootWindowNode)root).getRotation(); i++) {
//                rawImage = rawImage.getRotated();
//            }
//        }
        PaletteData palette = new PaletteData(
                rawImage.getRedMask(),
                rawImage.getGreenMask(),
                rawImage.getBlueMask());
        ImageData imageData = new ImageData(rawImage.width, rawImage.height,
                rawImage.bpp, palette, 1, rawImage.data);
        ImageLoader loader = new ImageLoader();
        loader.data = new ImageData[] { imageData };
        loader.save(screenshotFile.getAbsolutePath(), SWT.IMAGE_PNG);
        Image screenshot = new Image(Display.getDefault(), imageData);

        return new UiAutomatorResult(xmlDumpFile, model, screenshot);
    }



    @SuppressWarnings("serial")
    public static class UiAutomatorException extends Exception {
        public UiAutomatorException(String msg, Throwable t) {
            super(msg, t);
        }
    }

    public static class UiAutomatorResult {
        public final File uiHierarchy;
        public final UiAutomatorModel model;
        public final Image screenshot;

        public UiAutomatorResult(File uiXml, UiAutomatorModel m, Image s) {
            uiHierarchy = uiXml;
            model = m;
            screenshot = s;
        }
    }
}
