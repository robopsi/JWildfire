package org.jwildfire.create.tina.variation;

import org.jwildfire.create.tina.base.XForm;
import org.jwildfire.create.tina.base.XYZPoint;

public class PreSubFlameWFFunc extends SubFlameWFFunc {

  @Override
  public int getPriority() {
    return -1;
  }

  @Override
  public void transform(FlameTransformationContext pContext, XForm pXForm, XYZPoint pAffineTP, XYZPoint pVarTP, double pAmount) {
    super.transform(pContext, pXForm, pAffineTP, pVarTP, pAmount);
    pAffineTP.assign(pVarTP);
  }

  @Override
  public String getName() {
    return "pre_subflame_wf";
  }

}
