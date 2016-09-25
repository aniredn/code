require(package = "ggplot2")
require(package = "doBy")

urlpart1 = "http://stat.columbia.edu/~rachel/datasets/nyt"
urlpart2 = ".csv"

#Populating day 1 data
nyt_data=read.csv(url("http://stat.columbia.edu/~rachel/datasets/nyt1.csv"))
numrows <- c(0)
#Calculate number on entries for first day
numrows <- rbind(numrows,nrow(nyt_data))
sumt<- nrow(nyt_data)
for(i in 2:31)
{
  wholeurl=paste(paste(urlpart1,i,sep=""),urlpart2,sep="") 
  temp=read.csv(url(wholeurl))

  #Calculate and populate number of entries for each day
  temprows = nrow(temp)
  sumt = sumt+temprows
  numrows = rbind(numrows,sumt)
  nyt_data=rbind(nyt_data,temp) 
}


#Creating age categories for the data set
nyt_data$agecat <- cut(nyt_data$Age,c(-Inf,0,18,24,34,44,54,64,Inf))

siterange<- function(x){c(length(x),min(x),max(x),mean(x))}
summaryBy(Age~agecat, data = nyt_data, FUN = siterange)
summaryBy(Gender +Signed_In+Impressions+Clicks~agecat,data=nyt_data)

#Dividing the dataset based on Impressions and Clicks
nyt_data$hasimps <-cut(nyt_data$Impressions,c(-Inf,0,Inf))
nyt_data$scode[nyt_data$Impressions == 0] <- "NoImps" #Clicks with no Impressions are invalid
nyt_data$scode[nyt_data$Impressions > 0] <- "Imps"    #Impressions with no Clicks
nyt_data$scode[nyt_data$Clicks > 0] <- "Clicks"       #Impressions with Clicks

nyt_data$scode<- factor(nyt_data$scode)
clen <- function(x){c(length(x))}
etable<-summaryBy(Impressions~scode+Gender+agecat, data = nyt_data, FUN = clen) #Monthly Summary of data

#Calculating Click Through Rate(CTR) for each day in May 2012
ctr <- function(C,I){return(C/I)}
month_ctr<-c()
month_signedin<-c()
month_imp<-c()
for(i in 1: 31)
  {
    attach(nyt_data[numrows[i]:numrows[i+1],])
      a <-ctr(Clicks,Impressions)
      b<-sum(Signed_In)
      c<- sum(Impressions)
      
      month_imp <- rbind(month_imp,c)
      month_signedin <- rbind(month_signedin,b)
      month_ctr<-rbind(month_ctr, sum(a, na.rm = TRUE))
    detach(nyt_data[numrows[i]:numrows[i+1],])
}

rm(a, temp,i,j,sum,sumt, temprows)

#Plotting CTR for entire month
barplot(width =1,space = 1, height = month_ctr, beside = TRUE, xlab = "Days", ylab = "CTR")
#Plotting number of signed in users for entire month
barplot(width =1,space = 1, height = month_signedin, beside = TRUE, xlab = "Days", ylab = "Signed In", col = "blue")
#Plotting number of impressions users for entire month
barplot(width =1,space = 1, height = month_imp, beside = TRUE, xlab = "Days", ylab = "Impressions", col = "black")

#Details of Number of Impressions
boxplot(month_imp, ylab = "Number of Impressions")
summary(month_imp)

#Details of Number of users Signed In
boxplot(month_signedin, ylab = "Number of Signed In Users")
summary(month_signedin)

ggplot(subset(nyt_data, Impressions>0), aes(x=Clicks/Impressions, color = agecat)) + geom_density()
ggplot(subset(nyt_data,Clicks>0), aes(x=Clicks/Impressions, color = agecat))+ geom_density()
ggplot(subset(nyt_data,Clicks>0), aes(x=agecat,y=Clicks,file = agecat))+ geom_boxplot()
ggplot(subset(nyt_data, Clicks>0), aes(x=Clicks, color = agecat)) + geom_density()
ggplot(subset(nyt_data, Clicks>0), aes(x=Clicks/Impressions, fill = agecat))+geom_histogram(binwidth = 0.05)
