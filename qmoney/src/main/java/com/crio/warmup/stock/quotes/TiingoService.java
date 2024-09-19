package com.crio.warmup.stock.quotes;

import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.constant.Tiingo;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.client.RestTemplate;

public class TiingoService implements StockQuotesService {

  private RestTemplate restTemplate;
  private static String token=Tiingo.TIINGO_TOKEN;

  private static String getToken(){
    return token;
  }

  protected TiingoService(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }

  @Override
  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
    
        String url=buildUri(symbol, from, to);  
        String candlesString=restTemplate.getForObject(url,String.class);
        TiingoCandle[] candles=getObjectMapper().readValue(candlesString,TiingoCandle[].class);

        return Arrays.asList(candles);
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  private String buildUri(String symbol, LocalDate startDate, LocalDate endDate){

    String uriTemplate = "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token="+getToken();
    return uriTemplate;
  }
}
