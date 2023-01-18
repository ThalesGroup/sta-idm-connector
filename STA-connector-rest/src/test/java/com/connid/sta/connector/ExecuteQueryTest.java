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

import java.util.ArrayList;
import java.util.HashMap;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.junit.Assert;
import org.junit.Test;

public class ExecuteQueryTest extends BasicConfigurationForTests {

  @Test()
  public void searchAllUsersTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

    ArrayList<ConnectorObject> searchResult;
    TestSearchResultsHandler testSearchResultsHandler = new TestSearchResultsHandler();

    staRestConnector.executeQuery(ObjectClass.ACCOUNT, null, testSearchResultsHandler, options);

    searchResult = testSearchResultsHandler.getResult();
    Assert.assertFalse(searchResult.isEmpty());
  }

  @Test()
  public void searchAllGroupsTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

    ArrayList<ConnectorObject> searchResult;
    TestSearchResultsHandler testSearchResultsHandler = new TestSearchResultsHandler();

    staRestConnector.executeQuery(ObjectClass.GROUP, null, testSearchResultsHandler, options);

    searchResult = testSearchResultsHandler.getResult();
    Assert.assertFalse(searchResult.isEmpty());
  }
}