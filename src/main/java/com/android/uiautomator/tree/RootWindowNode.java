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

package com.android.uiautomator.tree;



public class RootWindowNode extends BasicTreeNode {

    private final String mWindowName;
    private Object[] mCachedAttributesArray;
    private int mRotation;

    public RootWindowNode(String windowName) {
        this(windowName, 0);
    }

    public RootWindowNode(String windowName, int rotation) {
        mWindowName = windowName;
        mRotation = rotation;
    }

    @Override
    public String toString() {
        return mWindowName;
    }

    @Override
    public Object[] getAttributesArray() {
        if (mCachedAttributesArray == null) {
            mCachedAttributesArray = new Object[]{new AttributePair("window-name", mWindowName)};
        }
        return mCachedAttributesArray;
    }

    public int getRotation() {
        return mRotation;
    }
}
