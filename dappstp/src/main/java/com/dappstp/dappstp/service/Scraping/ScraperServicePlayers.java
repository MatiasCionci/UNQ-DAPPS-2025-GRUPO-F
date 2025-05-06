
package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class ScraperServicePlayers {

    private static final String TARGET_URL = 
        "https://www.whoscored.com/teams/65/show/spain-barcelona";
    private static final String PROXY_USER = "90ce8794884d97bd268a162f69961e6cb08c827a";
    private static final String PROXY_PASS = "";
    private static final String PROXY_HOST = "api.zenrows.com";
    private static final int    PROXY_PORT = 8001;

    private final PlayersRepository playerRepository;

    public ScraperServicePlayers(PlayersRepository playerRepository) {
        this.playerRepository = playerRepository;
    }

    @Transactional
    public List<Players> scrapeAndSavePlayers() {
        try {
            ignoreCertWarning();

            // Proxy Basic Auth header
            String userInfo = PROXY_USER + ":" + PROXY_PASS;
            String basicAuth = Base64
                .getEncoder()
                .encodeToString(userInfo.getBytes(StandardCharsets.UTF_8));

            // Configure HTTP client with proxy
            Executor exec = Executor.newInstance()
                .authPreemptiveProxy(new HttpHost(PROXY_HOST, PROXY_PORT))
                .auth(new HttpHost(PROXY_HOST, PROXY_PORT),
                      PROXY_USER, PROXY_PASS.toCharArray());

            // Execute request via proxy
            String html = exec.execute(Request.get(TARGET_URL)
                    .viaProxy(new HttpHost(PROXY_HOST, PROXY_PORT))
                    .addHeader("Proxy-Authorization", "Basic " + basicAuth)
                    .connectTimeout(Timeout.ofSeconds(30))
                    .responseTimeout(Timeout.ofSeconds(60))
                )
                .returnContent()
                .asString();

            log.info("🔍 HTML obtenido ({} bytes)", html.length());

            // Parse with Jsoup
            Document doc = Jsoup.parse(html);
            Elements rows = doc.select("tbody#player-table-statistics-body tr");
            List<Players> players = new ArrayList<>();

            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 5) continue;

                String name    = extractName(cols.get(0));
                String matches = cols.get(4).text();
                int goals      = parseIntSafe(cols.get(6).text());
                int assists    = parseIntSafe(cols.get(7).text());
                double rating  = parseDoubleSafe(
                    cols.size() > 14 ? cols.get(14).text()
                                     : cols.last().text()
                );

                Players p = new Players();
                p.setName(name);
                p.setMatches(matches);
                p.setGoals(goals);
                p.setAssists(assists);
                p.setRating(rating);
                players.add(p);
            }

            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores.");
            }
            return players;

        } catch (Exception e) {
            log.error("Error en scraping con proxy:", e);
            return List.of();
        }
    }

    private static void ignoreCertWarning() throws Exception {
        SSLContext ctx = SSLContext.getInstance("TLS");
        TrustManager[] trustAll = new X509TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() { return null; }
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
            }
        };
        ctx.init(null, trustAll, null);
        SSLContext.setDefault(ctx);
    }

    private String extractName(Element cell) {
        try {
            Element link = cell.selectFirst("a.player-link");
            Element span = link.selectFirst("span.iconize-icon-left");
            String txt = (span != null ? span.text() : "").trim();
            return txt.isEmpty() ? link.text().trim() : txt;
        } catch (Exception e) {
            String[] parts = cell.text().split("\\R");
            return parts.length > 1 ? parts[1].trim() : parts[0].trim();
        }
    }

    private int parseIntSafe(String txt) {
        String n = txt.replaceAll("[^0-9]", "");
        return n.isEmpty() ? 0 : Integer.parseInt(n);
    }

    private double parseDoubleSafe(String txt) {
        String clean = txt.replace(",", ".").replaceAll("[^0-9.]", "");
        int i = clean.indexOf('.');
        if (i >= 0) {
            int j = clean.indexOf('.', i+1);
            if (j >= 0) clean = clean.substring(0, j);
        }
        return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
    }
}
