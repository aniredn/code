library(jsonlite)
#library(twitteR)


#Reading all JSON files into R
tweets_1 <- fromJSON("Data/apartment.json")
tweets_2 <- fromJSON("Data/condos.json")
tweets_3 <- fromJSON("Data/houses.json")

#Merging all dataframes
tweets_all<- rbind(tweets_1,tweets_2, tweets_3)

rm(tweets_3,tweets_2,tweets_1)

#Removing special characters
tweets_all$text<-gsub("[^[:alpha:] ]","",tweets_all$text)

#Convert text to lowercase
tweets_all$text<-tolower(tweets_all$text)

#Format Dates
tweets_all$created <- as.Date(tweets_all$created)

#Filtering tweets based on keywords
tweets.rent<- tweets_all[which(grepl("rent|rental|lease", tweets_all$text)),]
tweets.buy<- tweets_all[which(grepl("buy|purchase|sell|selling|sale", tweets_all$text)),]

#Plotting data from Twitter
barplot(c(nrow(tweets.rent), nrow(tweets.buy)), width = 0.5, ylab = "Number of Tweets", names = c("Renting", "Buying"), col = rainbow(6), ylim = c(0,2000))
barplot(c(sum(tweets.rent$favoriteCount),sum(tweets.buy$favoriteCount)), ylab = "Favourited Count", names= c("Renting", "Buying"), col = rainbow(2))

#Analyzing local data (Please include path to rolling sales data)
bk<-read.csv("/Users/anirudhreddy/Desktop/Rdata/dds_ch2_rollingsales/rollingsales_brooklyn.csv")
br<-read.csv("/Users/anirudhreddy/Desktop/Rdata/dds_ch2_rollingsales/rollingsales_bronx.csv")
mn <-read.csv("/Users/anirudhreddy/Desktop/Rdata/dds_ch2_rollingsales/rollingsales_manhattan.csv")
qn<-read.csv("/Users/anirudhreddy/Desktop/Rdata/dds_ch2_rollingsales/rollingsales_queens.csv")
si<-read.csv("/Users/anirudhreddy/Desktop/Rdata/dds_ch2_rollingsales/rollingsales_statenisland.csv")
local_all <- rbind(bk,br,mn,qn,si)


#Convert Column names to lower case
names(bk) <- tolower(names(bk))
names(br) <- tolower(names(br))
names(mn) <- tolower(names(mn))
names(qn) <- tolower(names(qn))
names(si) <- tolower(names(si))

## clean/format the data with regular expressions
bk$gross.sqft <- as.numeric(gsub("[^[:digit:]]","", bk$gross.square.feet))
bk$land.sqft <- as.numeric(gsub("[^[:digit:]]","", bk$land.square.feet))
bk$sale.date <- as.Date(bk$sale.date, "%m/%d/%y")
bk$year.built <- as.numeric(as.character(bk$year.built))

br$gross.sqft <- as.numeric(gsub("[^[:digit:]]","", br$gross.square.feet))
br$land.sqft <- as.numeric(gsub("[^[:digit:]]","", br$land.square.feet))
br$sale.date <- as.Date(br$sale.date, "%m/%d/%y")
br$year.built <- as.numeric(as.character(br$year.built))

mn$gross.sqft <- as.numeric(gsub("[^[:digit:]]","", mn$gross.square.feet))
mn$land.sqft <- as.numeric(gsub("[^[:digit:]]","", mn$land.square.feet))
mn$sale.date <- as.Date(mn$sale.date, "%m/%d/%y")
mn$year.built <- as.numeric(as.character(mn$year.built))

qn$gross.sqft <- as.numeric(gsub("[^[:digit:]]","", qn$gross.square.feet))
qn$land.sqft <- as.numeric(gsub("[^[:digit:]]","", qn$land.square.feet))
qn$sale.date <- as.Date(qn$sale.date, "%m/%d/%y")
qn$year.built <- as.numeric(as.character(qn$year.built))

si$gross.sqft <- as.numeric(gsub("[^[:digit:]]","", si$gross.square.feet))
si$land.sqft <- as.numeric(gsub("[^[:digit:]]","", si$land.square.feet))
si$sale.date <- as.Date(si$sale.date, "%m/%d/%y")
si$year.built <- as.numeric(as.character(si$year.built))


#Classifying rental apartments
bk.rent<- bk[which(grepl("RENTALS", bk$building.class.category)),]
br.rent<- br[which(grepl("RENTALS", br$building.class.category)),]
mn.rent<- mn[which(grepl("RENTALS", mn$building.class.category)),]
qn.rent<- qn[which(grepl("RENTALS", qn$building.class.category)),]
si.rent<- si[which(grepl("RENTALS", si$building.class.category)),]

barplot(c(nrow(bk.rent),nrow(br.rent),nrow(mn.rent),nrow(qn.rent), nrow(si.rent)), names=c("Brooklyn","Bronx","Manhattan", "Queens", "Staten Island"), ylab = "Number of Rentals", col = rainbow(5), ylim = c(0,3000))

