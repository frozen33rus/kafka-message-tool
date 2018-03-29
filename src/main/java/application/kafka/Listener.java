package application.kafka;

import application.model.modelobjects.KafkaListenerConfig;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.StringProperty;

public interface Listener {
    KafkaListenerConfig getListenerConfig();

    StringProperty loggedTextProperty();

    BooleanProperty isRunningProperty();

    void start();

    void appendLog(String text);

    String getCollectedLogs();

    void clearLogs();

    void stop();
}
