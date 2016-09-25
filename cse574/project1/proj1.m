%Probability Distributions and Bayesian Networks

UBitName = ['a' 'n' 'i' 'r' 'u' 'd' 'h' ' ' 'r' 'e' 'd' 'd' 'y' ' ' 'n' 'a' 'l' 'a' 'm' 'a' 'd' 'a'];
personNumber=['5' '0' '1' '6' '9' '2' '4' '0'];

%Extracting data from Excel file

filename='university data.xlsx';
val1 = xlsread(filename,'C2:C50'); %val1 -> CS Score
val2 = xlsread(filename,'D2:D50'); %val2 -> Research Overhead
val3 = xlsread(filename,'E2:E50'); %val3 -> Admin Base Pay
val4 = xlsread(filename,'F2:F50'); %val4 -> Tuition
%val5 = xlsread(filename,'G2:G50');
data= [val1 , val2 , val3 , val4];

%Calculating Mean, variance and standard Deviation
mu1 = mean(val1);
mu2 = mean(val2);
mu3 = mean(val3);
mu4 = mean(val4);

muMatrix = [val1, val2, val3, val4];

var1 = var(val1);
var2 = var(val2);
var3 = var(val3);
var4 = var(val4);

sigma1 = std(val1);
sigma2 = std(val2);
sigma3 = std(val3);
sigma4 = std(val4);

%Calculating Covariances and Correlation Coefficients

covarianceMat = cov(data);
correlationMat = corrcoef(data);

%Plotting Pairwise Data

[S,AX,BigAx,H,HAx] = plotmatrix(data,'.b');
title(BigAx,'Plot of Pairwise Data');

%Calculating loglikelihood

nd1 = normpdf(val1,mu1,sigma1); %or mvnpdf(val1,mu1,var1)
nd2 = normpdf(val2,mu2,sigma2);
nd3 = normpdf(val3,mu3,sigma3);
nd4 = normpdf(val4,mu4,sigma4);

logLikelihood= sum(log(nd1))+sum(log(nd2))+sum(log(nd3))+sum(log(nd4));

%Calculate BNgraph and BNloglikelihood

BNgraph = [0 0 0 0; 1 0 0 0; 0 1 0 1;1 1 0 0;];

%Calculating Conditional Probabilities depending on BNGraph

%P(V1,V2,V3,V4)= P(V1|V2,V4)*P(V2|V3,V4)*P(V3)*P(V4|V3)

%P(V1|V2,V4)= P(V1,V2,V4)/P(V2,V4)
pv1v2v4 = mvnpdf([data(:,1),data(:,2),data(:,4)],[mu1,mu2,mu4], cov([data(:,1),data(:,2),data(:,4)]));
pv2v4 = mvnpdf([data(:,2),data(:,4)],[mu2,mu4], cov([data(:,2),data(:,4)]));

%P(V2|V3,V4)= P(V2,V3,V4)/P(V3,V4)
pv2v3v4 = mvnpdf([data(:,2),data(:,3),data(:,4)],[mu2,mu3,mu4], cov([data(:,2),data(:,3),data(:,4)]));
pv3v4 = mvnpdf([data(:,2),data(:,3)],[mu2,mu3], cov([data(:,2),data(:,3)]));

%P(V3)
pv3 = mvnpdf(val3,mu3,var3);

%P(V4|V3) = P(V3,V4)/P(V3)
pv4v3 = mvnpdf([data(:,3),data(:,4)],[mu3,mu4], cov([data(:,3),data(:,4)]));

BNlogLikelihood = sum(log(pv1v2v4))-sum(log(pv2v4))+sum(log(pv2v3v4))-sum(log(pv3v4))+sum(log(pv3))+sum(log(pv3v4))-sum(log(pv3));

%Saving .mat file
filename1= 'proj1';
save(filename1);


disp Done;
