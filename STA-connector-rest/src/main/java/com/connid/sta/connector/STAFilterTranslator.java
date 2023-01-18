/**
 * Copyright © 2023 Thales Group
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.connid.sta.connector;

import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;

public class STAFilterTranslator extends AbstractFilterTranslator<STAFilter> {

  @Override
  protected STAFilter createEqualsExpression(EqualsFilter filter, boolean not) {
   /* if (not) {
      return null; // not supported
    }*/

    Attribute attribute = filter.getAttribute();

    if (Uid.NAME.equals(attribute.getName())) {
      if (attribute.getValue() != null && attribute.getValue().get(0) != null) {
        STAFilter staFilter = new STAFilter();
        staFilter.setByUid(String.valueOf(attribute.getValue().get(0)));
        return staFilter;
      }
    }
    return null; // not supported
  }
}
