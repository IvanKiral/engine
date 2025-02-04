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
package io.lumeer.core.template;

import io.lumeer.api.model.common.WithId;
import io.lumeer.core.auth.AuthenticatedUser;
import io.lumeer.engine.api.data.DataDocument;

import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.module.jaxb.JaxbAnnotationIntrospector;
import org.json.simple.JSONObject;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;
import java.util.Locale;

public class WithIdCreator {

   private DateTimeFormatter dateDecoder = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssX", Locale.forLanguageTag("en_US"));

   protected final TemplateParser templateParser;

   protected WithIdCreator(final TemplateParser templateParser) {
      this.templateParser = templateParser;
   }

   protected DataDocument translateDataDocument(final JSONObject o, final AuthenticatedUser defaultUser, long dateAddition) {
      final DataDocument data = new DataDocument();

      o.forEach((k, v) -> {
         if (!"_id".equals(k)) {
            if ("$USER".equals(v) || "$USER@lumeerio.com".equals(v)) {
               data.append((String) k, defaultUser.getUserEmail());
            } else if ("$USER.NAME".equals(v)) {
               data.append((String) k, defaultUser.getUserName());
            } else {
               var passed = false;
               if (v != null) {
                  try {
                     var accessor = dateDecoder.parse(v.toString());
                     var date = Date.from(ZonedDateTime.from(accessor).toInstant());
                     var resultDate = new Date(date.getTime() + dateAddition);

                     data.append((String) k, resultDate);
                     passed = true;
                  } catch (DateTimeParseException dtpe) {
                  }
               }

               if (!passed) {
                  data.append((String) k, v);
               }
            }

            // add date function here
         }
      });

      return data;
   }
}
