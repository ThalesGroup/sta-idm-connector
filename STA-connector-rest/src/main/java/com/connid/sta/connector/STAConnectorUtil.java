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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.OperationTimeoutException;
import org.identityconnectors.framework.common.exceptions.PermissionDeniedException;
import org.identityconnectors.framework.common.exceptions.PreconditionFailedException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class STAConnectorUtil {
  public static final String OBJECT_ID_FORMAT = "Object-Id-Format";
  public static final String HEX = "hex";
  public static final String HTTPS = "https://";
  public static final String SCIM = "scim/v2/";
  protected static final Log LOGGER = Log.getLog(STARestConnector.class);
  private static final String REST_CONTENT_TYPE = "application/json";
  private static final String SCIM_CONTENT_TYPE = "application/scim+json";
  private static final String ATTR_GROUP_MEMBERS = "members";
  private final CloseableHttpClient httpClient;
  STARestConfiguration configuration;

  public STAConnectorUtil(STARestConfiguration configuration) {
    this.configuration = configuration;
    this.httpClient = this.createHttpClient();

  }

  private CloseableHttpClient createHttpClient() {
    HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
    return httpClientBuilder.build();
  }

  public CloseableHttpClient getHttpClient() {
    return httpClient;
  }

  public CloseableHttpResponse execute(HttpUriRequest request) {
    try {
      final StringBuilder apiKey = new StringBuilder();
      if (this.configuration.getApiKey() != null) {
        this.configuration.getApiKey().access(chars -> apiKey.append(new String(chars)));
      }
      request.setHeader("apikey", apiKey.toString());
      return getHttpClient().execute(request);
    } catch (IOException e) {
      throw new ConnectorIOException(e.getMessage(), e);
    }
  }

  public void processResponse(CloseableHttpResponse response) {
    int statusCode = response.getStatusLine().getStatusCode();
    if (statusCode >= 200 && statusCode <= 302) {
      LOGGER.info("Got a response from STA with code " + statusCode);
      return;
    }
    processResponseErrors(response, statusCode);
  }

  private void processResponseErrors(CloseableHttpResponse response, int statusCode) {
    String responseBody = null;
    try {
      responseBody = EntityUtils.toString(response.getEntity());
    } catch (IOException e) {
      LOGGER.error("Cannot read STA's response body : " + e, e);
    }

    String message = "HTTP error " + statusCode + " " + response.getStatusLine().getReasonPhrase() + " : " + responseBody;

    switch (statusCode) {
      case 400:
      case 405:
      case 406:
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new ConnectorIOException(message);
      case 401:
      case 402:
      case 403:
      case 407:
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new PermissionDeniedException(message);
      case 404:
      case 410:
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new UnknownUidException(message);
      case 408:
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new OperationTimeoutException(message);
      case 412:
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new PreconditionFailedException(message);
      case 418:
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new UnsupportedOperationException("Sorry, the Operation is not supported: " + message);
      case 409:  //conflict
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new AlreadyExistsException(message);
      case 500:
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new RuntimeException("Internal server error");
      default:
        closeResponse(response);
        LOGGER.error("{0}", message);
        throw new ConnectorException(message);
    }
  }

  protected void closeResponse(CloseableHttpResponse response) {
    // to avoid pool waiting
    try {
      response.close();
    } catch (IOException e) {
      LOGGER.warn(e, "Error when trying to close response: " + response);
    }
  }

  protected String getStringAttr(Set<Attribute> attributes, String attrName) throws InvalidAttributeValueException {
    return getAttr(attributes, attrName, String.class);
  }

  protected <T> T getAttr(Set<Attribute> attributes, String attrName, Class<T> type) throws InvalidAttributeValueException {
    return getAttr(attributes, attrName, type, null);
  }

  public <T> T getAttr(Set<Attribute> attributes, String attrName, Class<T> type, T defaultVal) throws InvalidAttributeValueException {
    for (Attribute attr : attributes) {
      if (attrName.equals(attr.getName())) {
        List<Object> value = attr.getValue();
        if (value == null || value.isEmpty()) {
          // set empty value
          return null;
        }
        if (value.size() == 1) {
          Object val = value.get(0);
          if (val == null) {
            // set empty value
            return null;
          }
          if (type.isAssignableFrom(val.getClass())) {
            return (T) val;
          }
          throw new InvalidAttributeValueException(String.format("Unsupported type %s for attribute %s, value: ", val.getClass(), attrName));
        }
        throw new InvalidAttributeValueException(String.format("More than one value for attribute %s, values: %s", attrName, value));
      }
    }
    // set default value when attrName not in changed attributes
    return defaultVal;
  }

  protected <T> void addAttr(ConnectorObjectBuilder builder, String attrName, T attrVal) {
    if (attrVal != null) {
      builder.addAttribute(attrName, attrVal);
    }
  }

  // Populates the user JSON object with the available attributes in the set
  public void putIfExists(Set<Attribute> attributes, String fieldName, JSONObject jsonObject) {
    String fieldValue = getStringAttr(attributes, fieldName);
    if (fieldValue != null) {
      jsonObject.put(fieldName, fieldValue);
    }
  }


  public void getIfExists(JSONObject object, String attrName, ConnectorObjectBuilder builder) {
    if (object.has(attrName)) {
      if (object.get(attrName) != null && !JSONObject.NULL.equals(object.get(attrName))) {
        addAttr(builder, attrName, object.get(attrName));
      }
    }
  }

  protected void getMultiIfExists(JSONObject object, ConnectorObjectBuilder builder) {
    if (object.has(ATTR_GROUP_MEMBERS)) {
      JSONArray members = object.getJSONArray(ATTR_GROUP_MEMBERS);
      String[] membersArray = new String[members.length()];

      for (int i = 0; i < membersArray.length; i++) {
        membersArray[i] = members.getJSONObject(i).getString("id");
      }
      builder.addAttribute(ATTR_GROUP_MEMBERS, membersArray);
    }
  }

  // Returns the query string for paging
  public String getPagingQuery(int page, int pageSize) {
    StringBuilder queryBuilder = new StringBuilder();
    LOGGER.info("Creating paging query with page: {0}, pageSize: {1}", page, pageSize);
    queryBuilder.append("pageindex=").append(page).append("&pagesize=").append(pageSize);
    return queryBuilder.toString();
  }

  // Returns the JSON array of users/groups that are retrieved from STA
  /*protected JSONArray callRequest(HttpRequestBase request) throws IOException, JSONException {
    if (request.toString().contains("scim")) {
      request.setHeader("Content-Type", SCIM_CONTENT_TYPE);
      CloseableHttpResponse response = execute(request);
      processResponse(response);
      String result = EntityUtils.toString(response.getEntity());

      closeResponse(response);
      try {
        JSONObject responsejson = new JSONObject(result);
        return responsejson.getJSONArray("Resources");
      } catch (JSONException e) {
        throw new RuntimeException(result);
      }
    } else {
      request.setHeader("Content-Type", REST_CONTENT_TYPE);
      CloseableHttpResponse response = execute(request);
      processResponse(response);

      if (response.getEntity() == null) {
        return new JSONArray();
      }

      String result = EntityUtils.toString(response.getEntity());
      closeResponse(response);

      try {
        JSONObject responsejson = new JSONObject(result);
        JSONObject inside = (JSONObject) responsejson.get("page");
        return ((JSONArray) inside.get("items"));
      } catch (JSONException e) {
        throw new RuntimeException(result);
      }
    }
  }

  // Returns the JSON object for a particular user/group that is retrieved from STA
  protected JSONObject callRequest(HttpRequestBase request, boolean parseResult) throws IOException {
    request.setHeader("Content-Type", REST_CONTENT_TYPE);

    CloseableHttpResponse response = execute(request);
    processResponse(response);

    if (!parseResult) {
      closeResponse(response);
      return null;
    }

    String result = EntityUtils.toString(response.getEntity());
    closeResponse(response);
    JSONObject jsonObject = new JSONObject();

    if (StringUtil.isBlank(result)) {
      return jsonObject;
    }
    try {
      jsonObject = new JSONObject(result);
      return jsonObject;
    } catch (Exception e) {
      throw new RuntimeException(result);
    }
  }*/

  protected <T> T callRequest(HttpRequestBase request, Class<T> type, boolean parseResult) throws IOException {
    request.setHeader("Content-Type", REST_CONTENT_TYPE);
    CloseableHttpResponse response = execute(request);
    processResponse(response);

    if (!parseResult) {
      closeResponse(response);
      return null;
    }

    String result = EntityUtils.toString(response.getEntity());
    closeResponse(response);

    if (StringUtil.isBlank(result) && (type == JSONObject.class)) {
      return (T) new JSONObject();
    }

    try {
      if (type == JSONArray.class) {
        JSONObject responsejson = new JSONObject(result);
        if (request.toString().contains("scim")) {
          return (T) responsejson.getJSONArray("Resources");
        } else {
          JSONObject inside = (JSONObject) responsejson.get("page");
          return (T) ((JSONArray) inside.get("items"));
        }
      } else if (type == JSONObject.class) {
        return (T) new JSONObject(result);
      }
    } catch (JSONException e) {
      throw new RuntimeException(result);
    }
    return null;
  }


  // Return the JSON object for a particular user/group after creating or updating the user in STA
  protected JSONObject callRequest(HttpEntityEnclosingRequestBase request, JSONObject requestBody) throws IOException {
    request.setHeader("Content-Type", REST_CONTENT_TYPE);

    HttpEntity entity = new ByteArrayEntity(requestBody.toString().getBytes(StandardCharsets.UTF_8));
    request.setEntity(entity);

    CloseableHttpResponse response = execute(request);
    processResponse(response);
    String result = EntityUtils.toString(response.getEntity());
    closeResponse(response);
    JSONObject jsonObject = new JSONObject();

    if (StringUtil.isBlank(result)) {
      return jsonObject;
    }

    try {
      jsonObject = new JSONObject(result);
    } catch (Exception e) {
      throw new RuntimeException(result);
    }
    return jsonObject;
  }

  // Returns the URL encoded string of the input string
  public String urlEncoder(String value) throws UnsupportedEncodingException {
    return URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
  }
}
