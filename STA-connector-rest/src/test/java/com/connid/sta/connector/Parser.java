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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;

//Read values from properties file
public class Parser {

  private static final Log LOGGER = Log.getLog(Parser.class);

  private static final Properties PROPERTIES = new Properties();
  private static final String PROPERTIES_PATH = "/propertiesForTest.properties";
  private static final String API_BASEURL = "apiBaseURL";
  private static final String API_KEY = "apiKey";
  private static final String PAGE_SIZE = "pageSize";

  public Parser() {
    InputStream inputStream = null;
    try {
      inputStream = getClass().getResourceAsStream(PROPERTIES_PATH);
      PROPERTIES.load(inputStream);
    } catch (FileNotFoundException e) {
      LOGGER.error(e, "File not found: {0}", e.getLocalizedMessage());
    } catch (IOException e) {
      LOGGER.error(e, "IO exception occurred {0}", e.getLocalizedMessage());
    } finally {
      try {
        Objects.requireNonNull(inputStream).close();
      } catch (IOException e) {
        LOGGER.error("Error closing input stream {0}", e.getLocalizedMessage());
      }
    }
  }

  public String getApiBaseurl() {
    return (String) PROPERTIES.get(API_BASEURL);
  }

  public GuardedString getApiKey() {
    return new GuardedString(((String) PROPERTIES.get(API_KEY)).toCharArray());
  }

  public int getPageSize() {
    return (int) PROPERTIES.get(PAGE_SIZE);
  }
}
