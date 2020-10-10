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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UiNode extends BasicTreeNode {
    private static final Pattern BOUNDS_PATTERN = Pattern
            .compile("\\[-?(\\d+),-?(\\d+)\\]\\[-?(\\d+),-?(\\d+)\\]");
    // use LinkedHashMap to preserve the order of the attributes
    private final Map<String, String> mAttributes = new LinkedHashMap<String, String>();
    private String mDisplayName = "ShouldNotSeeMe";
    private Object[] mCachedAttributesArray;

    public void addAtrribute(String key, String value) {
        mAttributes.put(key, value);
        updateDisplayName();
        if ("bounds".equals(key)) {
            updateBounds(value);
        }
    }

    public Map<String, String> getAttributes() {
        return Collections.unmodifiableMap(mAttributes);
    }

    /**
     * Builds the display name based on attributes of the node
     */
    private void updateDisplayName() {
        String className = mAttributes.get("class");
        if (className == null)
            return;
        String text = mAttributes.get("text");
        if (text == null)
            return;
        String contentDescription = mAttributes.get("content-desc");
        if (contentDescription == null)
            return;
        String index = mAttributes.get("index");
        if (index == null)
            return;
        String bounds = mAttributes.get("bounds");
        if (bounds == null) {
            return;
        }
        // shorten the standard class names, otherwise it takes up too much space on UI
        className = className.replace("android.widget.", "");
        className = className.replace("android.view.", "");
        StringBuilder builder = new StringBuilder();
        builder.append('(');
        builder.append(index);
        builder.append(") ");
        builder.append(className);
        if (!text.isEmpty()) {
            builder.append(':');
            builder.append(text);
        }
        if (!contentDescription.isEmpty()) {
            builder.append(" {");
            builder.append(contentDescription);
            builder.append('}');
        }
        builder.append(' ');
        builder.append(bounds);
        mDisplayName = builder.toString();
    }

    private void updateBounds(String bounds) {
        Matcher m = BOUNDS_PATTERN.matcher(bounds);
        if (m.matches()) {
            x = Integer.parseInt(m.group(1));
            y = Integer.parseInt(m.group(2));
            width = Integer.parseInt(m.group(3)) - x;
            height = Integer.parseInt(m.group(4)) - y;
            mHasBounds = true;
        } else {
            throw new RuntimeException("Invalid bounds: " + bounds);
        }
    }

    @Override
    public String toString() {
        return mDisplayName;
    }

    public String getAttribute(String key) {
        return mAttributes.get(key);
    }

    @Override
    public Object[] getAttributesArray() {
        // this approach means we do not handle the situation where an attribute is added
        // after this function is first called. This is currently not a concern because the
        // tree is supposed to be readonly
        if (mCachedAttributesArray == null) {
            mCachedAttributesArray = new Object[mAttributes.size()];
            int i = 0;
            for (String attr : mAttributes.keySet()) {
                mCachedAttributesArray[i++] = new AttributePair(attr, mAttributes.get(attr));
            }
        }
        return mCachedAttributesArray;
    }
}
