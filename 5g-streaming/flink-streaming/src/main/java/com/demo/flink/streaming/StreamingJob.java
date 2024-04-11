package com.demo.flink.streaming;

import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;


import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;

public class StreamingJob {

	public static void main(String[] args) throws Exception {
		// set up the streaming execution environment
		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

		Properties properties = new Properties();
		properties.setProperty("bootstrap.servers", "my-cluster-kafka-bootstrap.openshift-operators.svc:9092");
		properties.setProperty("group.id", "test");

		FlinkKafkaConsumer<String> myConsumer = new FlinkKafkaConsumer<>("call-records", new SimpleStringSchema(),
				properties);

		DataStream<String> stream = env.addSource(myConsumer);

		//DataStream<Tuple2<String, Double>> processedStream = stream
				//.flatMap(new Tokenizer())
				//.keyBy(0)
				//.sum(1);

		SingleOutputStreamOperator<Void> sinkResults = AsyncDataStream.unorderedWait(stream, new DataGridSink("call_records"),
				1000, // the timeout defines how long an asynchronous operation take before it is finally considered failed
				TimeUnit.MILLISECONDS,
				100); //capacity This parameter defines how many asynchronous requests may be in progress at the same time.

		//processedStream.print();

	/*
		SingleOutputStreamOperator<String> sinkResults = AsyncDataStream.unorderedWait(stream, new DataGridSink(),
				1000, // the timeout defines how long an asynchronous operation take before it is finally considered failed
				TimeUnit.MILLISECONDS,
				100); //capacity This parameter defines how many asynchronous requests may be in progress at the same time.
*/
		env.execute("Flink Streaming Java API Skeleton");

		/*
		DataStream<Tuple2<String, Double>> keyedStream = env.addSource(myConsumer).flatMap(new Splitter()).keyBy(0)
				.timeWindow(Time.seconds(300))
				.apply(new WindowFunction<Tuple2<String, Double>, Tuple2<String, Double>, Tuple, TimeWindow>() {

					@Override
					public void apply(Tuple key, TimeWindow window, Iterable<Tuple2<String, Double>> input,
							Collector<Tuple2<String, Double>> out) throws Exception {
						double sum = 0L;
						int count = 0;
						for (Tuple2<String, Double> record : input) {
							sum += record.f1;
							count++;
						}

						Tuple2<String, Double> result = input.iterator().next();
						result.f1 = (sum/count);
						out.collect(result);

					}
				});

		keyedStream.print();

		// execute program
		env.execute("Flink Streaming Java API Skeleton");

		 */
	}

}

