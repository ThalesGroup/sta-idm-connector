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
import java.util.Map;
import org.identityconnectors.common.security.GuardedString;

public class BasicConfigurationForTests {

  private final Parser parser = new Parser();
  protected String apiBaseUrl;
  protected GuardedString apiKey;
  protected STARestConnector staRestConnector;
  protected STARestConfiguration staRestConfiguration;

  Map<String, Object> operationOptions = new HashMap<>();

  public BasicConfigurationForTests() {
    staRestConfiguration = new STARestConfiguration();
    staRestConnector = new STARestConnector();
  }

  protected STARestConfiguration getConfiguration() {
    STARestConfiguration staRestConfiguration = new STARestConfiguration();
    if (parser.getApiBaseurl() != null) {
      staRestConfiguration.setAPIBaseURL(parser.getApiBaseurl());
      this.apiBaseUrl = parser.getApiBaseurl();
    }
    staRestConfiguration.setApiKey(parser.getApiKey());
    //staRestConfiguration.setPageSize(parser.getPageSize());
    this.apiKey = parser.getApiKey();
    //this.pageSize = parser.getPageSize();
    return staRestConfiguration;
  }

  protected TestSearchResultsHandler getTestSearchResultHandler() {
    return new TestSearchResultsHandler();
  }
}
