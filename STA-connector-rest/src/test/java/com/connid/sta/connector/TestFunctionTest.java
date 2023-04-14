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

import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.junit.Test;

public class TestFunctionTest extends BasicConfigurationForTests {
  @Test()
  public void testForCorrectCreds() {
    staRestConnector = new STARestConnector();
    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);

    staRestConnector.test();
  }

  @Test(expected = PermissionDeniedException.class)
  public void testForIncorrectCreds() {
    staRestConnector = new STARestConnector();
    staRestConfiguration = getConfiguration();
    staRestConfiguration.setApiKey(new GuardedString("wrong-api-key".toCharArray()));
    staRestConnector.init(staRestConfiguration);

    staRestConnector.test();
  }
}
