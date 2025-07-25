package org.scoula.domain.chat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.springframework.stereotype.Component;

@Component
public class BadWordFilter {

      private List<String> badWords;

      @PostConstruct
      public void loadBadWords() {
          InputStream inputStream = getClass().getResourceAsStream("/badwords.txt");
          if (inputStream == null) {
              throw new RuntimeException("badwords.txt 파일을 찾을 수 없습니다.");
          }

          try (BufferedReader reader =
                  new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
              badWords =
                      reader.lines()
                              .map(String::toLowerCase)
                              .map(line -> line.replaceAll("\\s+", ""))
                              .collect(Collectors.toList());

          } catch (Exception e) {
              throw new RuntimeException("badwords.txt 로딩 실패", e);
          }
      }

      public boolean containsBadWord(String text) {
          if (text == null || text.isBlank()) return false;
          String normalized = text.toLowerCase().replaceAll("\\s+", "");
          return badWords.stream().anyMatch(normalized::contains);
      }
}
