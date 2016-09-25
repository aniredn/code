
% Project 3 - Classification
tic
UBitName = ['a' 'n' 'i' 'r' 'e' 'd' 'n'];
personNumber=['5' '0' '1' '6' '9' '2' '4' '0'];

%Extraction of Data (http://ufldl.stanford.edu/wiki/index.php/Using_the_MNIST_Dataset)

% Change the filenames if you've saved the files under different names
% On some platforms, the files might be saved as
% train-images.idx3-ubyte / train-labels.idx1-ubyte
images = loadMNISTImages('train-images-idx3-ubyte'); %D x N
labels = loadMNISTLabels('train-labels-idx1-ubyte'); %N x 1

%Dimensions
D = 784;
K = 10;
N = 60000;

x = images;
t = zeros(N,K);

for i=1:60000
    t(i,(labels(i,1) + 1)) = 1;
end

%Testing Data
images_test = loadMNISTImages('t10k-images-idx3-ubyte');
labels_test = loadMNISTLabels('t10k-labels-idx1-ubyte');

% LOGISTIC REGRESSION

iter_lr = 500; % 500
eta_lr = 0.0002; %0.0002 = 10%, 0.002 - 14%, 0.00002 - 12% - rand b
w = rand(D,K)*10; %rand
b_lr = rand(1,K)*10;
act = zeros(D,K+1);
err_entropy_lr = zeros(1,iter_lr); %preallocation
weight_lr = transpose(vertcat(w,b_lr));

Wlr = zeros(D,K);
blr = zeros(1,K);

% % Mini Batch Stochastic Gradient Descent - 10%
% one = ones(1,100);
% temp = 1;
% p=1;
% for j = 1:(N/100)
%     x_mini = x(:,temp:100*j);
%     x_1 = vertcat(x_mini,one);
%     for i = 1:iter_lr
%         act = (weight_lr*x_1);
%         y = transpose(softmax(act));
%         %Cross entropy error
%         err_temp = (t(temp:100*j,:).*log(y) + (1-t(temp:100*j,:)).*log(1-y)) ;
%         err_entropy_lr(1,p) = -sum(err_temp(:)) ;
%         p = p+1;
%         weight_lr_next = transpose(weight_lr) - eta_lr * x_1 *(y - t(temp:100*j,:));
%         weight_lr = transpose(weight_lr_next);
%     end
%     temp = 100*j+1;
% end

%Gradient Descent - 7-8%
one = ones(1,60000);
x_1 = vertcat(x,one);

% for i = 1:iter_lr   %500
%
%     act = (weight_lr*x_1);
%     y = transpose(softmax(act));
%
%     %Cross entropy error
%     err_temp = (t.*log(y) + (1-t).*log(1-y)) ;
%     err_entropy_lr(1,i) = -sum(err_temp(:)) ;
%
%     weight_lr_next = transpose(weight_lr) - eta_lr * x_1 *(y - t);
%     weight_lr = transpose(weight_lr_next);
% end
%
%
% %Extracting Wlr and blr
% blr = weight_lr_next(785,:);
% Wlr = weight_lr_next(1:784,:);


% Wnn1 = zeros(D,J);
% Wnn2 = zeros(J,K);
% bnn1 = zeros(1,J);
% bnn2 = zeros(1,K);




% SINGLE LAYER NEURAL NETWORK
iter_nn = 60000;
J = 100; % Number of nodes
h = 'sigmoid';
eta_nn = 0.001;

w1 = rand(D,J);
w2 = rand(J,K);
b_nn1 = ones(1,J);
b_nn2 = ones(1,K);

weight_nn1 = transpose(vertcat(w1,b_nn1));
weight_nn2 = transpose(vertcat(w2,b_nn2));
one2 = ones(1,60000);
x_1 = vertcat(x,one2); %785 x 60000

outer_iter = 10;
%Preallocation
Errors = zeros(1,outer_iter);

counter = 0;

for j =1:outer_iter
    
    Error = 0;
    delE1 = zeros(D+1,J); %h
    delE2 = zeros(J+1,K); %o
    
    for i = 1:iter_nn %60000
        
        %Extract the data from dataset
        
        xi = x_1(:,i);
        
        % Feed Forward Propagation
        z = weight_nn1 * xi;
       % zj = logsig(z); %hidden layer
        zj = (1 + exp((-1) * z)).^ -1;
        zj_1 = vertcat(zj,1);
        
        ak = weight_nn2 * zj_1;
        
        
%         yk = transpose(logsig(ak)); %outer layer %softmax
        yk = (1 + exp((-1) * ak')).^ -1;
        %BackPropagation
        
        d_k = yk - t(i,:); % 1 x 10
        
        t1 = (1 + exp((-1) * zj_1)).^ -1;
        t2 = (1 + exp((-1) * (1-zj_1))).^ -1;
        
        d_j = (weight_nn2' * d_k') .* ( t1.*t2); %1 x 100
        d_j = d_j';
        
        d_j(:,end) = [];
        
        delE1 = delE1 + xi*d_j; %785 x 100
        delE2 = delE2 +zj_1 *d_k;   %101 x 10
        
        Error = Error + sum( ( t(i,:).*log(yk )) + ((1-t(i,:)).*log(yk)) );
    end
    
    Error = Error - (eta_nn/2) * (sum(sum(weight_nn1.*weight_nn1))+ sum(sum(weight_nn2.*weight_nn2)));
    Errors(1,j) = -1*(Error / iter_nn);
    
    %Weight Updation
    
    tempA = ( delE1 ./ iter_nn  ) + eta_nn.*weight_nn1';
    tempB = ( delE2 ./ iter_nn ) + eta_nn.*weight_nn2';
    weight_nn1_next = weight_nn1 - tempA';
    weight_nn2_next = weight_nn2 - tempB';
    
    weight_nn1 = weight_nn1_next;
    weight_nn2 = weight_nn2_next;
    counter = counter +1
    
end

%Extracting  Wnn1 and bnn1
bnn1 = weight_nn1_next(:,785);
Wnn1 = weight_nn1_next(:,1:end-1);

bnn1 = transpose(bnn1);
Wnn1 = transpose(Wnn1);

%Extracting  Wnn2 and bnn2
bnn2 = weight_nn2_next(:,end);
Wnn2 = weight_nn2_next(:,1:end-1);

bnn2 = transpose(bnn2);
Wnn2 = transpose(Wnn2);

%reshape(train_images(:,6),28,28)
%imshow(ans)


toc
disp ('DONE');

filename1= 'proj3';
save(filename1,'Wlr','blr','Wnn1','Wnn2','bnn1','bnn2','h');