package com.wordnik.swagger.models.reader;

import java.util.*;

public class MessageBuilder {
  List<Message> messages = new ArrayList<Message>();
  void append(Message error) {
    messages.add(error);
  }

  public String toString() {
    StringBuilder b = new StringBuilder();
    for(Message error : messages) {
      b.append(error.toString()).append("\n");
    }

    return b.toString();
  }
}