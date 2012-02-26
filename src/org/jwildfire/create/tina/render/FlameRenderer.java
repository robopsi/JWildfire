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
package org.jwildfire.create.tina.render;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.jwildfire.base.Prefs;
import org.jwildfire.base.Tools;
import org.jwildfire.create.tina.base.Flame;
import org.jwildfire.create.tina.base.RasterPoint;
import org.jwildfire.create.tina.base.XYZPoint;
import org.jwildfire.create.tina.io.Flam3Reader;
import org.jwildfire.create.tina.palette.RenderColor;
import org.jwildfire.create.tina.random.RandomNumberGenerator;
import org.jwildfire.create.tina.random.SimpleRandomNumberGenerator;
import org.jwildfire.create.tina.swing.ProgressUpdater;
import org.jwildfire.create.tina.variation.DefaultFlameTransformationContextImpl;
import org.jwildfire.create.tina.variation.FastFlameTransformationContextImpl;
import org.jwildfire.create.tina.variation.FlameTransformationContext;
import org.jwildfire.image.Pixel;
import org.jwildfire.image.SimpleHDRImage;
import org.jwildfire.image.SimpleImage;
import org.jwildfire.io.ImageWriter;
import org.jwildfire.transform.ScaleAspect;
import org.jwildfire.transform.ScaleTransformer;

public class FlameRenderer {
  // constants
  private final static int MAX_FILTER_WIDTH = 25;
  // init in initRaster
  private int imageWidth;
  private int imageHeight;
  int rasterWidth;
  int rasterHeight;
  private int rasterSize;
  private int borderWidth;
  private int maxBorderWidth;
  LogDensityFilter logDensityFilter;
  GammaCorrectionFilter gammaCorrectionFilter;
  RasterPoint[][] raster;
  // init in initView
  double cosa;
  double sina;
  double camW;
  double camH;
  double rcX;
  double rcY;
  double bws;
  double bhs;
  private int renderScale = 1;
  // init in createColorMap
  RenderColor[] colorMap;
  double paletteIdxScl;
  RandomNumberGenerator random = new SimpleRandomNumberGenerator();
  // 3D stuff
  private boolean doProject3D = false;
  // init in init3D()
  private double cameraMatrix[][] = new double[3][3];
  //
  private ProgressUpdater progressUpdater;
  // 
  private final FlameTransformationContext flameTransformationContext;

  private final Flame flame;
  private final Prefs prefs;

  public FlameRenderer(Flame pFlame, Prefs pPrefs) {
    flame = pFlame;
    prefs = pPrefs;
    flameTransformationContext = prefs.getTinaRenderFastMath() > 0 ? new FastFlameTransformationContextImpl(this) : new DefaultFlameTransformationContextImpl(this);
  }

  private void init3D() {
    double yaw = -flame.getCamYaw() * Math.PI / 180.0;
    double pitch = flame.getCamPitch() * Math.PI / 180.0;
    cameraMatrix[0][0] = Math.cos(yaw);
    cameraMatrix[1][0] = -Math.sin(yaw);
    cameraMatrix[2][0] = 0;
    cameraMatrix[0][1] = Math.cos(pitch) * Math.sin(yaw);
    cameraMatrix[1][1] = Math.cos(pitch) * Math.cos(yaw);
    cameraMatrix[2][1] = -Math.sin(pitch);
    cameraMatrix[0][2] = Math.sin(pitch) * Math.sin(yaw);
    cameraMatrix[1][2] = Math.sin(pitch) * Math.cos(yaw);
    cameraMatrix[2][2] = Math.cos(pitch);
    doProject3D = Math.abs(flame.getCamYaw()) > Tools.EPSILON || Math.abs(flame.getCamPitch()) > Tools.EPSILON || Math.abs(flame.getCamPerspective()) > Tools.EPSILON || Math.abs(flame.getCamDOF()) > Tools.EPSILON;
  }

  private void initRaster(int pImageWidth, int pImageHeight) {
    imageWidth = pImageWidth;
    imageHeight = pImageHeight;
    logDensityFilter = new LogDensityFilter(flame);
    gammaCorrectionFilter = new GammaCorrectionFilter(flame);
    maxBorderWidth = (MAX_FILTER_WIDTH - 1) / 2;
    borderWidth = (logDensityFilter.getNoiseFilterSize() - 1) / 2;
    rasterWidth = imageWidth + 2 * maxBorderWidth;
    rasterHeight = imageHeight + 2 * maxBorderWidth;
    rasterSize = rasterWidth * rasterHeight;
    raster = new RasterPoint[rasterHeight][rasterWidth];
    for (int i = 0; i < rasterHeight; i++) {
      for (int j = 0; j < rasterWidth; j++) {
        raster[i][j] = new RasterPoint();
      }
    }
  }

  public void project(XYZPoint pPoint) {
    if (!doProject3D) {
      return;
    }
    double z = pPoint.z;
    double px = cameraMatrix[0][0] * pPoint.x + cameraMatrix[1][0] * pPoint.y;
    double py = cameraMatrix[0][1] * pPoint.x + cameraMatrix[1][1] * pPoint.y + cameraMatrix[2][1] * z;
    double pz = cameraMatrix[0][2] * pPoint.x + cameraMatrix[1][2] * pPoint.y + cameraMatrix[2][2] * z;
    double zr = 1.0 - flame.getCamPerspective() * pz;
    if (Math.abs(flame.getCamDOF()) > Tools.EPSILON) {
      double a = 2.0 * Math.PI * random.random();
      double dsina = Math.sin(a);
      double dcosa = Math.cos(a);
      double zdist = (flame.getCamZ() - pz);
      double dr;
      if (zdist > 0.0) {
        dr = random.random() * flame.getCamDOF() * 0.1 * zdist;
      }
      else {
        dr = 0.0;
      }
      pPoint.x = (px + dr * dcosa) / zr;
      pPoint.y = (py + dr * dsina) / zr;

    }
    else {
      pPoint.x = px / zr;
      pPoint.y = py / zr;
    }
  }

  public RenderedFlame renderFlame(RenderInfo pRenderInfo) {
    RenderedFlame res = new RenderedFlame();
    res.init(pRenderInfo);

    boolean renderNormal = true;
    boolean renderHDR = pRenderInfo.isRenderHDR();
    boolean renderHDRIntensityMap = pRenderInfo.isRenderHDRIntensityMap();

    if (flame.getXForms().size() == 0) {
      if (renderNormal) {
        if (renderScale > 0) {
          res.getImage().resetImage(res.getImage().getImageWidth() * renderScale, res.getImage().getImageHeight() * renderScale);
        }
        res.getImage().fillBackground(flame.getBGColorRed(), flame.getBGColorGreen(), flame.getBGColorBlue());
      }
      if (renderHDR) {
        res.getHDRImage().fillBackground(flame.getBGColorRed(), flame.getBGColorGreen(), flame.getBGColorBlue());
      }
      return res;
    }
    int spatialOversample = flame.getSampleDensity() >= 100.0 ? flame.getSpatialOversample() : 1;
    if (spatialOversample < 1 || spatialOversample > 6) {
      throw new IllegalArgumentException(String.valueOf(spatialOversample));
    }
    int colorOversample = flame.getSampleDensity() >= 100.0 ? flame.getColorOversample() : 1;
    if (colorOversample < 1 || colorOversample > 10) {
      throw new IllegalArgumentException(String.valueOf(colorOversample));
    }

    double origZoom = flame.getCamZoom();
    try {
      SimpleImage images[] = renderNormal ? new SimpleImage[colorOversample] : null;
      SimpleHDRImage hdrImages[] = renderHDR ? new SimpleHDRImage[colorOversample] : null;
      SimpleHDRImage hdrIntensityMapImages[] = renderHDRIntensityMap ? new SimpleHDRImage[colorOversample] : null;
      for (int i = 0; i < colorOversample; i++) {
        SimpleImage img = null;
        SimpleHDRImage hdrImg = null;
        SimpleHDRImage hdrIntensityMapImg = null;
        // spatial oversampling: create enlarged image
        if (spatialOversample > 1) {
          if (renderNormal) {
            img = new SimpleImage(res.getImage().getImageWidth() * spatialOversample, res.getImage().getImageHeight() * spatialOversample);
          }
          if (renderHDR) {
            hdrImg = new SimpleHDRImage(res.getHDRImage().getImageWidth() * spatialOversample, res.getHDRImage().getImageHeight() * spatialOversample);
          }
          //
          if (renderHDRIntensityMap) {
            hdrIntensityMapImg = new SimpleHDRImage(res.getHDRIntensityMap().getImageWidth() * spatialOversample, res.getHDRIntensityMap().getImageHeight() * spatialOversample);
          }
          if (i == 0) {
            flame.setCamZoom((double) spatialOversample * flame.getCamZoom());
          }
        }
        else {
          if (colorOversample > 1) {
            if (renderNormal) {
              img = new SimpleImage(res.getImage().getImageWidth(), res.getImage().getImageHeight());
            }
            if (renderHDR) {
              hdrImg = new SimpleHDRImage(res.getHDRImage().getImageWidth(), res.getHDRImage().getImageHeight());
            }
            if (renderHDRIntensityMap) {
              hdrIntensityMapImg = new SimpleHDRImage(res.getHDRIntensityMap().getImageWidth(), res.getHDRIntensityMap().getImageHeight());
            }
          }
          else {
            if (renderNormal) {
              img = res.getImage();
            }
            if (renderHDR) {
              hdrImg = res.getHDRImage();
            }
            if (renderHDR) {
              hdrIntensityMapImg = res.getHDRIntensityMap();
            }
          }
        }
        if (renderNormal) {
          images[i] = img;
        }
        if (renderHDR) {
          hdrImages[i] = hdrImg;
        }
        if (renderHDRIntensityMap) {
          hdrIntensityMapImages[i] = hdrIntensityMapImg;
        }

        if (renderNormal) {
          initRaster(img.getImageWidth(), img.getImageHeight());
        }
        else if (renderHDR) {
          initRaster(hdrImg.getImageWidth(), hdrImg.getImageHeight());
        }
        else if (renderHDRIntensityMap) {
          initRaster(hdrIntensityMapImg.getImageWidth(), hdrIntensityMapImg.getImageHeight());
        }
        else {
          throw new IllegalStateException();
        }

        init3D();
        createColorMap();
        initView();
        List<Flame> renderFlames = new ArrayList<Flame>();
        for (int t = 0; t < prefs.getTinaRenderThreads(); t++) {
          Flame renderFlame = flame.makeCopy();
          renderFlames.add(renderFlame);
          renderFlame.refreshModWeightTables(flameTransformationContext);
        }

        iterate(i, colorOversample, renderFlames);
        if (flame.getSampleDensity() <= 10.0) {
          renderImageSimple(img);
        }
        else {
          renderImage(img, hdrImg, hdrIntensityMapImg);
        }
      }

      // color oversampling
      SimpleImage img = null;
      SimpleHDRImage hdrImg = null;
      SimpleHDRImage hdrIntensityMapImg = null;
      if (colorOversample == 1) {
        if (renderNormal) {
          img = images[0];
        }
        if (renderHDR) {
          hdrImg = hdrImages[0];
        }
        if (renderHDRIntensityMap) {
          hdrIntensityMapImg = hdrIntensityMapImages[0];
        }
      }
      else {
        if (renderNormal) {
          int width = images[0].getImageWidth();
          int height = images[0].getImageHeight();
          if (spatialOversample > 1) {
            img = new SimpleImage(width, height);
          }
          else {
            img = res.getImage();
          }
          Pixel toolPixel = new Pixel();
          for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
              int r = 0, g = 0, b = 0;
              for (int k = 0; k < colorOversample; k++) {
                toolPixel.setARGBValue(images[k].getARGBValue(j, i));
                r += toolPixel.r;
                g += toolPixel.g;
                b += toolPixel.b;
              }
              toolPixel.r = Tools.roundColor((double) r / (double) colorOversample);
              toolPixel.g = Tools.roundColor((double) g / (double) colorOversample);
              toolPixel.b = Tools.roundColor((double) b / (double) colorOversample);
              img.setARGB(j, i, toolPixel.getARGBValue());
            }
          }
        }
        if (renderHDR) {
          int width = hdrImages[0].getImageWidth();
          int height = hdrImages[0].getImageHeight();
          if (spatialOversample > 1) {
            hdrImg = new SimpleHDRImage(width, height);
          }
          else {
            hdrImg = res.getHDRImage();
          }
          for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
              double r = 0, g = 0, b = 0;
              for (int k = 0; k < colorOversample; k++) {
                r += hdrImages[k].getRValue(j, i);
                g += hdrImages[k].getGValue(j, i);
                b += hdrImages[k].getBValue(j, i);
              }
              r /= (double) colorOversample;
              g /= (double) colorOversample;
              b /= (double) colorOversample;
              hdrImg.setRGB(j, i, (float) r, (float) g, (float) b);
            }
          }
        }
        if (renderHDRIntensityMap) {
          int width = hdrIntensityMapImages[0].getImageWidth();
          int height = hdrIntensityMapImages[0].getImageHeight();
          if (spatialOversample > 1) {
            hdrIntensityMapImg = new SimpleHDRImage(width, height);
          }
          else {
            hdrIntensityMapImg = res.getHDRIntensityMap();
          }
          for (int i = 0; i < height; i++) {
            for (int j = 0; j < width; j++) {
              double r = 0, g = 0, b = 0;
              for (int k = 0; k < colorOversample; k++) {
                r += hdrIntensityMapImages[k].getRValue(j, i);
                g += hdrIntensityMapImages[k].getGValue(j, i);
                b += hdrIntensityMapImages[k].getBValue(j, i);
              }
              r /= (double) colorOversample;
              g /= (double) colorOversample;
              b /= (double) colorOversample;
              hdrIntensityMapImg.setRGB(j, i, (float) r, (float) g, (float) b);
            }
          }
        }

      }

      // spatial oversampling: scale down      
      if (spatialOversample > 1) {
        if (renderNormal) {
          ScaleTransformer scaleT = new ScaleTransformer();
          scaleT.setScaleWidth(res.getImage().getImageWidth());
          scaleT.setScaleHeight(res.getImage().getImageHeight());
          scaleT.setAspect(ScaleAspect.IGNORE);
          scaleT.transformImage(img);
          res.getImage().setBufferedImage(img.getBufferedImg(), res.getImage().getImageWidth(), res.getImage().getImageHeight());
        }
        if (renderHDR) {
          hdrImg.sampleDown(spatialOversample);
          res.getHDRImage().assignImage(hdrImg);
        }
        if (renderHDRIntensityMap) {
          hdrIntensityMapImg.sampleDown(spatialOversample);
          res.getHDRIntensityMap().assignImage(hdrIntensityMapImg);
        }
      }
    }
    finally {
      flame.setCamZoom(origZoom);
    }
    return res;
  }

  private void renderImage(SimpleImage pImage, SimpleHDRImage pHDRImage, SimpleHDRImage pHDRIntensityMap) {
    if (renderScale > 1) {
      throw new IllegalArgumentException("renderScale != 1");
    }
    LogDensityPoint logDensityPnt = new LogDensityPoint();
    if (pImage != null) {
      logDensityFilter.setRaster(raster, rasterWidth, rasterHeight, pImage.getImageWidth(), pImage.getImageHeight());
    }
    else if (pHDRImage != null) {
      logDensityFilter.setRaster(raster, rasterWidth, rasterHeight, pHDRImage.getImageWidth(), pHDRImage.getImageHeight());
    }
    else if (pHDRIntensityMap != null) {
      logDensityFilter.setRaster(raster, rasterWidth, rasterHeight, pHDRIntensityMap.getImageWidth(), pHDRIntensityMap.getImageHeight());
    }
    else {
      throw new IllegalStateException();
    }

    if (pImage != null) {
      GammaCorrectedRGBPoint rbgPoint = new GammaCorrectedRGBPoint();
      for (int i = 0; i < pImage.getImageHeight(); i++) {
        for (int j = 0; j < pImage.getImageWidth(); j++) {
          logDensityFilter.transformPoint(logDensityPnt, j, i);
          gammaCorrectionFilter.transformPoint(logDensityPnt, rbgPoint);
          pImage.setRGB(j, i, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
        }
      }
    }

    if (pHDRImage != null) {
      GammaCorrectedHDRPoint rbgPoint = new GammaCorrectedHDRPoint();
      for (int i = 0; i < pHDRImage.getImageHeight(); i++) {
        for (int j = 0; j < pHDRImage.getImageWidth(); j++) {
          logDensityFilter.transformPointHDR(logDensityPnt, j, i);
          gammaCorrectionFilter.transformPointHDR(logDensityPnt, rbgPoint);
          pHDRImage.setRGB(j, i, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
        }
      }
    }

    if (pHDRIntensityMap != null) {
      for (int i = 0; i < pHDRIntensityMap.getImageHeight(); i++) {
        for (int j = 0; j < pHDRIntensityMap.getImageWidth(); j++) {
          logDensityFilter.transformPointHDR(logDensityPnt, j, i);
          pHDRIntensityMap.setRGB(j, i, (float) logDensityPnt.intensity, (float) logDensityPnt.intensity, (float) logDensityPnt.intensity);
        }
      }
    }
  }

  private void renderImageSimple(SimpleImage pImage) {
    LogDensityPoint logDensityPnt = new LogDensityPoint();
    GammaCorrectedRGBPoint rbgPoint = new GammaCorrectedRGBPoint();
    logDensityFilter.setRaster(raster, rasterWidth, rasterHeight, pImage.getImageWidth(), pImage.getImageHeight());
    if (renderScale == 2) {
      SimpleImage newImg = new SimpleImage(pImage.getImageWidth() * renderScale, pImage.getImageHeight() * renderScale);

      for (int i = 0; i < pImage.getImageHeight(); i++) {
        for (int j = 0; j < pImage.getImageWidth(); j++) {
          logDensityFilter.transformPointSimple(logDensityPnt, j, i);
          gammaCorrectionFilter.transformPointSimple(logDensityPnt, rbgPoint);
          int x = j * renderScale;
          int y = i * renderScale;

          newImg.setRGB(x, y, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
          newImg.setRGB(x + 1, y, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
          newImg.setRGB(x, y + 1, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
          newImg.setRGB(x + 1, y + 1, rbgPoint.red, rbgPoint.green, rbgPoint.blue);

        }
      }
      pImage.setBufferedImage(newImg.getBufferedImg(), newImg.getImageWidth(), newImg.getImageHeight());
    }
    else if (renderScale == 1) {
      for (int i = 0; i < pImage.getImageHeight(); i++) {
        for (int j = 0; j < pImage.getImageWidth(); j++) {
          logDensityFilter.transformPointSimple(logDensityPnt, j, i);
          gammaCorrectionFilter.transformPointSimple(logDensityPnt, rbgPoint);
          pImage.setRGB(j, i, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
        }
      }
    }
    else {
      throw new IllegalArgumentException("renderScale " + renderScale);
    }
  }

  private void iterate(int pPart, int pParts, List<Flame> pFlames) {
    long nSamples = (long) ((flame.getSampleDensity() * (double) rasterSize + 0.5));
    //    if (flame.getSampleDensity() > 50) {
    //      System.err.println("SAMPLES: " + nSamples);
    //    }
    int PROGRESS_STEPS = 100;
    if (progressUpdater != null && pPart == 0) {
      progressUpdater.initProgress((PROGRESS_STEPS - 1) * pParts);
    }
    long sampleProgressUpdateStep = nSamples / PROGRESS_STEPS;
    long nextProgressUpdate = sampleProgressUpdateStep;
    List<FlameRenderThread> threads = new ArrayList<FlameRenderThread>();
    int nThreads = pFlames.size();
    if (nThreads <= 1) {
      FlameRenderThread t = new FlameRenderThread(this, pFlames.get(0), nSamples / (long) nThreads);
      t.run();
    }
    else {
      for (int i = 0; i < nThreads; i++) {
        FlameRenderThread t = new FlameRenderThread(this, pFlames.get(i), nSamples / (long) nThreads);
        threads.add(t);
        new Thread(t).start();
      }
      boolean done = false;
      while (!done) {
        try {
          Thread.sleep(1);
        }
        catch (InterruptedException e) {
          e.printStackTrace();
        }
        done = true;
        long currSamples = 0;
        for (FlameRenderThread t : threads) {
          if (!t.isFinished()) {
            done = false;
          }
          currSamples += t.getCurrSample();
        }
        if (currSamples >= nextProgressUpdate) {
          if (progressUpdater != null) {
            int currProgress = (int) ((currSamples * PROGRESS_STEPS) / nSamples);
            progressUpdater.updateProgress(currProgress + pPart * PROGRESS_STEPS);
            nextProgressUpdate = (currProgress + 1) * sampleProgressUpdateStep;
          }
        }
      }
    }
  }

  private void initView() {
    double pixelsPerUnit = flame.getPixelsPerUnit() * flame.getCamZoom();
    double corner_x = flame.getCentreX() - (double) imageWidth / pixelsPerUnit / 2.0;
    double corner_y = flame.getCentreY() - (double) imageHeight / pixelsPerUnit / 2.0;
    double t0 = borderWidth / pixelsPerUnit;
    double t1 = borderWidth / pixelsPerUnit;
    double t2 = (2 * maxBorderWidth - borderWidth) / pixelsPerUnit;
    double t3 = (2 * maxBorderWidth - borderWidth) / pixelsPerUnit;

    double camX0 = corner_x - t0;
    double camY0 = corner_y - t1;
    double camX1 = corner_x + (double) imageWidth / pixelsPerUnit + t2;
    double camY1 = corner_y + (double) imageHeight / pixelsPerUnit + t3;

    camW = camX1 - camX0;
    double Xsize, Ysize;
    if (Math.abs(camW) > 0.01)
      Xsize = 1.0 / camW;
    else
      Xsize = 1.0;
    camH = camY1 - camY0;
    if (Math.abs(camH) > 0.01)
      Ysize = 1.0 / camH;
    else
      Ysize = 1;
    bws = (rasterWidth - 0.5) * Xsize;
    bhs = (rasterHeight - 0.5) * Ysize;

    cosa = Math.cos(-Math.PI * (flame.getCamRoll()) / 180.0);
    sina = Math.sin(-Math.PI * (flame.getCamRoll()) / 180.0);
    rcX = flame.getCentreX() * (1 - cosa) - flame.getCentreY() * sina - camX0;
    rcY = flame.getCentreY() * (1 - cosa) + flame.getCentreX() * sina - camY0;
  }

  private void createColorMap() {
    colorMap = flame.getPalette().createRenderPalette(flame.getWhiteLevel());
    paletteIdxScl = colorMap.length - 1;
  }

  public void setRandomNumberGenerator(RandomNumberGenerator random) {
    this.random = random;
  }

  public void setProgressUpdater(ProgressUpdater pProgressUpdater) {
    progressUpdater = pProgressUpdater;
  }

  public RasterPoint getRasterPoint(double pX, double pY) {
    int xIdx = (int) (bws * pX + 0.5);
    int yIdx = (int) (bhs * pY + 0.5);
    if (xIdx >= 0 && xIdx < rasterWidth && yIdx >= 0 && yIdx < rasterHeight) {
      return raster[yIdx][xIdx];
    }
    else {
      return null;
    }
  }

  public RandomNumberGenerator getRandomNumberGenerator() {
    return random;
  }

  protected FlameTransformationContext getFlameTransformationContext() {
    return flameTransformationContext;
  }

  // For quick benchmarking, should be removed later, can't get TPTP get to run on the whole app
  public static void main(String args[]) {
    try {
      Prefs prefs = new Prefs();
      prefs.loadFromFile();
      List<Flame> flames = new Flam3Reader().readFlames("C:\\TMP\\wf\\Apophysis\\benchmark1.flame");
      Flame flame = flames.get(0);
      FlameRenderer renderer = new FlameRenderer(flame, prefs);

      RenderInfo info = new RenderInfo(1920, 1080);

      double wScl = (double) info.getImageWidth() / (double) flame.getWidth();
      double hScl = (double) info.getImageHeight() / (double) flame.getHeight();
      flame.setPixelsPerUnit((wScl + hScl) * 0.5 * flame.getPixelsPerUnit());
      flame.setWidth(info.getImageWidth());
      flame.setHeight(info.getImageHeight());
      flame.setSampleDensity(100);
      flame.setSpatialOversample(1);
      flame.setColorOversample(1);
      flame.setSpatialFilterRadius(0);
      prefs.setTinaRenderThreads(1);
      long t0 = Calendar.getInstance().getTimeInMillis();
      RenderedFlame renderRes = renderer.renderFlame(info);
      long t1 = Calendar.getInstance().getTimeInMillis();
      System.err.println("RENDER TIME: " + ((double) (t1 - t0) / 1000.0) + "s");
      new ImageWriter().saveImage(renderRes.getImage(), "C:\\TMP\\wf\\Apophysis\\benchmark1.png");
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
  }

  public void setRenderScale(int pRenderScale) {
    renderScale = pRenderScale;
  }

  public RenderColor[] getColorMap() {
    return colorMap;
  }

}
