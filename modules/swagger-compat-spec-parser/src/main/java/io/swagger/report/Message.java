package io.swagger.report;

public class Message {
    String path;
    String message;
    Severity severity;

    public Message(String path, String message, Severity severity) {
        this.path = path;
        this.message = message;
        this.severity = severity;
    }

    public String getPath() {
        return path;
    }

    public String getMessage() {
        return message;
    }

    public Severity getSeverity() {
        return severity;
    }

    public String toString() {
        StringBuilder b = new StringBuilder();

        if (path != null) {
            b.append(path);
        }

        b.append("\t");

        if (message != null) {
            b.append(message);
        }

        b.append("\t");

        if (severity != null) {
            b.append(severity);
        }

        return b.toString();
    }
}
