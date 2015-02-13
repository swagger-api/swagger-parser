package io.swagger.report;


import java.util.ArrayList;
import java.util.List;

public class MessageBuilder {
    List<Message> messages = new ArrayList<>();
    Severity highestSeverity = Severity.OPTIONAL;

    public Severity getHighestSeverity() {
        return highestSeverity;
    }

    public void append(Message message) {
        messages.add(message);

        if (message.getSeverity().isMoreSevere(highestSeverity)) {
            highestSeverity = message.getSeverity();
        }
    }

    public String toString() {
        StringBuilder b = new StringBuilder();
        for (Message error : messages) {
            b.append(error.toString()).append("\n");
        }

        return b.toString();
    }
}