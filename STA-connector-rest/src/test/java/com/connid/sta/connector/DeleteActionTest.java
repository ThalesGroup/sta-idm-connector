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
import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.FilterBuilder;
import org.junit.Test;
import org.testng.Assert;

public class DeleteActionTest extends BasicConfigurationForTests {

  @Test(expected = UnknownUidException.class)
  public void deleteUserTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

    Set<Attribute> account = new HashSet<>();
    account.add(AttributeBuilder.build("userName", "testUnit"));
    account.add(AttributeBuilder.build("__NAME__", "testUnit"));
    account.add(AttributeBuilder.build("firstName", "Test"));
    account.add(AttributeBuilder.build("lastName", "Unit"));
    account.add(AttributeBuilder.build("email", "test.unit@example.com"));
    ObjectClass objectClass = ObjectClass.ACCOUNT;

    Uid testUid = staRestConnector.create(objectClass, account, options);
    staRestConnector.delete(objectClass, testUid, options);

    ArrayList<ConnectorObject> resultsAccount;
    TestSearchResultsHandler testHandler = getTestSearchResultHandler();

    EqualsFilter filter = (EqualsFilter) FilterBuilder.equalTo(testUid);
    STAFilter staFilter = new STAFilterTranslator().createEqualsExpression(filter, false);

    staRestConnector.executeQuery(objectClass, staFilter, testHandler, options);
    resultsAccount = testHandler.getResult();

    for (ConnectorObject object : resultsAccount) {
      if (object.getUid().equals(testUid)) {
        Assert.fail();
        break;
      }
    }
  }

  @Test(expected = UnknownUidException.class)
  public void deleteGroupTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

    Set<Attribute> account = new HashSet<>();
    account.add(AttributeBuilder.build("name", "testdeleteGroup"));
    account.add(AttributeBuilder.build("__NAME__", "testdeleteGroup"));
    ObjectClass objectClass = ObjectClass.GROUP;

    Uid testUid = staRestConnector.create(objectClass, account, options);
    staRestConnector.delete(objectClass, testUid, options);

    ArrayList<ConnectorObject> resultsGroup;
    TestSearchResultsHandler testHandler = getTestSearchResultHandler();

    EqualsFilter filter = (EqualsFilter) FilterBuilder.equalTo(testUid);
    STAFilter staFilter = new STAFilterTranslator().createEqualsExpression(filter, false);

    staRestConnector.executeQuery(objectClass, staFilter, testHandler, options);
    resultsGroup = testHandler.getResult();

    for (ConnectorObject object : resultsGroup) {
      if (object.getUid().equals(testUid)) {
        Assert.fail();
        break;
      }
    }
  }
}
