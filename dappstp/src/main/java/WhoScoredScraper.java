import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
public class WhoScoredScraper {
    public static void main(String[] args) {
        try {
            String url = "https://es.whoscored.com/statistics";
            
            // Simular una petición de navegador
            Document doc = Jsoup.connect(url)
            .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/98.0.4758.102 Safari/537.36")
            .referrer("https://www.google.com/")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Accept-Language", "es-ES,es;q=0.9")
            .header("Connection", "keep-alive")
            .get();

            Element table = doc.getElementById("top-team-stats-summary-grid");
            if (table == null) {
                System.out.println("No se encontró la tabla.");
                return;
            }

            Elements rows = table.select("tbody tr");
            for (Element row : rows) {
                Elements cols = row.select("td");
                if (cols.size() < 9) continue;

                String equipo = cols.get(0).text();
                String campeonato = cols.get(1).text();
                String goles = cols.get(2).text();
                String tirosPorPartido = cols.get(3).text();
                String disciplina = cols.get(4).text();
                String posesion = cols.get(5).text();
                String aciertoPase = cols.get(6).text();
                String aereos = cols.get(7).text();
                String rating = cols.get(8).text();

                System.out.println("Equipo: " + equipo);
                System.out.println("Campeonato: " + campeonato);
                System.out.println("Goles: " + goles);
                System.out.println("Tiros por partido: " + tirosPorPartido);
                System.out.println("Disciplina: " + disciplina);
                System.out.println("Posesión: " + posesion);
                System.out.println("Acierto de pase: " + aciertoPase);
                System.out.println("Aéreos: " + aereos);
                System.out.println("Rating: " + rating);
                System.out.println("---------------------------------------------------");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}