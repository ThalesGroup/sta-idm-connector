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
import java.net.URI;
import java.util.List;
import java.util.Set;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.Uid;
import org.json.JSONArray;
import org.json.JSONObject;

public class GroupsUtil extends STAConnectorUtil {
  private static final String GROUPS = "groups";
  // STA group attribute names
  private static final String ATTR_UID = "id";
  private static final String ATTR_SCHEMA_VERSION_NUMBER = "schemaVersionNumber";
  private static final String ATTR_SYNCHRONIZED = "isSynchronized";
  private static final String ATTR_SCIM_GROUP_NAME = "displayName"; //from scim
  private static final String ATTR_NORMAL_GROUP_NAME = "name";
  private static final String ATTR_GROUP_DESCRIPTION = "description";
  private static final String ATTR_GROUP_MEMBERS = "members";
  private static final String VALUE = "value";

  public GroupsUtil(STARestConfiguration configuration) {
    super(configuration);
  }

  // Build the GroupObjectClass (Object class for Groups) object for schema
  public void buildGroupObjectClass(SchemaBuilder schemabuilder) {
    ObjectClassInfoBuilder objectClassInfoBuilder = new ObjectClassInfoBuilder();
    objectClassInfoBuilder.setType(ObjectClass.GROUP_NAME);

        /*
        "id": "16777217",
        "schemaVersionNumber": "1.0",
        "name": "Group 1",
        "description": "First group.",
        "isSynchronized": false
        */

    AttributeInfoBuilder attributeIdBuilder = new AttributeInfoBuilder(ATTR_UID);
    attributeIdBuilder.setUpdateable(false);
    objectClassInfoBuilder.addAttributeInfo(attributeIdBuilder.build());

    AttributeInfoBuilder attributeSchemaVersionBuilder = new AttributeInfoBuilder(ATTR_SCHEMA_VERSION_NUMBER);
    objectClassInfoBuilder.addAttributeInfo(attributeSchemaVersionBuilder.build());

    AttributeInfoBuilder attributeNameBuilder = new AttributeInfoBuilder(ATTR_SCIM_GROUP_NAME); //displayname
    objectClassInfoBuilder.addAttributeInfo(attributeNameBuilder.build());

    AttributeInfoBuilder attributeGNameBuilder = new AttributeInfoBuilder(ATTR_NORMAL_GROUP_NAME);
    objectClassInfoBuilder.addAttributeInfo(attributeGNameBuilder.build());

    AttributeInfoBuilder attributeMembershipBuilder = new AttributeInfoBuilder(ATTR_GROUP_MEMBERS);
    attributeMembershipBuilder.setRequired(false).setType(String.class).setCreateable(true).setUpdateable(true).setMultiValued(true)
        .setReturnedByDefault(false);
    objectClassInfoBuilder.addAttributeInfo(attributeMembershipBuilder.build());

    AttributeInfoBuilder attributeDescriptionBuilder = new AttributeInfoBuilder(ATTR_GROUP_DESCRIPTION);
    objectClassInfoBuilder.addAttributeInfo(attributeDescriptionBuilder.build());

    AttributeInfoBuilder attributeSynchronizedBuilder = new AttributeInfoBuilder(ATTR_SYNCHRONIZED);
    objectClassInfoBuilder.addAttributeInfo(attributeSynchronizedBuilder.build());

    schemabuilder.defineObjectClass(objectClassInfoBuilder.build());
  }

  // Calls the appropriate functions to add/remove members to/from a group
  public void updateGroupMembership(Uid uid, Set<AttributeDelta> attributesDelta, OperationOptions operationOptions) throws IOException {
    LOGGER.info("updateGroupMembership on uid: {0}, attributesDelta: {1}, options: {2}", uid.getValue(), attributesDelta, operationOptions);

    for (AttributeDelta attributeDelta : attributesDelta) {
      if (attributeDelta.getName().equalsIgnoreCase(ATTR_GROUP_MEMBERS)) {
        addOrRemoveMembersFromGroup(uid, attributeDelta);
      }
    }
  }

  // Execute the search query for groups
  public void executeQueryForGroups(STAFilter query, ResultsHandler handler) {
    try {
      // Find group by Uid (Group's Primary Key)
      if (query != null && query.getByUid() != null) {
        getGroup(query, handler);
      } else {
        getGroups(handler);
      }
    } catch (IOException e) {
      throw new ConnectorIOException(e.getMessage(), e);
    }
  }

  // Creates or updates the group in STA based on the provided parameters
  public Uid createOrUpdateGroup(Uid uid, Set<Attribute> attributes) {
    LOGGER.info("createOrUpdateGroup, Uid: {0}, attributes: {1}", uid, attributes);
    if (attributes == null || attributes.isEmpty()) {
      LOGGER.error("Request ignored, Empty attributes");
      throw new InvalidAttributeValueException("Request ignored, Empty attributes");
    }
    boolean create = uid == null;

    JSONObject groupObject = new JSONObject();

    String name = getStringAttr(attributes, Name.NAME);
    if (create && StringUtil.isBlank(name)) {
      throw new InvalidAttributeValueException("Missing mandatory attribute: " + Name.NAME);
    }
    if (name != null) {
      groupObject.put(ATTR_NORMAL_GROUP_NAME, name);
    }

    putIfExists(attributes, ATTR_UID, groupObject);
    putIfExists(attributes, ATTR_NORMAL_GROUP_NAME, groupObject);
    putIfExists(attributes, ATTR_SCHEMA_VERSION_NUMBER, groupObject);
    putIfExists(attributes, ATTR_GROUP_DESCRIPTION, groupObject);
    putIfExists(attributes, ATTR_SYNCHRONIZED, groupObject);

    try {
      HttpEntityEnclosingRequestBase request;
      if (create) {
        request = new HttpPost(getGroupBaseURL(false));
        request.setHeader(OBJECT_ID_FORMAT, HEX);
      } else {  //update
        request = new HttpPatch(String.format("%s/%s", getGroupBaseURL(false), urlEncoder(uid.getUidValue())));
        request.setHeader(OBJECT_ID_FORMAT, HEX);
      }
      JSONObject jsonResponse = callRequest(request, groupObject);
      String newUid = jsonResponse.getString(ATTR_UID);

      if (newUid == null) {
        assert uid != null;
        LOGGER.error("Group[uid = {0}] update failed", uid.getUidValue());
      }
      return new Uid(newUid);
    } catch (Exception e) {
      throw new ConnectorIOException(e.getMessage(), e);
    }
  }

  // Deletes a group in STA
  public void deleteGroup(Uid uid) {
    try {
      LOGGER.info("Delete group with Uid: {0}", uid);
      HttpDelete request = new HttpDelete(String.format("%s/%s", getGroupBaseURL(false), urlEncoder(uid.getUidValue())));
      callRequest(request, JSONObject.class, false);
    } catch (IOException e) {
      throw new ConnectorIOException(e.getMessage(), e);
    }
  }

  private void getGroup(STAFilter query, ResultsHandler handler) throws IOException {
    HttpGet request = new HttpGet(String.format("%s/%s", getGroupBaseURL(true), urlEncoder(query.getByUid())));
    JSONObject groupObject = callRequest(request, JSONObject.class, true);
    modifySTAGroupResponse(groupObject);
    ConnectorObject connectorObject = convertGroupToConnectorObject(groupObject);
    handler.handle(connectorObject);
  }

  private void getGroups(ResultsHandler handler) throws IOException {
    int pageSize = this.configuration.getPageSize();
    int currentPage = 0;
    boolean finish = false;

    while (!finish) {
      String pagingQueryString = getPagingQuery(currentPage, pageSize);
      pagingQueryString = pagingQueryString.replace("pagesize", "count");
      HttpGet request = new HttpGet(String.format("%s?%s", getGroupBaseURL(true), pagingQueryString));
      finish = processSTAGroups(request, handler);
      currentPage++;
    }
  }

  // Modify the group JSONObject to get the members in a group
  private void modifySTAGroupResponse(JSONObject group) {
    group.remove("schemas");
    group.put(ATTR_NORMAL_GROUP_NAME, group.get(ATTR_SCIM_GROUP_NAME));
    group.remove(ATTR_SCIM_GROUP_NAME);

    String groupKey = "urn:ietf:params:scim:schemas:extension:stagroupextension:2.0:Group";
    String description = group.getJSONObject(groupKey).has(ATTR_GROUP_DESCRIPTION) ? group.getJSONObject(groupKey).getString(ATTR_GROUP_DESCRIPTION) : "";
    group.put(ATTR_GROUP_DESCRIPTION, description);
    group.put(ATTR_SYNCHRONIZED, group.getJSONObject(groupKey).get(ATTR_SYNCHRONIZED).toString());
    group.remove(groupKey);
    JSONArray members = group.getJSONArray(ATTR_GROUP_MEMBERS);

    for (int i = 0; i < members.length(); i++) {
      members.getJSONObject(i).put(ATTR_UID, members.getJSONObject(i).get(VALUE));
    }

    group.remove(ATTR_GROUP_MEMBERS);
    group.put(ATTR_GROUP_MEMBERS, members);
  }

  // Converts the group object from STA to a ConnID object to store in midPoint
  private ConnectorObject convertGroupToConnectorObject(JSONObject group) {
    ConnectorObjectBuilder builder = new ConnectorObjectBuilder();
    builder.setObjectClass(ObjectClass.GROUP);
    builder.setUid(new Uid(group.getString(ATTR_UID)));

    if (group.has(ATTR_NORMAL_GROUP_NAME)) {
      builder.setName(group.getString(ATTR_NORMAL_GROUP_NAME));
    }

    getIfExists(group, ATTR_GROUP_DESCRIPTION, builder);
    getIfExists(group, ATTR_UID, builder);
    getIfExists(group, ATTR_NORMAL_GROUP_NAME, builder);
    getIfExists(group, ATTR_SCIM_GROUP_NAME, builder);
    getIfExists(group, ATTR_SCHEMA_VERSION_NUMBER, builder);
    getIfExists(group, ATTR_SYNCHRONIZED, builder);
    getMultiIfExists(group, builder);

    return builder.build();
  }

  // Adds or removes members from a group in STA based on the provided parameters
  private void addOrRemoveMembersFromGroup(Uid uid, AttributeDelta attributeDelta) throws IOException {
    LOGGER.info("AddOrRemoveMemberFromGroup {0} , {1}", uid, attributeDelta);

    List<Object> valuesToRemove = attributeDelta.getValuesToRemove();
    if (valuesToRemove != null && !valuesToRemove.isEmpty()) {
      //HttpDelete delRequest = new HttpDelete(getGroupBaseURL(false) + "/" + uid.getUidValue() + "/" + ATTR_GROUP_MEMBERS + "/");  // request for remove a user from a group
      removeMembersFromGroup(uid, valuesToRemove);
    }

    List<Object> valuesToAdd = attributeDelta.getValuesToAdd();
    if (valuesToAdd != null && !valuesToAdd.isEmpty()) {
      HttpEntityEnclosingRequestBase addRequest =
          new HttpPost(getGroupBaseURL(false) + "/" + uid.getUidValue() + "/" + ATTR_GROUP_MEMBERS);  // request for add a user to a group
      addMembersToGroup(addRequest, valuesToAdd);
    }
  }

  // Adds users to a group
  private void addMembersToGroup(HttpEntityEnclosingRequestBase request, List<Object> valuesToAdd) throws IOException {
    for (Object valToAdd : valuesToAdd) {
      request.setHeader(OBJECT_ID_FORMAT, HEX);
      if (valToAdd != null) {
        JSONObject jsonObject = new JSONObject();
        String userId = (String) valToAdd;
        jsonObject.put(ATTR_UID, userId);
        jsonObject.put("type", "User");
        callRequest(request, jsonObject);
      }
    }
  }

  // Removes users from a group
  private void removeMembersFromGroup(Uid uid, List<Object> valuesToRemove) throws IOException {
    for (Object valToRemove : valuesToRemove) {
      if (valToRemove != null) {
        String userID = (String) valToRemove;
        HttpDelete delRequest = new HttpDelete(String.format("%s/%s/%s/%s", getGroupBaseURL(false), uid.getUidValue(), ATTR_GROUP_MEMBERS,
            urlEncoder(userID)));  // request for remove a user from a group
        delRequest.setHeader(OBJECT_ID_FORMAT, HEX);
        //request.setURI(URI.create(request.getURI().toString().concat(userID)));
        callRequest(delRequest, JSONObject.class, false);
      }
    }
  }

  // Returns the base URLs for API calls that are related to groups
  private String getGroupBaseURL(Boolean scim) {
    if (scim) {
      URI restBaseUrl = URI.create(this.configuration.getAPIBaseURL());
      return HTTPS + restBaseUrl.getHost() + restBaseUrl.getPath().substring(7) + SCIM + GROUPS;
    } else {
      return (this.configuration.getAPIBaseURL() + GROUPS);
    }
  }

  // Processes the groups after retrieving them from STA
  private boolean processSTAGroups(HttpGet request, ResultsHandler handler) throws IOException {
    JSONArray groups = callRequest(request, JSONArray.class, true);

    for (int i = 0; i < groups.length(); i++) {
      if (i % 10 == 0) {
        LOGGER.info("ExecuteQuery: processing {0}. of {1} groups", i, groups.length());
      }

      HttpGet groupRequest = new HttpGet(String.format("%s/%s", getGroupBaseURL(true), groups.getJSONObject(i).getString(ATTR_UID)));
      JSONObject group = callRequest(groupRequest, JSONObject.class, true);
      modifySTAGroupResponse(group);

      ConnectorObject connectorObject = convertGroupToConnectorObject(group);
      boolean finish = !handler.handle(connectorObject);
      if (finish) {
        return true;
      }
    }
    // last page exceeded?
    return this.configuration.getPageSize() >= groups.length();
  }
}

