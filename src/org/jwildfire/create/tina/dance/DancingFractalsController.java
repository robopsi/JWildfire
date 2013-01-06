/*
  JWildfire - an image and animation processor written in Java 
  Copyright (C) 1995-2011 Andreas Maschke

  This is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser 
  General Public License as published by the Free Software Foundation; either version 2.1 of the 
  License, or (at your option) any later version.
 
  This software is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without 
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU 
  Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License along with this software; 
  if not, write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
  02110-1301 USA, or see the FSF site: http://www.fsf.org.
*/
package org.jwildfire.create.tina.dance;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.jwildfire.base.Prefs;
import org.jwildfire.base.Tools;
import org.jwildfire.create.tina.audio.JLayerInterface;
import org.jwildfire.create.tina.audio.RecordedFFT;
import org.jwildfire.create.tina.base.Flame;
import org.jwildfire.create.tina.dance.action.ActionRecorder;
import org.jwildfire.create.tina.dance.action.PostRecordFlameGenerator;
import org.jwildfire.create.tina.dance.model.AnimationModelService;
import org.jwildfire.create.tina.dance.model.PlainProperty;
import org.jwildfire.create.tina.dance.model.PropertyNode;
import org.jwildfire.create.tina.io.Flam3Reader;
import org.jwildfire.create.tina.io.Flam3Writer;
import org.jwildfire.create.tina.randomflame.RandomFlameGenerator;
import org.jwildfire.create.tina.randomflame.RandomFlameGeneratorList;
import org.jwildfire.create.tina.randomflame.RandomFlameGeneratorSampler;
import org.jwildfire.create.tina.render.FlameRenderer;
import org.jwildfire.create.tina.render.RenderInfo;
import org.jwildfire.create.tina.render.RenderedFlame;
import org.jwildfire.create.tina.swing.FlameFileChooser;
import org.jwildfire.create.tina.swing.FlameHolder;
import org.jwildfire.create.tina.swing.FlamePanel;
import org.jwildfire.create.tina.swing.JWFNumberField;
import org.jwildfire.create.tina.swing.SoundFileChooser;
import org.jwildfire.create.tina.swing.TinaController;
import org.jwildfire.image.SimpleImage;
import org.jwildfire.swing.ErrorHandler;
import org.jwildfire.swing.ImagePanel;
import org.jwildfire.transform.TextTransformer;
import org.jwildfire.transform.TextTransformer.FontStyle;
import org.jwildfire.transform.TextTransformer.HAlignment;
import org.jwildfire.transform.TextTransformer.Mode;
import org.jwildfire.transform.TextTransformer.VAlignment;

public class DancingFractalsController {
  public static final int PAGE_INDEX = 3;
  private final ErrorHandler errorHandler;
  private final TinaController parentCtrl;
  private final Prefs prefs;
  private final JButton loadSoundBtn;
  private final JButton addFromClipboardBtn;
  private final JButton addFromEditorBtn;
  private final JButton addFromDiscBtn;
  private final JWFNumberField randomCountIEd;
  private final JButton genRandFlamesBtn;
  private final JComboBox randomGenCmb;
  private final JPanel poolFlamePreviewPnl;
  private final JSlider borderSizeSlider;
  private final JPanel flameRootPanel;
  private final JPanel graph1RootPanel;
  private final JButton flameToEditorBtn;
  private final JButton deleteFlameBtn;
  private final JTextField framesPerSecondIEd;
  private final JTextField morphFrameCountIEd;
  private final JButton startShowButton;
  private final JButton stopShowButton;
  private final JCheckBox doRecordCBx;
  private final JButton saveAllFlamesBtn;
  private final JComboBox flamesCmb;
  private final JCheckBox drawTrianglesCbx;
  private final JCheckBox drawFFTCbx;
  private final JCheckBox drawFPSCbx;
  private final JTree flamePropertiesTree;

  JLayerInterface jLayer = new JLayerInterface();
  private FlamePanel flamePanel = null;
  private FlamePanel poolFlamePreviewFlamePanel = null;
  private ImagePanel graph1Panel = null;
  private List<DancingFlame> dancingFlames = new ArrayList<DancingFlame>();
  private boolean refreshing;
  private RealtimeAnimRenderThread renderThread;
  private ActionRecorder actionRecorder;

  private RecordedFFT fft;
  private String soundFilename;
  private boolean running = false;
  private final PoolFlameHolder poolFlameHolder;

  public DancingFractalsController(TinaController pParent, ErrorHandler pErrorHandler, JPanel pRealtimeFlamePnl, JPanel pRealtimeGraph1Pnl,
      JButton pLoadSoundBtn, JButton pAddFromClipboardBtn, JButton pAddFromEditorBtn, JButton pAddFromDiscBtn, JWFNumberField pRandomCountIEd,
      JButton pGenRandFlamesBtn, JComboBox pRandomGenCmb, JPanel pPoolFlamePreviewPnl, JSlider pBorderSizeSlider,
      JButton pFlameToEditorBtn, JButton pDeleteFlameBtn, JTextField pFramesPerSecondIEd, JTextField pMorphFrameCountIEd,
      JButton pStartShowButton, JButton pStopShowButton, JCheckBox pDoRecordCBx, JButton pSaveAllFlamesBtn,
      JComboBox pFlamesCmb, JCheckBox pDrawTrianglesCbx, JCheckBox pDrawFFTCbx, JCheckBox pDrawFPSCbx, JTree pFlamePropertiesTree) {
    parentCtrl = pParent;
    errorHandler = pErrorHandler;
    prefs = parentCtrl.getPrefs();
    flameRootPanel = pRealtimeFlamePnl;
    graph1RootPanel = pRealtimeGraph1Pnl;
    loadSoundBtn = pLoadSoundBtn;
    addFromClipboardBtn = pAddFromClipboardBtn;
    addFromEditorBtn = pAddFromEditorBtn;
    addFromDiscBtn = pAddFromDiscBtn;
    randomCountIEd = pRandomCountIEd;
    genRandFlamesBtn = pGenRandFlamesBtn;
    randomGenCmb = pRandomGenCmb;
    poolFlamePreviewPnl = pPoolFlamePreviewPnl;
    borderSizeSlider = pBorderSizeSlider;
    flameToEditorBtn = pFlameToEditorBtn;
    deleteFlameBtn = pDeleteFlameBtn;
    framesPerSecondIEd = pFramesPerSecondIEd;
    morphFrameCountIEd = pMorphFrameCountIEd;
    startShowButton = pStartShowButton;
    stopShowButton = pStopShowButton;
    doRecordCBx = pDoRecordCBx;
    saveAllFlamesBtn = pSaveAllFlamesBtn;
    flamesCmb = pFlamesCmb;
    drawTrianglesCbx = pDrawTrianglesCbx;
    drawFFTCbx = pDrawFFTCbx;
    drawFPSCbx = pDrawFPSCbx;
    flamePropertiesTree = pFlamePropertiesTree;
    poolFlameHolder = new PoolFlameHolder();

    refreshPoolFlames();
    enableControls();
  }

  private FlamePanel getFlamePanel() {
    if (flamePanel == null && flameRootPanel != null) {
      int borderWidth = flameRootPanel.getBorder().getBorderInsets(flameRootPanel).left;
      int width = flameRootPanel.getWidth() - borderWidth;
      int height = flameRootPanel.getHeight() - borderWidth;
      if (width < 16 || height < 16)
        return null;
      SimpleImage img = new SimpleImage(width, height);
      img.fillBackground(0, 0, 0);
      // TODO
      flamePanel = new FlamePanel(img, 0, 0, flameRootPanel.getWidth() - borderWidth, null, null, null);
      // TODO right aspect
      flamePanel.setRenderWidth(640);
      flamePanel.setRenderHeight(480);
      flameRootPanel.add(flamePanel, BorderLayout.CENTER);
      flameRootPanel.getParent().validate();
      flameRootPanel.repaint();
    }
    flamePanel.setFlameHolder(renderThread);
    return flamePanel;
  }

  private FlamePanel getPoolPreviewFlamePanel() {
    if (poolFlamePreviewFlamePanel == null && poolFlamePreviewPnl != null) {
      int width = poolFlamePreviewPnl.getWidth();
      int height = poolFlamePreviewPnl.getHeight();
      SimpleImage img = new SimpleImage(width, height);
      img.fillBackground(0, 0, 0);
      poolFlamePreviewFlamePanel = new FlamePanel(img, 0, 0, poolFlamePreviewPnl.getWidth(), poolFlameHolder, null, null);
      // TODO right aspect
      poolFlamePreviewFlamePanel.setRenderWidth(640);
      poolFlamePreviewFlamePanel.setRenderHeight(480);
      poolFlamePreviewFlamePanel.setDrawTriangles(false);
      poolFlamePreviewPnl.add(poolFlamePreviewFlamePanel, BorderLayout.CENTER);
      poolFlamePreviewPnl.getParent().validate();
      poolFlamePreviewPnl.repaint();
    }
    return poolFlamePreviewFlamePanel;
  }

  private ImagePanel getGraph1Panel() {
    if (graph1Panel == null && graph1RootPanel != null) {
      int width = graph1RootPanel.getWidth();
      int height = graph1RootPanel.getHeight();
      SimpleImage img = new SimpleImage(width, height);
      img.fillBackground(0, 0, 0);
      graph1Panel = new ImagePanel(img, 0, 0, graph1RootPanel.getWidth());
      graph1RootPanel.add(graph1Panel, BorderLayout.CENTER);
      graph1RootPanel.getParent().validate();
      graph1RootPanel.repaint();
    }
    return graph1Panel;
  }

  public void refreshFlameImage(Flame flame, boolean pDrawTriangles, double pFPS, boolean pDrawFPS) {
    FlamePanel imgPanel = getFlamePanel();
    if (imgPanel == null)
      return;
    Rectangle bounds = imgPanel.getImageBounds();
    int width = bounds.width;
    int height = bounds.height;
    if (width >= 16 && height >= 16) {
      RenderInfo info = new RenderInfo(width, height);
      if (flame != null) {
        double oldSpatialFilterRadius = flame.getSpatialFilterRadius();
        int oldSpatialOversample = flame.getSpatialOversample();
        int oldColorOversample = flame.getColorOversample();
        double oldSampleDensity = flame.getSampleDensity();
        imgPanel.setDrawTriangles(pDrawTriangles);
        try {
          double wScl = (double) info.getImageWidth() / (double) flame.getWidth();
          double hScl = (double) info.getImageHeight() / (double) flame.getHeight();
          flame.setPixelsPerUnit((wScl + hScl) * 0.5 * flame.getPixelsPerUnit());
          flame.setWidth(info.getImageWidth());
          flame.setHeight(info.getImageHeight());

          FlameRenderer renderer = new FlameRenderer(flame, prefs, false);
          renderer.setProgressUpdater(null);

          new FlamePreparer(prefs).prepareFlame(flame);

          RenderedFlame res = renderer.renderFlame(info);
          SimpleImage img = res.getImage();
          if (pDrawFPS) {
            TextTransformer txt = new TextTransformer();
            txt.setText1("fps: " + Tools.doubleToString(pFPS));
            txt.setAntialiasing(false);
            txt.setColor(Color.LIGHT_GRAY);
            txt.setMode(Mode.NORMAL);
            txt.setFontStyle(FontStyle.PLAIN);
            txt.setFontName("Arial");
            txt.setFontSize(10);
            txt.setHAlign(HAlignment.LEFT);
            txt.setVAlign(VAlignment.BOTTOM);
            txt.transformImage(img);
          }
          imgPanel.setImage(img);
        }
        finally {
          flame.setSpatialFilterRadius(oldSpatialFilterRadius);
          flame.setSpatialOversample(oldSpatialOversample);
          flame.setColorOversample(oldColorOversample);
          flame.setSampleDensity(oldSampleDensity);
        }
      }
    }
    else {
      try {
        imgPanel.setImage(new SimpleImage(width, height));
      }
      catch (Exception ex) {
        ex.printStackTrace();
      }
    }
    flameRootPanel.repaint();
  }

  public void refreshPoolPreviewFlameImage(Flame flame) {
    FlamePanel imgPanel = getPoolPreviewFlamePanel();
    if (imgPanel == null)
      return;
    Rectangle bounds = imgPanel.getImageBounds();
    int width = bounds.width;
    int height = bounds.height;
    if (width >= 16 && height >= 16) {
      RenderInfo info = new RenderInfo(width, height);
      if (flame != null) {
        imgPanel.setDrawTriangles(false);
        double wScl = (double) info.getImageWidth() / (double) flame.getWidth();
        double hScl = (double) info.getImageHeight() / (double) flame.getHeight();
        flame.setPixelsPerUnit((wScl + hScl) * 0.5 * flame.getPixelsPerUnit());
        flame.setWidth(info.getImageWidth());
        flame.setHeight(info.getImageHeight());
        FlameRenderer renderer = new FlameRenderer(flame, prefs, false);
        renderer.setProgressUpdater(null);
        new FlamePreparer(prefs).prepareFlame(flame);
        RenderedFlame res = renderer.renderFlame(info);
        imgPanel.setImage(res.getImage());
      }
      else {
        imgPanel.setImage(new SimpleImage(width, height));
      }
    }
    else {
      imgPanel.setImage(new SimpleImage(width, height));
    }
    poolFlamePreviewPnl.repaint();
  }

  public void startShow() {
    try {
      if (dancingFlames.size() == 0)
        throw new Exception("No flames to animate");
      if (soundFilename == null || soundFilename.length() == 0)
        throw new Exception("No sound file specified");
      if (fft == null)
        throw new Exception("No FFT data");
      jLayer.stop();
      jLayer.play(soundFilename);
      startRender();
      running = true;
      enableControls();
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

  public void importFlame(Flame pFlame) {
    if (pFlame != null) {
      dancingFlames.add(new DancingFlame(pFlame));
      refreshPoolFlames();
      enableControls();
    }
  }

  public void startRender() throws Exception {
    stopRender();
    DancingFlame selFlame = flamesCmb.getSelectedIndex() >= 0 && flamesCmb.getSelectedIndex() < dancingFlames.size() ? dancingFlames.get(flamesCmb.getSelectedIndex()) : null;
    renderThread = new RealtimeAnimRenderThread(this);
    renderThread.getFlameStack().addFlame(selFlame.getFlame(), 0);
    actionRecorder = new ActionRecorder(renderThread);
    renderThread.setFFTData(fft);
    renderThread.setMusicPlayer(jLayer);
    renderThread.setFFTPanel(getGraph1Panel());
    renderThread.setFramesPerSecond(Integer.parseInt(framesPerSecondIEd.getText()));
    renderThread.setDrawTriangles(drawTrianglesCbx.isSelected());
    renderThread.setDrawFFT(drawFFTCbx.isSelected());
    renderThread.setDrawFPS(drawFPSCbx.isSelected());
    actionRecorder.recordStart(selFlame.getFlame());
    new Thread(renderThread).start();
  }

  public void stopRender() throws Exception {
    if (renderThread != null) {
      if (doRecordCBx.isSelected()) {
        actionRecorder.recordStop();
      }
      renderThread.setForceAbort(true);
      if (doRecordCBx.isSelected()) {
        JFileChooser chooser = new FlameFileChooser(prefs);
        if (prefs.getOutputFlamePath() != null) {
          try {
            chooser.setCurrentDirectory(new File(prefs.getOutputFlamePath()));
          }
          catch (Exception ex) {
            ex.printStackTrace();
          }
        }
        if (chooser.showSaveDialog(flameRootPanel) == JFileChooser.APPROVE_OPTION) {
          File file = chooser.getSelectedFile();
          prefs.setLastOutputFlameFile(file);
          PostRecordFlameGenerator generator = new PostRecordFlameGenerator(getParentCtrl().getPrefs(), actionRecorder, renderThread, fft);
          generator.createRecordedFlameFiles(file.getAbsolutePath());
        }
      }
      renderThread = null;
      actionRecorder = null;
    }
  }

  public void genRandomFlames() {
    try {
      final int IMG_WIDTH = 80;
      final int IMG_HEIGHT = 60;
      int count = (int) ((Double) randomCountIEd.getValue() + 0.5);
      for (int i = 0; i < count; i++) {

        RandomFlameGenerator randGen = RandomFlameGeneratorList.getRandomFlameGeneratorInstance((String) randomGenCmb.getSelectedItem(), true);
        int palettePoints = 3 + (int) (Math.random() * 68.0);
        RandomFlameGeneratorSampler sampler = new RandomFlameGeneratorSampler(IMG_WIDTH, IMG_HEIGHT, prefs, randGen, palettePoints);
        dancingFlames.add(new DancingFlame(sampler.createSample().getFlame()));
      }
      refreshPoolFlames();
      enableControls();
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

  public void loadSoundButton_clicked() {
    try {
      JFileChooser chooser = new SoundFileChooser(prefs);
      if (prefs.getInputFlamePath() != null) {
        try {
          chooser.setCurrentDirectory(new File(prefs.getInputSoundFilePath()));
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
      }
      if (chooser.showOpenDialog(flameRootPanel) == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        prefs.setLastInputSoundFile(file);
        setSoundFilename(file.getAbsolutePath());
        enableControls();
      }
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

  private void setSoundFilename(String pFilename) throws Exception {
    if (pFilename != null && pFilename.length() > 0) {
      soundFilename = pFilename;
      fft = jLayer.recordFFT(soundFilename);
    }
    else {
      soundFilename = null;
      fft = null;
    }
  }

  private String getFlameCaption(Flame pFlame) {
    return pFlame.getName().equals("") ? String.valueOf(pFlame.hashCode()) : pFlame.getName();
  }

  private void refreshPoolFlames() {
    boolean oldRefreshing = refreshing;
    refreshing = true;
    try {
      refreshFlamesCmb();
      refreshFlamePropertiesTree();
    }
    finally {
      refreshing = oldRefreshing;
    }
  }

  private void refreshFlamesCmb() {
    DancingFlame selFlame = flamesCmb.getSelectedIndex() >= 0 && flamesCmb.getSelectedIndex() < dancingFlames.size() ? dancingFlames.get(flamesCmb.getSelectedIndex()) : null;
    int newSelIdx = -1;
    flamesCmb.removeAllItems();
    for (int i = 0; i < dancingFlames.size(); i++) {
      DancingFlame flame = dancingFlames.get(i);
      if (newSelIdx < 0 && flame.equals(selFlame)) {
        newSelIdx = i;
      }
      flamesCmb.addItem(getFlameCaption(flame.getFlame()));
    }
    flamesCmb.setSelectedIndex(newSelIdx >= 0 ? newSelIdx : dancingFlames.size() > 0 ? 0 : -1);
  }

  private class FlamePropertiesTreeNode<T> extends DefaultMutableTreeNode {
    private static final long serialVersionUID = 1L;
    private final T nodeData;

    public FlamePropertiesTreeNode(String pCaption, T pNodeData, boolean pAllowsChildren) {
      super(pCaption, pAllowsChildren);
      nodeData = pNodeData;
    }

    public T getNodeData() {
      return nodeData;
    }
  }

  private void refreshFlamePropertiesTree() {
    FlamePropertiesTreeNode<Object> root = new FlamePropertiesTreeNode<Object>("Flames", null, true);
    for (DancingFlame flame : dancingFlames) {
      FlamePropertiesTreeNode<DancingFlame> flameNode = new FlamePropertiesTreeNode<DancingFlame>(getFlameCaption(flame.getFlame()), flame, true);
      PropertyNode model = AnimationModelService.createModel(flame.getFlame());
      addNodesToTree(model, flameNode);
      root.add(flameNode);
    }
    flamePropertiesTree.setModel(new DefaultTreeModel(root));
  }

  private void addNodesToTree(PropertyNode pModel, FlamePropertiesTreeNode<?> pParentNode) {
    for (PropertyNode subNode : pModel.getChields()) {
      FlamePropertiesTreeNode child = new FlamePropertiesTreeNode(subNode.getName(), subNode, true);
      pParentNode.add(child);
      addNodesToTree(subNode, child);
    }
    for (PlainProperty property : pModel.getProperties()) {
      FlamePropertiesTreeNode child = new FlamePropertiesTreeNode(property.getName(), property, false);
      pParentNode.add(child);
    }
  }

  public void loadFlameFromClipboardButton_clicked() {
    List<Flame> newFlames = null;
    try {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable clipData = clipboard.getContents(clipboard);
      if (clipData != null) {
        if (clipData.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          String xml = (String) (clipData.getTransferData(
              DataFlavor.stringFlavor));
          newFlames = new Flam3Reader(prefs).readFlamesfromXML(xml);
        }
      }
      if (newFlames == null || newFlames.size() < 1) {
        throw new Exception("There is currently no valid flame in the clipboard");
      }
      else {
        for (Flame newFlame : newFlames) {
          dancingFlames.add(new DancingFlame(newFlame));
        }
        refreshPoolFlames();
        enableControls();
      }
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

  public void loadFlameButton_clicked() {
    try {
      JFileChooser chooser = new FlameFileChooser(prefs);
      if (prefs.getInputFlamePath() != null) {
        try {
          chooser.setCurrentDirectory(new File(prefs.getInputFlamePath()));
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
      }
      chooser.setMultiSelectionEnabled(true);
      if (chooser.showOpenDialog(poolFlamePreviewPnl) == JFileChooser.APPROVE_OPTION) {
        for (File file : chooser.getSelectedFiles()) {
          List<Flame> newFlames = new Flam3Reader(prefs).readFlames(file.getAbsolutePath());
          prefs.setLastInputFlameFile(file);
          if (newFlames != null && newFlames.size() > 0) {
            for (Flame newFlame : newFlames) {
              dancingFlames.add(new DancingFlame(newFlame));
            }
          }
        }
        refreshPoolFlames();
        enableControls();
      }
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

  public void borderSizeSlider_changed() {
    int value = borderSizeSlider.getValue();
    int currValue = flameRootPanel.getBorder().getBorderInsets(flameRootPanel).left;
    if (currValue != value) {
      flameRootPanel.setBorder(new EmptyBorder(0, 0, value, value));
      if (flamePanel != null) {
        FlamePanel oldFlamePanel = flamePanel;
        flamePanel = null;
        flameRootPanel.remove(oldFlamePanel);
        flameRootPanel.getParent().validate();
        flameRootPanel.repaint();
      }
    }
  }

  public void flameToEditorBtn_clicked() {
    Flame flame = poolFlameHolder.getFlame();
    if (flame != null) {
      parentCtrl.importFlame(flame);
      parentCtrl.getRootTabbedPane().setSelectedIndex(0);
    }
  }

  public void deleteFlameBtn_clicked() {
    Flame flame = poolFlameHolder.getFlame();
    if (flame != null) {
      dancingFlames.remove(flame);
      refreshPoolFlames();
      enableControls();
    }
  }

  public void stopShow() {
    try {
      if (jLayer != null) {
        jLayer.stop();
      }
      stopRender();
      running = false;
      enableControls();
    }
    catch (Throwable ex) {
      errorHandler.handleError(ex);
    }
  }

  private void enableControls() {
    loadSoundBtn.setEnabled(!running);
    addFromClipboardBtn.setEnabled(!running);
    addFromEditorBtn.setEnabled(!running);
    addFromDiscBtn.setEnabled(!running);
    randomCountIEd.setEnabled(!running);
    genRandFlamesBtn.setEnabled(!running);
    randomGenCmb.setEnabled(!running);
    flameToEditorBtn.setEnabled(dancingFlames.size() > 0 && poolFlameHolder.getFlame() != null);
    deleteFlameBtn.setEnabled(dancingFlames.size() > 0 && poolFlameHolder.getFlame() != null);
    framesPerSecondIEd.setEnabled(!running);
    borderSizeSlider.setEnabled(true);
    morphFrameCountIEd.setEnabled(true);
    startShowButton.setEnabled(!running && dancingFlames != null && dancingFlames.size() > 0 && soundFilename != null && soundFilename.length() > 0 && fft != null);
    stopShowButton.setEnabled(running);
    doRecordCBx.setEnabled(!running);
    saveAllFlamesBtn.setEnabled(!running && dancingFlames != null && dancingFlames.size() > 0);
  }

  protected TinaController getParentCtrl() {
    return parentCtrl;
  }

  public void saveAllFlamesBtn_clicked() {
    try {
      JFileChooser chooser = new FlameFileChooser(prefs);
      if (prefs.getOutputFlamePath() != null) {
        try {
          chooser.setCurrentDirectory(new File(prefs.getOutputFlamePath()));
        }
        catch (Exception ex) {
          ex.printStackTrace();
        }
      }
      if (chooser.showSaveDialog(flameRootPanel) == JFileChooser.APPROVE_OPTION) {
        File file = chooser.getSelectedFile();
        prefs.setLastOutputFlameFile(file);
        if (dancingFlames.size() > 0) {
          String fn = file.getName();
          {
            int p = fn.indexOf(".flame");
            if (p > 0 && p == fn.length() - 6) {
              fn = fn.substring(0, p);
            }
          }

          int fileIdx = 1;
          for (DancingFlame flame : dancingFlames) {
            String hs = String.valueOf(fileIdx++);
            while (hs.length() < 5) {
              hs = "0" + hs;
            }
            new Flam3Writer().writeFlame(flame.getFlame(), new File(file.getParent(), fn + hs + ".flame").getAbsolutePath());
          }
        }
      }
    }
    catch (Exception ex) {
      errorHandler.handleError(ex);
    }
  }

  public void drawTrianglesCBx_changed() {
    if (renderThread != null) {
      renderThread.setDrawTriangles(drawTrianglesCbx.isSelected());
    }
  }

  public void drawFFTCBx_changed() {
    if (renderThread != null) {
      renderThread.setDrawFFT(drawFFTCbx.isSelected());
    }
  }

  public void drawFPSCBx_changed() {
    if (renderThread != null) {
      renderThread.setDrawFPS(drawFPSCbx.isSelected());
    }
  }

  public void flameCmb_changed() {
    if (!refreshing && renderThread != null) {
      DancingFlame selFlame = flamesCmb.getSelectedIndex() >= 0 && flamesCmb.getSelectedIndex() < dancingFlames.size() ? dancingFlames.get(flamesCmb.getSelectedIndex()) : null;
      if (selFlame != null && renderThread != null) {
        int morphFrameCount = Integer.parseInt(morphFrameCountIEd.getText());
        renderThread.getFlameStack().addFlame(selFlame.getFlame(), morphFrameCount);
        if (actionRecorder != null)
          actionRecorder.recordFlameChange(selFlame.getFlame(), morphFrameCount);
      }
    }
  }

  private class PoolFlameHolder implements FlameHolder {

    @Override
    public Flame getFlame() {
      Object[] selection = flamePropertiesTree.getSelectionPaths();
      if (selection != null && selection.length > 1) {
        FlamePropertiesTreeNode flameNode = (FlamePropertiesTreeNode) selection[1];
        DancingFlame selFlame = (DancingFlame) flameNode.getNodeData();
        return selFlame.getFlame();
      }
      return null;
    }

  }

  public void flamePropertiesTree_changed(TreeSelectionEvent e) {
    if (!refreshing) {
      TreePath path = e.getNewLeadSelectionPath();
      if (path != null) {
        Object[] selection = path.getPath();
        if (selection != null && selection.length > 1) {
          FlamePropertiesTreeNode flameNode = (FlamePropertiesTreeNode) selection[1];
          DancingFlame selFlame = (DancingFlame) flameNode.getNodeData();
          refreshPoolPreviewFlameImage(selFlame.getFlame());
        }
      }
    }
  }

}
