# server.R

source("aniredn_Rshiny.R")

shinyServer(function(input, output) {
  
  output$text1 <- renderText({ 
    "Selected plot is shown below - "
  })
  
  plotType<- function(x){
    if(x == 1) hist(trump$created, breaks = input$dayweek, col = 'darkblue', main = "Donald Trump's Popularity", xlab = "Time")
    else if(x ==2) hist(sanders$created, breaks = input$dayweek, col = 'darkblue', main = "Bernie Sander's Popularity", xlab = "Time")
    else if(x ==3) hist(clinton$created, breaks = input$dayweek, col = 'darkblue', main = "Hillary Clinton's Popularity", xlab = "Time")
    else if(x ==4) hist(carson$created, breaks = input$dayweek, col = 'darkblue', main = "Ben Carson's Popularity", xlab = "Time")
    else if(x ==5) hist(cruz$created, breaks = input$dayweek, col = 'darkblue', main = "Ted Cruz's Popularity", xlab = "Time")
    else if(x==6) barplot(c(nrow(trump), nrow(sanders),nrow(clinton),nrow(carson),nrow(cruz)), names = c("Trump","Sanders","Clinton","Carson","Cruz"), main = "Total Number of Tweets", col = "darkblue")
  }
  
  observeEvent(input$fetch,domain = getDefaultReactiveDomain(),{
    library(streamR)
    load("Data/my_oauth.Rdata")
    file_name<- "live_tweets.json"
    filterStream(track = "#US", tweets = 100, file.name = file_name, oauth = my_oauth, timeout = 60)
    live_t<- parseTweets(file_name)
   
   detach(package:streamR, unload = TRUE)
   detach(package:rjson,unload = TRUE)
   library(jsonlite)
    ltrump<- live_t[which(grepl("donald|trump", live_t$text)),]
    lcarson<- live_t[which(grepl("ben|carson", live_t$text)),]
    lsanders<- live_t[which(grepl("bernie|sanders", live_t$text)),]
    lclinton<-live_t[which(grepl("hillary|clinton", live_t$text)),]
    lcruz<- live_t[which(grepl("ted|cruz", live_t$text)),]
  
   output$plot1 <-renderPlot( barplot(c(nrow(ltrump), nrow(lsanders),nrow(lclinton),nrow(lcarson),nrow(lcruz)), names = c("Trump","Sanders","Clinton","Carson","Cruz"), main = "Total Number of Tweets", col = "darkblue")
)  })
  
  
  output$plot1 <- renderPlot(
    plotType(input$candidate)
  )
}
)