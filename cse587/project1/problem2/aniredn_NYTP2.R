require(package = "ggplot2")
require(package = "doBy")

#Download data and store in dataframe
data1 <- read.csv(file = "http://stat.columbia.edu/~rachel/datasets/nyt1.csv", header = TRUE)
head(data1)

#Create a new column in the dataframe and assign the respective age category to each row
data1$agecat <- cut(data1$Age,c(-Inf,0,18,24,34,44,54,64,Inf))

siterange<- function(x){c(length(x),min(x),max(x),mean(x))}
summaryBy(Age~agecat, data = data1, FUN = siterange)
summaryBy(Gender +Signed_In+Impressions+Clicks~agecat,data=data1)

#Plotting number of impressions for different age categories
ggplot(data1, aes(x=Impressions, fill = agecat))+geom_histogram(binwidth = 1)
ggplot(data1, aes(x=agecat, y = Impressions, fill = agecat))+geom_boxplot()

#Create a new column in the dataframe to categorize each row based on Impressions
data1$hasimps <-cut(data1$Impressions,c(-Inf,0,Inf))
summaryBy(Clicks~hasImps, data=data1, FUN = siterange)

ggplot(subset(data1, Impressions>0), aes(x=Clicks/Impressions, color = agecat)) + geom_density()
ggplot(subset(data1,Clicks>0), aes(x=Clicks/Impressions, color = agecat))+ geom_density()
ggplot(subset(data1,Clicks>0), aes(x=agecat,y=Clicks,file = agecat))+ geom_boxplot()
ggplot(subset(data1, Clicks>0), aes(x=Clicks, color = agecat)) + geom_density()

#Create a new column to store additional information regarding clicks and Impressions
data1$scode[data1$Impressions == 0] <- "NoImps"
data1$scode[data1$Impressions > 0] <- "Imps"
data1$scode[data1$Clicks > 0] <- "Clicks"

data1$scode<- factor(data1$scode)
clen <- function(x){c(length(x))}
etable<-summaryBy(Impressions~scode+Gender+agecat, data = data1, FUN = clen)


