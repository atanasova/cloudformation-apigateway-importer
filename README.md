# cloudformation-apigateway-importer
This software is property of RBN, please use without any warranty. 

Deploying AWS Api Gateway with Cloud Formation and AWS Lambda

Includes:
1. source and jar api-gateway-importer.jar
2. cf.json - Cloud Formtaion Template
3. MyApiGateway.json Swagger api gateway file for import


How to implement:

1. create S3 bucket and take a note of it. Use lowercase letters, avoid using periods. Example name:  mybucket
2. place your Api Gateway Lambda code in mybucket
3. place your Swagger File MyApiGateway.json in mybucket
4. place api-gateway-importer.jar in mybucket
3. update cloud formation template so that it defines all your lambda functions. All defined lambda functions must be present in mybucket
4. Create the stack 




