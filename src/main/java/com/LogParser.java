package com;

import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.collections4.CollectionUtils;

class LogParser {

  private Map<String, List<String>> ipAddressUrlMap;

  private String path;

  LogParser(String path) {
    ipAddressUrlMap = new HashMap<>();
    this.path = path;
  }

  void readFile() {
    File file = new File(path);
    try (BufferedReader br = new BufferedReader(new FileReader(file))) {
      String line;
      while ((line = br.readLine()) != null) {
        try {
          parseLine(line);
        } catch (FileParseException e) {
          System.err.println("Omitting line " + line + "error: " + e.getMessage());
        }
      }
    } catch (FileNotFoundException e) {
      throw new RuntimeException("File not found :(" + e.getMessage());
    } catch (IOException e) {
      throw new RuntimeException("Something went wrong" + e.getMessage());
    }
  }

  protected void parseLine(String line) throws FileParseException {

    //1. extract ip address
    String[] splited = splitFromIpAddress(line);
    if (splited.length < 2) {
      throw new FileParseException("Cannot extract ip address");
    }
    String ipAddress = splited[0];

    //2. extractUrl
    String restOfTheLine = splited[1];
    Optional<String> maybeUrl = extractAccessedUrl(restOfTheLine);
    String url = maybeUrl.orElseThrow(() -> new FileParseException("ip address not present"));

    //3. put in the map
    storeInfo(ipAddress, url);
  }

  private void storeInfo(String ipAddress, String url) {
    List<String> val = ipAddressUrlMap.putIfAbsent(ipAddress, new ArrayList<>() {{
      add(url);
    }});
    if (CollectionUtils.isEmpty(val)) {
      ipAddressUrlMap.get(ipAddress).add(url);
    }
  }

  protected String[] splitFromIpAddress(String line) {
    String regex = "\\s-\\s.{2}";
    return line.split(regex);
  }

  protected Optional<String> extractAccessedUrl(String log) {

    Pattern pattern = Pattern.compile("GET\\s(.*)\\sHTTP/1.1");
    Matcher matcher = pattern.matcher(log);
    if (matcher.find()) {
      return Optional.of(matcher.group(1));
    }
    return Optional.empty();
  }

  void printStatistics() {
    getMostVisited3Urls(ipAddressUrlMap).forEach(System.out::println);
    getTop3ActiveIpAddresses(ipAddressUrlMap).forEach(System.out::println);
    System.out.println(getTotalUniqueIpAddresses(ipAddressUrlMap));
  }

  protected int getTotalUniqueIpAddresses(Map<String, List<String>> stringListMap) {
    return stringListMap.size();
  }

  protected List<String> getMostVisited3Urls(Map<String, List<String>> stringListMap) {
    Stream<List<String>> stream = stringListMap.values().stream();
    return stream.flatMap(Collection::stream)
        .collect(groupingBy(Function.identity(), counting()))
        .entrySet().stream().sorted((o1, o2) -> Long.compare(o2.getValue(), o1.getValue()))
        .limit(3)
        .map(Entry::getKey).collect(Collectors.toList());
  }

  protected List<String> getTop3ActiveIpAddresses(Map<String, List<String>> map) {
    List<String> list = new ArrayList<>();
    map.entrySet().stream()
        .sorted((o1, o2) -> Integer.compare(o2.getValue().size(), o1.getValue().size()))
        .limit(3)
        .forEachOrdered(c -> list.add(c.getKey()));
    return list;
  }
}