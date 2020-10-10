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

import com.android.uiautomator.actions.ExpandAllAction;
import com.android.uiautomator.actions.ImageHelper;
import com.android.uiautomator.actions.ToggleNafAction;
import com.android.uiautomator.tree.AttributePair;
import com.android.uiautomator.tree.BasicTreeNode;
import com.android.uiautomator.tree.BasicTreeNodeContentProvider;
import com.android.uiautomator.tree.UiNode;

import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Transform;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Tree;

import java.io.File;
import java.util.List;

public class UiAutomatorView extends Composite {
    private static final int IMG_BORDER = 2;

    // The screenshot area is made of a stack layout of two components: screenshot canvas and
    // a "specify screenshot" button. If a screenshot is already available, then that is displayed
    // on the canvas. If it is not availble, then the "specify screenshot" button is displayed.
    private Composite mScreenshotComposite;
    private StackLayout mStackLayout;
    private Composite mSetScreenshotComposite;
    private Canvas mScreenshotCanvas;

    private TreeViewer mTreeViewer;
    private TableViewer mTableViewer;

    private float mScale = 1.0f;
    private int mDx, mDy;

    private UiAutomatorModel mModel;
    private File mModelFile;
    private Image mScreenshot;

    private List<BasicTreeNode> mSearchResult;
    private int mSearchResultIndex;
    private ToolItem itemDeleteAndInfo;
    private Text searchTextarea;
    private Cursor mOrginialCursor;
    private ToolItem itemPrev, itemNext;
    private ToolItem coordinateLabel;

    private String mLastSearchedTerm;

    private Cursor mCrossCursor;

    public UiAutomatorView(Composite parent, int style) {
        super(parent, SWT.NONE);
        setLayout(new FillLayout());

        SashForm baseSash = new SashForm(this, SWT.HORIZONTAL);
        mOrginialCursor = getShell().getCursor();
        mCrossCursor = new Cursor(getDisplay(), SWT.CURSOR_CROSS);
        mScreenshotComposite = new Composite(baseSash, SWT.BORDER);
        mStackLayout = new StackLayout();
        mScreenshotComposite.setLayout(mStackLayout);
        // draw the canvas with border, so the divider area for sash form can be highlighted
        mScreenshotCanvas = new Canvas(mScreenshotComposite, SWT.BORDER);
        mStackLayout.topControl = mScreenshotCanvas;
        mScreenshotComposite.layout();

        // set cursor when enter canvas
        mScreenshotCanvas.addListener(SWT.MouseEnter, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                getShell().setCursor(mCrossCursor);
            }
        });
        mScreenshotCanvas.addListener(SWT.MouseExit, new Listener() {
            @Override
            public void handleEvent(Event arg0) {
                getShell().setCursor(mOrginialCursor);
            }
        });

        mScreenshotCanvas.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseUp(MouseEvent e) {
                if (mModel != null) {
                    mModel.toggleExploreMode();
                    redrawScreenshot();
                }
            }
        });
        mScreenshotCanvas.setBackground(
                getShell().getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
        mScreenshotCanvas.addPaintListener(new PaintListener() {
            @Override
            public void paintControl(PaintEvent e) {
                if (mScreenshot != null) {
                    updateScreenshotTransformation();
                    // shifting the image here, so that there's a border around screen shot
                    // this makes highlighting red rectangles on the screen shot edges more visible
                    Transform t = new Transform(e.gc.getDevice());
                    t.translate(mDx, mDy);
                    t.scale(mScale, mScale);
                    e.gc.setTransform(t);
                    e.gc.drawImage(mScreenshot, 0, 0);
                    // this resets the transformation to identity transform, i.e. no change
                    // we don't use transformation here because it will cause the line pattern
                    // and line width of highlight rect to be scaled, causing to appear to be blurry
                    e.gc.setTransform(null);
                    if (mModel.shouldShowNafNodes()) {
                        // highlight the "Not Accessibility Friendly" nodes
                        e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));
                        e.gc.setBackground(e.gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));
                        for (Rectangle r : mModel.getNafNodes()) {
                            e.gc.setAlpha(50);
                            e.gc.fillRectangle(mDx + getScaledSize(r.x), mDy + getScaledSize(r.y),
                                    getScaledSize(r.width), getScaledSize(r.height));
                            e.gc.setAlpha(255);
                            e.gc.setLineStyle(SWT.LINE_SOLID);
                            e.gc.setLineWidth(2);
                            e.gc.drawRectangle(mDx + getScaledSize(r.x), mDy + getScaledSize(r.y),
                                    getScaledSize(r.width), getScaledSize(r.height));
                        }
                    }

                    // draw the search result rects
                    if (mSearchResult != null){
                        for (BasicTreeNode result : mSearchResult){
                            if (result instanceof UiNode) {
                                UiNode uiNode = (UiNode) result;
                                Rectangle rect = new Rectangle(
                                        uiNode.x, uiNode.y, uiNode.width, uiNode.height);
                                e.gc.setForeground(
                                        e.gc.getDevice().getSystemColor(SWT.COLOR_YELLOW));
                                e.gc.setLineStyle(SWT.LINE_DASH);
                                e.gc.setLineWidth(1);
                                e.gc.drawRectangle(mDx + getScaledSize(rect.x),
                                        mDy + getScaledSize(rect.y),
                                        getScaledSize(rect.width), getScaledSize(rect.height));
                            }
                        }
                    }

                    // draw the mouseover rects
                    Rectangle rect = mModel.getCurrentDrawingRect();
                    if (rect != null) {
                        e.gc.setForeground(e.gc.getDevice().getSystemColor(SWT.COLOR_RED));
                        if (mModel.isExploreMode()) {
                            // when we highlight nodes dynamically on mouse move,
                            // use dashed borders
                            e.gc.setLineStyle(SWT.LINE_DASH);
                            e.gc.setLineWidth(1);
                        } else {
                            // when highlighting nodes on tree node selection,
                            // use solid borders
                            e.gc.setLineStyle(SWT.LINE_SOLID);
                            e.gc.setLineWidth(2);
                        }
                        e.gc.drawRectangle(mDx + getScaledSize(rect.x), mDy + getScaledSize(rect.y),
                                getScaledSize(rect.width), getScaledSize(rect.height));
                    }
                }
            }
        });
        mScreenshotCanvas.addMouseMoveListener(new MouseMoveListener() {
            @Override
            public void mouseMove(MouseEvent e) {
                if (mModel != null) {
                    int x = getInverseScaledSize(e.x - mDx);
                    int y = getInverseScaledSize(e.y - mDy);
                    // show coordinate
                    coordinateLabel.setText(String.format("(%d,%d)", x,y));
                    if (mModel.isExploreMode()) {
                        BasicTreeNode node = mModel.updateSelectionForCoordinates(x, y);
                        if (node != null) {
                            updateTreeSelection(node);
                        }
                    }
                }
            }
        });

        mSetScreenshotComposite = new Composite(mScreenshotComposite, SWT.NONE);
        mSetScreenshotComposite.setLayout(new GridLayout());

        final Button setScreenshotButton = new Button(mSetScreenshotComposite, SWT.PUSH);
        setScreenshotButton.setText("Specify Screenshot...");
        setScreenshotButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent arg0) {
                FileDialog fd = new FileDialog(setScreenshotButton.getShell());
                fd.setFilterExtensions(new String[] {"*.png" });
                if (mModelFile != null) {
                    fd.setFilterPath(mModelFile.getParent());
                }
                String screenshotPath = fd.open();
                if (screenshotPath == null) {
                    return;
                }

                ImageData[] data;
                try {
                    data = new ImageLoader().load(screenshotPath);
                } catch (Exception e) {
                    return;
                }

                // "data" is an array, probably used to handle images that has multiple frames
                // i.e. gifs or icons, we just care if it has at least one here
                if (data.length < 1) {
                    return;
                }

                mScreenshot = new Image(Display.getDefault(), data[0]);
                redrawScreenshot();
            }
        });

        // right sash is split into 2 parts: upper-right and lower-right
        // both are composites with borders, so that the horizontal divider can be highlighted by
        // the borders
        SashForm rightSash = new SashForm(baseSash, SWT.VERTICAL);

        // upper-right base contains the toolbar and the tree
        Composite upperRightBase = new Composite(rightSash, SWT.BORDER);
        upperRightBase.setLayout(new GridLayout(1, false));

        ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
        toolBarManager.add(new ExpandAllAction(this));
        toolBarManager.add(new ToggleNafAction(this));
        ToolBar searchtoolbar = toolBarManager.createControl(upperRightBase);

        // add search box and navigation buttons for search results
        ToolItem itemSeparator = new ToolItem(searchtoolbar, SWT.SEPARATOR | SWT.RIGHT);
        searchTextarea = new Text(searchtoolbar, SWT.BORDER | SWT.SINGLE | SWT.SEARCH);
        searchTextarea.pack();
        itemSeparator.setWidth(searchTextarea.getBounds().width);
        itemSeparator.setControl(searchTextarea);
        itemPrev = new ToolItem(searchtoolbar, SWT.SIMPLE);
        itemPrev.setImage(ImageHelper.loadImageDescriptorFromResource("images/prev.png")
                .createImage());
        itemNext = new ToolItem(searchtoolbar, SWT.SIMPLE);
        itemNext.setImage(ImageHelper.loadImageDescriptorFromResource("images/next.png")
                .createImage());
        itemDeleteAndInfo = new ToolItem(searchtoolbar, SWT.SIMPLE);
        itemDeleteAndInfo.setImage(ImageHelper.loadImageDescriptorFromResource("images/delete.png")
                .createImage());
        itemDeleteAndInfo.setToolTipText("Clear search results");
        coordinateLabel = new ToolItem(searchtoolbar, SWT.SIMPLE);
        coordinateLabel.setText("");
        coordinateLabel.setEnabled(false);

        // add search function
        searchTextarea.addKeyListener(new KeyListener() {
            @Override
            public void keyReleased(KeyEvent event) {
                if (event.keyCode == SWT.CR) {
                    String term = searchTextarea.getText();
                    if (!term.isEmpty()) {
                        if (term.equals(mLastSearchedTerm)) {
                            nextSearchResult();
                            return;
                        }
                        clearSearchResult();
                        mSearchResult = mModel.searchNode(term);
                        if (!mSearchResult.isEmpty()) {
                            mSearchResultIndex = 0;
                            updateSearchResultSelection();
                            mLastSearchedTerm = term;
                        }
                    }
                }
            }

            @Override
            public void keyPressed(KeyEvent event) {
            }
        });
        SelectionListener l = new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent se) {
                if (se.getSource() == itemPrev) {
                    prevSearchResult();
                 } else if (se.getSource() == itemNext) {
                    nextSearchResult();
                 } else if (se.getSource() == itemDeleteAndInfo) {
                    searchTextarea.setText("");
                    clearSearchResult();
                 }
            }
        };
        itemPrev.addSelectionListener(l);
        itemNext.addSelectionListener(l);
        itemDeleteAndInfo.addSelectionListener(l);

        searchtoolbar.pack();
        searchtoolbar.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        mTreeViewer = new TreeViewer(upperRightBase, SWT.NONE);
        mTreeViewer.setContentProvider(new BasicTreeNodeContentProvider());
        // default LabelProvider uses toString() to generate text to display
        mTreeViewer.setLabelProvider(new LabelProvider());
        mTreeViewer.addSelectionChangedListener(new ISelectionChangedListener() {
            @Override
            public void selectionChanged(SelectionChangedEvent event) {
                BasicTreeNode selectedNode = null;
                if (event.getSelection() instanceof IStructuredSelection) {
                    IStructuredSelection selection = (IStructuredSelection) event.getSelection();
                    Object o = selection.getFirstElement();
                    if (o instanceof BasicTreeNode) {
                        selectedNode = (BasicTreeNode) o;
                    }
                }

                mModel.setSelectedNode(selectedNode);
                redrawScreenshot();
                if (selectedNode != null) {
                    loadAttributeTable();
                }
            }
        });
        Tree tree = mTreeViewer.getTree();
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));
        // move focus so that it's not on tool bar (looks weird)
        tree.setFocus();

        // lower-right base contains the detail group
        Composite lowerRightBase = new Composite(rightSash, SWT.BORDER);
        lowerRightBase.setLayout(new FillLayout());
        Group grpNodeDetail = new Group(lowerRightBase, SWT.NONE);
        grpNodeDetail.setLayout(new FillLayout(SWT.HORIZONTAL));
        grpNodeDetail.setText("Node Detail");

        Composite tableContainer = new Composite(grpNodeDetail, SWT.NONE);

        TableColumnLayout columnLayout = new TableColumnLayout();
        tableContainer.setLayout(columnLayout);

        mTableViewer = new TableViewer(tableContainer, SWT.NONE | SWT.FULL_SELECTION);
        Table table = mTableViewer.getTable();
        table.setLinesVisible(true);
        // use ArrayContentProvider here, it assumes the input to the TableViewer
        // is an array, where each element represents a row in the table
        mTableViewer.setContentProvider(new ArrayContentProvider());

        TableViewerColumn tableViewerColumnKey = new TableViewerColumn(mTableViewer, SWT.NONE);
        TableColumn tblclmnKey = tableViewerColumnKey.getColumn();
        tableViewerColumnKey.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof AttributePair) {
                    // first column, shows the attribute name
                    return ((AttributePair) element).key;
                }
                return super.getText(element);
            }
        });
        columnLayout.setColumnData(tblclmnKey,
                new ColumnWeightData(1, ColumnWeightData.MINIMUM_WIDTH, true));

        TableViewerColumn tableViewerColumnValue = new TableViewerColumn(mTableViewer, SWT.NONE);
        tableViewerColumnValue.setEditingSupport(new AttributeTableEditingSupport(mTableViewer));
        TableColumn tblclmnValue = tableViewerColumnValue.getColumn();
        columnLayout.setColumnData(tblclmnValue,
                new ColumnWeightData(2, ColumnWeightData.MINIMUM_WIDTH, true));
        tableViewerColumnValue.setLabelProvider(new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof AttributePair) {
                    // second column, shows the attribute value
                    return ((AttributePair) element).value;
                }
                return super.getText(element);
            }
        });
        // sets the ratio of the vertical split: left 5 vs right 3
        baseSash.setWeights(new int[] {5, 3 });
    }

    protected void prevSearchResult() {
        if (mSearchResult == null)
            return;
        if(mSearchResult.isEmpty()){
            mSearchResult = null;
            return;
        }
        mSearchResultIndex = mSearchResultIndex - 1;
        if (mSearchResultIndex < 0){
            mSearchResultIndex += mSearchResult.size();
        }
        updateSearchResultSelection();
    }
    protected void clearSearchResult() {
        itemDeleteAndInfo.setText("");
        mSearchResult = null;
        mSearchResultIndex = 0;
        mLastSearchedTerm = "";
        mScreenshotCanvas.redraw();
    }
    protected void nextSearchResult() {
        if (mSearchResult == null)
            return;
        if(mSearchResult.isEmpty()){
            mSearchResult = null;
            return;
        }
        mSearchResultIndex = (mSearchResultIndex + 1) % mSearchResult.size();
        updateSearchResultSelection();
    }

    private void updateSearchResultSelection() {
        updateTreeSelection(mSearchResult.get(mSearchResultIndex));
        itemDeleteAndInfo.setText("" + (mSearchResultIndex + 1) + "/"
                + mSearchResult.size());
    }

    private int getScaledSize(int size) {
        if (mScale == 1.0f) {
            return size;
        } else {
            return new Double(Math.floor((size * mScale))).intValue();
        }
    }

    private int getInverseScaledSize(int size) {
        if (mScale == 1.0f) {
            return size;
        } else {
            return new Double(Math.floor((size / mScale))).intValue();
        }
    }

    private void updateScreenshotTransformation() {
        Rectangle canvas = mScreenshotCanvas.getBounds();
        Rectangle image = mScreenshot.getBounds();
        float scaleX = (canvas.width - 2 * IMG_BORDER - 1) / (float) image.width;
        float scaleY = (canvas.height - 2 * IMG_BORDER - 1) / (float) image.height;

        // use the smaller scale here so that we can fit the entire screenshot
        mScale = Math.min(scaleX, scaleY);
        // calculate translation values to center the image on the canvas
        mDx = (canvas.width - getScaledSize(image.width) - IMG_BORDER * 2) / 2 + IMG_BORDER;
        mDy = (canvas.height - getScaledSize(image.height) - IMG_BORDER * 2) / 2 + IMG_BORDER;
    }

    private class AttributeTableEditingSupport extends EditingSupport {

        private TableViewer mViewer;

        public AttributeTableEditingSupport(TableViewer viewer) {
            super(viewer);
            mViewer = viewer;
        }

        @Override
        protected boolean canEdit(Object arg0) {
            return true;
        }

        @Override
        protected CellEditor getCellEditor(Object arg0) {
            return new TextCellEditor(mViewer.getTable());
        }

        @Override
        protected Object getValue(Object o) {
            return ((AttributePair) o).value;
        }

        @Override
        protected void setValue(Object arg0, Object arg1) {
        }
    }

    /**
     * Causes a redraw of the canvas.
     *
     * The drawing code of canvas will handle highlighted nodes and etc based on data
     * retrieved from Model
     */
    public void redrawScreenshot() {
        if (mScreenshot == null) {
            mStackLayout.topControl = mSetScreenshotComposite;
        } else {
            mStackLayout.topControl = mScreenshotCanvas;
        }
        mScreenshotComposite.layout();

        mScreenshotCanvas.redraw();
    }

    public void setInputHierarchy(Object input) {
        mTreeViewer.setInput(input);
    }

    public void loadAttributeTable() {
        // update the lower right corner table to show the attributes of the node
        mTableViewer.setInput(mModel.getSelectedNode().getAttributesArray());
    }

    public void expandAll() {
        mTreeViewer.expandAll();
    }

    public void updateTreeSelection(BasicTreeNode node) {
        mTreeViewer.setSelection(new StructuredSelection(node), true);
    }

    public void setModel(UiAutomatorModel model, File modelBackingFile, Image screenshot) {
        mModel = model;
        mModelFile = modelBackingFile;

        if (mScreenshot != null) {
            mScreenshot.dispose();
        }
        mScreenshot = screenshot;
        clearSearchResult();
        redrawScreenshot();
        // load xml into tree
        BasicTreeNode wrapper = new BasicTreeNode();
        // putting another root node on top of existing root node
        // because Tree seems to like to hide the root node
        wrapper.addChild(mModel.getXmlRootNode());
        setInputHierarchy(wrapper);
        mTreeViewer.getTree().setFocus();

    }

    public boolean shouldShowNafNodes() {
        return mModel != null ? mModel.shouldShowNafNodes() : false;
    }

    public void toggleShowNaf() {
        if (mModel != null) {
            mModel.toggleShowNaf();
        }
    }

    public Image getScreenShot() {
        return mScreenshot;
    }

    public File getModelFile() {
        return mModelFile;
    }
}
