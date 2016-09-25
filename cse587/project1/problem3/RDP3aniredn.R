require(gdata)
require(plyr)
bk<-read.csv("rollingsales_brooklyn.csv")
head(bk)
summary(bk)

#Create a new column containing Sale Price as a numeric value
bk$SALE.PRICE.N <- as.numeric(gsub("[^[:digit:]]","",bk$SALE.PRICE))

#Check if there are any NaN in the Sale Price Column
count(is.na(bk$SALE.PRICE.N))
 
#Convert Column names to lower case
names(bk) <- tolower(names(bk))

## clean/format the data with regular expressions
bk$gross.sqft <- as.numeric(gsub("[^[:digit:]]","", bk$gross.square.feet))
bk$land.sqft <- as.numeric(gsub("[^[:digit:]]","", bk$land.square.feet))

bk$sale.date <- as.Date(bk$sale.date, "%m/%d/%y")
bk$year.built <- as.numeric(as.character(bk$year.built))

attach(bk)
hist(sale.price.n)
hist(sale.price.n[sale.price.n>0])
boxplot(sale.price.n[!sale.price.n==0], ylab= "Sale Price", outline = FALSE)
length(sale.price.n[!sale.price.n==0])
hist(gross.sqft)
detach(bk)

bk.sale <- bk[bk$sale.price.n!=0,]
plot(bk.sale$gross.sqft,bk.sale$sale.price.n)
plot(log(bk.sale$gross.sqft),log(bk.sale$sale.price.n))

bk.homes <- bk.sale[which(grepl("FAMILY",bk.sale$building.class.category)),]
plot(log(bk.homes$gross.sqft),log(bk.homes$sale.price.n))

bk.homes$outliers <- (log(bk.homes$sale.price.n) <=5) + 0
bk.homes <- bk.homes[which(bk.homes$outliers==0),]
plot(log(bk.homes$gross.sqft),log(bk.homes$sale.price.n))
