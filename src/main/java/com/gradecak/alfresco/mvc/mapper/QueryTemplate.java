package com.gradecak.alfresco.mvc.mapper;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.service.ServiceRegistry;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.search.ResultSet;
import org.alfresco.service.cmr.search.ResultSetRow;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.service.cmr.search.SearchService;
import org.alfresco.service.namespace.QName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Assert;

import com.gradecak.alfresco.mvc.Query;

public class QueryTemplate {

  private static final Logger LOGGER = LoggerFactory.getLogger(QueryTemplate.class);

  private int defaultMaxItems = 100;
  private ServiceRegistry serviceRegistry;

  public QueryTemplate() {}

  public QueryTemplate(final ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public QueryTemplate(final ServiceRegistry serviceRegistry, final int maxItems) {
    this.serviceRegistry = serviceRegistry;
    this.defaultMaxItems = maxItems;
  }

  public <T> T queryForObject(final NodeRef nodeRef, final NodeMapper<T> mapper) {
    Assert.notNull(mapper);
    Assert.notNull(nodeRef);

    Map<QName, Serializable> properties = serviceRegistry.getNodeService().getProperties(nodeRef);
    return mapper.mapNode(properties);
  }

  public <T> T queryForObject(final Query query, final NodeMapper<T> mapper) {
    return queryForObject(query.toString(), mapper);
  }

  public <T> T queryForObject(final String query, final NodeMapper<T> mapper) throws IncorrectResultSizeException {
    Assert.notNull(mapper);
    Assert.hasText(query);

    List<T> list = queryForList(query.toString(), mapper, defaultMaxItems, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE,
        SearchService.LANGUAGE_LUCENE);
    int size = list.size();
    switch (size) {
    case 0:
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("No result found. Returning null.");
      }
      return null;
    case 1:
      return list.get(0);
    default:
      LOGGER.error(IncorrectResultSizeException.DEFAULT_MESSAGE_ID, size);
      throw new IncorrectResultSizeException(size);
    }
  }

  public <T> List<T> queryForList(final Query query, final NodeMapper<T> mapper) {
    return queryForList(query.toString(), mapper, defaultMaxItems, StoreRef.STORE_REF_WORKSPACE_SPACESSTORE, SearchService.LANGUAGE_LUCENE);
  }

  public <T> List<T> queryForList(final String query, final NodeMapper<T> mapper, final int maxItems, final StoreRef store,
      final String searchLanguage) {
    Assert.notNull(query);
    Assert.notNull(mapper);
    Assert.isTrue(maxItems > 0, "maxItems must be a positive integer");
    Assert.notNull(store);
    Assert.hasText(searchLanguage);

    List<T> list = new ArrayList<T>();

    SearchParameters sp = new SearchParameters();
    sp.addStore(store);
    sp.setLanguage(searchLanguage);
    sp.setMaxItems(maxItems);
    sp.setQuery(query.toString());
    ResultSet results = null;
    try {
      results = serviceRegistry.getSearchService().query(sp);
      if (results != null && results.length() != 0) {
        for (ResultSetRow resultSetRow : results) {
          NodeRef nodeRef = resultSetRow.getNodeRef();
          if (serviceRegistry.getNodeService().exists(nodeRef)) {
            if (LOGGER.isDebugEnabled()) {
              LOGGER.debug("Adding a row to the result list. NodeRef: " + nodeRef);
            }
            list.add(queryForObject(nodeRef, mapper));
          }
        }
      }
    } finally {
      if (results != null) {
        results.close();
      }
    }

    return list;
  }

  public void setServiceRegistry(ServiceRegistry serviceRegistry) {
    this.serviceRegistry = serviceRegistry;
  }

  public void setDefaultMaxItems(int maxItems) {
    this.defaultMaxItems = maxItems;
  }
}
