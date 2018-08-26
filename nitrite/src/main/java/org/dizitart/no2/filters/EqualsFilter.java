/*
 *
 * Copyright 2017-2018 Nitrite author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.dizitart.no2.filters;

import lombok.Getter;
import lombok.ToString;
import org.dizitart.no2.Document;
import org.dizitart.no2.NitriteId;
import org.dizitart.no2.collection.IndexType;
import org.dizitart.no2.exceptions.FilterException;
import org.dizitart.no2.index.ComparableIndexer;
import org.dizitart.no2.store.NitriteMap;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.dizitart.no2.common.Constants.DOC_ID;
import static org.dizitart.no2.exceptions.ErrorCodes.FE_EQUAL_NOT_COMPARABLE;
import static org.dizitart.no2.exceptions.ErrorCodes.FE_EQ_NOT_SPATIAL;
import static org.dizitart.no2.exceptions.ErrorMessage.errorMessage;
import static org.dizitart.no2.util.DocumentUtils.getFieldValue;
import static org.dizitart.no2.util.EqualsUtils.deepEquals;

@Getter
@ToString
class EqualsFilter extends BaseFilter {
    private String field;
    private Object value;

    EqualsFilter(String field, Object value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public Set<NitriteId> apply(NitriteMap<NitriteId, Document> documentMap) {
        if (field.equals(DOC_ID)) {
            Set<NitriteId> nitriteIdSet = new LinkedHashSet<>();
            NitriteId nitriteId = null;
            if (value instanceof Long) {
                nitriteId = NitriteId.createId((Long) value);
            }

            if (nitriteId != null) {
                if (documentMap.containsKey(nitriteId)) {
                    nitriteIdSet.add(nitriteId);
                }
            }
            return nitriteIdSet;
        } else if (indexedQueryTemplate.hasIndex(field)
                && !indexedQueryTemplate.isIndexing(field)
                && value != null) {

            if (indexedQueryTemplate.findIndex(field).getIndexType() == IndexType.Spatial) {
                throw new FilterException(errorMessage("eq cannot be used as a spatial filter",
                        FE_EQ_NOT_SPATIAL));
            }

            if (value instanceof Comparable) {
                ComparableIndexer comparableIndexer = indexedQueryTemplate.getComparableIndexer();
                return comparableIndexer.findEqual(field, (Comparable) value);
            } else {
                throw new FilterException(errorMessage(value + " is not comparable",
                        FE_EQUAL_NOT_COMPARABLE));
            }
        } else {
            return matchedSet(documentMap);
        }
    }

    private Set<NitriteId> matchedSet(NitriteMap<NitriteId, Document> documentMap) {
        Set<NitriteId> nitriteIdSet = new LinkedHashSet<>();
        for (Map.Entry<NitriteId, Document> entry: documentMap.entrySet()) {
            Document document = entry.getValue();
            Object fieldValue = getFieldValue(document, field);
            if (deepEquals(fieldValue, value)) {
                nitriteIdSet.add(entry.getKey());
            }
        }
        return nitriteIdSet;
    }
}
