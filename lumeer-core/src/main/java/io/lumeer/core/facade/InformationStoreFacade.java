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

import io.lumeer.api.model.InformationRecord;
import io.lumeer.core.adapter.InformationStoreAdapter;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.storage.api.dao.InformationStoreDao;

import javax.annotation.PostConstruct;
import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

@RequestScoped
public class InformationStoreFacade extends AbstractFacade {

   @Inject
   private AuthenticatedUser authenticatedUser;

   @Inject
   private InformationStoreDao informationStoreDao;

   private InformationStoreAdapter informationStoreAdapter;

   @PostConstruct
   public void init() {
      informationStoreAdapter = new InformationStoreAdapter(informationStoreDao);
   }

   public InformationRecord addInformation(final InformationRecord informationRecord) {
      return informationStoreAdapter.addInformation(informationRecord, authenticatedUser.getCurrentUserId());
   }

   public InformationRecord getInformation(final String id) {
      return informationStoreAdapter.getInformation(id, authenticatedUser.getCurrentUserId());
   }

}
