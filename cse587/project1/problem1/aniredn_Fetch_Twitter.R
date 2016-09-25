library(jsonlite)
library(twitteR)
#Setup Twitter Connection

setup_twitter_oauth("nvET2KHwicFkxWuKqhgCwO7M3","zA91KODJZ868Egs91Ojn6Jbet5gOdqByzCrV9LzPNXYjQkJJFc","150665360-6rHsbmQ8R3vsLclkrZ52nKSSURwpJLvPLDhtYunk","wjLm0GJMp1USiDQrZFWTPe95weBwTN6QQaGiwrscDowFG")

#Search for Tweets
tweets<- searchTwitter("house", n=10000)

#Strip retweets
tweets<- strip_retweets(tweets)

#Convert Tweets list to dataframe
tw.df<-twListToDF(tweets)

#Convert Tweets dataframe to JSON Object
tw.json<-toJSON(tw.df, pretty = TRUE, dataframe = "rows")

#Save JSON file
write(tw.json, file = "rentnyc.json")


