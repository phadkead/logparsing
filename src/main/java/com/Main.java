package com;

public class Main {

  public static void main(String[] args) {

    final String path = "src/test/resources/test.log";

    LogParser logParser = new LogParser(path);
    logParser.readFile();
    logParser.printStatistics();
  }
}
