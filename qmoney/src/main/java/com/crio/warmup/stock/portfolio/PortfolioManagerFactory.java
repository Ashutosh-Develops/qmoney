
package com.crio.warmup.stock.portfolio;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.crio.warmup.stock.quotes.StockQuoteServiceFactory;
import com.crio.warmup.stock.quotes.StockQuotesService;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerFactory {
   
  public static PortfolioManager getPortfolioManager(RestTemplate restTemplate) {
     
     StockQuotesService stockQuotesService=StockQuoteServiceFactory.INSTANCE.getService("", restTemplate);
     PortfolioManager portfolioManager=new PortfolioManagerImpl(restTemplate,stockQuotesService);
     return portfolioManager;
     
  }

   public static PortfolioManager getPortfolioManager(String provider,
     RestTemplate restTemplate) {

      StockQuotesService stockQuotesService=StockQuoteServiceFactory.INSTANCE.getService(provider, restTemplate);  
      PortfolioManager portfolioManager=new PortfolioManagerImpl(restTemplate,stockQuotesService);
      
      return portfolioManager;
    
   }

}
