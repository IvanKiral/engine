/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Lumeer.io, s.r.o. and/or its affiliates.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package io.lumeer.core.facade;

import io.lumeer.api.model.Attribute;
import io.lumeer.api.model.Collection;
import io.lumeer.api.model.FileAttachment;
import io.lumeer.api.model.LinkInstance;
import io.lumeer.api.model.LinkPermissionsType;
import io.lumeer.api.model.LinkType;
import io.lumeer.api.model.Permission;
import io.lumeer.api.model.ResourceType;
import io.lumeer.api.model.RoleType;
import io.lumeer.api.model.Rule;
import io.lumeer.api.model.common.AttributesResource;
import io.lumeer.api.util.CollectionUtil;
import io.lumeer.api.util.ResourceUtils;
import io.lumeer.core.adapter.LinkTypeAdapter;
import io.lumeer.core.adapter.ResourceAdapter;
import io.lumeer.core.exception.BadFormatException;
import io.lumeer.core.exception.NoPermissionException;
import io.lumeer.core.facade.conversion.ConversionFacade;
import io.lumeer.storage.api.dao.CollectionDao;
import io.lumeer.storage.api.dao.LinkDataDao;
import io.lumeer.storage.api.dao.LinkInstanceDao;
import io.lumeer.storage.api.dao.LinkTypeDao;
import io.lumeer.storage.api.dao.ResourceCommentDao;
import io.lumeer.storage.api.dao.UserDao;
import io.lumeer.storage.api.dao.ViewDao;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class LinkTypeFacade extends AbstractFacade {

   @Inject
   private LinkTypeDao linkTypeDao;

   @Inject
   private CollectionDao collectionDao;

   @Inject
   private LinkDataDao linkDataDao;

   @Inject
   private LinkInstanceDao linkInstanceDao;

   @Inject
   private FileAttachmentFacade fileAttachmentFacade;

   @Inject
   private ResourceCommentDao resourceCommentDao;

   @Inject
   private ViewDao viewDao;

   @Inject
   private UserDao userDao;

   @Inject
   private ConversionFacade conversionFacade;

   private LinkTypeAdapter adapter;
   private ResourceAdapter resourceAdapter;

   @PostConstruct
   public void init() {
      adapter = new LinkTypeAdapter(linkTypeDao, linkInstanceDao);
      resourceAdapter = new ResourceAdapter(permissionsChecker.getPermissionAdapter(), collectionDao, linkTypeDao, viewDao, userDao);
   }

   public LinkTypeAdapter getAdapter() {
      return adapter;
   }

   public LinkType createLinkType(LinkType linkType) {
      return createLinkType(linkType, false);
   }

   public LinkType createLinkType(LinkType linkType, boolean skipLimits) {
      checkProjectRole(RoleType.LinkContribute);
      checkLinkTypeCollections(linkType);

      if (!skipLimits) {
         permissionsChecker.checkFunctionsLimit(linkType);
         permissionsChecker.checkRulesLimit(linkType);
      }
      permissionsChecker.checkRulesPermissions(linkType.getRules());

      if (linkType.getPermissionsType() == LinkPermissionsType.Custom) {
         Permission defaultUserPermission = Permission.buildWithRoles(getCurrentUserId(), LinkType.ROLES);
         linkType.getPermissions().updateUserPermissions(defaultUserPermission);
      }

      List<Attribute> attributes = linkType.getAttributes();

      linkType.setAttributes(null);
      linkType.setLastAttributeNum(0);
      linkType.setLinksCount(0L);
      mapLinkTypeCreationValues(linkType);

      LinkType storedLinkType = linkTypeDao.createLinkType(linkType);
      linkDataDao.createDataRepository(storedLinkType.getId());

      if (attributes.size() > 0 && permissionsChecker.hasRoleInLinkType(storedLinkType, RoleType.AttributeEdit)) {
         storedLinkType.setAttributes(createLinkTypeAttributes(storedLinkType, attributes));
      }

      return storedLinkType;
   }

   public LinkType updateLinkType(String id, LinkType linkType) {
      return updateLinkType(id, linkType, false);
   }

   public LinkType updateLinkType(String id, LinkType linkType, final boolean skipFceLimits) {
      LinkType storedLinkType = checkLinkTypePermission(id, RoleType.Read);
      if (!storedLinkType.getCollectionIds().containsAll(linkType.getCollectionIds())) {
         throw new BadFormatException("Can not change LinkType collectionIds property");
      }

      if (!skipFceLimits) {
         if (linkType.someRuleChangedOrAdded(storedLinkType)) {
            permissionsChecker.checkRulesLimit(linkType);
         }
         if (linkType.someFunctionChangedOrAdded(storedLinkType)) {
            permissionsChecker.checkFunctionsLimit(linkType);
         }
      }
      permissionsChecker.checkRulesPermissions(linkType.getRules());
      permissionsChecker.checkAttributesFunctionAccess(linkType.getAttributes());

      LinkType updatingLinkType = new LinkType(storedLinkType);
      updatingLinkType.patch(linkType, permissionsChecker.getActualRoles(storedLinkType));
      keepUnmodifiableFields(updatingLinkType, storedLinkType);
      mapLinkTypeUpdateValues(linkType);

      return mapLinkTypeData(linkTypeDao.updateLinkType(id, updatingLinkType, storedLinkType));
   }

   public LinkType updateLinkTypeRules(String id, LinkType linkType, final boolean skipFceLimits) {
      LinkType storedLinkType = checkLinkTypePermission(id, RoleType.TechConfig);
      if (!storedLinkType.getCollectionIds().containsAll(linkType.getCollectionIds())) {
         throw new BadFormatException("Can not change LinkType collectionIds property");
      }
      permissionsChecker.checkRulesPermissions(linkType.getRules());

      LinkType updatingLinkType = new LinkType(storedLinkType);
      updatingLinkType.setRules(linkType.getRules());
      if (!skipFceLimits && updatingLinkType.someRuleChangedOrAdded(storedLinkType)) {
         permissionsChecker.checkRulesLimit(linkType);
      }

      keepUnmodifiableFields(updatingLinkType, storedLinkType);

      return mapLinkTypeData(linkTypeDao.updateLinkType(id, updatingLinkType, storedLinkType));
   }

   private void keepUnmodifiableFields(LinkType linkType, LinkType storedLinkType) {
      linkType.setId(storedLinkType.getId());
      linkType.setAttributes(storedLinkType.getAttributes());
      linkType.setLastAttributeNum(storedLinkType.getLastAttributeNum());
      linkType.setCollectionIds(storedLinkType.getCollectionIds());
   }

   public void deleteLinkType(String id) {
      LinkType linkType = linkTypeDao.getLinkType(id);
      permissionsChecker.checkCanDelete(linkType);

      linkTypeDao.deleteLinkType(id);
      deleteLinkTypeBasedData(linkType.getId());
   }

   private void deleteLinkTypeBasedData(final String linkTypeId) {
      var linkInstanceIds = linkInstanceDao.getLinkInstancesByLinkType(linkTypeId).stream().map(LinkInstance::getId).collect(Collectors.toSet());
      resourceCommentDao.deleteComments(ResourceType.LINK, linkInstanceIds);

      linkInstanceDao.deleteLinkInstancesByLinkTypesIds(Collections.singleton(linkTypeId));
      linkDataDao.deleteDataRepository(linkTypeId);
      deleteAutoLinkRulesByLinkType(linkTypeId);
      fileAttachmentFacade.removeAllFileAttachments(linkTypeId, FileAttachment.AttachmentType.LINK);
   }

   private void deleteAutoLinkRulesByLinkType(final String linkTypeId) {
      collectionDao.getAllCollections().stream()
                   .filter(collection -> CollectionUtil.containsAutoLinkRuleLinkType(collection, linkTypeId))
                   .collect(Collectors.toList())
                   .forEach(collection -> {
                      final Collection originalCollection = collection.copy();
                      filterAutoLinkRulesByLinkType(collection, linkTypeId);
                      mapResourceUpdateValues(collection);
                      collectionDao.updateCollection(collection.getId(), collection, originalCollection);
                   });
   }

   private void filterAutoLinkRulesByLinkType(final Collection collection, final String linkTypeId) {
      collection.setRules(collection.getRules().entrySet()
                                    .stream().filter(entry -> !CollectionUtil.containsAutoLinkRuleLinkType(entry.getValue(), linkTypeId))
                                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));
   }

   public LinkType getLinkType(final String linkTypeId) {
      var linkType = linkTypeDao.getLinkType(linkTypeId);
      if (permissionsChecker.hasRoleInLinkTypeWithView(linkType, RoleType.Read)) {
         return mapLinkTypeData(linkType);
      }

      var linkTypeReadersInViews = resourceAdapter.getLinkTypeTransitiveReaders(getOrganization(), getProject(), linkTypeId);
      if (linkTypeReadersInViews.contains(getCurrentUserId())) {
         return mapLinkTypeData(linkType);
      }

      throw new NoPermissionException(ResourceType.LINK_TYPE.toString());
   }

   public List<LinkType> getLinkTypes() {
      return mapLinkTypesData(resourceAdapter.getLinkTypes(getOrganization(), getProject(), getCurrentUserId()));
   }

   public List<LinkType> getAllLinkTypes() {
      return mapLinkTypesData(resourceAdapter.getAllLinkTypes(getOrganization(), getProject(), getCurrentUserId()));
   }

   public List<LinkType> getLinkTypesPublic() {
      if (permissionsChecker.isPublic()) {
         return mapLinkTypesData(linkTypeDao.getAllLinkTypes());
      }

      return List.of();
   }

   private LinkType mapLinkTypeData(LinkType linkType) {
      return mapLinkType(adapter.mapLinkTypeComputedProperties(linkType));
   }

   private List<LinkType> mapLinkTypesData(List<LinkType> linkTypes) {
      return adapter.mapLinkTypesComputedProperties(linkTypes)
                    .stream()
                    .peek(this::mapLinkType)
                    .collect(Collectors.toList());
   }

   public java.util.Collection<Attribute> createLinkTypeAttributes(final String linkTypeId, final java.util.Collection<Attribute> attributes) {
      LinkType linkType = checkLinkTypePermission(linkTypeId, RoleType.AttributeEdit);
      return createLinkTypeAttributes(linkType, attributes);
   }

   public java.util.Collection<Attribute> createLinkTypeAttributes(final LinkType linkType, final java.util.Collection<Attribute> attributes) {
      permissionsChecker.checkAttributesFunctionAccess(attributes);
      permissionsChecker.checkRoleInLinkType(linkType, RoleType.AttributeEdit);
      final LinkType bookedAttributesLinkType = linkTypeDao.bookAttributesNum(linkType.getId(), linkType, attributes.size());

      int lastAttributeNum = bookedAttributesLinkType.getLastAttributeNum() - attributes.size() + 1;
      var actualRoles = permissionsChecker.getActualRoles(linkType);

      for (Attribute attribute : attributes) {
         attribute.setId(AttributesResource.ATTRIBUTE_PREFIX + lastAttributeNum++);
         attribute.patchCreation(actualRoles);
         bookedAttributesLinkType.createAttribute(attribute);

         if (attribute.isFunctionDefined()) {
            permissionsChecker.checkFunctionsLimit(bookedAttributesLinkType);

         }
      }

      mapLinkTypeUpdateValues(linkType);
      linkTypeDao.updateLinkType(linkType.getId(), bookedAttributesLinkType, linkType);

      return attributes;
   }

   public java.util.Collection<Attribute> createLinkTypeAttributesWithoutPushNotification(final String linkTypeId, final java.util.Collection<Attribute> attributes, boolean skipLimits) {
      LinkType linkType = checkLinkTypePermission(linkTypeId, RoleType.AttributeEdit);
      permissionsChecker.checkAttributesFunctionAccess(attributes);
      LinkType originalLinkType = new LinkType(linkType);

      var actualRoles = permissionsChecker.getActualRoles(linkType);

      var sortedAttributes = attributes.stream().sorted((a, b) -> a.getId().compareToIgnoreCase(b.getId())).collect(Collectors.toList());
      for (Attribute attribute : sortedAttributes) {
         attribute.patchCreation(actualRoles);
         linkType.createAttribute(attribute);

         if (!skipLimits && attribute.isFunctionDefined()) {
            permissionsChecker.checkFunctionsLimit(linkType);
         }
      }

      linkTypeDao.updateLinkType(linkType.getId(), linkType, originalLinkType, false);

      return attributes;
   }

   public LinkType upsertRule(final String linkTypeId, final String ruleId, final Rule rule) {
      final LinkType linkType = checkLinkTypePermission(linkTypeId, RoleType.TechConfig);

      LinkType originalLinkType = new LinkType(linkType);

      Map<String, Rule> rules = Objects.requireNonNullElse(linkType.getRules(), new HashMap<>());

      Rule originalRule = rules.get(ruleId);
      rule.checkConfiguration(originalRule);

      permissionsChecker.checkRulePermissions(rule);

      rules.put(ruleId, rule);
      linkType.setRules(rules);
      mapLinkTypeUpdateValues(linkType);

      final LinkType updatedLinkType = linkTypeDao.updateLinkType(linkType.getId(), linkType, originalLinkType);
      return mapLinkTypeData(updatedLinkType);
   }

   public Attribute updateLinkTypeAttribute(final String linkTypeId, final String attributeId, final Attribute attribute) {
      return updateLinkTypeAttribute(linkTypeId, attributeId, attribute, false);
   }

   public Attribute updateLinkTypeAttribute(final String linkTypeId, final String attributeId, final Attribute attribute, final boolean skipFceLimits) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      final Attribute originalAttribute = ResourceUtils.findAttribute(linkType.getAttributes(), attributeId);
      if (originalAttribute == null) {
         return attribute;
      }

      LinkType originalLinkType = new LinkType(linkType);
      permissionsChecker.checkAnyRoleInLinkType(linkType, Set.of(RoleType.AttributeEdit, RoleType.TechConfig));

      Attribute updatingAttribute = originalAttribute.copy();
      updatingAttribute.patch(attribute, permissionsChecker.getActualRoles(linkType));

      if (updatingAttribute.isFunctionDefined()) {
         permissionsChecker.checkFunctionRuleAccess(updatingAttribute.getFunction().getJs(), RoleType.Read);
      } else {
         updatingAttribute.setFunction(null);
      }

      linkType.updateAttribute(attributeId, updatingAttribute);
      mapLinkTypeUpdateValues(linkType);

      if (!skipFceLimits && updatingAttribute.functionChangedOrAdded(originalAttribute)) {
         permissionsChecker.checkFunctionsLimit(linkType);
      }

      linkTypeDao.updateLinkType(linkTypeId, linkType, originalLinkType);
      conversionFacade.convertStoredDocuments(linkType, originalAttribute, updatingAttribute);

      return attribute;
   }

   public void deleteLinkTypeAttribute(final String linkTypeId, final String attributeId) {
      LinkType linkType = checkLinkTypePermission(linkTypeId, RoleType.AttributeEdit);
      LinkType originalLinkType = new LinkType(linkType);

      linkDataDao.deleteAttribute(linkTypeId, attributeId);

      linkType.deleteAttribute(attributeId);
      mapLinkTypeUpdateValues(linkType);

      linkTypeDao.updateLinkType(linkTypeId, linkType, originalLinkType);

      fileAttachmentFacade.removeAllFileAttachments(linkTypeId, attributeId, FileAttachment.AttachmentType.LINK);
   }

   private void checkLinkTypeCollections(LinkType linkType) {
      if (linkType.getCollectionIds() == null || linkType.getCollectionIds().size() != 2) {
         throw new BadFormatException("Invalid number of collectionIds in LinkType.");
      }
      linkType.getCollectionIds().forEach(collectionId ->
            permissionsChecker.checkAnyRole(collectionDao.getCollectionById(collectionId), Set.of(RoleType.Read))
      );
   }

   private void checkLinkTypePermission(LinkType linkType, RoleType role) {
      permissionsChecker.checkRoleInLinkType(linkType, role);
   }

   private LinkType checkLinkTypePermission(String linkTypeId, RoleType role) {
      LinkType linkType = linkTypeDao.getLinkType(linkTypeId);
      checkLinkTypePermission(linkType, role);
      return linkType;
   }

   private void checkProjectRole(RoleType role) {
      permissionsChecker.checkRole(getProject(), role);
   }

}
