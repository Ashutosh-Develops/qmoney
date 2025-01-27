# QMONEY
QMoney is a visual stock portfolio analyzer. It helps portfolio managers make trade recommendations for their clients.This project specifically implements the backend logic used by the QMoney WebApp.

## Application Features
  * Application computes the annualized returns based on stock purchase date and holding period. Stock data for the concerned stocks within the specified time duration is fetched using Tiingo API.

## Technologies 
  * Java, Jackson.

## Getting Started
  * Download the repository on local machine.
  * Import the downloaded project in your editor, ex- Intellij, Eclipse, or VSCode.
  * Now, run `./gradlew clean build` to get the executable jar.
  * Move the executable qmoney-0.0.1-SNAPSHOT.jarfrom `./build/libs/` to the classpath of the client application program.
  * Now Add the following to your client code.
    ```
     PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(restTemplate);
     List<AnnualizedReturn> annualizedReturns= portfolioManager.calculateAnnualizedReturn(portfolioTrades,endDate);
    
    ```
    ```
      portfolioTrades is a list of PortfolioTrade object and the it contains following fields.
    
      "symbol": "AAPL",
      "quantity": 100,
      "tradeType": "BUY",
      "purchaseDate": "2019-01-02"
    
    ```
