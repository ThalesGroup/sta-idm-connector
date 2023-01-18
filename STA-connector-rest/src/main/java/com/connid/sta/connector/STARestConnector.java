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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateDeltaOp;
import org.identityconnectors.framework.spi.operations.UpdateOp;
import org.json.JSONArray;
import org.json.JSONObject;

@ConnectorClass(displayNameKey = "REST based connector for SafeNet Trusted Access", configurationClass = STARestConfiguration.class)
public class STARestConnector
    implements Connector, PoolableConnector, TestOp, SchemaOp, CreateOp, DeleteOp, UpdateOp, UpdateDeltaOp, SyncOp, SearchOp<STAFilter> {
  private static final Log LOGGER = Log.getLog(STARestConnector.class);
  private static final String AUTHORIZED = "authorized";

  private STARestConfiguration configuration;

  @Override
  public Configuration getConfiguration() {
    return configuration;
  }

  @Override
  public void init(Configuration configuration) {
    LOGGER.info("Initializing STA IdM connector");
    this.configuration = (STARestConfiguration) configuration;
  }

  @Override
  public void dispose() {
    LOGGER.info("Dispose");
    configuration = null;
  }

  @Override
  // Method for testing the initial validation of the API key
  public void test() {
    HttpGet testRequest = new HttpGet(configuration.getAPIBaseURL() + AUTHORIZED);
    CloseableHttpResponse testResponse = getSTAConnectorUtil().execute(testRequest);
    getSTAConnectorUtil().processResponse(testResponse);
  }

  @Override
  // necessary to override checkAlive()
  public void checkAlive() {
    // TODO quicker test
  }

  public UsersUtil getUsersUtil() {
    return new UsersUtil(configuration);
  }

  public GroupsUtil getGroupsUtil() {
    return new GroupsUtil(configuration);
  }

  public STAConnectorUtil getSTAConnectorUtil() {
    return new STAConnectorUtil(configuration);
  }

  @Override
  // To build and return the schema that is supported by STA connector
  public Schema schema() {
    SchemaBuilder schemaBuilder = new SchemaBuilder(STARestConnector.class);

    getUsersUtil().buildAccountObjectClass(schemaBuilder);
    getGroupsUtil().buildGroupObjectClass(schemaBuilder);

    return schemaBuilder.build();
  }

  // Returns native query for each filter (cannot be null)
  public FilterTranslator<STAFilter> createFilterTranslator(ObjectClass objectClass, OperationOptions operationOptions) {
    return new STAFilterTranslator();
  }

  // Executes the search operation by using the query that is returned by createFilterTranslator()
  public void executeQuery(ObjectClass objectClass, STAFilter query, ResultsHandler handler, OperationOptions options) {
    if (objectClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
      LOGGER.info("ExecuteQuery on {0},\nQuery: {1},\nOptions: {2}", objectClass, query, options);
      getUsersUtil().executeQueryForUsers(query, handler);
    } else if (objectClass.is(ObjectClass.GROUP_NAME)) { //  __GROUP__
      LOGGER.info("ExecuteQuery on {0},\nQuery: {1},\nOptions: {2}", objectClass, query, options);
      getGroupsUtil().executeQueryForGroups(query, handler);
    } else { // not found
      LOGGER.error("The value of the ObjectClass parameter ({0}) is unsupported.", objectClass);
      throw new UnsupportedOperationException("The value of the ObjectClass parameter " + objectClass + " is unsupported.");
    }
  }

  // Calls the appropriate function to create user/group in STA
  @Override
  public Uid create(ObjectClass objectClass, Set<Attribute> attributes, OperationOptions operationOptions) {
    if (objectClass.is(ObjectClass.ACCOUNT_NAME))   //  __ACCOUNT__
    {
      return getUsersUtil().createOrUpdateUser(null, attributes);
    } else if (objectClass.is(ObjectClass.GROUP_NAME))   //  __GROUP__
    {
      return getGroupsUtil().createOrUpdateGroup(null, attributes);
    } else {   // not found
      LOGGER.error("The value of the ObjectClass parameter ({0}) is unsupported.", objectClass);
      throw new UnsupportedOperationException("The value of the ObjectClass parameter " + objectClass + " is unsupported.");
    }
  }

  // Calls the appropriate function to update user/group in STA
  @Override
  public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> attributes, OperationOptions operationOptions) {
    if (objectClass.is(ObjectClass.ACCOUNT_NAME)) // __ACCOUNT__
    {
      return getUsersUtil().createOrUpdateUser(uid, attributes);
    } else if (objectClass.is(ObjectClass.GROUP_NAME)) // __GROUP__
    {
      return getGroupsUtil().createOrUpdateGroup(uid, attributes);
    } else {// not found
      LOGGER.error("The value of the ObjectClass parameter ({0}) is unsupported.", objectClass);
      throw new UnsupportedOperationException("The value of the ObjectClass parameter " + objectClass + " is unsupported.");
    }
  }

  // Newer version of update function: has attributeDelta, which contains changes for each attribute.
  @Override
  public Set<AttributeDelta> updateDelta(ObjectClass objectClass, Uid uid, Set<AttributeDelta> attributesDelta, OperationOptions operationOptions) {
    if (objectClass == null) {
      LOGGER.error("Parameter of type ObjectClass is not provided.");
      throw new InvalidAttributeValueException("Parameter of type ObjectClass is not provided.");
    }

    if (attributesDelta == null) {
      LOGGER.error("Parameter of type Set<AttributeDelta> not provided.");
      throw new InvalidAttributeValueException("Parameter of type Set<AttributeDelta> not provided.");
    }

    if (operationOptions == null) {
      LOGGER.error("Parameter of type OperationOptions not provided.");
      throw new InvalidAttributeValueException("Parameter of type OperationOptions not provided.");
    }

    LOGGER.info("UpdateDelta with ObjectClass: {0} , uid: {1} , attributesDelta: {2} , Operationoptions: {3}  ", objectClass, uid, attributesDelta,
        operationOptions);

    Set<Attribute> attributesToReplace = new HashSet<>();
    Set<AttributeDelta> attributesDeltaMultivalue = new HashSet<>();

    for (AttributeDelta attributeDelta : attributesDelta) {
      List<Object> valuesToReplace = attributeDelta.getValuesToReplace();
      if (valuesToReplace != null) {
        attributesToReplace.add(AttributeBuilder.build(attributeDelta.getName(), valuesToReplace));
      } else {
        attributesDeltaMultivalue.add(attributeDelta);
      }
    }

    if (objectClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
      if (!attributesToReplace.isEmpty()) {
        getUsersUtil().createOrUpdateUser(uid, attributesToReplace);
      }
    } else if (objectClass.is(ObjectClass.GROUP_NAME)) { // __GROUP__
      if (!attributesToReplace.isEmpty()) {
        getGroupsUtil().createOrUpdateGroup(uid, attributesToReplace);
      }

      if (!attributesDeltaMultivalue.isEmpty()) {
        try {
          getGroupsUtil().updateGroupMembership(uid, attributesDeltaMultivalue, operationOptions);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    } else {
      LOGGER.error("The value of the ObjectClass parameter ({0}) is unsupported.", objectClass);
      throw new UnsupportedOperationException("The value of the ObjectClass parameter " + objectClass + " is unsupported.");
    }
    return null;
  }

  // Calls the appropriate function to delete user/group in STA
  @Override
  public void delete(ObjectClass objectClass, Uid uid, OperationOptions operationOptions) {

    if (objectClass.is(ObjectClass.ACCOUNT_NAME)) { // __ACCOUNT__
      getUsersUtil().deleteUser(uid);
    } else if (objectClass.is(ObjectClass.GROUP_NAME)) {  // __GROUP__
      getGroupsUtil().deleteGroup(uid);
    } else {
      LOGGER.error("The value of the ObjectClass parameter ({0}) is unsupported.", objectClass);
      throw new UnsupportedOperationException("The value of the ObjectClass parameter " + objectClass + " is unsupported.");
    }
  }

  // Implements the live synchronization functionality in the connector
  @Override
  public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, OperationOptions options) {
    if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
      if (token == null) {
        token = getLatestSyncToken(objectClass);
      }

      LOGGER.info("STARTING SYNC for {0} with Sync Token = {1}", ObjectClass.ACCOUNT_NAME, token);
      /*
            Call SCIM Api to get
            1. list of Users
            2. total count of users
            3. users per page
            */

      int processedUsers = 0;
      HttpRequestBase scimRequest = new HttpGet(getUsersUtil().getUserBaseURL(true));
      CloseableHttpResponse response = getSTAConnectorUtil().execute(scimRequest);

      String result;
      try {
        result = EntityUtils.toString(response.getEntity());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      getSTAConnectorUtil().closeResponse(response);
      JSONObject responsejson = new JSONObject(result);

      try {
        findUsersToSync(responsejson.getJSONArray("Resources"), token, handler, processedUsers);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }

      int totalUsers = (int) responsejson.get("totalResults");
      int itemsPerPage = (int) responsejson.get("itemsPerPage");
      int startIndex = (int) responsejson.get("startIndex");
      startIndex++;
      int totalPages = totalUsers / itemsPerPage + 1;

      while (startIndex <= totalPages) {
        HttpRequestBase nextScimRequest = new HttpGet(String.format("%s?startIndex=%d", getUsersUtil().getUserBaseURL(true), startIndex));
        CloseableHttpResponse nextResponse = getSTAConnectorUtil().execute(nextScimRequest);

        String nextResult;
        try {
          nextResult = EntityUtils.toString(nextResponse.getEntity());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        getSTAConnectorUtil().closeResponse(nextResponse);
        JSONObject nextResponseJson = new JSONObject(nextResult);
        try {
          findUsersToSync(nextResponseJson.getJSONArray("Resources"), token, handler, processedUsers);
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        }
        startIndex++;
      }
    }
  }

  @Override
  // Returns the current time stamp as of the latest syncToken
  public SyncToken getLatestSyncToken(ObjectClass objectClass) {
    return new SyncToken(System.currentTimeMillis());
  }

  // Utility function for sync to find the user records that have changed since the last sync
  public void findUsersToSync(JSONArray users, SyncToken token, SyncResultsHandler handler, int processedUsers) throws UnsupportedEncodingException {
    for (int i = 0; i < users.length(); i++) {
      String modifiedTIme = (String) users.getJSONObject(i).getJSONObject("meta").get("lastModified");
      long usersModifiedTimeInMillis = Instant.parse(modifiedTIme).toEpochMilli();

      try {
        if (usersModifiedTimeInMillis > (Long) token.getValue()) {
          processedUsers++;
          HttpGet request = new HttpGet(String.format("%s/%s?isUid=false", getUsersUtil().getUserBaseURL(false),
              getSTAConnectorUtil().urlEncoder((String) users.getJSONObject(i).get("userName"))));
          request.setHeader("Object-Id-Format", "hex");
          JSONObject userObject = getSTAConnectorUtil().callRequest(request, true);
          ConnectorObject connectorObject = getUsersUtil().convertUserToConnectorObject(userObject);

          SyncDeltaBuilder builder = new SyncDeltaBuilder();
          builder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
          builder.setObjectClass(ObjectClass.ACCOUNT);
          SyncToken finalToken = getLatestSyncToken(ObjectClass.ACCOUNT);
          builder.setToken(finalToken);
          builder.setObject(connectorObject);
          handler.handle(builder.build());
        }
      } catch (IOException e) {
        throw new ConnectorIOException(e.getMessage(), e);
      }
    }
  }
}

