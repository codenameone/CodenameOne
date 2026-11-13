/*
 * Copyright (c) 2012, Codename One and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Codename One designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Codename One through http://www.codenameone.com/ if you
 * need additional information or have any questions.
 */
package com.codename1.backend.dynamo;

import com.codename1.backend.Json;
import com.codename1.backend.Web;
import com.codename1.backend.aws.Aws;
import com.codename1.backend.aws.Credentials;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// A DynamoDB client.
///
/// ```java
/// DynamoDb db = DynamoDb.forRegion(null);            // AWS_REGION
/// Map item = new LinkedHashMap();
/// item.put("id", "42");
/// item.put("name", "Ada");
/// db.putItem("users", item);
/// Map back = db.getItem("users", Collections.singletonMap("id", "42"));
/// ```
///
/// The typed methods take and return plain values (see [Items]). Anything they
/// do not cover -- a projection, a return-values option, a global secondary
/// index -- goes through [#call(String, Map)], which takes DynamoDB's own request
/// document and returns its response document unchanged.
///
/// Thread safe: every call is an independent signed HTTPS request.
public final class DynamoDb {
    private static final String TARGET_PREFIX = "DynamoDB_20120810.";
    private static final long REFRESH_MARGIN_MILLIS = 300000L;
    private static final int TABLE_WAIT_ATTEMPTS = 300;
    private static final long TABLE_WAIT_MILLIS = 1000L;

    private final String region;
    private final String host;
    private final boolean secure;
    private final boolean refreshCredentials;
    private Credentials credentials;
    private boolean consistentReads = true;

    private DynamoDb(Credentials credentials, String region, String host, boolean secure,
            boolean refreshCredentials) {
        this.credentials = credentials;
        this.region = region;
        this.host = host;
        this.secure = secure;
        this.refreshCredentials = refreshCredentials;
    }

    /// DynamoDB in one AWS region, with credentials resolved the usual way.
    ///
    /// A null region reads AWS_REGION, then AWS_DEFAULT_REGION.
    public static DynamoDb forRegion(String region) throws IOException {
        String resolved = region != null && region.length() > 0
                ? region : System.getenv("AWS_REGION");
        if (resolved == null || resolved.length() == 0) {
            resolved = System.getenv("AWS_DEFAULT_REGION");
        }
        if (resolved == null || resolved.length() == 0) {
            throw new IOException("No AWS region: pass one, or set AWS_REGION");
        }
        requireRegionName(resolved);
        boolean china = resolved.startsWith("cn-");
        return new DynamoDb(Credentials.resolve(), resolved,
                "dynamodb." + resolved + (china ? ".amazonaws.com.cn" : ".amazonaws.com"),
                true, true);
    }

    /// A DynamoDB-compatible endpoint, such as DynamoDB Local.
    ///
    /// `endpoint` is a host with an optional port and scheme, for example
    /// `http://localhost:8000`. TLS is used unless it says `http://`.
    public static DynamoDb forEndpoint(Credentials credentials, String region,
            String endpoint) {
        String h = endpoint == null ? "" : endpoint;
        boolean tls = true;
        if (h.regionMatches(true, 0, "http://", 0, 7)) {
            tls = false;
            h = h.substring(7);
        } else if (h.regionMatches(true, 0, "https://", 0, 8)) {
            h = h.substring(8);
        }
        while (h.endsWith("/")) {
            h = h.substring(0, h.length() - 1);
        }
        return new DynamoDb(credentials, region == null ? "us-east-1" : region, h, tls,
                false);
    }

    private static void requireRegionName(String region) throws IOException {
        for (int i = 0; i < region.length(); i++) {
            char c = region.charAt(i);
            // One hostname label, because the region is interpolated into the host:
            // a slash or a dot would move the request to somebody else's server.
            boolean ok = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '-';
            if (!ok) {
                throw new IOException("Not an AWS region: " + region);
            }
        }
    }

    // Unsynchronized, as in S3: two threads may both refresh and one answer is
    // dropped, which costs a metadata round trip and nothing else.
    private Credentials credentials() throws IOException {
        if (refreshCredentials && credentials.isExpiring(REFRESH_MARGIN_MILLIS)) {
            try {
                credentials = Credentials.resolve();
            } catch (IOException err) {
                // Keep signing with what is in hand until it has actually expired;
                // a metadata endpoint that blinks must not become an outage.
                if (credentials.isExpiring(0)) {
                    throw err;
                }
            }
        }
        return credentials;
    }

    /// Sends one operation, such as `"Query"`, and returns the response document.
    public Map call(String operation, Map request) throws IOException {
        byte[] body = Json.write(request == null ? new LinkedHashMap() : request)
                .getBytes("UTF-8");
        Map headers = new LinkedHashMap();
        headers.put("content-type", "application/x-amz-json-1.0");
        headers.put("x-amz-target", TARGET_PREFIX + operation);
        Web.Result result = Aws.send(credentials(), region, "dynamodb", "POST", host, "/",
                null, headers, body, null, secure);
        String text = result.getBodyAsString();
        Map response = text == null || text.length() == 0
                ? new LinkedHashMap() : Json.parseObject(text);
        if (result.getStatus() != 200) {
            Object type = response.get("__type");
            String t = type == null ? "HTTP " + result.getStatus() : String.valueOf(type);
            int hash = t.lastIndexOf('#');
            Object message = response.get("message");
            if (message == null) {
                message = response.get("Message");
            }
            throw new DynamoDbException(hash >= 0 ? t.substring(hash + 1) : t,
                    message == null ? "" : String.valueOf(message), result.getStatus());
        }
        return response;
    }

    /// The item with this key, or null when there is none.
    public Map getItem(String table, Map key) throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        req.put("Key", Items.toAttributes(key));
        consistent(req, null);
        Object item = call("GetItem", req).get("Item");
        return item instanceof Map ? Items.fromAttributes((Map) item) : null;
    }

    /// Writes an item, replacing any with the same key.
    public void putItem(String table, Map item) throws IOException {
        putItem(table, item, null, null);
    }

    /// Writes an item only if `condition` holds, for example
    /// `"attribute_not_exists(id)"`; a false condition throws a
    /// [DynamoDbException] whose [DynamoDbException#isConditionalCheckFailed()]
    /// is true. `values` binds the `:name` placeholders of the condition.
    public void putItem(String table, Map item, String condition, Map values)
            throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        req.put("Item", Items.toAttributes(item));
        expression(req, "ConditionExpression", condition, values);
        call("PutItem", req);
    }

    /// Applies an update expression such as `"SET #n = :n ADD visits :one"` and
    /// returns the item as it is afterwards.
    ///
    /// `names` binds the `#name` placeholders and `values` the `:name` ones;
    /// either may be null. `condition` may be null.
    public Map updateItem(String table, Map key, String update, String condition,
            Map names, Map values) throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        req.put("Key", Items.toAttributes(key));
        req.put("UpdateExpression", update);
        req.put("ReturnValues", "ALL_NEW");
        if (names != null && !names.isEmpty()) {
            req.put("ExpressionAttributeNames", names);
        }
        expression(req, "ConditionExpression", condition, values);
        Object attrs = call("UpdateItem", req).get("Attributes");
        return attrs instanceof Map ? Items.fromAttributes((Map) attrs) : null;
    }

    /// Deletes the item with this key; nothing happens when there is none.
    public void deleteItem(String table, Map key) throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        req.put("Key", Items.toAttributes(key));
        call("DeleteItem", req);
    }

    /// Every item a key condition matches, following pagination to the end.
    ///
    /// `index` names a secondary index, or null for the table itself.
    /// `limit` stops after that many items; zero reads them all.
    public List query(String table, String index, String keyCondition, Map names,
            Map values, int limit) throws IOException {
        return query(table, index, keyCondition, null, names, values, limit);
    }

    /// Like [#query(String, String, String, Map, Map, int)], with a filter
    /// expression (or null) applied to the items the key condition reads.
    /// DynamoDB filters after reading, so the filter narrows what is returned,
    /// not what is read and paid for.
    public List query(String table, String index, String keyCondition, String filter,
            Map names, Map values, int limit) throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        if (index != null) {
            req.put("IndexName", index);
        }
        req.put("KeyConditionExpression", keyCondition);
        consistent(req, index);
        if (names != null && !names.isEmpty()) {
            req.put("ExpressionAttributeNames", names);
        }
        expression(req, "FilterExpression", filter, values);
        return pages("Query", req, limit);
    }

    /// Every item of a table, optionally filtered, following pagination.
    ///
    /// A scan reads the whole table and is billed for it; prefer [#query].
    public List scan(String table, String filter, Map names, Map values, int limit)
            throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        consistent(req, null);
        if (names != null && !names.isEmpty()) {
            req.put("ExpressionAttributeNames", names);
        }
        expression(req, "FilterExpression", filter, values);
        return pages("Scan", req, limit);
    }

    /// Whether reads of a table are strongly consistent; true by default.
    ///
    /// DynamoDB's own default is eventually consistent, under which an item
    /// just written can be missing from the next read, and a version read for
    /// an optimistic update can be stale. Strong reads cost twice the read
    /// capacity. A secondary index read is always eventually consistent.
    public void setConsistentReads(boolean consistent) {
        consistentReads = consistent;
    }

    private void consistent(Map req, String index) {
        // DynamoDB refuses ConsistentRead on a global secondary index.
        if (consistentReads && index == null) {
            req.put("ConsistentRead", Boolean.TRUE);
        }
    }

    private List pages(String op, Map req, int limit) throws IOException {
        List out = new ArrayList();
        while (true) {
            if (limit > 0) {
                req.put("Limit", Integer.valueOf(limit - out.size()));
            }
            Map resp = call(op, req);
            Object items = resp.get("Items");
            if (items instanceof List) {
                List l = (List) items;
                for (Object eachLItem : l) {
                    Object v = eachLItem;
                    if (v instanceof Map) {
                        out.add(Items.fromAttributes((Map) v));
                    }
                }
            }
            Object next = resp.get("LastEvaluatedKey");
            if (!(next instanceof Map) || (limit > 0 && out.size() >= limit)) {
                return out;
            }
            req.put("ExclusiveStartKey", next);
        }
    }

    /// Writes all `items` to `table`, in batches of 25, retrying what DynamoDB
    /// reports unprocessed.
    public void batchPut(String table, List items) throws IOException {
        for (int start = 0; start < items.size(); start += 25) {
            List requests = new ArrayList();
            int end = Math.min(items.size(), start + 25);
            for (int i = start; i < end; i++) {
                Map put = new LinkedHashMap();
                put.put("Item", Items.toAttributes((Map) items.get(i)));
                Map wr = new LinkedHashMap();
                wr.put("PutRequest", put);
                requests.add(wr);
            }
            Map byTable = new LinkedHashMap();
            byTable.put(table, requests);
            for (int attempt = 0; byTable != null && !byTable.isEmpty(); attempt++) {
                if (attempt > 8) {
                    throw new IOException("DynamoDB left items unprocessed after "
                            + attempt + " attempts");
                }
                if (attempt > 0) {
                    backoff(attempt);
                }
                Map req = new LinkedHashMap();
                req.put("RequestItems", byTable);
                Object left = call("BatchWriteItem", req).get("UnprocessedItems");
                byTable = left instanceof Map ? (Map) left : null;
            }
        }
    }

    private static void backoff(int attempt) throws IOException {
        try {
            Thread.sleep(Math.min(2000L, 50L << attempt));
        } catch (InterruptedException err) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while retrying a batch write", err);
        }
    }

    /// Creates a table keyed by a string partition key and, when `sortKey` is not
    /// null, a string sort key, billed on demand. An existing table is left alone.
    public void createTable(String table, String partitionKey, String sortKey)
            throws IOException {
        createTable(table, partitionKey, false, sortKey, false);
    }

    /// Like [#createTable(String, String, String)], declaring each key as a
    /// number (`N`) or a string (`S`). DynamoDB refuses a write whose key value
    /// is not of the declared type, so a numeric key needs `N`.
    public void createTable(String table, String partitionKey, boolean numericPartition,
            String sortKey, boolean numericSort) throws IOException {
        List attrs = new ArrayList();
        List schema = new ArrayList();
        attrs.add(attr(partitionKey, numericPartition));
        schema.add(key(partitionKey, "HASH"));
        if (sortKey != null) {
            attrs.add(attr(sortKey, numericSort));
            schema.add(key(sortKey, "RANGE"));
        }
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        req.put("AttributeDefinitions", attrs);
        req.put("KeySchema", schema);
        req.put("BillingMode", "PAY_PER_REQUEST");
        try {
            call("CreateTable", req);
        } catch (DynamoDbException err) {
            if (!"ResourceInUseException".equals(err.getType())) {
                throw err;
            }
        }
        awaitActive(table);
    }

    /// Waits until `table` can be read and written. A new table is CREATING for
    /// some seconds, and every item operation on it fails until it is ACTIVE.
    private void awaitActive(String table) throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        for (int attempt = 0; attempt < TABLE_WAIT_ATTEMPTS; attempt++) {
            Object desc = call("DescribeTable", req).get("Table");
            if (desc instanceof Map && "ACTIVE".equals(((Map) desc).get("TableStatus"))) {
                return;
            }
            try {
                Thread.sleep(TABLE_WAIT_MILLIS);
            } catch (InterruptedException err) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted waiting for table " + table, err);
            }
        }
        throw new IOException("Table " + table + " did not become ACTIVE");
    }

    /// Deletes a table; a missing one is not an error.
    public void deleteTable(String table) throws IOException {
        Map req = new LinkedHashMap();
        req.put("TableName", table);
        try {
            call("DeleteTable", req);
        } catch (DynamoDbException err) {
            if (!"ResourceNotFoundException".equals(err.getType())) {
                throw err;
            }
        }
    }

    private static Map attr(String name, boolean numeric) {
        Map m = new LinkedHashMap();
        m.put("AttributeName", name);
        m.put("AttributeType", numeric ? "N" : "S");
        return m;
    }

    private static Map key(String name, String type) {
        Map m = new LinkedHashMap();
        m.put("AttributeName", name);
        m.put("KeyType", type);
        return m;
    }

    private static void expression(Map req, String field, String expression, Map values) {
        if (field != null && expression != null) {
            req.put(field, expression);
        }
        if (values != null && !values.isEmpty()) {
            req.put("ExpressionAttributeValues", Items.toAttributes(values));
        }
    }
}
