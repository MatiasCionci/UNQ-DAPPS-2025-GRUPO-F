package com.dappstp.dappstp.service.Scraping;

import com.dappstp.dappstp.model.Players;
import com.dappstp.dappstp.repository.PlayersRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.fluent.Executor;
import org.apache.hc.client5.http.fluent.Request;
import org.apache.hc.core5.http.HttpHost;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.net.URI;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
@Slf4j
public class ScraperServicePlayers {

    private final PlayersRepository playerRepository;

    /** 
     * Aquí ponés tu URI de proxy ZenRows, con tu API key como usuario:
     *    http://APIKEY:@api.zenrows.com:8001
     */
    private static final String ZENROWS_PROXY_URI = "http://90ce8794884d97bd268a162f69961e6cb08c827a:@api.zenrows.com:8001";
    /** URL a raspar */
    private static final String TARGET_URL = "https://www.whoscored.com/teams/65/show/spain-barcelona";

    public ScraperServicePlayers(PlayersRepository playerRepository) {
        this.playerRepository = playerRepository;
        ignoreCertWarning();
    }

    @Transactional
    public List<Players> scrapeAndSavePlayers() {
        List<Players> players = new ArrayList<>();
        try {
            // 1) Descarga el HTML a través de ZenRows proxy
            String html = fetchViaZenRows(TARGET_URL, ZENROWS_PROXY_URI);
            log.info("🔍 Fragmento de página (primeros 2000 chars):\n{}",
                     html.substring(0, Math.min(2000, html.length())));

            // 2) Parseo con Jsoup
            Document doc = Jsoup.parse(html);
            Elements rows = doc.select("tbody#player-table-statistics-body tr");
            log.info("🎯 Filas encontradas: {}", rows.size());

            // 3) Extraer datos de cada fila
            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 5) continue;

                String name = extractName(cols.get(0));
                String matches = cols.get(4).text().trim();
                int goals = parseIntSafe(cols.get(6).text());
                int assists = parseIntSafe(cols.get(7).text());
                double rating = parseDoubleSafe(
                    cols.size() > 14 ? cols.get(14).text() : cols.last().text()
                );

                Players p = new Players();
                p.setName(name);
                p.setMatches(matches);
                p.setGoals(goals);
                p.setAssists(assists);
                p.setRating(rating);
                players.add(p);
            }

            // 4) Guardar en BD
            if (!players.isEmpty()) {
                playerRepository.saveAll(players);
                log.info("✅ {} jugadores guardados.", players.size());
            } else {
                log.warn("⚠️ No se procesaron jugadores.");
            }

        } catch (Exception e) {
            log.error("Error en scraping:", e);
        }
        return players;
    }

    /**  
     * Realiza la petición GET al target URL usando ZenRows como proxy con autenticación.
     */
    private String fetchViaZenRows(String targetUrl, String proxyUriString) throws Exception {
        URI proxyUri = new URI(proxyUriString);
        // userInfo viene como "APIKEY:"
        String[] userInfo = proxyUri.getUserInfo().split(":", 2);
        String user = userInfo[0];
        String pass = userInfo.length > 1 ? userInfo[1] : "";
        String basicAuth = Base64.getEncoder()
                                .encodeToString(proxyUri.getUserInfo().getBytes());

        return Executor.newInstance()
            .auth(new HttpHost(proxyUri.getHost(), proxyUri.getPort()), user, pass.toCharArray())
            .authPreemptiveProxy(new HttpHost(proxyUri.getHost(), proxyUri.getPort()))
            .execute(Request.get(targetUrl)
                .addHeader("Proxy-Authorization", "Basic " + basicAuth)
                .viaProxy(HttpHost.create(proxyUriString))
            )
            .returnContent()
            .asString();
    }

    /**
     * Ignora warnings de certificados SSL para que no reviente al conectar al proxy.
     */
    private static void ignoreCertWarning() {
        TrustManager[] trustAllCerts = new X509TrustManager[]{ new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() { return null; }
            public void checkClientTrusted(X509Certificate[] certs, String authType) { }
            public void checkServerTrusted(X509Certificate[] certs, String authType) { }
        } };
        try {
            SSLContext ctx = SSLContext.getInstance("SSL");
            ctx.init(null, trustAllCerts, null);
            SSLContext.setDefault(ctx);
        } catch (Exception ignored) {}
    }

    // Métodos auxiliares de parseo

    private String extractName(Element cell) {
        try {
            Element span = cell.selectFirst("a.player-link span.iconize-icon-left");
            if (span != null && !span.text().isBlank()) {
                return span.text().trim();
            }
            return cell.selectFirst("a.player-link").text().trim();
        } catch (Exception e) {
            String[] parts = cell.text().split("\\R");
            return parts.length > 1 ? parts[1].trim() : parts[0].trim();
        }
    }

    private int parseIntSafe(String txt) {
        String num = txt.replaceAll("[^\\d]", "");
        return num.isEmpty() ? 0 : Integer.parseInt(num);
    }

    private double parseDoubleSafe(String txt) {
        String clean = txt.replace(",", ".").replaceAll("[^\\d.]", "");
        int i = clean.indexOf('.');
        if (i >= 0) {
            int j = clean.indexOf('.', i + 1);
            if (j >= 0) clean = clean.substring(0, j);
        }
        return clean.isEmpty() ? 0.0 : Double.parseDouble(clean);
    }
}
