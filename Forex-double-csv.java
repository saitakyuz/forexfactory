import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.URL;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Forex {


    private List<String> cookies;
    private HttpsURLConnection conn;
    private static String lastOpenTread;
    private static String lastCloseTread;
    private static String symbolFilter = "";
    private static String openTreadFileName="C4F.csv";
    private static String closeTreadFileName="C4FClose.csv";

    private final String USER_AGENT = "Mozilla/5.0";

    public static void main(String[] args) throws Exception {

        String ozetUrl = "https://www.forexfactory.com/trades.php";

        Forex forex = new Forex();

        CookieHandler.setDefault(new CookieManager());
        boolean notYet = true;
        while (notYet) {

            try {

                String result = forex.GetPageContent(ozetUrl);
                notYet = readHtmlAndWriteToExcell(result);
                Thread.sleep(1 * 1000);

            }catch (Exception e) {
                System.out.println(e.toString());
            }


        }
        System.out.println(forex.toString());
    }

    private static boolean readHtmlAndWriteToExcell(String result) {
        Document doc = Jsoup.parse(result);

        try {
            Elements elements = doc.getElementsByClass("trades_activity__table");
            List<TradeInfo> openTradeInfoList = new ArrayList<TradeInfo>();
            List<TradeInfo> closeTradeInfoList = new ArrayList<TradeInfo>();

            for (Element element : elements) {

                Elements aElements = element.getElementsByClass("trades_activity__row");
                for (Element aElement : aElements) {

                    Element trade = aElement.getElementsByClass("trades_activity__overview").first();
                    Element tradeActivityInfo = aElement.getElementsByClass("trades_activity__info").first().select("a").first();
                    Element traderT = aElement.getElementsByClass("trader").first().select("a").last();
                    Element tradeReturnT = aElement.getElementsByClass("trades_activity__cell--return").first().select("span").first();
                    Element tradePipsT = aElement.getElementsByClass("trades_activity__cell--pips").first().select("span").first();
                    String tradeDateMs = aElement.getElementsByClass("trades_activity__row").first().attr("id");
                    String[] tradePariteT = trade.select("strong").first().text().split("\\<");
                    String[] tradeT = tradePariteT[0].split(" ");
                    String[] tradeDateMsArray = tradeDateMs.split("\\_");
                    if (tradeDateMsArray[2].equalsIgnoreCase(lastOpenTread) || tradeDateMsArray[2].equalsIgnoreCase(lastCloseTread)) {
                        break;
                    }
                    String tradePariteTemp = tradeT[0] ;
                    tradePariteTemp = tradePariteTemp.replace("/","");

                    if(symbolFilter.isEmpty() || symbolFilter.equalsIgnoreCase(tradePariteTemp)){
                        TradeInfo tradeInfo = new TradeInfo();

                        if ("BUY".equalsIgnoreCase(trade.getElementsByClass("trades_activity__direction").first().text())){
                            if("Opened Long".equalsIgnoreCase(tradeActivityInfo.text())){
                                tradeInfo.setTradeTypeControl("1");
                            }else if ("Scaled In".equalsIgnoreCase(tradeActivityInfo.text())){
                                tradeInfo.setTradeTypeControl("1");
                            }
                            else if("Closed Short".equalsIgnoreCase(tradeActivityInfo.text())){
                                tradeInfo.setTradeTypeControl("99");
                            }else if ( "Scaled Out".equalsIgnoreCase(tradeActivityInfo.text())){
                                tradeInfo.setTradeTypeControl("99");
                            }


                        }else if("SELL".equalsIgnoreCase(trade.getElementsByClass("trades_activity__direction").first().text())){

                            if("Opened Short".equalsIgnoreCase(tradeActivityInfo.text())){
                                tradeInfo.setTradeTypeControl("0");
                            }else if("Scaled In".equalsIgnoreCase(tradeActivityInfo.text())){
                                tradeInfo.setTradeTypeControl("0");
                            } else if("Closed Long".equalsIgnoreCase(tradeActivityInfo.text())){
                                tradeInfo.setTradeTypeControl("99");
                            }else if ("Scaled Out".equalsIgnoreCase(tradeActivityInfo.text())){
                                tradeInfo.setTradeTypeControl("99");
                            }

                        }



                        tradeInfo.setTradeDate(convertDate(tradeDateMsArray[3] + "000"));
                        tradeInfo.setTradeId(tradeDateMsArray[2]);
                        tradeInfo.setTradeName(trade.getElementsByClass("trades_activity__direction").first().text());
                        tradeInfo.setTradeParite(tradePariteTemp);
                        tradeInfo.setTradePips(tradePipsT.text());
                        tradeInfo.setTradePrice(tradeT[2].replace("~",""));
                        tradeInfo.setTrader(traderT.text());
                        tradeInfo.setTradeReturn(tradeReturnT.text());
                        tradeInfo.setTradeType(tradeActivityInfo.text());


                        //kapamasi gelen orderi sil
                        if (tradeInfo.getTradeTypeControl().equalsIgnoreCase("99")){
                            closeTradeInfoList.add(tradeInfo);
                        }else{

                            openTradeInfoList.add(tradeInfo);
                        }

                        System.out.println(tradeInfo.toString());
                    }

                }


                if (openTradeInfoList.size() > 0) {
                    CsvFileWriter csvFileWriter = new CsvFileWriter(openTreadFileName);
                    csvFileWriter.writeCsvFile(openTradeInfoList);
                    lastOpenTread = openTradeInfoList.get(0).getTradeId();
                }

                if (closeTradeInfoList.size() > 0) {
                    CsvFileWriter csvFileWriter = new CsvFileWriter(closeTreadFileName);
                    csvFileWriter.writeCsvFile(closeTradeInfoList);
                    lastCloseTread = closeTradeInfoList.get(0).getTradeId(); //emin olamadim kapattım
                }


            }

        } catch (Exception e) {
            System.out.println(e.toString());
        }

        return true;
    }

    private static String convertDate(String date) {
        Timestamp timestamp = new Timestamp(new Long(date));
        Date currentDate = new Date(timestamp.getTime());
        DateFormat df = new SimpleDateFormat("dd:MM:yy HH:mm");
        return df.format(currentDate);
    }

    private String GetPageContent(String url) throws Exception {

        URL obj = new URL(url);
        conn = (HttpsURLConnection) obj.openConnection();

        // default is GET
        conn.setRequestMethod("GET");

        conn.setUseCaches(false);

        // act like a browser
        conn.setRequestProperty("User-Agent", USER_AGENT);
        conn.setRequestProperty("Accept",
                "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        conn.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
        if (cookies != null) {
            for (String cookie : this.cookies) {
                conn.addRequestProperty("Cookie", cookie.split(";", 1)[0]);
            }
        }
        int responseCode = conn.getResponseCode();
        System.out.println("\nSending 'GET' request to URL : " + url);
        System.out.println("Response Code : " + responseCode);
        System.out.println("Time : " + new Date());

        BufferedReader in =
                new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        // Get the response cookies
        setCookies(conn.getHeaderFields().get("Set-Cookie"));

        return response.toString();

    }

    public List<String> getCookies() {
        return cookies;
    }

    public void setCookies(List<String> cookies) {
        this.cookies = cookies;
    }

}

