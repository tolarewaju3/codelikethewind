package com.demo.flink.streaming;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import org.apache.flink.util.Collector;

public final class Tokenizer implements FlatMapFunction<String, Tuple2<String,
        Double>> {
    @Override
    public void flatMap(String value, Collector<Tuple2<String, Double>> out) {
        // normalize and split the line into words
        String[] words = value.split(",");

        String location = words[0];
        Double signalStrength = Double.valueOf(words[1]);

        out.collect(new Tuple2<>(location, signalStrength));

    }

}
