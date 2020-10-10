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

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import java.io.File;

/**
 * Implements a file selection dialog for both screen shot and xml dump file
 *
 * "OK" button won't be enabled unless both files are selected
 * It also has a convenience feature such that if one file has been picked, and the other
 * file path is empty, then selection for the other file will start from the same base folder
 *
 */
public class OpenDialog extends Dialog {
    private static final int FIXED_TEXT_FIELD_WIDTH = 300;
    private static final int DEFAULT_LAYOUT_SPACING = 10;
    private Text mScreenshotText;
    private Text mXmlText;
    private boolean mFileChanged = false;
    private Button mOkButton;

    private static File sScreenshotFile;
    private static File sXmlDumpFile;

    /**
     * Create the dialog.
     * @param parentShell
     */
    public OpenDialog(Shell parentShell) {
        super(parentShell);
        setShellStyle(SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
    }

    /**
     * Create contents of the dialog.
     * @param parent
     */
    @Override
    protected Control createDialogArea(Composite parent) {
        Composite container = (Composite) super.createDialogArea(parent);
        GridLayout gl_container = new GridLayout(1, false);
        gl_container.verticalSpacing = DEFAULT_LAYOUT_SPACING;
        gl_container.horizontalSpacing = DEFAULT_LAYOUT_SPACING;
        gl_container.marginWidth = DEFAULT_LAYOUT_SPACING;
        gl_container.marginHeight = DEFAULT_LAYOUT_SPACING;
        container.setLayout(gl_container);

        Group openScreenshotGroup = new Group(container, SWT.NONE);
        openScreenshotGroup.setLayout(new GridLayout(2, false));
        openScreenshotGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        openScreenshotGroup.setText("Screenshot");

        mScreenshotText = new Text(openScreenshotGroup, SWT.BORDER | SWT.READ_ONLY);
        if (sScreenshotFile != null) {
            mScreenshotText.setText(sScreenshotFile.getAbsolutePath());
        }
        GridData gd_screenShotText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_screenShotText.minimumWidth = FIXED_TEXT_FIELD_WIDTH;
        gd_screenShotText.widthHint = FIXED_TEXT_FIELD_WIDTH;
        mScreenshotText.setLayoutData(gd_screenShotText);

        Button openScreenshotButton = new Button(openScreenshotGroup, SWT.NONE);
        openScreenshotButton.setText("...");
        openScreenshotButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                handleOpenScreenshotFile();
            }
        });

        Group openXmlGroup = new Group(container, SWT.NONE);
        openXmlGroup.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1));
        openXmlGroup.setText("UI XML Dump");
        openXmlGroup.setLayout(new GridLayout(2, false));

        mXmlText = new Text(openXmlGroup, SWT.BORDER | SWT.READ_ONLY);
        mXmlText.setEditable(false);
        if (sXmlDumpFile != null) {
            mXmlText.setText(sXmlDumpFile.getAbsolutePath());
        }
        GridData gd_xmlText = new GridData(SWT.FILL, SWT.CENTER, true, false, 1, 1);
        gd_xmlText.minimumWidth = FIXED_TEXT_FIELD_WIDTH;
        gd_xmlText.widthHint = FIXED_TEXT_FIELD_WIDTH;
        mXmlText.setLayoutData(gd_xmlText);

        Button openXmlButton = new Button(openXmlGroup, SWT.NONE);
        openXmlButton.setText("...");
        openXmlButton.addListener(SWT.Selection, new Listener() {
            @Override
            public void handleEvent(Event event) {
                handleOpenXmlDumpFile();
            }
        });

        return container;
    }

    /**
     * Create contents of the button bar.
     * @param parent
     */
    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        mOkButton = createButton(parent, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
        updateButtonState();
    }

    /**
     * Return the initial size of the dialog.
     */
    @Override
    protected Point getInitialSize() {
        return new Point(368, 233);
    }

    @Override
    protected void configureShell(Shell newShell) {
        super.configureShell(newShell);
        newShell.setText("Open UI Dump Files");
    }

    private void handleOpenScreenshotFile() {
        FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
        fd.setText("Open Screenshot File");
        File initialFile = sScreenshotFile;
        // if file has never been selected before, try to base initial path on the mXmlDumpFile
        if (initialFile == null && sXmlDumpFile != null && sXmlDumpFile.isFile()) {
            initialFile = sXmlDumpFile.getParentFile();
        }
        if (initialFile != null) {
            if (initialFile.isFile()) {
                fd.setFileName(initialFile.getAbsolutePath());
            } else if (initialFile.isDirectory()) {
                fd.setFilterPath(initialFile.getAbsolutePath());
            }
        }
        String[] filter = {"*.png"};
        fd.setFilterExtensions(filter);
        String selected = fd.open();
        if (selected != null) {
            sScreenshotFile = new File(selected);
            mScreenshotText.setText(selected);
            mFileChanged = true;
        }
        updateButtonState();
    }

    private void handleOpenXmlDumpFile() {
        FileDialog fd = new FileDialog(getShell(), SWT.OPEN);
        fd.setText("Open UI Dump XML File");
        File initialFile = sXmlDumpFile;
        // if file has never been selected before, try to base initial path on the mScreenshotFile
        if (initialFile == null && sScreenshotFile != null && sScreenshotFile.isFile()) {
            initialFile = sScreenshotFile.getParentFile();
        }
        if (initialFile != null) {
            if (initialFile.isFile()) {
                fd.setFileName(initialFile.getAbsolutePath());
            } else if (initialFile.isDirectory()) {
                fd.setFilterPath(initialFile.getAbsolutePath());
            }
        }
        String initialPath = mXmlText.getText();
        if (initialPath.isEmpty() && sScreenshotFile != null && sScreenshotFile.isFile()) {
            initialPath = sScreenshotFile.getParentFile().getAbsolutePath();
        }
        String[] filter = {"*.uix"};
        fd.setFilterExtensions(filter);
        String selected = fd.open();
        if (selected != null) {
            sXmlDumpFile = new File(selected);
            mXmlText.setText(selected);
            mFileChanged = true;
        }
        updateButtonState();
    }

    private void updateButtonState() {
        mOkButton.setEnabled(sXmlDumpFile != null && sXmlDumpFile.isFile());
    }

    public boolean hasFileChanged() {
        return mFileChanged;
    }

    public File getScreenshotFile() {
        return sScreenshotFile;
    }

    public File getXmlDumpFile() {
        return sXmlDumpFile;
    }
}
