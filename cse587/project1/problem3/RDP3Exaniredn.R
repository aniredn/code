require(gdata)
require(plyr)
bk<-read.csv("rollingsales_brooklyn.csv")
br<-read.csv("rollingsales_bronx.csv")
mn <-read.csv("rollingsales_manhattan.csv")
qn<-read.csv("rollingsales_queens.csv")
si<-read.csv("rollingsales_statenisland.csv")

#Create a new Column containing Sale Price as a numeric value
bk$SALE.PRICE.N <- as.numeric(gsub("[^[:digit:]]","",bk$SALE.PRICE))
br$SALE.PRICE.N <- as.numeric(gsub("[^[:digit:]]","",br$SALE.PRICE))
mn$SALE.PRICE.N <- as.numeric(gsub("[^[:digit:]]","",mn$SALE.PRICE))
qn$SALE.PRICE.N <- as.numeric(gsub("[^[:digit:]]","",qn$SALE.PRICE))
si$SALE.PRICE.N <- as.numeric(gsub("[^[:digit:]]","",si$SALE.PRICE))

#Check if there are any NaN in the Sale Price Column
count(is.na(bk$SALE.PRICE.N))
count(is.na(br$SALE.PRICE.N))
count(is.na(mn$SALE.PRICE.N))
count(is.na(qn$SALE.PRICE.N))
count(is.na(si$SALE.PRICE.N))

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

#Create a new dataframe without houses having non zero sale prices or abnormally low prices
bk.sale <- bk[bk$sale.price.n!=0 & bk$sale.price.n>1000,]
br.sale <- br[br$sale.price.n!=0 & br$sale.price.n>1000,]
mn.sale <- mn[mn$sale.price.n!=0 & mn$sale.price.n>1000,]
qn.sale <- qn[qn$sale.price.n!=0 & qn$sale.price.n>1000,]
si.sale <- si[si$sale.price.n!=0 & si$sale.price.n>1000,]

div<-function(x,y){return(x/y)}

#Calculating price per square feet and storing it in a new column
bk.sale$persft<- div(bk.sale$sale.price.n,bk.sale$gross.sqft)
br.sale$persft<- div(br.sale$sale.price.n,br.sale$gross.sqft)
mn.sale$persft<- div(mn.sale$sale.price.n,mn.sale$gross.sqft)
qn.sale$persft<- div(qn.sale$sale.price.n,qn.sale$gross.sqft)
si.sale$persft<- div(si.sale$sale.price.n,si.sale$gross.sqft)


#Calculating average price per squarefeet for different boroughs
par(mfrow=c(1,5))
boxplot(bk.sale$persft[bk.sale$gross.sqft>0], outline = FALSE, xlab = "Brooklyn", ylab = "Price per sft")
boxplot(br.sale$persft[br.sale$gross.sqft>0], outline = FALSE, xlab = "Bronx", ylab = "Price per sft")
boxplot(mn.sale$persft[mn.sale$gross.sqft>0], outline = FALSE, xlab = "Manhatten", ylab = "Price per sft")
boxplot(qn.sale$persft[qn.sale$gross.sqft>0], outline = FALSE, xlab = "Queens", ylab = "Price per sft")
boxplot(si.sale$persft[si.sale$gross.sqft>0], outline = FALSE, xlab = "Staten Island", ylab = "Price per sft")


#Create a new dataframe for Family Houses
bk.homes <- bk.sale[which(grepl("FAMILY",bk.sale$building.class.category)),]
br.homes <- br.sale[which(grepl("FAMILY",br.sale$building.class.category)),]
mn.homes <- mn.sale[which(grepl("FAMILY",mn.sale$building.class.category)),]
qn.homes <- qn.sale[which(grepl("FAMILY",qn.sale$building.class.category)),]
si.homes <- si.sale[which(grepl("FAMILY",si.sale$building.class.category)),]


#One Bedroom Houses
bk.homes.one <- bk.homes[which(grepl("ONE",bk.homes$building.class.category)),]
br.homes.one <- br.homes[which(grepl("ONE",br.homes$building.class.category)),]
mn.homes.one <- mn.homes[which(grepl("ONE",mn.homes$building.class.category)),]
qn.homes.one <- qn.homes[which(grepl("ONE",qn.homes$building.class.category)),]
si.homes.one <- si.homes[which(grepl("ONE",si.homes$building.class.category)),]

#Two Bedroom Houses
bk.homes.two <- bk.homes[which(grepl("TWO",bk.homes$building.class.category)),]
br.homes.two <- br.homes[which(grepl("TWO",br.homes$building.class.category)),]
mn.homes.two <- mn.homes[which(grepl("TWO",mn.homes$building.class.category)),]
qn.homes.two <- qn.homes[which(grepl("TWO",qn.homes$building.class.category)),]
si.homes.two <- si.homes[which(grepl("TWO",si.homes$building.class.category)),]

#Three Bedroom Houses
bk.homes.three <- bk.homes[which(grepl("THREE",bk.homes$building.class.category)),]
br.homes.three <- br.homes[which(grepl("THREE",br.homes$building.class.category)),]
mn.homes.three <- mn.homes[which(grepl("THREE",mn.homes$building.class.category)),]
qn.homes.three <- qn.homes[which(grepl("THREE",qn.homes$building.class.category)),]
si.homes.three <- si.homes[which(grepl("THREE",si.homes$building.class.category)),]

boxplot(ylab = "Sale price",bk$sale.price.n[bk$sale.price.n!=0],br$sale.price.n[br$sale.price.n!=0],mn$sale.price.n[mn$sale.price.n!=0],qn$sale.price.n[qn$sale.price.n!=0],si$sale.price.n[si$sale.price.n!=0], outline = FALSE, names = c("Brooklyn","Bronx","Manhattan","Queens","Staten Island"), col = "aquamarine")

#Plotting number of sales in each month for different boroughs
par(mfrow=c(2,3))
hist(bk.sale$sale.date, breaks = "months", xlab = "Brooklyn Sales", main = "", col = "gray")
hist(br.sale$sale.date, breaks = "months", xlab = "Bronx Sales", main = "", col = "gray")
hist(mn.sale$sale.date, breaks = "months", xlab = "Manhattan Sales", main = "", col = "gray")
hist(qn.sale$sale.date, breaks = "months", xlab = "Queens Sales", main = "", col = "gray")
hist(si.sale$sale.date, breaks = "months", xlab = "Staten Island Sales", main = "", col = "gray")

#Plot of total number of sales in each region
barplot(c(nrow(bk),nrow(br) ,nrow(mn),nrow(qn),nrow(si)), names= c("Brooklyn","Bronx","Manhattan","Queens","Staten Island"), width = 1)

par(mfrow=c(2,3))
barplot(c(nrow(bk.homes.one), nrow(bk.homes.two), nrow(bk.homes.three)), xlab = "Brooklyn", ylab = "Number of Sales", names= c("One", "Two", "Three"))
barplot(c(nrow(br.homes.one), nrow(br.homes.two), nrow(br.homes.three)), xlab = "Bronx", ylab = "Number of Sales", names= c("One", "Two", "Three"))
barplot(c(nrow(mn.homes.one), nrow(mn.homes.two), nrow(mn.homes.three)), xlab = "Manhattan", ylab = "Number of Sales", names= c("One", "Two", "Three"))
barplot(c(nrow(qn.homes.one), nrow(qn.homes.two), nrow(qn.homes.three)), xlab = "Queens", ylab = "Number of Sales", names= c("One", "Two", "Three"))
barplot(c(nrow(si.homes.one), nrow(si.homes.two), nrow(si.homes.three)), xlab = "Staten Island", ylab = "Number of Sales", names= c("One", "Two", "Three"))
