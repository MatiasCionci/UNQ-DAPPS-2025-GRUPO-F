package com.dappstp.dappstp.service.Scraping;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class JsoupBarcelonaPlayers {
    public static void main(String[] args) throws Exception {
        String url = "https://es.whoscored.com/Teams/65/Show/Spain-Barcelona";
        Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
            .timeout(60000)
            .get();

        Element tbody = doc.getElementById("player-table-statistics-body");
        if (tbody == null) {
            System.err.println("No encontré la tabla de jugadores.");
            return;
        }

        Elements rows = tbody.select("tr");
        for (Element row : rows) {
            Elements cols = row.select("td");
            if (cols.size() < 15) continue;

            // Nombre (segunda línea del primer td)
            String[] lines = cols.get(0).text().split("\\n");
            String name = lines.length>1 ? lines[1].trim() : lines[0].trim();
            String matches = cols.get(4).text().trim();
            String goals   = cols.get(6).text().trim();
            String assists = cols.get(7).text().trim();
            String rating  = cols.get(14).text().trim();

            System.out.printf("%s — Jugados: %s, Goles: %s, Asis: %s, Rating: %s%n",
                              name, matches, goals, assists, rating);
        }
    }
}

