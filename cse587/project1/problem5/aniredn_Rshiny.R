library(shiny)

# Package streamR requires package rjson, detach rjson and streamR to run this part
require(jsonlite)


#Reading election data from a local file
tweets_1 <- fromJSON("Data/uselections.json")
tweets_2 <- fromJSON("Data/uselections13363.json")

#Combining both the data frames
tweets_all<- rbind(tweets_1,tweets_2)
rm(tweets_1,tweets_2)

#Remove all special characters. Only keep alhphanumeric values
tweets_all$text<-gsub("[^[:alpha:] ]","",tweets_all$text)

#Convert to lower case
tweets_all$text<-tolower(tweets_all$text)

#Format the tweet created field
tweets_all$created <- as.Date(tweets_all$created)

#Classify tweets based on keywords
trump<- tweets_all[which(grepl("donald|trump", tweets_all$text)),]
carson<- tweets_all[which(grepl("ben|carson", tweets_all$text)),]
sanders<- tweets_all[which(grepl("bernie|sanders", tweets_all$text)),]
clinton<- tweets_all[which(grepl("hillary|clinton", tweets_all$text)),]
cruz<- tweets_all[which(grepl("ted|cruz", tweets_all$text)),]



 