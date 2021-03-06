/*
 * Copyright 2016 Shikhar Bhushan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dynamok.sink;

import com.amazonaws.regions.Regions;
import org.apache.kafka.common.config.AbstractConfig;
import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigException;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

class ConnectorConfig extends AbstractConfig {

    private enum Keys {
        ;
        static final String REGION = "region";
        static final String TABLE_FORMAT = "table.format";
        static final String KAFKA_ATTRIBUTES = "kafka.attributes";
        static final String IGNORE_RECORD_KEY = "ignore.record.key";
        static final String IGNORE_RECORD_VALUE = "ignore.record.value";
        static final String TOP_KEY_ATTRIBUTE = "top.key.attribute";
        static final String TOP_VALUE_ATTRIBUTE = "top.value.attribute";
        static final String MAX_RETRIES = "max.retries";
        static final String RETRY_BACKOFF_MS = "retry.backoff.ms";
    }

    static final ConfigDef CONFIG_DEF = new ConfigDef()
            .define(Keys.REGION, ConfigDef.Type.STRING, ConfigDef.NO_DEFAULT_VALUE, (key, regionName) -> {
                if (!Arrays.stream(Regions.values()).anyMatch(x -> x.getName().equals(regionName))) {
                    throw new ConfigException("Invalid AWS region: " + regionName);
                }
            }, ConfigDef.Importance.HIGH, "AWS region for the source DynamoDB.")
            .define(Keys.TABLE_FORMAT, ConfigDef.Type.STRING, "${topic}",
                    ConfigDef.Importance.HIGH, "Format string for destination DynamoDB table name, use ``${topic}`` as placeholder for source topic.")
            .define(Keys.KAFKA_ATTRIBUTES, ConfigDef.Type.LIST, "kafka_topic,kafka_partition,kafka_offset", (key, names) -> {
                final List namesList = (List) names;
                if (!namesList.isEmpty() && namesList.size() != 3)
                    throw new ConfigException(Keys.KAFKA_ATTRIBUTES,
                            "Must be empty or contain exactly 3 attribute names mapping to the topic, partition and offset, but was: " + namesList);
            }, ConfigDef.Importance.HIGH, "Trio of ``topic,partition,offset`` attribute names to include in records, set to empty to omit these attributes.")
            .define(Keys.IGNORE_RECORD_KEY, ConfigDef.Type.BOOLEAN, false,
                    ConfigDef.Importance.MEDIUM, "Whether to ignore Kafka record keys in preparing the DynamoDB record.")
            .define(Keys.IGNORE_RECORD_VALUE, ConfigDef.Type.BOOLEAN, false,
                    ConfigDef.Importance.MEDIUM, "Whether to ignore Kafka record value in preparing the DynamoDB record.")
            .define(Keys.TOP_KEY_ATTRIBUTE, ConfigDef.Type.STRING, "",
                    ConfigDef.Importance.MEDIUM, "DynamoDB attribute name to use for the record key. " +
                            "Leave empty if no top-level envelope attribute is desired.")
            .define(Keys.TOP_VALUE_ATTRIBUTE, ConfigDef.Type.STRING, "",
                    ConfigDef.Importance.MEDIUM, "DynamoDB attribute name to use for the record value. " +
                            "Leave empty if no top-level envelope attribute is desired.")
            .define(Keys.MAX_RETRIES, ConfigDef.Type.INT, 10,
                    ConfigDef.Importance.MEDIUM, "The maximum number of times to retry on errors before failing the task.")
            .define(Keys.RETRY_BACKOFF_MS, ConfigDef.Type.INT, 3000,
                    ConfigDef.Importance.MEDIUM, "The time in milliseconds to wait following an error before a retry attempt is made.");

    final Regions region;
    final String tableFormat;
    final KafkaCoordinateNames kafkaCoordinateNames;
    final boolean ignoreRecordKey;
    final boolean ignoreRecordValue;
    final String topKeyAttribute;
    final String topValueAttribute;
    final int maxRetries;
    final int retryBackoffMs;

    ConnectorConfig(Map<String, String> props) {
        super(CONFIG_DEF, props);
        region = Regions.fromName(getString(Keys.REGION));
        tableFormat = getString(Keys.TABLE_FORMAT);
        kafkaCoordinateNames = kafkaCoordinateNamesFromConfig(getList(Keys.KAFKA_ATTRIBUTES));
        ignoreRecordKey = getBoolean(Keys.IGNORE_RECORD_KEY);
        ignoreRecordValue = getBoolean(Keys.IGNORE_RECORD_VALUE);
        topKeyAttribute = getString(Keys.TOP_KEY_ATTRIBUTE);
        topValueAttribute = getString(Keys.TOP_VALUE_ATTRIBUTE);
        maxRetries = getInt(Keys.MAX_RETRIES);
        retryBackoffMs = getInt(Keys.RETRY_BACKOFF_MS);
    }

    private static KafkaCoordinateNames kafkaCoordinateNamesFromConfig(List<String> names) {
        if (names.isEmpty()) return null;
        final Iterator<String> it = names.iterator();
        return new KafkaCoordinateNames(it.next(), it.next(), it.next());
    }

    public static void main(String... args) {
        System.out.println(CONFIG_DEF.toRst());
    }

}
