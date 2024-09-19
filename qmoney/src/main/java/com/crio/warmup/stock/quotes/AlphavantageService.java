
package com.crio.warmup.stock.quotes;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import com.crio.warmup.stock.dto.AlphavantageCandle;
import com.crio.warmup.stock.constant.Alphavantage;
import com.crio.warmup.stock.dto.AlphavantageDailyResponse;
import com.crio.warmup.stock.dto.Candle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.cglib.core.Local;
import org.springframework.web.client.RestTemplate;

public class AlphavantageService implements StockQuotesService {

  private RestTemplate restTemplate;
  private static String API_KEY=Alphavantage.ALPHAVANTAGE_API_KEY;
  
  
  
  protected AlphavantageService(RestTemplate restTemplate){
    this.restTemplate=restTemplate;
  }
  
  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
  
    // build the url 
    String url = buildUri(symbol);


    // Using the url to get the Candles 
    AlphavantageDailyResponse alphavantageResponse=this.restTemplate.getForObject(url,AlphavantageDailyResponse.class);

    // alphavantage response contains a map with day of trade mapped to corrsponding  high , low, close value. 

    // The candle we need is a list of all candles between startDate (from) and endDate(to), 

    List<Candle> candles = getCandlesFromAlphavantageResponse(alphavantageResponse,from,to);
      
    
    return candles;
  }
  

  private List<Candle> getCandlesFromAlphavantageResponse(
      AlphavantageDailyResponse alphavantageResponse,LocalDate startDate,LocalDate endDate) {
     

    List<Candle> candles=new ArrayList<>();
    
    // iterate the map 
    for(Map.Entry<LocalDate,AlphavantageCandle> entry:alphavantageResponse.getCandles().entrySet()){

      LocalDate currentDate=entry.getKey();

      if(currentDate.isEqual(startDate)||currentDate.isEqual(endDate)||currentDate.isAfter(startDate)&&currentDate.isBefore(endDate)){
        AlphavantageCandle alphavantageCandle=entry.getValue();
        alphavantageCandle.setDate(currentDate);
        candles.add(alphavantageCandle);
      }

    }
      
    // Sort the result based on dates 
    Collections.sort(candles,getAlphavantageComparator());
    
    return candles;
  }


  private Comparator<Candle> getAlphavantageComparator(){
      
    Comparator<Candle> alphavantageComparator = new Comparator<Candle>() {
      @Override
      public int compare(Candle o1, Candle o2) {
          return o1.getDate().compareTo(o2.getDate());
      }
    };

    return alphavantageComparator;
  }

  





  private String buildUri(String symbol){

     // Sample URL to fetch historical(20+ years) daily trade data  for a given stock 
     // https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol=IBM&outputsize=full&apikey=ORJQUREI48OGJXRS

     String url="https://www.alphavantage.co/query?function=TIME_SERIES_DAILY&symbol="+symbol+"&outputsize=full&apikey="+API_KEY;

     return url;
  }

}

