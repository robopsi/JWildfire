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
package org.jwildfire.create.tina.variation;

import org.jwildfire.base.Tools;
import org.jwildfire.create.tina.base.XForm;
import org.jwildfire.create.tina.base.XYZPoint;
import org.jwildfire.create.tina.palette.RenderColor;
import org.jwildfire.image.Pixel;
import org.jwildfire.image.SimpleHDRImage;
import org.jwildfire.image.SimpleImage;
import org.jwildfire.image.WFImage;

public class PostColorMapWFFunc extends VariationFunc {

  private static final String PARAM_SCALEX = "scale_x";
  private static final String PARAM_SCALEY = "scale_y";
  private static final String PARAM_SCALEZ = "scale_z";
  private static final String PARAM_OFFSETX = "offset_x";
  private static final String PARAM_OFFSETY = "offset_y";
  private static final String PARAM_OFFSETZ = "offset_z";
  private static final String PARAM_TILEX = "tile_x";
  private static final String PARAM_TILEY = "tile_y";

  private static final String RESSOURCE_IMAGE_FILENAME = "image_filename";

  private static final String[] paramNames = { PARAM_SCALEX, PARAM_SCALEY, PARAM_SCALEZ, PARAM_OFFSETX, PARAM_OFFSETY, PARAM_OFFSETZ, PARAM_TILEX, PARAM_TILEY };
  private static final String[] ressourceNames = { RESSOURCE_IMAGE_FILENAME };

  private double scaleX = 1.0;
  private double scaleY = 1.0;
  private double scaleZ = 0.0;
  private double offsetX = 0.0;
  private double offsetY = 0.0;
  private double offsetZ = 0.0;
  private int tileX = 1;
  private int tileY = 1;
  private String imageFilename = null;

  // derived params
  private int imgWidth, imgHeight;
  private Pixel toolPixel = new Pixel();
  private float[] rgbArray = new float[3];

  @Override
  public void transform(FlameTransformationContext pContext, XForm pXForm, XYZPoint pAffineTP, XYZPoint pVarTP, double pAmount) {

    //    if (pContext != null)
    //      return;
    double x = (pVarTP.x - offsetX + 1.0) / scaleX * 0.5 * (double) (imgWidth - 1);
    double y = (pVarTP.y - offsetY + 1.0) / scaleY * 0.5 * (double) (imgHeight - 1);
    int ix = Tools.FTOI(x);
    int iy = Tools.FTOI(y);
    if (this.tileX == 1) {
      while (ix < 0) {
        ix += imgWidth;
      }
      while (ix >= imgWidth) {
        ix -= imgWidth;
      }
    }
    if (this.tileY == 1) {
      while (iy < 0) {
        iy += imgHeight;
      }
      while (iy >= imgHeight) {
        iy -= imgHeight;
      }
    }
    if (ix >= 0 && ix < imgWidth && iy >= 0 && iy < imgHeight) {
      if (colorMap instanceof SimpleImage) {
        toolPixel.setARGBValue(((SimpleImage) colorMap).getARGBValue(
            ix, iy));
        pVarTP.rgbColor = true;
        pVarTP.redColor = toolPixel.r;
        pVarTP.greenColor = toolPixel.g;
        pVarTP.blueColor = toolPixel.b;
      }
      else {
        ((SimpleHDRImage) colorMap).getRGBValues(rgbArray, ix, iy);
        pVarTP.rgbColor = true;
        pVarTP.redColor = rgbArray[0];
        pVarTP.greenColor = rgbArray[0];
        pVarTP.blueColor = rgbArray[0];
      }
    }
    else {
      pVarTP.rgbColor = true;
      pVarTP.redColor = 0;
      pVarTP.greenColor = 0;
      pVarTP.blueColor = 0;
    }
    double dz = this.offsetZ;
    if (Math.abs(scaleZ) > Tools.EPSILON) {
      double intensity = (0.299 * pVarTP.redColor + 0.588 * pVarTP.greenColor + 0.113 * pVarTP.blueColor) / 255.0;
      dz += scaleZ * intensity;
    }
    pVarTP.z += dz;
    pVarTP.color = getColorIdx(pVarTP.redColor, pVarTP.greenColor, pVarTP.blueColor);
  }

  @Override
  public String[] getParameterNames() {
    return paramNames;
  }

  @Override
  public Object[] getParameterValues() {
    return new Object[] { scaleX, scaleY, scaleZ, offsetX, offsetY, offsetZ, tileX, tileY };
  }

  @Override
  public void setParameter(String pName, double pValue) {
    if (PARAM_SCALEX.equalsIgnoreCase(pName))
      scaleX = pValue;
    else if (PARAM_SCALEY.equalsIgnoreCase(pName))
      scaleY = pValue;
    else if (PARAM_SCALEZ.equalsIgnoreCase(pName))
      scaleZ = pValue;
    else if (PARAM_OFFSETX.equalsIgnoreCase(pName))
      offsetX = pValue;
    else if (PARAM_OFFSETY.equalsIgnoreCase(pName))
      offsetY = pValue;
    else if (PARAM_OFFSETZ.equalsIgnoreCase(pName))
      offsetZ = pValue;
    else if (PARAM_TILEX.equalsIgnoreCase(pName))
      tileX = Tools.FTOI(pValue);
    else if (PARAM_TILEY.equalsIgnoreCase(pName))
      tileY = Tools.FTOI(pValue);
    else
      throw new IllegalArgumentException(pName);
  }

  @Override
  public String getName() {
    return "post_colormap_wf";
  }

  @Override
  public int getPriority() {
    return 1;
  }

  private WFImage colorMap;
  private RenderColor[] renderColors;

  private double getColorIdx(double pR, double pG, double pB) {
    int nearestIdx = 0;
    RenderColor color = renderColors[0];
    double dr, dg, db;
    dr = (color.red - pR);
    dg = (color.green - pG);
    db = (color.blue - pB);
    double nearestDist = Math.sqrt(dr * dr + dg * dg + db * db);
    for (int i = 1; i < renderColors.length; i++) {
      color = renderColors[i];
      dr = (color.red - pR);
      dg = (color.green - pG);
      db = (color.blue - pB);
      double dist = Math.sqrt(dr * dr + dg * dg + db * db);
      if (dist < nearestDist) {
        nearestDist = dist;
        nearestIdx = i;
      }
    }
    return (double) nearestIdx / (double) (renderColors.length - 1);
  }

  @Override
  public void init(FlameTransformationContext pContext, XForm pXForm) {
    colorMap = null;
    renderColors = pContext.getFlameRenderer().getColorMap();
    if (imageFilename != null && imageFilename.length() > 0) {
      try {
        colorMap = RessourceManager.getImage(imageFilename);
      }
      catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (colorMap == null) {
      colorMap = new SimpleImage(320, 256);
    }
    imgWidth = colorMap.getImageWidth();
    imgHeight = colorMap.getImageHeight();
  }

  @Override
  public String[] getRessourceNames() {
    return ressourceNames;
  }

  @Override
  public byte[][] getRessourceValues() {
    return new byte[][] { (imageFilename != null ? imageFilename.getBytes() : null) };
  }

  @Override
  public void setRessource(String pName, byte[] pValue) {
    if (RESSOURCE_IMAGE_FILENAME.equalsIgnoreCase(pName)) {
      imageFilename = pValue != null ? new String(pValue) : "";
      colorMap = null;
    }
    else
      throw new IllegalArgumentException(pName);
  }

}