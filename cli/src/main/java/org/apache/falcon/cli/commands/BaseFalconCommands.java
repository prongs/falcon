/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.falcon.cli.commands;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.falcon.client.FalconCLIException;
import org.apache.falcon.client.FalconClient;

import org.springframework.shell.core.ExecutionProcessor;
import org.springframework.shell.event.ParseResult;

import com.google.common.collect.Sets;


public class BaseFalconCommands implements ExecutionProcessor {
  public static final String URL_OPTION = "url";
  private static final String FALCON_URL = "FALCON_URL";
  private static final String DO_AS = "DO_AS";
  public static final String CLIENT_PROPERTIES = "/client.properties";
  private final Properties clientProperties;
  private final FalconClient client;
  private final String doAsUser;

  public BaseFalconCommands() {
    try {
      clientProperties = getClientProperties();
      String falconUrl = getFalconEndpoint();
      doAsUser = getDoAsUserProperty();
      client = new FalconClient(falconUrl, clientProperties);
    } catch (IOException | FalconCLIException e) {
      throw new RuntimeException("Couldn't start CLI", e);
    }
  }

  private String getDoAsUserProperty() {
    String url = System.getenv(FALCON_URL);
    if (url == null) {
      if (clientProperties.containsKey("do.as")) {
        url = clientProperties.getProperty("do.as");
      }
    }
    return url;
  }

  private Properties getClientProperties() throws IOException {
    InputStream inputStream = null;
    try {
      inputStream = BaseFalconCommands.class.getResourceAsStream(CLIENT_PROPERTIES);
      Properties prop = new Properties();
      if (inputStream != null) {
        prop.load(inputStream);
      }
      return prop;
    } finally {
      IOUtils.closeQuietly(inputStream);
    }
  }

  protected String getFalconEndpoint() throws FalconCLIException, IOException {
    String url = System.getenv(FALCON_URL);
    if (url == null) {
      if (clientProperties.containsKey("falcon.url")) {
        url = clientProperties.getProperty("falcon.url");
      }
    }
    if (url == null) {
      throw new FalconCLIException("Failed to get falcon url from cmdline, or environment or client properties");
    }

    return url;
  }

  @Override
  public ParseResult beforeInvocation(ParseResult parseResult) {
    Object[] args = parseResult.getArguments();
    if (args != null && Sets.newHashSet(args).size() == 1) {
      if (args[0] instanceof String) {
        String[] split = ((String) args[0]).split("\\s+");
        Object[] newArgs = new String[args.length];
        System.arraycopy(split, 0, newArgs, 0, split.length);
        parseResult = new ParseResult(parseResult.getMethod(), parseResult.getInstance(), newArgs);
      }
    }
    return parseResult;
  }

  @Override
  public void afterReturningInvocation(ParseResult parseResult, Object o) {

  }

  @Override
  public void afterThrowingInvocation(ParseResult parseResult, Throwable throwable) {

  }

  public FalconClient getClient() {
    return client;
  }

  public String getDoAsUser() {
    return doAsUser;
  }
}
