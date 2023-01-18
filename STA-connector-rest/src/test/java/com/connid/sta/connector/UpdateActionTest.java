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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeDeltaBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Test;

public class UpdateActionTest extends BasicConfigurationForTests {

  @Test(expected = UnknownUidException.class)
  public void updateUserWithUnknownUidTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());

    Set<AttributeDelta> account = new HashSet<>();
    account.add(AttributeDeltaBuilder.build("lastName", (Object[]) new String[] {"TestName"}));
    ObjectClass objectClass = ObjectClass.ACCOUNT;

    staRestConnector.updateDelta(objectClass, new Uid("00000000000000"), account, options);
  }

  @Test(expected = ConnectorIOException.class)
  public void updateGroupWithUnknownUidTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());

    Set<AttributeDelta> group = new HashSet<>();
    group.add(AttributeDeltaBuilder.build("description", (Object[]) new String[] {"This is a test description."}));
    ObjectClass objectClass = ObjectClass.GROUP;

    staRestConnector.updateDelta(objectClass, new Uid("16777217"), group, options);
  }

  @Test()
  public void updateUserTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

    Set<Attribute> account = new HashSet<>();
    account.add(AttributeBuilder.build("userName", "testUnit"));
    account.add(AttributeBuilder.build("__NAME__", "testUnit"));
    account.add(AttributeBuilder.build("firstName", "Test"));
    account.add(AttributeBuilder.build("lastName", "Unit"));
    account.add(AttributeBuilder.build("email", "test.Unit@example.com"));
    ObjectClass objectClass = ObjectClass.ACCOUNT;

    Uid testUid = staRestConnector.create(objectClass, account, options);

    Set<AttributeDelta> updateAccount = new HashSet<>();
    updateAccount.add(AttributeDeltaBuilder.build("firstName", (Object[]) new String[] {"Test update"}));
    updateAccount.add(AttributeDeltaBuilder.build("lastName", (Object[]) new String[] {"Unit update"}));

    staRestConnector.updateDelta(objectClass, testUid, updateAccount, options);

    staRestConnector.delete(objectClass, testUid, options);
    //Assert.assertNotNull(testUid);
  }

  @Test()
  public void updateGroupTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

    Set<Attribute> group = new HashSet<>();
    group.add(AttributeBuilder.build("name", "Unit group"));
    group.add(AttributeBuilder.build("__NAME__", "Unit group"));
    group.add(AttributeBuilder.build("description", "This is description."));
    ObjectClass objectClass = ObjectClass.GROUP;

    Uid testUid = staRestConnector.create(objectClass, group, options);

    Set<AttributeDelta> updateGroup = new HashSet<>();
    updateGroup.add(AttributeDeltaBuilder.build("description", (Object[]) new String[] {"This is a test description."}));

    staRestConnector.updateDelta(objectClass, testUid, updateGroup, options);

    staRestConnector.delete(objectClass, testUid, options);
    //Assert.assertNotNull(testUid);
  }
}
