package org.rhq.plugins.jslee;

import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.security.auth.login.LoginException;
import javax.slee.management.ResourceManagementMBean;
import javax.slee.resource.ResourceAdaptorID;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.rhq.core.domain.configuration.PropertySimple;
import org.rhq.core.pluginapi.inventory.DiscoveredResourceDetails;
import org.rhq.core.pluginapi.inventory.InvalidPluginConfigurationException;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryComponent;
import org.rhq.core.pluginapi.inventory.ResourceDiscoveryContext;
import org.rhq.plugins.jslee.utils.MBeanServerUtils;

public class RAEntityLinkDiscoveryComponent implements ResourceDiscoveryComponent<RAEntityComponent> {

  private final Log log = LogFactory.getLog(this.getClass());

  private String raEntityName;
  private ResourceAdaptorID raID;

  public Set<DiscoveredResourceDetails> discoverResources(ResourceDiscoveryContext<RAEntityComponent> context)
  throws InvalidPluginConfigurationException, Exception {
    if(log.isDebugEnabled()) {
      log.debug("RAEntityLinkDiscoveryComponent.discoverResources() called");
    }

    Set<DiscoveredResourceDetails> discoveredLinks = new HashSet<DiscoveredResourceDetails>();

    MBeanServerUtils mbeanUtils = context.getParentResourceComponent().getMBeanServerUtils();

    try {
      MBeanServerConnection connection = mbeanUtils.getConnection();
      mbeanUtils.login();

      ObjectName resourceManagement = new ObjectName(ResourceManagementMBean.OBJECT_NAME);

      ResourceManagementMBean resourceManagementMBean = (ResourceManagementMBean) MBeanServerInvocationHandler
      .newProxyInstance(connection, resourceManagement, javax.slee.management.ResourceManagementMBean.class,
          false);

      raEntityName = context.getParentResourceComponent().getRAEntityName();
      this.raID = context.getParentResourceComponent().getResourceAdaptorID();

      String[] links = resourceManagementMBean.getLinkNames(raEntityName);

      for (String link : links) {
        String description = "Link : " + link + " For ResourceAdaptor Entity : " + raEntityName;

        DiscoveredResourceDetails discoveredEntity = new DiscoveredResourceDetails(context.getResourceType(), link,
            link+" Link", raID.getVersion(), description, null, null);
        discoveredEntity.getPluginConfiguration().put(new PropertySimple("linkName", link));

        discoveredLinks.add(discoveredEntity);
      }

      if(log.isInfoEnabled()) {
        log.info("Discovered " + discoveredLinks.size() + " JAIN SLEE Resource Adaptor Entity Link Components.");
      }
      return discoveredLinks;
    }
    finally {
      try {
        mbeanUtils.logout();
      }
      catch (LoginException e) {
        if(log.isDebugEnabled()) {
          log.debug("Failed to logout from secured JMX", e);
        }
      }
    }
  }
}
