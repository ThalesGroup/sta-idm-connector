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
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.junit.Test;

public class CreateActionTest extends BasicConfigurationForTests {

  @Test(expected = UnsupportedOperationException.class)
  public void createWithUnsupportedObjectClassTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    Set<Attribute> account = new HashSet<>();
    account.add(AttributeBuilder.build("username", "testUser"));
    ObjectClass objectClass = new ObjectClass("UnsupportedObjectClass");

    staRestConnector.create(objectClass, account, options);
  }

  @Test(expected = InvalidAttributeValueException.class)
  public void createUserWithEmptyAttributesTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());

    Set<Attribute> account = new HashSet<>();
    ObjectClass objectClass = ObjectClass.ACCOUNT;

    staRestConnector.create(objectClass, account, options);
  }

  @Test(expected = InvalidAttributeValueException.class)
  public void createGroupWithEmptyAttributesTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());

    Set<Attribute> account = new HashSet<>();
    ObjectClass objectClass = ObjectClass.GROUP;

    staRestConnector.create(objectClass, account, options);
  }

  @Test(expected = InvalidAttributeValueException.class)
  public void createUserWithoutMandatoryAttributesTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());

    Set<Attribute> account = new HashSet<>();
    account.add(AttributeBuilder.build("userName", "Noida"));
    ObjectClass objectClass = ObjectClass.ACCOUNT;

    staRestConnector.create(objectClass, account, options);
  }

  @Test(expected = InvalidAttributeValueException.class)
  public void createGroupWithoutMandatoryAttributesTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());

    Set<Attribute> group = new HashSet<>();
    group.add(AttributeBuilder.build("description", "Group description"));
    ObjectClass objectClass = ObjectClass.GROUP;

    staRestConnector.create(objectClass, group, options);
  }

  @Test(expected = AlreadyExistsException.class)
  public void createUserWithExistingUsernameTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

    Set<Attribute> account = new HashSet<>();
    account.add(AttributeBuilder.build("userName", "testUser"));
    account.add(AttributeBuilder.build("__NAME__", "testUser"));
    account.add(AttributeBuilder.build("firstName", "Test"));
    account.add(AttributeBuilder.build("lastName", "User"));
    account.add(AttributeBuilder.build("email", "test.user@example.com"));
    ObjectClass objectClass = ObjectClass.ACCOUNT;

    Uid testUid = staRestConnector.create(objectClass, account, options);

    try {
      staRestConnector.create(objectClass, account, options);
    } finally {
      staRestConnector.delete(objectClass, testUid, options);
    }
  }

  @Test(expected = ConnectorIOException.class) // already exists
  public void createGroupWithExistingNameTest() {

    staRestConfiguration = getConfiguration();
    staRestConnector.init(staRestConfiguration);
    OperationOptions options = new OperationOptions(new HashMap<>());
    operationOptions.put("ALLOW_PARTIAL_ATTRIBUTE_VALUES", true);

    Set<Attribute> group = new HashSet<>();
    group.add(AttributeBuilder.build("name", "testgroup"));
    group.add(AttributeBuilder.build("__NAME__", "testgroup"));
    ObjectClass objectClass = ObjectClass.GROUP;

    Uid testUid = staRestConnector.create(objectClass, group, options);

    try {
      staRestConnector.create(objectClass, group, options);
    } finally {
      staRestConnector.delete(objectClass, testUid, options);
    }
  }
}
