package org.scoula.domain.home.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RootController {

      @GetMapping("/")
      public String root() {
          return "Server is running!";
      }
}
