package com.crio.warmup.stock.dto;

import java.time.LocalDate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown=true)
public class AlphavantageCandle implements Candle,Comparable<AlphavantageCandle> {

  @JsonProperty("1. open")
  private Double open;

  @JsonProperty("2. high")
  private Double high;

  @JsonProperty("3. low")
  private Double low;

  @JsonProperty("4. close")
  private Double close;
 
  private LocalDate date;

  

  @Override
  public Double getOpen() {   
    return open;
  }

  public void setOpen(Double open){
    this.open=open;
  }

  @Override
  public Double getClose() {
    // TODO Auto-generated method stub
    return close;
  }
   
  public void setClose(Double close){
    this.close=close;
  }

  @Override
  public Double getHigh() {
    // TODO Auto-generated method stub
    return high;
  }

  public void setHigh(Double high){
    this.high=high;
  }

  @Override
  public Double getLow() {
    // TODO Auto-generated method stub
    return low;
  }

  public void setLow(Double low){
    this.low=low;
  }
  @Override
  public LocalDate getDate() {
    // TODO Auto-generated method stub
    return date;
  }

  public void setDate(LocalDate date){
    this.date=date;
  }
  @Override
  public int compareTo(AlphavantageCandle o) {
   
     if(this.date.isBefore(o.getDate())){
       return -1;
     }else if(this.date.isAfter(o.getDate())){
      return 1;
     }

     return 0;
  }
}

