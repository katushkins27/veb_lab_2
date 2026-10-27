import com.fastcgi.FCGIInterface;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class FastCGIServer {
    public static void main(String[] args){
        FCGIInterface fcgiInterface = new FCGIInterface();
        while (fcgiInterface.FCGIaccept() >= 0){
            long startTime = System.nanoTime();
            try {
                String requestMethod = System.getProperty("REQUEST_METHOD", "GET");
                String queryString = System.getProperty("QUERY_STRING", "");

                if (!"GET".equalsIgnoreCase(requestMethod)) {
                    sendErrorResponse(405, "Method GET is only supported.");
                    continue;
                }

                if (queryString == null || queryString.isEmpty()) {
                    sendErrorResponse(400, "Parametrs x, y, r are required.");
                    continue;
                }

                Map<String, String> parameters = parseQueryString(queryString);
                if (!parameters.containsKey("x") || !parameters.containsKey("y") || !parameters.containsKey("r")){
                    sendErrorResponse(400, "Have no one of the required parameters: x,y or r.");
                    continue;
                }
                String xRaw = parameters.get("x").trim().replace(",", ".");
                String yRaw = parameters.get("y").trim().replace(",", ".");
                String rRaw = parameters.get("r").trim().replace(",", ".");

                if (!isValidX(xRaw) || !isValidY(yRaw) || !isValidR(rRaw)) {
                    sendErrorResponse(422, "One of the required parameters not within the specified range.");
                    continue;
                }

                BigDecimal x = new BigDecimal(xRaw);
                BigDecimal y = new BigDecimal(yRaw);
                BigDecimal r = new BigDecimal(rRaw);

                boolean hit = checkArea(x, y, r);

                long executionTimeNano = System.nanoTime() - startTime;
                long serverTimestamp = System.currentTimeMillis();
                //String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));

                String jsonResponse = String.format("{\"x\": %s, \"y\": \"%s\", \"r\": %s, \"hit\": %b, \"currentTime\": %d, \"executionTimeNano\": %d}",
                        x.toPlainString(),
                        y.toPlainString(),
                        r.toPlainString(),
                        hit,
                        serverTimestamp,
                        executionTimeNano);

                sendJsonResponse(200, jsonResponse);
            } catch (Exception e) {
                sendErrorResponse(500, "Server Error" + e.getMessage());
            }
        }
    }
    private static void sendErrorResponse(int status, String message) {
        String json = "{\"error\": \"" + message.replace("\"", "\\\"") + "\"}";
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 " + status + "\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Content-Length: " + bytes.length + "\r\n" +
                        "\r\n" +
                        json;
        System.out.print(response);
        System.out.flush();
    }

    private static Map<String, String> parseQueryString(String query) {
        Map<String, String> parametrs = new HashMap<>();
        String[] pairs = query.split("&");
        for (String pair: pairs){
            int index = pair.indexOf("=");
            if (index > 0 && index < pair.length()-1){
                parametrs.put(pair.substring(0, index), pair.substring(index+1));
            }
        }
        return parametrs;
    }

    private static boolean isValidX(String xStr){
        try{
            BigDecimal x = new BigDecimal(xStr);
            String[] allowed = {"-2", "-1.5", "-1", "-0.5", "0", "0.5", "1", "1.5", "2"};
            for (String val: allowed){
                if (x.compareTo(new BigDecimal(val)) == 0) return true;
            }
        } catch (Exception ignored){}
        return false;
    }

    private static boolean isValidY(String yStr){
        try{
            BigDecimal y = new BigDecimal(yStr);
            BigDecimal min = new BigDecimal("-3");
            BigDecimal max = new BigDecimal("5");
            return y.compareTo(min) > 0 && y.compareTo(max) < 0;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static boolean isValidR(String rStr){
        try{
            BigDecimal r = new BigDecimal(rStr);
            String[] allowed = {"1", "1.5", "2", "2.5", "3"};
            for (String val: allowed){
                if (r.compareTo(new BigDecimal(val)) == 0) return true;
            }
        } catch (Exception ignored){}
        return false;
    }

    private static boolean checkArea(BigDecimal x, BigDecimal y, BigDecimal r){
        BigDecimal zero = BigDecimal.ZERO;
        BigDecimal two = new BigDecimal("2");
        BigDecimal rHalf = r.divide(two);

        int xCmp = x.compareTo(zero);
        int yCmp = y.compareTo(zero);
        //прямоугольник
        if (xCmp >= 0 && yCmp>=0) {
            return x.compareTo(rHalf) <= 0 && y.compareTo(r) <= 0;
        }
        //пустая вторая четверть
        if (xCmp <0 && yCmp >0){
            return false;
        }
        //сектор
        if (xCmp <= 0 && yCmp <= 0) {
            BigDecimal leftSide = x.multiply(x).add(y.multiply(y));
            BigDecimal rightSide = r.multiply(r);
            return leftSide.compareTo(rightSide) <= 0;
        }
        //треугольник
        if (xCmp >= 0 && yCmp <= 0) {
            BigDecimal line = x.multiply(two).subtract(r);
            return y.compareTo(line) >= 0;
        }
        return false;
    }
    private static void sendJsonResponse(int status, String jsonBody){
        byte[] bytes = jsonBody.getBytes(StandardCharsets.UTF_8);
        String response = "HTTP/1.1 " + status + " OK\r\n" +
                        "Content-Type: application/json; charset=UTF-8\r\n" +
                        "Content-Length: " + bytes.length + "\r\n" +
                        "\r\n"+
                        jsonBody;
        System.out.print(response);
        System.out.flush();
    }
}
