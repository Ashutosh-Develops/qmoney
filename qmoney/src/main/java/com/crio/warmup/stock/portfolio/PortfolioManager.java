
package com.crio.warmup.stock.portfolio;

import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.fasterxml.jackson.core.JsonProcessingException;
import java.time.LocalDate;
import java.util.List;

public interface PortfolioManager {

  List<AnnualizedReturn> calculateAnnualizedReturn(List<PortfolioTrade> portfolioTrades,
      LocalDate endDate)throws JsonProcessingException
  ;
}

