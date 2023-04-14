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
import java.net.URI;
import java.time.Instant;
import java.util.Set;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONObject;

public class UsersUtil extends STAConnectorUtil {
  private static final String USER = "users";

  // STA user attribute names
  private static final String ATTR_UID = "id";
  private static final String ATTR_SCHEMA_VERSION_NUMBER = "schemaVersionNumber";
  private static final String ATTR_UNAME = "userName";        //Required
  private static final String ATTR_FIRST_NAME = "firstName";   //Required
  private static final String ATTR_LAST_NAME = "lastName";     //Required
  private static final String ATTR_MAIL = "email";            //Required
  private static final String ATTR_UPN = "userPrincipalName";
  private static final String ATTR_MOBILE_NUMBER = "mobileNumber";
  private static final String ATTR_ALIAS_1 = "alias1";
  private static final String ATTR_ALIAS_2 = "alias2";
  private static final String ATTR_ALIAS_3 = "alias3";
  private static final String ATTR_ALIAS_4 = "alias4";
  private static final String ATTR_CUSTOM_1 = "custom1";
  private static final String ATTR_CUSTOM_2 = "custom2";
  private static final String ATTR_CUSTOM_3 = "custom3";
  private static final String ATTR_ADDRESS = "address";
  private static final String ATTR_CITY = "city";
  private static final String ATTR_STATE = "state";
  private static final String ATTR_COUNTRY = "country";
  private static final String ATTR_POSTAL_CODE = "postalCode";
  private static final String ATTR_SYNCHRONIZED = "isSynchronized";
  private static final String ATTR_IMMUTABLE_ID = "immutableId";
  private static final String ATTR_EXTERNAL_ID = "externalId";
  private static final String ATTR_STATUS = "isActive";
  private static final String STATUS_ENABLED = "true";
  private static final String STATUS_BLOCKED = "false";


  public UsersUtil(STARestConfiguration configuration) {
    super(configuration);
  }

  private STARestConfiguration getConfiguration() {
    return this.configuration;
  }

  // Build the AccountObjectClass (object class for users) object for the schema
  public void buildAccountObjectClass(SchemaBuilder schemaBuilder) {
    ObjectClassInfoBuilder objectClassInfoBuilder = new ObjectClassInfoBuilder();
    objectClassInfoBuilder.setType(ObjectClass.ACCOUNT_NAME);

    // UID & NAME are defaults
        /*
        "id": "adef1bc23",
        "schemaVersionNumber": "1.0",
        "userName": "jsmith",
        "firstName": "Joe",
        "lastName": "Smith",
        "userPrincipalName": "jsmith@email.com",
        "email": "jsmith@email.com",
        "mobileNumber": "555-555-1234",
        "alias1": "joe",
        "alias2": "joe.smith",
        "alias3": "joe3",
        "alias4": "joe.smith4",
        "address": "123 Main Street",
        "city": "Springfield",
        "state": "Ohio",
        "country": "USA",
        "postalCode": "12345",
        "isSynchronized": false,
        "immutableId": "2ad445a9-db75-4c6e-b694-7b55a527ca85",
        "isActive": false,
        "externalId": "2ad445a9-db75-4c6e-b694-7b55a527ca84"
        */

    AttributeInfoBuilder attributeIdBuilder = new AttributeInfoBuilder(ATTR_UID);
    attributeIdBuilder.setRequired(true);
    attributeIdBuilder.setReturnedByDefault(true);
    attributeIdBuilder.setUpdateable(false);
    objectClassInfoBuilder.addAttributeInfo(attributeIdBuilder.build());

    AttributeInfoBuilder attributeSchemaversionBuilder = new AttributeInfoBuilder(ATTR_SCHEMA_VERSION_NUMBER);
    objectClassInfoBuilder.addAttributeInfo(attributeSchemaversionBuilder.build());

    AttributeInfoBuilder attributeUserNameBuilder = new AttributeInfoBuilder(ATTR_UNAME);
    attributeUserNameBuilder.setRequired(true);
    attributeUserNameBuilder.setReturnedByDefault(true);
    objectClassInfoBuilder.addAttributeInfo(attributeUserNameBuilder.build());

    AttributeInfoBuilder attributeFirstNameBuilder = new AttributeInfoBuilder(ATTR_FIRST_NAME);
    attributeFirstNameBuilder.setRequired(true);
    objectClassInfoBuilder.addAttributeInfo(attributeFirstNameBuilder.build());

    AttributeInfoBuilder attributeLastNameBuilder = new AttributeInfoBuilder(ATTR_LAST_NAME);
    attributeLastNameBuilder.setRequired(true);
    objectClassInfoBuilder.addAttributeInfo(attributeLastNameBuilder.build());

    AttributeInfoBuilder attributeUpnBuilder = new AttributeInfoBuilder(ATTR_UPN);
    objectClassInfoBuilder.addAttributeInfo(attributeUpnBuilder.build());

    AttributeInfoBuilder attributeEmailBuilder = new AttributeInfoBuilder(ATTR_MAIL);
    attributeUserNameBuilder.setRequired(true);
    objectClassInfoBuilder.addAttributeInfo(attributeEmailBuilder.build());

    AttributeInfoBuilder attributeMobileNumberBuilder = new AttributeInfoBuilder(ATTR_MOBILE_NUMBER);
    objectClassInfoBuilder.addAttributeInfo(attributeMobileNumberBuilder.build());

    AttributeInfoBuilder attributeAlias1Builder = new AttributeInfoBuilder(ATTR_ALIAS_1);
    objectClassInfoBuilder.addAttributeInfo(attributeAlias1Builder.build());

    AttributeInfoBuilder attributeAlias2Builder = new AttributeInfoBuilder(ATTR_ALIAS_2);
    objectClassInfoBuilder.addAttributeInfo(attributeAlias2Builder.build());

    AttributeInfoBuilder attributeAlias3Builder = new AttributeInfoBuilder(ATTR_ALIAS_3);
    objectClassInfoBuilder.addAttributeInfo(attributeAlias3Builder.build());

    AttributeInfoBuilder attributeAlias4Builder = new AttributeInfoBuilder(ATTR_ALIAS_4);
    objectClassInfoBuilder.addAttributeInfo(attributeAlias4Builder.build());

    AttributeInfoBuilder attributeAddressBuilder = new AttributeInfoBuilder(ATTR_ADDRESS);
    objectClassInfoBuilder.addAttributeInfo(attributeAddressBuilder.build());

    AttributeInfoBuilder attributeCityBuilder = new AttributeInfoBuilder(ATTR_CITY);
    objectClassInfoBuilder.addAttributeInfo(attributeCityBuilder.build());

    AttributeInfoBuilder attributeStateBuilder = new AttributeInfoBuilder(ATTR_STATE);
    objectClassInfoBuilder.addAttributeInfo(attributeStateBuilder.build());

    AttributeInfoBuilder attributeCountryBuilder = new AttributeInfoBuilder(ATTR_COUNTRY);
    objectClassInfoBuilder.addAttributeInfo(attributeCountryBuilder.build());

    AttributeInfoBuilder attributePostalCodeBuilder = new AttributeInfoBuilder(ATTR_POSTAL_CODE);
    objectClassInfoBuilder.addAttributeInfo(attributePostalCodeBuilder.build());

    AttributeInfoBuilder attributeSynchronizedBuilder = new AttributeInfoBuilder(ATTR_SYNCHRONIZED);
    objectClassInfoBuilder.addAttributeInfo(attributeSynchronizedBuilder.build());

    AttributeInfoBuilder attributeImmutableBuilder = new AttributeInfoBuilder(ATTR_IMMUTABLE_ID);
    objectClassInfoBuilder.addAttributeInfo(attributeImmutableBuilder.build());

    AttributeInfoBuilder attributeStatusBuilder = new AttributeInfoBuilder(ATTR_STATUS);
    objectClassInfoBuilder.addAttributeInfo(attributeStatusBuilder.build());

    AttributeInfoBuilder attributeCustom1Builder = new AttributeInfoBuilder(ATTR_CUSTOM_1);
    objectClassInfoBuilder.addAttributeInfo(attributeCustom1Builder.build());

    AttributeInfoBuilder attributeCustom2Builder = new AttributeInfoBuilder(ATTR_CUSTOM_2);
    objectClassInfoBuilder.addAttributeInfo(attributeCustom2Builder.build());

    AttributeInfoBuilder attributeCusto31Builder = new AttributeInfoBuilder(ATTR_CUSTOM_3);
    objectClassInfoBuilder.addAttributeInfo(attributeCusto31Builder.build());

    AttributeInfoBuilder attributeExternalIdBuilder = new AttributeInfoBuilder(ATTR_EXTERNAL_ID);
    objectClassInfoBuilder.addAttributeInfo(attributeExternalIdBuilder.build());

    schemaBuilder.defineObjectClass(objectClassInfoBuilder.build());
  }

  // Execute search query for users
  public void executeQueryForUsers(STAFilter query, ResultsHandler handler) {
    try {
      // Find user by Uid (User's Primary Key)
      if (query != null && query.getByUid() != null) {
        getUser(query, handler);
      } else {
        getUsers(handler);
      }
    } catch (IOException e) {
      throw new ConnectorIOException(e.getMessage(), e);
    }
  }

  private void getUser(STAFilter query, ResultsHandler handler) throws IOException {
    HttpGet request = new HttpGet(String.format("%s/%s?isUid=true", getUserBaseURL(false), urlEncoder(query.getByUid())));
    request.setHeader(OBJECT_ID_FORMAT, HEX);
    JSONObject userObject = callRequest(request, JSONObject.class, true);
    ConnectorObject connectorObject = convertUserToConnectorObject(userObject);
    handler.handle(connectorObject);
  }

  private void getUsers(ResultsHandler handler) throws IOException {
    int pageSize = getConfiguration().getPageSize();
    int currentPage = 0;
    boolean finish = false;
    while (!finish) {
      String pagingQueryString = getPagingQuery(currentPage, pageSize);
      HttpGet request = new HttpGet(String.format("%s?%s", getUserBaseURL(false), pagingQueryString));
      request.setHeader(OBJECT_ID_FORMAT, HEX);
      finish = processSTAUsers(request, handler);
      currentPage++;
    }
  }

  // Creates or updates a user in STA based on the provided parameters
  public Uid createOrUpdateUser(Uid uid, Set<Attribute> attributes) {
    if (attributes == null || attributes.isEmpty()) {
      LOGGER.error("Request ignored, Empty attributes");
      throw new InvalidAttributeValueException("Request ignored, empty attributes");
    }
    JSONObject userObject = new JSONObject();

    boolean create = uid == null;

    String mail = getStringAttr(attributes, ATTR_MAIL);
    if (create && StringUtil.isBlank(mail)) {
      throw new InvalidAttributeValueException("Missing mandatory attribute: " + ATTR_MAIL);
    }

    String uName = getStringAttr(attributes, ATTR_UNAME);
    if (create && StringUtil.isBlank(uName)) {
      throw new InvalidAttributeValueException("Missing mandatory attribute: " + ATTR_UNAME);
    }

    String name = getStringAttr(attributes, Name.NAME);
    if (create && StringUtil.isBlank(name)) {
      throw new InvalidAttributeValueException("Missing mandatory attribute: " + Name.NAME);
    }
    if (name != null) {
      userObject.put(ATTR_UNAME, name);
    }

    Boolean enable = getAttr(attributes, OperationalAttributes.ENABLE_NAME, Boolean.class);

    if (enable != null) {
      userObject.put(ATTR_STATUS, enable ? STATUS_ENABLED : STATUS_BLOCKED);
    }

    putIfExists(attributes, ATTR_UNAME, userObject);
    putIfExists(attributes, ATTR_FIRST_NAME, userObject);
    putIfExists(attributes, ATTR_LAST_NAME, userObject);
    putIfExists(attributes, ATTR_MAIL, userObject);
    putIfExists(attributes, ATTR_UPN, userObject);
    putIfExists(attributes, ATTR_ADDRESS, userObject);
    putIfExists(attributes, ATTR_SCHEMA_VERSION_NUMBER, userObject);
    putIfExists(attributes, ATTR_MOBILE_NUMBER, userObject);
    putIfExists(attributes, ATTR_ALIAS_1, userObject);
    putIfExists(attributes, ATTR_ALIAS_2, userObject);
    putIfExists(attributes, ATTR_ALIAS_3, userObject);
    putIfExists(attributes, ATTR_ALIAS_4, userObject);
    putIfExists(attributes, ATTR_CUSTOM_1, userObject);
    putIfExists(attributes, ATTR_CUSTOM_2, userObject);
    putIfExists(attributes, ATTR_CUSTOM_3, userObject);
    putIfExists(attributes, ATTR_CITY, userObject);
    putIfExists(attributes, ATTR_COUNTRY, userObject);
    putIfExists(attributes, ATTR_POSTAL_CODE, userObject);
    putIfExists(attributes, ATTR_SYNCHRONIZED, userObject);
    putIfExists(attributes, ATTR_IMMUTABLE_ID, userObject);
    putIfExists(attributes, ATTR_STATE, userObject);
    putIfExists(attributes, ATTR_STATUS, userObject);
    putIfExists(attributes, ATTR_UID, userObject);
    putIfExists(attributes, ATTR_EXTERNAL_ID, userObject);

    try {
      HttpEntityEnclosingRequestBase request;
      if (create) {
        request = new HttpPost(getUserBaseURL(false));
        request.setHeader(OBJECT_ID_FORMAT, HEX);
      } else { // update
        request = new HttpPatch(String.format("%s/%s?isUid=true", getUserBaseURL(false), urlEncoder(uid.getUidValue())));
        request.setHeader("Object-Id-Name", "ObjectId");
        request.setHeader(OBJECT_ID_FORMAT, HEX);
      }

      JSONObject jsonResponse = callRequest(request, userObject);
      String newUid = jsonResponse.getString(ATTR_UID);
      if (newUid == null) {
        assert uid != null;
        LOGGER.error("User[uid = {0}] update failed", uid.getUidValue());
      }
      return new Uid(newUid);

    } catch (IOException e) {
      throw new ConnectorIOException(e.getMessage(), e);
    }
  }

  // Deletes a user in STA
  public void deleteUser(Uid uid) {
    try {
      LOGGER.info("Delete user with Uid: {0}", uid);
      HttpDelete request = new HttpDelete(String.format("%s/%s", getUserBaseURL(false), urlEncoder(uid.getUidValue())));
      request.setHeader("Object-Id-Name", "ObjectId");
      request.setHeader(OBJECT_ID_FORMAT, HEX);
      callRequest(request, JSONObject.class, false);
    } catch (IOException e) {
      throw new ConnectorIOException(e.getMessage(), e);
    }
  }

  // Converts the user object from STA to a ConnID object to store in midPoint
  public ConnectorObject convertUserToConnectorObject(JSONObject user) throws IOException {
    ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

    builder.setUid(new Uid(user.getString(ATTR_UID)));

    if (user.has(ATTR_UNAME)) {
      builder.setName(user.getString(ATTR_UNAME));
    }

    getIfExists(user, ATTR_SCHEMA_VERSION_NUMBER, builder);
    getIfExists(user, ATTR_UNAME, builder);
    getIfExists(user, ATTR_FIRST_NAME, builder);
    getIfExists(user, ATTR_LAST_NAME, builder);
    getIfExists(user, ATTR_UID, builder);
    getIfExists(user, ATTR_UPN, builder);
    getIfExists(user, ATTR_MAIL, builder);
    getIfExists(user, ATTR_MOBILE_NUMBER, builder);
    getIfExists(user, ATTR_ADDRESS, builder);
    getIfExists(user, ATTR_CITY, builder);
    getIfExists(user, ATTR_STATE, builder);
    getIfExists(user, ATTR_POSTAL_CODE, builder);
    getIfExists(user, ATTR_SYNCHRONIZED, builder);
    getIfExists(user, ATTR_STATUS, builder);
    getIfExists(user, ATTR_ALIAS_1, builder);
    getIfExists(user, ATTR_ALIAS_2, builder);
    getIfExists(user, ATTR_ALIAS_3, builder);
    getIfExists(user, ATTR_ALIAS_4, builder);
    getIfExists(user, ATTR_CUSTOM_1, builder);
    getIfExists(user, ATTR_CUSTOM_2, builder);
    getIfExists(user, ATTR_CUSTOM_3, builder);
    getIfExists(user, ATTR_EXTERNAL_ID, builder);
    getIfExists(user, ATTR_IMMUTABLE_ID, builder);

    return builder.build();
  }

  // Returns the base URLs for API calls that are related to users
  public String getUserBaseURL(Boolean scim) {
    if (scim) {
      URI restBaseUrl = URI.create(this.configuration.getAPIBaseURL());
      return HTTPS + restBaseUrl.getHost() + restBaseUrl.getPath().substring(7) + SCIM + USER;
    } else {
      return (getConfiguration().getAPIBaseURL() + USER);
    }
  }

  // Processes the users after retrieving them from STA
  private boolean processSTAUsers(HttpGet request, ResultsHandler handler) throws IOException {
    JSONArray users = callRequest(request, JSONArray.class, true);

    for (int i = 0; i < users.length(); i++) {
      if (i % 10 == 0) {
        LOGGER.info("ExecuteQuery: processing {0}. of {1} users", i, users.length());
      }

      JSONObject user = users.getJSONObject(i);
      ConnectorObject connectorObject = convertUserToConnectorObject(user);
      boolean finish = !handler.handle(connectorObject);
      if (finish) {
        return true;
      }
    }
    // last page exceed
    return getConfiguration().getPageSize() > users.length();
  }

  public void liveSyncForUsers(SyncToken token, SyncResultsHandler handler) {
    if (token == null) {
      //token = getLatestSyncToken(objectClass);
      token = new STARestConnector().getLatestSyncToken(ObjectClass.ACCOUNT);
    }

    LOGGER.info("STARTING SYNC for {0} with Sync Token = {1}", ObjectClass.ACCOUNT_NAME, token);
      /*
            Call SCIM Api to get
            1. list of Users
            2. total count of users
            3. users per page
            */

    int processedUsers = 0;
    HttpRequestBase scimRequest = new HttpGet(getUserBaseURL(true));
    CloseableHttpResponse response = execute(scimRequest);

    String result;
    try {
      result = EntityUtils.toString(response.getEntity());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    closeResponse(response);
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
      HttpRequestBase nextScimRequest = new HttpGet(String.format("%s?startIndex=%d", getUserBaseURL(true), startIndex));
      CloseableHttpResponse nextResponse = execute(nextScimRequest);

      String nextResult;
      try {
        nextResult = EntityUtils.toString(nextResponse.getEntity());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      closeResponse(nextResponse);
      JSONObject nextResponseJson = new JSONObject(nextResult);
      try {
        findUsersToSync(nextResponseJson.getJSONArray("Resources"), token, handler, processedUsers);
      } catch (UnsupportedEncodingException e) {
        throw new RuntimeException(e);
      }
      startIndex++;
    }
  }

  // Utility function for sync to find the user records that have changed since the last sync
  public void findUsersToSync(JSONArray users, SyncToken token, SyncResultsHandler handler, int processedUsers) throws UnsupportedEncodingException {
    for (int i = 0; i < users.length(); i++) {
      String modifiedTIme = (String) users.getJSONObject(i).getJSONObject("meta").get("lastModified");
      long usersModifiedTimeInMillis = Instant.parse(modifiedTIme).toEpochMilli();

      try {
        if (usersModifiedTimeInMillis > (Long) token.getValue()) {
          processedUsers++;
          HttpGet request = new HttpGet(String.format("%s/%s?isUid=false", getUserBaseURL(false), urlEncoder((String) users.getJSONObject(i).get("userName"))));
          request.setHeader("Object-Id-Format", "hex");
          JSONObject userObject = callRequest(request, JSONObject.class, true);
          ConnectorObject connectorObject = convertUserToConnectorObject(userObject);

          SyncDeltaBuilder builder = new SyncDeltaBuilder();
          builder.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
          builder.setObjectClass(ObjectClass.ACCOUNT);
          SyncToken finalToken = new STARestConnector().getLatestSyncToken(ObjectClass.ACCOUNT);
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
