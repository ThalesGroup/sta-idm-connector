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
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

public class STARestConfiguration extends AbstractConfiguration {
  private GuardedString apikey;
  private int pageSize = 20;
  private String apiBaseUrl;

  @ConfigurationProperty(order = 15, displayMessageKey = "Api Key", helpMessageKey = "The Api Key for your STA tenant.", required = true)
  public GuardedString getApiKey() {
    return this.apikey;
  }

  public void setApiKey(GuardedString apikey) {
    this.apikey = apikey;
  }

  @ConfigurationProperty(order = 12, displayMessageKey = "Page Size", helpMessageKey = "The number of records to fetch in each request.", required = true)
  public int getPageSize() {
    return this.pageSize;
  }

  public void setPageSize(int pageSize) {
    this.pageSize = pageSize;
  }

  @ConfigurationProperty(order = 20, displayMessageKey = "REST API Endpoint URL", helpMessageKey = "The Rest API endpoint URL of your STA tenant.", required = true)
  public String getAPIBaseURL() {
    return this.apiBaseUrl;
  }

  public void setAPIBaseURL(String apiBaseUrl) {
    this.apiBaseUrl = apiBaseUrl;
  }

  public void validate() {
    //todo
  }
}
