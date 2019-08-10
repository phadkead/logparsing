package com;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.Test;

public class LogParserTest {

  private static final String path = "src/test/resources/test.log";

  @Test
  public void shouldReadTheLogFile() {
    LogParser parser = new LogParser("src/test/resources/test.log");
    parser.readFile();
  }

  @Test
  public void shouldExtractIPAddress() {
    //given
    LogParser parser = new LogParser(path);
    String givenUrl = "177.71.128.21 - - "
        + "[10/Jul/2018:22:21:28 +0200] \"GET /intranet-analytics/ HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0 (X11; U; Linux x86_64; fr-FR) AppleWebKit/534.7 (KHTML, like Gecko) Epiphany/2.30.6 Safari/534.7\"\n";

    //when
    String[] result = parser.splitFromIpAddress(
        givenUrl);
    //then
    assertEquals("177.71.128.21", result[0]);

    //given
    String givenWrongUrl = "50.112.00.11 - admin [11/Jul/2018:17:31:56 +0200] "
        + "\"GET /asset.js HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/536.6 (KHTML, like Gecko) Chrome/20.0.1092.0 Safari/536.6\"\n";
    //when
    String[] result2 = parser.splitFromIpAddress(givenWrongUrl);
    //then
    assertEquals("50.112.00.11", result2[0]);
  }

  @Test
  public void shouldGiveErrorIfNoIPAddress() {
    LogParser parser = new LogParser(path);
    try {
      String given = "[10/Jul/2018:22:21:28 +0200] "
          + "\"GET /intranet-analytics/ HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0 (X11; U; Linux x86_64; fr-FR) AppleWebKit/534.7 (KHTML, like Gecko) Epiphany/2.30.6 Safari/534.7\"\n";
      parser.parseLine(given);
      fail("Expected an Exception to be thrown");

    } catch (FileParseException ex) {
      assertEquals(ex.getMessage(), "Cannot extract ip address");
    }
  }

  @Test
  public void shouldExtractURL() {
    LogParser parser = new LogParser(path);
    Optional<String> actual = parser.extractAccessedUrl("[10/Jul/2018:22:21:28 +0200] "
        + "\"GET /intranet-analytics/ HTTP/1.1\" 200 3574 \"-\" \"Mozilla/5.0 (X11; U; Linux x86_64; fr-FR) AppleWebKit/534.7 (KHTML, like Gecko) Epiphany/2.30.6 Safari/534.7");

    assertTrue(actual.isPresent());
    assertEquals("/intranet-analytics/", actual.get());
  }

  @Test
  public void shouldGetMostVisitedUrls() {
    Map<String, List<String>> map = Map.of(
        "50.112.00.11",
        List.of("/intranet-analytics", "/intranet-analytics", "/intranet-analytics", "/test",
            "/logout"),
        "12.12.12.12", List.of("/intranet-analytics", "/a2", "/a3", "/test", "/logout", "/logout"));

    LogParser parser = new LogParser(path);

    List<String> actual = parser.getMostVisited3Urls(map);
    assertArrayEquals(
        List.of("/intranet-analytics", "/logout", "/test").toArray(),
        actual.toArray());
  }

  @Test
  public void shouldGetNumberOfUniqueIpAddresses() {
    Map<String, List<String>> map = Map.of(
        "50.112.00.11",
        List.of("/intranet-analytics", "/intranet-analytics", "/intranet-analytics", "/test",
            "/logout"),
        "12.12.12.12", List.of("/intranet-analytics", "/a2", "/a3", "/test", "/logout", "/logout"));

    LogParser parser = new LogParser(path);

    int actual = parser.getTotalUniqueIpAddresses(map);
    assertEquals(2, actual);
  }

  @Test
  public void shouldGetTop3ActiveIpAddresses() {
    Map<String, List<String>> map = Map.of(
        "50.112.00.11", List.of("/intranet", "/a1", "/intranet", "/test", "/a2"),
        "12.12.12.12", List.of("/intranet-analytics", "/a2", "/a3", "/test", "/logout", "/logout"),
        "12.12.12.13", List.of("/intranet-analytics", "/logout"),
        "12.12.12.11", List.of("/intranet-analytics", "/a2", "/a3", "/test"),
        "12.12.12.10", List.of("/intranet-analytics", "/a2"));

    LogParser parser = new LogParser(path);

    List<String> actual = parser.getTop3ActiveIpAddresses(map);
    assertArrayEquals(new String[]{"12.12.12.12", "50.112.00.11", "12.12.12.11"}, actual.toArray());
  }

}
