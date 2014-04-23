package com.wordnik.swagger.report;

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
    b.append(path).append("\t").append(message).append("\t").append(severity);

    return b.toString();
  }
}
