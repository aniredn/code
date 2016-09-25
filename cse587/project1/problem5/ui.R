# ui.R

shinyUI(fluidPage(
  titlePanel("US Presidential Candidate's Popularity Trends"),
  
  tags$head(
    tags$style("body {background-color: gray; }")
  ),
  
  sidebarLayout(
    sidebarPanel(
      helpText("Plot of Twitter trends for Presidential Candidates between February 21 and March 1"),    
      radioButtons("dayweek", label = h4("Choose Time Period"),
                   choices = c("Daily" = 'days', "Weekly" = 'week'),selected = 'days'),
      
      radioButtons("candidate", label = h4("Choose Candidate"),
                   choices = c("Trump" =1,"Sanders"=2, "Clinton" = 3, "Carson"=4,"Cruz"=5,"Compare All" =6)),
      
      actionButton("fetch","Plot live data")
      
    ),
    
    mainPanel(
      textOutput("text1"),
      plotOutput("plot1")
    )
    
    
  )
))