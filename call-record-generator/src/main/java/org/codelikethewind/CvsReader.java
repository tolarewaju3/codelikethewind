package org.codelikethewind;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class CvsReader {
    public static List<String> getCallRecords(String filePath) {
        List<String> callRecords = new ArrayList<String>();

        try(
                BufferedReader br = new BufferedReader(new InputStreamReader(
                CvsReader.class.getResourceAsStream("/" + filePath)));
                CSVParser parser = CSVFormat.DEFAULT.withDelimiter(',').withHeader().parse(br);
        ) {
            for(CSVRecord record : parser) {
                String timestamp = record.get("Timestamp");
                String location = record.get("Locality");
                String latitude = record.get("Latitude");
                String longitude = record.get("Longitude");

                String latency = record.get("Latency (ms)");

                String signalStrength = record.get("Signal Strength (dBm)");
                String networkType = record.get("Network Type");

                String signalMetric = location + ","  + signalStrength + "," + networkType;

                callRecords.add(signalMetric);
            }
        } catch (Exception e) {
            System.out.println(e);
        }

        return callRecords;
    }
}
