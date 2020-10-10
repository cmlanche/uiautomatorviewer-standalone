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

import com.android.uiautomator.tree.AttributePair;
import com.android.uiautomator.tree.BasicTreeNode;
import com.android.uiautomator.tree.BasicTreeNode.IFindNodeListener;
import com.android.uiautomator.tree.UiHierarchyXmlLoader;
import com.android.uiautomator.tree.UiNode;

import org.eclipse.swt.graphics.Rectangle;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class UiAutomatorModel {
    private BasicTreeNode mRootNode;
    private BasicTreeNode mSelectedNode;
    private Rectangle mCurrentDrawingRect;
    private List<Rectangle> mNafNodes;

    // determines whether we lookup the leaf UI node on mouse move of screenshot image
    private boolean mExploreMode = true;

    private boolean mShowNafNodes = false;
    private List<BasicTreeNode> mNodelist;
    private Set<String> mSearchKeySet = new HashSet<String>();

    public UiAutomatorModel(File xmlDumpFile) {
        mSearchKeySet.add("text");
        mSearchKeySet.add("content-desc");

        UiHierarchyXmlLoader loader = new UiHierarchyXmlLoader();
        BasicTreeNode rootNode = loader.parseXml(xmlDumpFile.getAbsolutePath());
        if (rootNode == null) {
            System.err.println("null rootnode after parsing.");
            throw new IllegalArgumentException("Invalid ui automator hierarchy file.");
        }

        mNafNodes = loader.getNafNodes();
        if (mRootNode != null) {
            mRootNode.clearAllChildren();
        }

        mRootNode = rootNode;
        mExploreMode = true;
        mNodelist = loader.getAllNodes();
    }

    public BasicTreeNode getXmlRootNode() {
        return mRootNode;
    }

    public BasicTreeNode getSelectedNode() {
        return mSelectedNode;
    }

    /**
     * change node selection in the Model recalculate the rect to highlight,
     * also notifies the View to refresh accordingly
     *
     * @param node
     */
    public void setSelectedNode(BasicTreeNode node) {
        mSelectedNode = node;
        if (mSelectedNode instanceof UiNode) {
            UiNode uiNode = (UiNode) mSelectedNode;
            mCurrentDrawingRect = new Rectangle(uiNode.x, uiNode.y, uiNode.width, uiNode.height);
        } else {
            mCurrentDrawingRect = null;
        }
    }

    public Rectangle getCurrentDrawingRect() {
        return mCurrentDrawingRect;
    }

    /**
     * Do a search in tree to find a leaf node or deepest parent node containing the coordinate
     *
     * @param x
     * @param y
     * @return
     */
    public BasicTreeNode updateSelectionForCoordinates(int x, int y) {
        BasicTreeNode node = null;

        if (mRootNode != null) {
            MinAreaFindNodeListener listener = new MinAreaFindNodeListener();
            boolean found = mRootNode.findLeafMostNodesAtPoint(x, y, listener);
            if (found && listener.mNode != null && !listener.mNode.equals(mSelectedNode)) {
                node = listener.mNode;
            }
        }

        return node;
    }

    public boolean isExploreMode() {
        return mExploreMode;
    }

    public void toggleExploreMode() {
        mExploreMode = !mExploreMode;
    }

    public void setExploreMode(boolean exploreMode) {
        mExploreMode = exploreMode;
    }

    private static class MinAreaFindNodeListener implements IFindNodeListener {
        BasicTreeNode mNode = null;

        @Override
        public void onFoundNode(BasicTreeNode node) {
            if (mNode == null) {
                mNode = node;
            } else {
                if ((node.height * node.width) < (mNode.height * mNode.width)) {
                    mNode = node;
                }
            }
        }
    }

    public List<Rectangle> getNafNodes() {
        return mNafNodes;
    }

    public void toggleShowNaf() {
        mShowNafNodes = !mShowNafNodes;
    }

    public boolean shouldShowNafNodes() {
        return mShowNafNodes;
    }

    public List<BasicTreeNode> searchNode(String tofind) {
        List<BasicTreeNode> result = new LinkedList<BasicTreeNode>();
        for (BasicTreeNode node : mNodelist) {
            Object[] attrs = node.getAttributesArray();
            for (Object attr : attrs) {
                if (!mSearchKeySet.contains(((AttributePair) attr).key))
                    continue;
                if (((AttributePair) attr).value.toLowerCase().contains(tofind.toLowerCase())) {
                    result.add(node);
                    break;
                }
            }
        }
        return result;
    }
}
