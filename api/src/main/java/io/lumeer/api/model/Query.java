/*
 * Lumeer: Modern Data Definition and Processing Platform
 *
 * Copyright (C) since 2017 Answer Institute, s.r.o. and/or its affiliates.
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
package io.lumeer.api.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Collections;
import java.util.Set;

public class Query {

   private final Set<String> filters;
   private final Set<String> collectionIds;
   private final Set<String> documentIds;
   private final Set<String> linkTypeIds;
   private final String fulltext;
   private final Integer page;
   private final Integer pageSize;

   public Query() {
      this.filters = Collections.emptySet();
      this.collectionIds = Collections.emptySet();
      this.documentIds = Collections.emptySet();
      this.linkTypeIds = Collections.emptySet();
      this.fulltext = "";
      this.page = null;
      this.pageSize = null;
   }

   public Query(String fulltext) {
      this.fulltext = fulltext;

      this.collectionIds = Collections.emptySet();
      this.documentIds = Collections.emptySet();
      this.linkTypeIds = Collections.emptySet();
      this.filters = Collections.emptySet();
      this.page = 0;
      this.pageSize = 0;
   }

   public Query(Integer page, Integer pageSize) {
      this.page = page;
      this.pageSize = pageSize;

      this.collectionIds = Collections.emptySet();
      this.documentIds = Collections.emptySet();
      this.linkTypeIds = Collections.emptySet();
      this.filters = Collections.emptySet();
      this.fulltext = "";
   }

   public Query(Set<String> collectionIds, Set<String> linkTypeIds, Set<String> documentIds) {
      this.collectionIds = collectionIds != null ? collectionIds : Collections.emptySet();
      this.linkTypeIds = linkTypeIds != null ? linkTypeIds : Collections.emptySet();
      this.documentIds = documentIds != null ? documentIds : Collections.emptySet();

      this.filters = Collections.emptySet();
      this.page = 0;
      this.pageSize = 0;
      this.fulltext = "";
   }

   public Query(Query query) {
      this.filters = query.getFilters();
      this.fulltext = query.getFulltext();
      this.collectionIds = query.getCollectionIds();
      this.linkTypeIds = query.getLinkTypeIds();
      this.documentIds = query.getDocumentIds();
      this.page = query.getPage();
      this.pageSize = query.getPageSize();
   }

   @JsonCreator
   public Query(@JsonProperty("filters") final Set<String> filters,
         @JsonProperty("collectionIds") final Set<String> collectionIds,
         @JsonProperty("linkTypeIds") final Set<String> linkTypeIds,
         @JsonProperty("documentIds") final Set<String> documentIds,
         @JsonProperty("fulltext") final String fulltext,
         @JsonProperty("page") final Integer page,
         @JsonProperty("pageSize") final Integer pageSize) {
      this.filters = filters != null ? filters : Collections.emptySet();
      this.collectionIds = collectionIds != null ? collectionIds : Collections.emptySet();
      this.linkTypeIds = linkTypeIds != null ? linkTypeIds : Collections.emptySet();
      this.documentIds = documentIds != null ? documentIds : Collections.emptySet();
      this.fulltext = fulltext;
      this.page = page;
      this.pageSize = pageSize;
   }

   public Set<String> getFilters() {
      return filters;
   }

   public Set<String> getDocumentIds() {
      return documentIds;
   }

   public Set<String> getLinkTypeIds() {
      return linkTypeIds;
   }

   public Set<String> getCollectionIds() {
      return collectionIds;
   }

   public String getFulltext() {
      return fulltext;
   }

   public Integer getPage() {
      return page;
   }

   public Integer getPageSize() {
      return pageSize;
   }

   @Override
   public boolean equals(final Object o) {
      if (this == o) {
         return true;
      }
      if (!(o instanceof Query)) {
         return false;
      }

      final Query query = (Query) o;

      if (getFilters() != null ? !getFilters().equals(query.getFilters()) : query.getFilters() != null) {
         return false;
      }
      if (getCollectionIds() != null ? !getCollectionIds().equals(query.getCollectionIds()) : query.getCollectionIds() != null) {
         return false;
      }
      if (getDocumentIds() != null ? !getDocumentIds().equals(query.getDocumentIds()) : query.getDocumentIds() != null) {
         return false;
      }
      if (getLinkTypeIds() != null ? !getLinkTypeIds().equals(query.getLinkTypeIds()) : query.getLinkTypeIds() != null) {
         return false;
      }
      if (getFulltext() != null ? !getFulltext().equals(query.getFulltext()) : query.getFulltext() != null) {
         return false;
      }
      if (getPage() != null ? !getPage().equals(query.getPage()) : query.getPage() != null) {
         return false;
      }
      return getPageSize() != null ? getPageSize().equals(query.getPageSize()) : query.getPageSize() == null;
   }

   @Override
   public int hashCode() {
      int result = getFilters() != null ? getFilters().hashCode() : 0;
      result = 31 * result + (getCollectionIds() != null ? getCollectionIds().hashCode() : 0);
      result = 31 * result + (getDocumentIds() != null ? getDocumentIds().hashCode() : 0);
      result = 31 * result + (getLinkTypeIds() != null ? getLinkTypeIds().hashCode() : 0);
      result = 31 * result + (getFulltext() != null ? getFulltext().hashCode() : 0);
      result = 31 * result + (getPage() != null ? getPage().hashCode() : 0);
      result = 31 * result + (getPageSize() != null ? getPageSize().hashCode() : 0);
      return result;
   }

   @Override
   public String toString() {
      return "JsonQuery{" +
            ", filters=" + filters +
            ", collectionIds=" + collectionIds +
            ", documentIds=" + documentIds +
            ", linkTypeIds=" + linkTypeIds +
            ", fulltext='" + fulltext + '\'' +
            ", page=" + page +
            ", pageSize=" + pageSize +
            '}';
   }

   public boolean isMoreSpecificThan(Query otherQuery) {
      if (otherQuery.getCollectionIds() != null && otherQuery.getCollectionIds().size() > 0) {
         if (getCollectionIds() == null || !getCollectionIds().containsAll(otherQuery.getCollectionIds())) {
            return false;
         }
      }

      if (otherQuery.getDocumentIds() != null && otherQuery.getDocumentIds().size() > 0) {
         if (getDocumentIds() == null || !getDocumentIds().containsAll(otherQuery.getDocumentIds())) {
            return false;
         }
      }

      if (otherQuery.getLinkTypeIds() != null && otherQuery.getLinkTypeIds().size() > 0) {
         if (getLinkTypeIds() == null || !getLinkTypeIds().containsAll(otherQuery.getLinkTypeIds())) {
            return false;
         }
      }

      if (otherQuery.getFilters() != null && otherQuery.getFilters().size() > 0) {
         if (getFilters() == null || !getFilters().containsAll(otherQuery.getFilters())) {
            return false;
         }
      }

      if (otherQuery.getFulltext() != null) {
         if (getFulltext() == null || !getFulltext().startsWith(otherQuery.getFulltext())) {
            return false;
         }
      }

      return true;
   }
}
