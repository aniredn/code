
%Learning to rank using Linear Regression

UBitName = ['a' 'n' 'i' 'r' 'e' 'd' 'n'];
personNumber=['5' '0' '1' '6' '9' '2' '4' '0'];

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%Extracting real data 
load('Querylevelnorm.mat');
real_data = transpose(Querylevelnorm);

D1 = 46;
rN1 = 55700;
rN2 = 6960;

%Training Data - 80%
data_train = real_data(2:47,1:rN1); %55700 rows
y_train = transpose(real_data(1,1:rN1));
y_train_t = real_data(1,1:rN1);
trainInd1 = zeros(rN1,1); %rN1 x 1 array
for i = 1:rN1
    trainInd1(i,1) = i;
end

%Validation Data - 10%
data_val = real_data(2:47,55701:62660); %6960 rows
y_val = transpose(real_data(1,55701:62660));
y_val_t = real_data(1,55701:62660);
validInd1 = zeros(rN2,1);  %rN2 x 1 array
j=1;
for i = 55701:62660
    validInd1(j,1) = i;
    j = j+1;
end

%Testing Data - 10%
data_test = real_data(2:47,62661:end); % 6963 rows
y_test = transpose(real_data(1,62661:end));
y_test_t = real_data(1,62661:end);

%Linear Regression using Batch Method
M1 = 10; 
mu1  = zeros(D1,M1); %D x M matrix
Sigma1 = zeros(D1,D1,M1); %D x D x M matrix
lambda1 = 0.4;
I = eye(M1,M1);

for i = 1:M1
     Int = randi([2 rN1],1,1);
   mu1(:,i) = data_train(1:D1,Int);
%     mu1(:,i) = data_train(1:D1,10*i+34);
end

Sigma1(:,:,1) = diag(diag(cov(transpose(data_train))));
for i = 1:D1 
    if(Sigma1(i,i,1) == 0)
        Sigma1(i,i,1) = 0.1;                
    end
    Sigma1(i,i,1) = 0.5*Sigma1(i,i,1);
end
Sigma1(46,46,1) = 0.1;
for i = 2:M1
    Sigma1(:,:,i) = Sigma1(:,:,1);
end

%Calculation of Design Matrix for Training Data
r_phi_train = zeros(rN1,M1);
r_phi_train(:,1) = 1;

for j = 2:M1
for i = 1:rN1
   t1 = transpose(data_train(:,i) - mu1(:,j));
   t2 = data_train(:,i) - mu1(:,j);
   r_phi_train(i,j) = exp(-0.5*( (t1 / Sigma1(:,:,j)) *t2));
end
end

r_phi_train_t = transpose(r_phi_train);
w1 = (inv((lambda1*I)+(r_phi_train_t*r_phi_train)))*(r_phi_train_t*y_train);
w1_t = transpose(w1);



%Calculation of Design Matrix for Validation Data
r_phi_val = zeros(rN2,M1);
r_phi_val(:,1) = 1;

for j = 2:M1
for i = 1:rN2
   t1 = transpose(data_val(:,i) - mu1(:,j));
   t2 = data_val(:,i) - mu1(:,j);
   r_phi_val(i,j) = exp(-0.5*( (t1 / Sigma1(:,:,j)) *t2));
end
end

r_phi_val_t = transpose(r_phi_val);

%Error Calculation for Real and Synthetic data
trainPer1 = sqrt(transpose(r_phi_train * w1 - y_train)*(r_phi_train * w1 - y_train)/rN1);

validPer1 = sqrt(transpose(r_phi_val * w1 - y_val)*(r_phi_val * w1 - y_val)/rN2);


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%

%Extracting Synthetic data

load('synthetic.mat');

syn_data = x;
target = t;

D2 = 10;
sN1 = 1600;
sN2 = 400;

%Training Data - 80%
s_train = syn_data(1:10,1:sN1); %1600 rows
t_train = target(1:1600,1);
t_train_t = transpose(t_train);
trainInd2 = zeros(sN1,1); %sN1 x 1 array
for i = 1:sN1
    trainInd2(i,1) = i;
end

%Validation Data - 20%
s_val = syn_data(1:10,1601:end); %400 rows
t_val = target(1601:end,1);
t_val_t = transpose(t_val);
validInd2 = zeros(sN2,1);  %sN2 x 1 array
j=1601;
for i = 1:sN2
    validInd2(i,1) = j;
    j = j+1;
end

%Linear Regression using Batch Method
M2 =5; 
mu2  = zeros(D2,M2); %D x M matrix
Sigma2 = zeros(D2,D2,M2); %DxDxM matrix
lambda2 = 0.3;
I2 = eye(M2,M2);

for i = 1:M2
     Int = randi([1 sN1],1,1);
     mu2(:,i) = s_train(1:D2,Int);
%     mu2(:,i) = s_train(1:D2,10*i+45);
end


Sigma2(:,:,1) = diag(diag(cov(transpose(s_train))));
for i = 1:D2 
    if(Sigma2(i,i,1) == 0)
        Sigma2(i,i,1) = 0.1;                
    end
    Sigma2(i,i,1) = 0.5*Sigma2(i,i,1);
end

for i = 2:M2
    Sigma2(:,:,i) = Sigma2(:,:,1);
end

%Calculation of Design Matrix for Training Data
s_phi_train = zeros(sN1,M2);
s_phi_train(:,1) = 1;

for j = 2:M2
for i = 1:sN1
   t1 = transpose(s_train(:,i) - mu2(:,j));
   t2 = s_train(:,i) - mu2(:,j);
   s_phi_train(i,j) = exp(-0.5*( (t1 / Sigma2(:,:,j)) *t2));
end
end

s_phi_train_t = transpose(s_phi_train);
w2 = (inv((lambda2*I2)+(s_phi_train_t*s_phi_train)))*(s_phi_train_t*t_train);
w2_t = transpose(w2);

%Calculation of Design Matrix for Validation Data
s_phi_val = zeros(sN2,M2);
s_phi_val(:,1) = 1;

for j = 2:M2
for i = 1:sN2
   t1 = transpose(s_val(:,i) - mu2(:,j));
   t2 = s_val(:,i) - mu2(:,j);
   s_phi_val(i,j) = exp(-0.5*( (t1 / Sigma2(:,:,j)) *t2)); 
end
end

s_phi_val_t = transpose(s_phi_val);

%Error Calculation for Synthetic data
trainPer2 = sqrt(transpose(s_phi_train * w2 - t_train)*(s_phi_train * w2 - t_train)/sN1);

validPer2 = sqrt(transpose(s_phi_val * w2 - t_val)*(s_phi_val * w2 - t_val)/sN2);


%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%Stochastic Gradient Method for Real data

E1 =55700;
w01 = 10*rand(M1,1); % M x 1 matrix
dw1 = zeros(M1,E1); % M x E matrix
eta1 = zeros(1,E1); % 1 x E matrix 
wt1 = zeros(M1,E1);
ERMS1 = zeros(1,E1);

wt1(:,1) = w01;
eta1(:,1) = 1;
new_eta1 = eta1(:,1);
ERMS1(1,1)= sqrt(transpose(r_phi_train * w01 - y_train)*(r_phi_train * w01 - y_train)/rN1);

for i = 1:E1
    
    deltaE = (( -y_train(i,1) + ( transpose(wt1(:,i)) * r_phi_train_t(:,i) ) )* r_phi_train_t(:,i)) + lambda1 * wt1(:,i);
    
    dw1(:,i) =  -1 * new_eta1 * deltaE;
    
    wt1(:,i+1) = wt1(:,i) + dw1(:,i);
    
    ERMS1(1,i+1)= sqrt(transpose(r_phi_train * wt1(:,i+1) - y_train)*(r_phi_train * wt1(:,i+1) - y_train)/rN1);
    
    eta1(:,i) = new_eta1;
    
    if(ERMS1(1,i+1) > ERMS1(1,i))
       new_eta1 = 0.5*new_eta1;
    end
     if(ERMS1(1,i)>ERMS1(1,i+1))
       new_eta1 = 1.1*new_eta1;
     end
    
     if(ERMS1(1,i) == ERMS1(1,i+1))
       new_eta1 = 1.0*new_eta1;
     end

end

%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%
%Stochastic Gradient Method for Synthetic data

E2 =1600;
w02 = 10*rand(M2,1);
dw2 = zeros(M2,E2); % M x E matrix
eta2 = zeros(1,E2); % 1 x E matrix
wt2 = zeros(M2,E2);
ERMS2 = zeros(1,E2);
wt2(:,1) = w02;
eta2(:,1) = 0.8;
new_eta2 = eta2(:,1);

ERMS2(1,1)= sqrt(transpose(s_phi_train * w02 - t_train)*(s_phi_train * w02 - t_train)/sN1);

for i = 1:E2
    
    deltaE = (( - t_train(i,1) + ( transpose(wt2(:,i)) * s_phi_train_t(:,i) ) )* s_phi_train_t(:,i)) + lambda2*wt2(:,i);
    
    dw2(:,i) =  -1* new_eta2 * deltaE;
    
    wt2(:,i+1) = wt2(:,i) + dw2(:,i);
    
    ERMS2(1,i+1)= sqrt(transpose(s_phi_train * wt2(:,i+1) - t_train)*(s_phi_train * wt2(:,i+1) - t_train)/sN1);
    eta2(:,i) = new_eta2;

    if (ERMS2(1,i)<ERMS2(1,i+1))
       new_eta2 = 0.5*new_eta2;
    end  
    
    if(ERMS2(1,i)>ERMS2(1,i+1))
      new_eta2 = 1.1*new_eta2;
    end
    
    if(ERMS2(1,i) == ERMS2(1,i+1))
      new_eta2 = 1.0*new_eta2;
    end
    
end

filename1= 'proj2';
save(filename1);









