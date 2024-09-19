
package com.crio.warmup.stock.quotes;

import org.springframework.web.client.RestTemplate;

public enum StockQuoteServiceFactory {


  INSTANCE;

  public StockQuotesService getService(String provider,  RestTemplate restTemplate) {

     StockQuotesService stockQuotesService=null;
    
     if(provider.equalsIgnoreCase("tiingo")){
        stockQuotesService=new TiingoService(restTemplate);
     }else{
       stockQuotesService=new AlphavantageService(restTemplate);
     }

     return stockQuotesService;
  }
}
