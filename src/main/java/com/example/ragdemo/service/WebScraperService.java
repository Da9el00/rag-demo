package com.example.ragdemo.service;

import com.example.ragdemo.models.PageContent;
import com.example.ragdemo.models.Section;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Holds the page title (from the first <h1>, if any)
 * and the list of heading→text sections.
 */

@Service
public class WebScraperService {

  private static final String CHROME_UA =
          "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                  + "AppleWebKit/537.36 (KHTML, like Gecko) "
                  + "Chrome/116.0.0.0 Safari/537.36";

  public PageContent scrapePage(String url) {
    try {
      // get cookies
      Connection.Response init = Jsoup.connect("https://www.coolshop.dk")
              .userAgent(CHROME_UA)
              .timeout(10_000)
              .method(Connection.Method.GET)
              .execute();

      // fetch the real page
      Connection.Response res = Jsoup.connect(url)
              .userAgent(CHROME_UA)
              .referrer("https://www.google.com/")
              .header("Accept-Language", "en-US,en;q=0.9")
              .cookies(init.cookies())
              .timeout(10_000)
              .ignoreHttpErrors(true)
              .execute();

      if (res.statusCode() == 403) {
        return new PageContent("", Collections.emptyList());
      }

      Document doc = res.parse();
      doc.select("script, style, nav, footer, header, .advertisement, .sidebar").remove();

      // 1) pull the <h1> if it exists
      Element h1 = doc.selectFirst("h1");
      String title = h1 != null ? h1.text().trim() : "";

      // 2) choose <article> if possible
      Element container = doc.selectFirst("article");
      if (container == null) container = doc.body();

      // 3) walk and build sections
      List<Section> sections = new ArrayList<>();
      Deque<String> headingStack = new ArrayDeque<>();
      headingStack.push("INTRO");
      StringBuilder buf = new StringBuilder();

      collectSections(container, sections, headingStack, buf);

      // 4) flush last buffer
      sections.add(new Section(headingStack.peek(), buf.toString().trim()));

      return new PageContent(title, sections);

    } catch (IOException e) {
      return new PageContent("", Collections.emptyList());
    }
  }

  private void collectSections(Element root,
                               List<Section> sections,
                               Deque<String> headingStack,
                               StringBuilder buf) {
    for (Node node : root.childNodes()) {
      if (node instanceof Element el) {
        if (el.tagName().matches("h[1-6]")) {
          sections.add(new Section(headingStack.peek(), buf.toString().trim()));
          buf.setLength(0);
          headingStack.push(el.text());
        } else {
          collectSections(el, sections, headingStack, buf);
        }
      } else if (node instanceof TextNode tn) {
        buf.append(tn.text()).append(" ");
      }
    }
  }
}
