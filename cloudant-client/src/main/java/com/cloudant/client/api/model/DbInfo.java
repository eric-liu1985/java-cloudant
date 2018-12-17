/*
 * Copyright © 2015, 2018 IBM Corp. All rights reserved.
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

package com.cloudant.client.api.model;


import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.annotations.SerializedName;

/**
 * Encapsulates information about a database instance.
 *
 * @author Ganesh K Choudhary
 */
public class DbInfo {

    /**
     * Encapsulates database properties.
     */
    public static class Props {

        private boolean partitioned = false;

        /**
         * Get the database partitioned property.
         *
         * @return database partition property
         */
        public boolean getPartitioned() {
            return partitioned;
        }
    }

    @SerializedName("db_name")
    private String dbName;
    @SerializedName("doc_count")
    private long docCount;
    @SerializedName("doc_del_count")
    private String docDelCount;
    @SerializedName("update_seq")
    private JsonElement updateSeq;
    @SerializedName("purge_seq")
    private JsonElement purgeSeq;
    @SerializedName("compact_running")
    private boolean compactRunning;
    @SerializedName("disk_size")
    private long diskSize;
    @SerializedName("instance_start_time")
    private long instanceStartTime;
    @SerializedName("disk_format_version")
    private int diskFormatVersion;
    private Props props;

    public String getDbName() {
        return dbName;
    }

    public long getDocCount() {
        return docCount;
    }

    public String getDocDelCount() {
        return docDelCount;
    }

    public String getUpdateSeq() {
        return updateSeq.toString();
    }

    /**
     * Use {@link #getStringPurgeSeq()} instead.
     *
     * The value 0 is returned if the {@code purged_seq} field cannot be cast as a primitive long.
     * In later versions of CouchDB (&gt;=2.3.x) {@code purged_seq} is an opaque string.
     *
     * @return Number of purge operations on the database.
     */
    @Deprecated
    public long getPurgeSeq() {
        try {
            JsonPrimitive purgeSeqPrim = purgeSeq.getAsJsonPrimitive();
            return purgeSeqPrim.getAsLong();
        } catch (IllegalStateException e) {
            // Suppress exception if the element is of type JsonArray but contains more than a
            // single element.
        } catch (NumberFormatException e) {
            // Suppress exception if the element is not a JsonPrimitive and is not a valid long
            // value.
        }
        // Return 0 when value cannot be cast as a primitive long value.
        return 0;
    }

    /**
     * An opaque string that describes the state of purge operations across the database.
     *
     * @return Purge sequence.
     */
    public String getStringPurgeSeq() {
        return purgeSeq.toString();
    }

    public boolean isCompactRunning() {
        return compactRunning;
    }

    public long getDiskSize() {
        return diskSize;
    }

    public long getInstanceStartTime() {
        return instanceStartTime;
    }

    public int getDiskFormatVersion() {
        return diskFormatVersion;
    }

    /**
     * Get the database properties.
     *
     * @return database properties
     */
    public Props getProps() {
        return props;
    }

    @Override
    public String toString() {
        return String
                .format("CouchDbInfo [dbName=%s, docCount=%s, docDelCount=%s, updateSeq=%s, " +
                                "purgeSeq=%s, compactRunning=%s, diskSize=%s, instanceStartTime=%s, diskFormatVersion=%s]",
                        dbName, docCount, docDelCount, updateSeq, purgeSeq,
                        compactRunning, diskSize, instanceStartTime,
                        diskFormatVersion);
    }


}
