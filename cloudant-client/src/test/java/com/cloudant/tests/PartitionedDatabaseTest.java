/*
 * Copyright © 2018, 2018 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.tests;

import static com.cloudant.client.api.query.Expression.eq;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cloudant.client.api.Database;
import com.cloudant.client.api.DesignDocumentManager;
import com.cloudant.client.api.model.DesignDocument;
import com.cloudant.client.api.model.Document;
import com.cloudant.client.api.model.SearchResult;
import com.cloudant.client.api.query.QueryBuilder;
import com.cloudant.client.api.query.QueryResult;
import com.cloudant.client.api.views.Key;
import com.cloudant.client.api.views.ViewRequest;
import com.cloudant.client.api.views.ViewResponse;
import com.cloudant.test.main.RequiresCloudant;
import com.cloudant.tests.base.TestWithDbPerClass;
import com.cloudant.tests.util.Utils;
import com.google.gson.JsonObject;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiresCloudant
public class PartitionedDatabaseTest extends TestWithDbPerClass {

    private static final String testDbName = "partitioned_database_" + Utils.generateUUID();

    private static class PartitionDocument extends Document {
        private String foo = "bar";
    }

    @BeforeAll
    public static void setUp() throws Exception {
        // Create partitioned data set for testing.
        account.createPartitionedDB(testDbName);
        Database db = account.database(testDbName, false);
        List<PartitionDocument> docs = new ArrayList<>();
        List<String> partitionKeys = Arrays.asList("keyA", "keyB", "keyC");
        for (int i = 0; i < 30; i++) {
            PartitionDocument doc = new PartitionDocument();
            doc.setId(partitionKeys.get(i % 3) + ":doc" + Integer.toString(i));
            docs.add(doc);
        }
        db.bulk(docs);
    }

    @Test
    public void testCreatePartitionedDatabase() {
        String dbName = "partitioned_database_" + Utils.generateUUID();
        account.createPartitionedDB(dbName);

        Database db = account.database(testDbName, false);
        assertEquals(true, db.info().getPartitioned());
    }

    @Test
    public void testCreatePartitionDesignDocument() {
        Database db = account.database(testDbName, false);
        String ddocId = "partitioned_ddoc_empty";

        // Create partitioned design document and save to remote server.
        DesignDocument ddoc = new DesignDocument();
        ddoc.setId(ddocId);

        Map<String, Boolean> options = new HashMap<>();
        options.put("partitioned", true);
        ddoc.setOptions(options);

        DesignDocumentManager ddocManager = db.getDesignDocumentManager();
        ddocManager.put(ddoc);

        // Fetch design document from remote server.
        DesignDocument ddoc2 = ddocManager.get(ddocId);
        Map<String, Boolean> options2 = ddoc2.getOptions();

        assertNotNull(options2);
        assertTrue(options2.getOrDefault("partitioned", false));
    }


    @Test
    public void testPartitionedView() throws IOException {
        Database db = account.database(testDbName, false);
        String ddocId = "partitioned_ddoc_view";

        // Create design document.
        DesignDocument ddoc = new DesignDocument();
        ddoc.setId(ddocId);

        Map<String, Boolean> options = new HashMap<>();
        options.put("partitioned", true);
        ddoc.setOptions(options);

        DesignDocument.MapReduce mr = new DesignDocument.MapReduce();
        mr.setMap("function(doc) { emit(doc._id, 1); }");
        Map<String, DesignDocument.MapReduce> view = new HashMap<>();
        view.put("view", mr);
        ddoc.setViews(view);

        DesignDocumentManager ddocManager = db.getDesignDocumentManager();
        ddocManager.put(ddoc);

        String partitionKey = "keyA";
        ViewRequest<String, String> viewRequest = db.getViewRequestBuilder(partitionKey, ddocId, "view")
                .newRequest(Key.Type.STRING, String.class)
                .build();

        int count = 0;
        for (ViewResponse.Row<String, String> i : viewRequest.getResponse().getRows()) {
            assertTrue(i.getKey().startsWith(partitionKey));
            count++;
        }
        assertEquals(10, count);
    }

    @Test
    public void testPartitionedSearch() throws IOException {
        Database db = account.database(testDbName, false);
        String ddocId = "partitioned_ddoc_search";

        // Create design document.
        DesignDocument ddoc = new DesignDocument();
        ddoc.setId(ddocId);

        Map<String, Boolean> options = new HashMap<>();
        options.put("partitioned", true);
        ddoc.setOptions(options);

        JsonObject index = new JsonObject();
        index.addProperty("index", "function(doc) { index(\"id\", doc._id, {\"store\": true}); }");
        JsonObject search = new JsonObject();
        search.add("search", index);
        ddoc.setIndexes(search);

        DesignDocumentManager ddocManager = db.getDesignDocumentManager();
        ddocManager.put(ddoc);

        String partitionKey = "keyB";
        SearchResult<PartitionDocument> results = db
                .search(partitionKey, "partitioned_ddoc_search/search")
                .querySearchResult("*:*", PartitionDocument.class);

        int count = 0;
        for (SearchResult.SearchResultRow i : results.getRows()) {
            assertTrue(i.getId().startsWith(partitionKey));
            count++;
        }
        assertEquals(10, count);
    }

    @Test
    public void testPartitionedQuery() throws IOException {
        Database db = account.database(testDbName, false);
        String ddocId = "partitioned_ddoc_query";

        // Create index.
        db.createIndex("{\"index\": {\"fields\": [\"foo\"]}, \"partitioned\": true, \"name\": " +
                "\"foo-index\", \"ddoc\": \"" + ddocId + "\", \"type\": \"json\"}");

        String partitionKey = "keyC";
        QueryResult<PartitionDocument> results = db
                .query(partitionKey, new QueryBuilder(eq("foo", "bar"))
                        .useIndex(ddocId).build(), PartitionDocument.class);

        // Ensure query runs against partitioned index created above.
        assertNull(results.getWarning());

        int count = 0;
        for (PartitionDocument i : results.getDocs()) {
            assertTrue(i.getId().startsWith(partitionKey));
            count++;
        }
        assertEquals(10, count);
    }
}
