package org.codelikethewind;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;

import java.util.List;

@ApplicationScoped
public class CallRecordEmitter {

    @Inject
    @Channel("callrecord-out")
    Emitter<String> emitter;

    @ConfigProperty(name = "kafka.bootstrap.servers")
    String kafkaUrl;

    @ConfigProperty(name = "mp.messaging.outgoing.callrecord-out.topic")
    String kafkaTopic;

    void onStart(@Observes StartupEvent ev) {
        System.out.println("-----------------------------");
        System.out.println("Starting Call Record Emitter");
        System.out.println("Kafka URL: " + kafkaUrl);
        System.out.println("Kafka Topic: " + kafkaTopic);

        List<String> callRecords = CvsReader.getCallRecords("signal_metrics.csv");

        for (String callRecord : callRecords){
            try {
                Thread.sleep(1000);
                emitter.send(callRecord);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

        }
    }
}
