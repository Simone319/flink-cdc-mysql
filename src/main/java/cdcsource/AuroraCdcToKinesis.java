package cdcsource;

import model.function.AsyncCustomerOrderRequest;
import com.ververica.cdc.connectors.mysql.source.MySqlSource;
import com.ververica.cdc.connectors.mysql.table.StartupOptions;
import com.ververica.cdc.debezium.JsonDebeziumDeserializationSchema;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisProducer;
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class AuroraCdcToKinesis {
    private static final Logger log = LoggerFactory.getLogger(AuroraCdcToKinesis.class);

    public static void main(String[] args) throws Exception {
        // set up the streaming execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        DataStreamSource<String> orderSource = env.fromSource(
                createAuroraCDCSource(), WatermarkStrategy.noWatermarks(), "Order Source");

        DataStream<String> resultStream =
                AsyncDataStream.unorderedWait(orderSource, new AsyncCustomerOrderRequest(log), 1000, TimeUnit.MILLISECONDS, 100);

        resultStream.addSink(createSinkFromStaticConfig());

        env.execute("Flink Streaming Java API From Aurora CDC to Kinesis.");
    }

    private static MySqlSource<String> createAuroraCDCSource() {
        MySqlSource<String> mySqlSource = MySqlSource.<String>builder()
                .hostname("my-hostname")
                .port(3306)
                .databaseList("database") // set captured database
                .tableList("database.tables") // set captured table
                .username("admin")
                .password("password")
                .startupOptions(StartupOptions.initial())
                .deserializer(new JsonDebeziumDeserializationSchema()) // converts SourceRecord to JSON String
                .build();

        return mySqlSource;
    }

    private static FlinkKinesisProducer<String> createSinkFromStaticConfig() {
        Properties outputProperties = new Properties();
        outputProperties.setProperty(ConsumerConfigConstants.AWS_REGION, "cn-north-1");
        outputProperties.setProperty("AggregationEnabled", "false");

        FlinkKinesisProducer<String> sink = new FlinkKinesisProducer<>(new SimpleStringSchema(), outputProperties);
        sink.setDefaultStream("MyKinesisStream");
        sink.setDefaultPartition("0");
        return sink;
    }
}
