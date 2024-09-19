package com.crio.warmup.stock;


import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.constant.Tiingo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {

  private static String token=Tiingo.TIINGO_TOKEN;

  public static String getToken(){
    return token;
  }

   static class Stock implements Comparable<Stock>{

      private String stockName;
      private double closingPrice;


      Stock(String stockName,double closingPrice){
         this.stockName=stockName;
         this.closingPrice=closingPrice;
      }

      public String getStockName(){
        return this.stockName;
      }

      public double getClosingPrice(){
        return this.closingPrice;
      }
      
      // Sorts in ascending order 
      public int compareTo(Stock stock){

        if(this.closingPrice<stock.closingPrice){
          return -1;
        }else if(this.closingPrice > stock.closingPrice){
          return 1;
        }else{
          return 0;
        }
      }


   }

   public static List<String> mainReadFile(String[] args) throws StreamReadException, DatabindException, IOException, URISyntaxException{

      if(args.length==0){
        return Arrays.asList("");
      }  

      String fileName=args[0];
      List<PortfolioTrade> portfolioTrades=deserializeTradesFromJsonFile(resolveFileFromResources(fileName));
      List<String> symbols=extractSymbolsFromPortfolioTrades(portfolioTrades);

      return symbols;

   }

  public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {

    // args[0] return the name of the json file
    // args[1] return the end date

    String fileName=args[0];
    String endDate=args[1];
    String token=Tiingo.TIINGO_TOKEN;;

    List<String> closingPriceStocks=new ArrayList<>();
    List<Stock> stocks=new ArrayList<>();

  
    // Get the portfolio trades by deserializing json file ,which is args[0]
  
    List<PortfolioTrade> portfolioTrades= readTradesFromJson(fileName);

    // For each portfolio trade get the stock name 
    for(PortfolioTrade portfolioTrade:portfolioTrades){

        validatePurchaseDate(portfolioTrade,LocalDate.parse(endDate));

        // Get the URL for the portfolio date
        String url=prepareUrl(portfolioTrade,LocalDate.parse(endDate), token);
        
        // Make Get request to tingo using the above url and get the closing price
        String closingPrice=getClosingPriceOfTheStockFromTiingo(url);
        
        // No trade was done on that day
        if(closingPrice.length()==0)continue;

        Stock stock=new Stock(portfolioTrade.getSymbol(),Double.parseDouble(closingPrice));

        // 
        stocks.add(stock);

        // Add the closing price to the closing prices list
       // closingPrices.add(closingPrice);

    }

     // Sort the stocks (We will get stocks with incresing closing price)
     Collections.sort(stocks); 

     for(Stock stock:stocks){
       closingPriceStocks.add(stock.getStockName());
     }
     

    return closingPriceStocks;
  }

  private static void validatePurchaseDate(PortfolioTrade portfolioTrade,LocalDate endTime){
     // if end date is less than purchase Date then throw Exception
     if(endTime.isBefore(portfolioTrade.getPurchaseDate())){
      throw new RuntimeException("Stock purchase date cannot be greater than the specified date");
    }
  }

  private static String getClosingPriceOfTheStockFromTiingo(String url) throws JsonMappingException, JsonProcessingException{


    // Make the rest call 

    HttpHeaders headers=new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    HttpEntity<String> entity=new HttpEntity<String>(headers);
    RestTemplate restTemplate=new RestTemplate();

    String response = restTemplate.exchange(url,HttpMethod.GET,entity,String.class).getBody();

    

    List<Candle> candles=deserializeTiingoResponse(response);
    
    
    String closingPrice=getClosingPriceOnTheEndDay(candles);
    
   
    return closingPrice;

  }

  private static List<Candle> getCandlesFromTheUrl(String url) throws JsonMappingException, JsonProcessingException{

    HttpHeaders headers=new HttpHeaders();
    headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
    HttpEntity<String> entity=new HttpEntity<String>(headers);
    RestTemplate restTemplate=new RestTemplate();

    String response = restTemplate.exchange(url,HttpMethod.GET,entity,String.class).getBody();
    List<Candle> candles=deserializeTiingoResponse(response);

    return candles;    
  }
  

  private static String getClosingPriceOnTheEndDay(List<Candle> candles){

    int n=candles.size();
    String closingPrice=String.valueOf(n==0?"":candles.get(n-1).getClose());
    
    return closingPrice;
  }
  
  
  
  private static List<Candle> deserializeTiingoResponse(String response) throws JsonMappingException, JsonProcessingException{

         ObjectMapper objectMapper=getObjectMapper();

         Candle[] tiingoCandles=objectMapper.readValue(response,TiingoCandle[].class);

         return Arrays.asList(tiingoCandles);
  }

  private static void printJsonObject(Object object) throws IOException {
    Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
    ObjectMapper mapper = new ObjectMapper();
    logger.info(mapper.writeValueAsString(object));
  }

  private static File resolveFileFromResources(String filename) throws URISyntaxException {
    return Paths.get(
        Thread.currentThread().getContextClassLoader().getResource(filename).toURI()).toFile();
  }

  private static ObjectMapper getObjectMapper() {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    return objectMapper;
  }

  
  private static List<PortfolioTrade> deserializeTradesFromJsonFile(File jsonFile) throws StreamReadException, DatabindException, IOException{

    ObjectMapper objectMapper = getObjectMapper();
    PortfolioTrade[] portfolioTrades=objectMapper.readValue(jsonFile,PortfolioTrade[].class);
    List<PortfolioTrade> portfolioTradesList=Arrays.asList(portfolioTrades);

    return portfolioTradesList;
  }

  private static List<String> extractSymbolsFromPortfolioTrades(List<PortfolioTrade> portfolioTrade){
    
    List<String> tradeSymbols=new ArrayList<>();
   
   
    for(PortfolioTrade trade:portfolioTrade){
       tradeSymbols.add(trade.getSymbol());
    }
    

    return tradeSymbols;

  }

  public static List<PortfolioTrade> readTradesFromJson(String filename) throws IOException, URISyntaxException {

    // Get the file from the filename

    File jsonFile=resolveFileFromResources(filename);

    // deserialize the json to get trades within the portfolio
    List<PortfolioTrade> portfolioTradesList=deserializeTradesFromJsonFile(jsonFile);


    // Extract the Trade symbol from Portfolio trade 

     return portfolioTradesList;
  }


 
  public static String prepareUrl(PortfolioTrade trade, LocalDate localDate, String token) {

     // Start date is the purchase date 
     LocalDate endDate=localDate;
     LocalDate startDate=trade.getPurchaseDate();

     String url=prepareUrl(trade,startDate, endDate,token);

     return url;
  }

  // prepate URL to get stock details from start date range to end date range 

  public static String prepareUrl(PortfolioTrade trade, LocalDate startRange,LocalDate endRange, String token){
     
    String symbol=trade.getSymbol();
    String startDate=startRange.toString();
    String endDate=endRange.toString();

   


    String baseUrl="https://api.tiingo.com/tiingo/daily/";
    String url=baseUrl+symbol+"/prices?"+"startDate="+startDate+"&"+"endDate="+endDate+"&"+"token="+token;

    return url;
    
  }

  static Double getOpeningPriceOnStartDate(List<Candle> candles) {
     
     Double openingPrice = candles.get(0).getOpen();
     return openingPrice;
  }


  public static Double getClosingPriceOnEndDate(List<Candle> candles) {

     Double closingPrice=candles.get(candles.size()-1).getClose();
     return closingPrice;
  }


  public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) throws JsonMappingException, JsonProcessingException {
     
     // validate invalid json dates
     validatePurchaseDate(trade,endDate);

     // prepare URL
     String url = prepareUrl(trade,endDate, token);

     // Call the API and get the candles
     List<Candle> candles=getCandlesFromTheUrl(url);

     return candles;
  }

  public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
      throws IOException, URISyntaxException {

     
     String fileName=args[0];
     LocalDate endDate = LocalDate.parse(args[1]);

     List<AnnualizedReturn> annualizedReturns=new ArrayList<>();
    
     // get the trades 
     List<PortfolioTrade> portfolioTrades=deserializeTradesFromJsonFile(resolveFileFromResources(fileName));

     for(PortfolioTrade trade:portfolioTrades){
        
        List<Candle> candles=fetchCandles(trade, endDate, getToken());
        Double openingPrice= getOpeningPriceOnStartDate(candles);
        Double sellingPrice = getClosingPriceOnEndDate(candles);
        AnnualizedReturn annualizedReturn=calculateAnnualizedReturns(endDate, trade, openingPrice,sellingPrice);
        annualizedReturns.add(annualizedReturn);
     }

     Collections.sort(annualizedReturns);
     return annualizedReturns;
  }

  public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
      PortfolioTrade trade, Double buyPrice, Double sellPrice) {
      
      int stockQuantity= trade.getQuantity();
      Double totalReturn = (sellPrice*stockQuantity - buyPrice*stockQuantity)/(buyPrice*stockQuantity);

      long numOfDays=ChronoUnit.DAYS.between(trade.getPurchaseDate(),endDate);
      Double numOfYears = (numOfDays*1.0)/365;
      Double annualizedReturn = Math.pow((1+totalReturn),(1.0/numOfYears))-1.0;


      return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
  }




public static List<String> debugOutputs() {

    String valueOfArgument0 = "trades.json";
    String resultOfResolveFilePathArgs0 = "/home/crio-user/workspace/ashuadubeyudemy-ME_QMONEY_V2/qmoney/bin/main/trades.json";
    String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@6150c3ec";
    String functionNameFromTestFileInStackTrace = "mainReadFile";
    String lineNumberFromTestFileInStackTrace = "29";


   return Arrays.asList(new String[]{valueOfArgument0, resultOfResolveFilePathArgs0,
       toStringOfObjectMapper, functionNameFromTestFileInStackTrace,
       lineNumberFromTestFileInStackTrace});
 }























  // TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();
       PortfolioTrade[] portfolioTrades=objectMapper.readValue(contents,PortfolioTrade[].class);
       PortfolioManager portfolioManager=PortfolioManagerFactory.getPortfolioManager(new RestTemplate());
       return portfolioManager.calculateAnnualizedReturn(Arrays.asList(portfolioTrades), endDate);
  }

  private static String readFileAsString(String file) throws URISyntaxException, StreamReadException, DatabindException, IOException{

    File f=resolveFileFromResources(file);
    ObjectMapper objectMapper=getObjectMapper();
    String fileAsString=objectMapper.readValue(f,String.class);

    return fileAsString;

  }





















  public static void main(String[] args) throws Exception {
    Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
    ThreadContext.put("runId", UUID.randomUUID().toString());

    
  //   printJsonObject(mainReadQuotes(args));
     
   // printJsonObject(mainCalculateSingleReturn(args));

    printJsonObject(mainCalculateReturnsAfterRefactor(args));



  }
}

