/*
  JWildfire - an image and animation processor written in Java 
  Copyright (C) 1995-2015 Andreas Maschke

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
package org.jwildfire.create.tina.render.image;

import org.jwildfire.create.tina.render.GammaCorrectedRGBPoint;
import org.jwildfire.create.tina.render.GammaCorrectionFilter;
import org.jwildfire.create.tina.render.LogDensityFilter;
import org.jwildfire.create.tina.render.LogDensityPoint;
import org.jwildfire.image.SimpleImage;

public class RenderImageSimpleScaledThread extends AbstractImageRenderThread {
  private final LogDensityFilter logDensityFilter;
  private final GammaCorrectionFilter gammaCorrectionFilter;
  private final int startRow, endRow;
  private final LogDensityPoint logDensityPnt;
  private final GammaCorrectedRGBPoint rbgPoint;
  private final SimpleImage img;
  private final SimpleImage newImg;
  private final int renderScale;

  public RenderImageSimpleScaledThread(LogDensityFilter pLogDensityFilter, GammaCorrectionFilter pGammaCorrectionFilter, int pRenderScale, int pStartRow, int pEndRow, SimpleImage pImg, SimpleImage pNewImg) {
    logDensityFilter = pLogDensityFilter;
    gammaCorrectionFilter = pGammaCorrectionFilter;
    renderScale = pRenderScale;
    startRow = pStartRow;
    endRow = pEndRow;
    logDensityPnt = new LogDensityPoint();
    rbgPoint = new GammaCorrectedRGBPoint();
    img = pImg;
    newImg = pNewImg;
  }

  @Override
  public void run() {
    setDone(false);
    try {
      for (int i = startRow; i < endRow; i++) {
        for (int j = 0; j < img.getImageWidth(); j++) {
          logDensityFilter.transformPointSimple(logDensityPnt, j, i);
          gammaCorrectionFilter.transformPoint(logDensityPnt, rbgPoint, j, i);
          int x = j * renderScale;
          int y = i * renderScale;

          newImg.setARGB(x, y, rbgPoint.alpha, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
          newImg.setARGB(x + 1, y, rbgPoint.alpha, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
          newImg.setARGB(x, y + 1, rbgPoint.alpha, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
          newImg.setARGB(x + 1, y + 1, rbgPoint.alpha, rbgPoint.red, rbgPoint.green, rbgPoint.blue);
        }
      }
    }
    finally {
      setDone(true);
    }
  }

}
