//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package org.apache.kafka.common.security;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import org.apache.kafka.common.config.types.Password;
import org.apache.kafka.common.network.ListenerName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JaasContext {
  private static final Logger LOG = LoggerFactory.getLogger(JaasUtils.class);
  private static final String GLOBAL_CONTEXT_NAME_SERVER = "KafkaServer";
  private static final String GLOBAL_CONTEXT_NAME_CLIENT = "KafkaClient";
  private final String name;
  private final JaasContext.Type type;
  private final Configuration configuration;
  private final List<AppConfigurationEntry> configurationEntries;
  private final Password dynamicJaasConfig;

  public static JaasContext loadServerContext(ListenerName listenerName, String mechanism, Map<String, ?> configs) {
    if (listenerName == null) {
      throw new IllegalArgumentException("listenerName should not be null for SERVER");
    } else if (mechanism == null) {
      throw new IllegalArgumentException("mechanism should not be null for SERVER");
    } else {
      String globalContextName = "KafkaServer";
      String listenerContextName = listenerName.value().toLowerCase(Locale.ROOT) + "." + "KafkaServer";
      Password dynamicJaasConfig = (Password)configs.get(mechanism.toLowerCase(Locale.ROOT) + "." + "sasl.jaas.config");
      if (dynamicJaasConfig == null && configs.get("sasl.jaas.config") != null) {
        LOG.warn("Server config {} should be prefixed with SASL mechanism name, ignoring config", "sasl.jaas.config");
      }

      return load(JaasContext.Type.SERVER, listenerContextName, globalContextName, dynamicJaasConfig);
    }
  }

  public static JaasContext loadClientContext(Map<String, ?> configs) {
    String globalContextName = "KafkaClient";
    Password dynamicJaasConfig = (Password)configs.get("sasl.jaas.config");
    return load(JaasContext.Type.CLIENT, (String)null, globalContextName, dynamicJaasConfig);
  }

  static JaasContext load(JaasContext.Type contextType, String listenerContextName, String globalContextName, Password dynamicJaasConfig) {
    if (dynamicJaasConfig != null) {
      JaasConfig jaasConfig = new JaasConfig(globalContextName, dynamicJaasConfig.value());
      AppConfigurationEntry[] contextModules = jaasConfig.getAppConfigurationEntry(globalContextName);
      if (contextModules != null && contextModules.length != 0) {
        if (contextModules.length != 1) {
          throw new IllegalArgumentException("JAAS config property contains " + contextModules.length + " login modules, should be 1 module");
        } else {
          return new JaasContext(globalContextName, contextType, jaasConfig, dynamicJaasConfig);
        }
      } else {
        throw new IllegalArgumentException("JAAS config property does not contain any login modules");
      }
    } else {
      return defaultContext(contextType, listenerContextName, globalContextName);
    }
  }

  private static JaasContext defaultContext(JaasContext.Type contextType, String listenerContextName, String globalContextName) {
    String jaasConfigFile = System.getProperty("java.security.auth.login.config");
    if (jaasConfigFile == null) {
      if (contextType == JaasContext.Type.CLIENT) {
        LOG.debug("System property 'java.security.auth.login.config' and Kafka SASL property 'sasl.jaas.config' are not set, using default JAAS configuration.");
      } else {
        LOG.debug("System property 'java.security.auth.login.config' is not set, using default JAAS configuration.");
      }
    }

    Configuration jaasConfig = Configuration.getConfiguration();
    AppConfigurationEntry[] configEntries = null;
    String contextName = globalContextName;
    if (listenerContextName != null) {
      configEntries = jaasConfig.getAppConfigurationEntry(listenerContextName);
      if (configEntries != null) {
        contextName = listenerContextName;
      }
    }

    if (configEntries == null) {
      configEntries = jaasConfig.getAppConfigurationEntry(globalContextName);
    }

    if (configEntries == null) {
      String listenerNameText = listenerContextName == null ? "" : " or '" + listenerContextName + "'";
      String errorMessage = "Could not find a '" + globalContextName + "'" + listenerNameText + " entry in the JAAS configuration. System property '" + "java.security.auth.login.config" + "' is " + (jaasConfigFile == null ? "not set" : jaasConfigFile);
      throw new IllegalArgumentException(errorMessage);
    } else {
      return new JaasContext(contextName, contextType, jaasConfig, (Password)null);
    }
  }

  public JaasContext(String name, JaasContext.Type type, Configuration configuration, Password dynamicJaasConfig) {
    this.name = name;
    this.type = type;
    this.configuration = configuration;
    AppConfigurationEntry[] entries = configuration.getAppConfigurationEntry(name);
    if (entries == null) {
      throw new IllegalArgumentException("Could not find a '" + name + "' entry in this JAAS configuration.");
    } else {
      this.configurationEntries = Collections.unmodifiableList(new ArrayList(Arrays.asList(entries)));
      this.dynamicJaasConfig = dynamicJaasConfig;
    }
  }

  public String name() {
    return this.name;
  }

  public JaasContext.Type type() {
    return this.type;
  }

  public Configuration configuration() {
    return this.configuration;
  }

  public List<AppConfigurationEntry> configurationEntries() {
    return this.configurationEntries;
  }

  public Password dynamicJaasConfig() {
    return this.dynamicJaasConfig;
  }

  public String configEntryOption(String key, String loginModuleName) {
    Iterator var3 = this.configurationEntries.iterator();

    Object val;
    do {
      AppConfigurationEntry entry;
      do {
        if (!var3.hasNext()) {
          return null;
        }

        entry = (AppConfigurationEntry)var3.next();
      } while(loginModuleName != null && !loginModuleName.equals(entry.getLoginModuleName()));

      val = entry.getOptions().get(key);
    } while(val == null);

    return (String)val;
  }

  public static enum Type {
    CLIENT,
    SERVER;

    private Type() {
    }
  }
}
