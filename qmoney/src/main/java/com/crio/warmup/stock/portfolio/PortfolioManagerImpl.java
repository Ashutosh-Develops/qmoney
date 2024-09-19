
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {


   private RestTemplate restTemplate; 
   private StockQuotesService stockQuotesService;
   private static String token="bc9f1069a0ddd9726cfd4b54244617ed93918295";


   private static String getToken(){
      return token;
   }


  // Caution: Do not delete or modify the constructor, or else your build will break!
  // This is absolutely necessary for backward compatibility
  protected PortfolioManagerImpl(RestTemplate restTemplate,StockQuotesService stockQuotesService) {
    this.restTemplate = restTemplate;
    this.stockQuotesService=stockQuotesService;
  }

  protected PortfolioManagerImpl(RestTemplate restTemplate){
    this.restTemplate=restTemplate;
  }


  protected PortfolioManagerImpl(StockQuotesService stockQuotesService){
    this.stockQuotesService=stockQuotesService;
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws JsonProcessingException {
      
      // Stock quote service to use either tiingo or Alphavantage . 
      List<Candle> candles=this.stockQuotesService.getStockQuote(symbol, from, to);
      return candles;
  }

  private List<Candle> fetchCandlesFromTiingo(String url){
    Candle[] candles=this.restTemplate.getForObject(url,TiingoCandle[].class);
    return Arrays.asList(candles);
  }

  protected String buildUri(String symbol, LocalDate startDate, LocalDate endDate) {
     
       String uriTemplate = "https://api.tiingo.com/tiingo/daily/"+symbol+"/prices?startDate="+startDate.toString()+"&endDate="+endDate.toString()+"&token="+getToken();
       return uriTemplate;
  }


  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate) throws JsonProcessingException{

    // TODO Auto-generated method stub
    List<AnnualizedReturn> annualizedReturns=new ArrayList<>();
    // Iterate through each Portfolio Trade 
    for(PortfolioTrade trade:portfolioTrades){
       // 1. For each portfolio trade get the candles using tiingo API

       List<Candle> candles=getStockQuote(trade.getSymbol(),trade.getPurchaseDate(),endDate);
       if(candles.size()==0)continue;      // Currently bypassing. In future could throw exception if require
       Double openingPrice= getOpeningPriceOnStartDate(candles);
       Double sellingPrice = getClosingPriceOnEndDate(candles);
       // 2. Using the candles calculate the annualized return the trade
       AnnualizedReturn annualizedReturn=calculateAnnualizedReturnHelper(endDate, trade,openingPrice,sellingPrice);

        // 3. Add the Annualized Return to the Annualized returns list
       annualizedReturns.add(annualizedReturn);
      
    }
    // Sort the Annualized Returns List in descending order.  // Default Comparison in Annualized return is in descending order

    Collections.sort(annualizedReturns);
    return annualizedReturns;
  }

  private Double getClosingPriceOnEndDate(List<Candle> candles) {  
    return candles.get(candles.size()-1).getClose();
  }

  private Double getOpeningPriceOnStartDate(List<Candle> candles) { 
    return candles.get(0).getOpen();
  }

  private AnnualizedReturn calculateAnnualizedReturnHelper(LocalDate endDate, PortfolioTrade trade, Double buyPrice, Double sellPrice) {
  
      int stockQuantity= trade.getQuantity();
      Double totalReturn = (sellPrice*stockQuantity - buyPrice*stockQuantity)/(buyPrice*stockQuantity);

      long numOfDays=ChronoUnit.DAYS.between(trade.getPurchaseDate(),endDate);
      Double numOfYears = (numOfDays*1.0)/365;
      Double annualizedReturn = Math.pow((1+totalReturn),(1.0/numOfYears))-1.0;


      return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }

}
