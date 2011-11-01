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

import org.jwildfire.create.tina.base.XForm;
import org.jwildfire.create.tina.base.XYZPoint;

public class RectanglesFunc extends VariationFunc {
  private static final String PARAM_X = "x";
  private static final String PARAM_Y = "y";
  private static final String[] paramNames = { PARAM_X, PARAM_Y };

  private double x, y;

  @Override
  public void transform(TransformationContext pContext, XForm pXForm, XYZPoint pAffineTP, XYZPoint pVarTP, double pAmount) {
    final double EPSILON = 1.0e-10;
    if (Math.abs(x) < EPSILON) {
      pVarTP.x += pAmount * pAffineTP.x;
    }
    else {
      pVarTP.x += pAmount * ((2 * Math.floor(pAffineTP.x / x) + 1) * x - pAffineTP.x);
    }
    if (Math.abs(y) < EPSILON) {
      pVarTP.y += pAmount * pAffineTP.y;
    }
    else {
      pVarTP.y += pAmount * ((2 * Math.floor(pAffineTP.y / y) + 1) * y - pAffineTP.y);
    }
  }

  @Override
  public String[] getParameterNames() {
    return paramNames;
  }

  @Override
  public void setParameter(String pName, double pValue) {
    if (PARAM_X.equalsIgnoreCase(pName))
      x = pValue;
    else if (PARAM_Y.equalsIgnoreCase(pName))
      y = pValue;
    else
      throw new IllegalArgumentException(pName);
  }

  @Override
  public String getName() {
    return "rectangles";
  }
}