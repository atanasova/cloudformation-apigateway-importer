# cloudformation-apigateway-importer
This software is property of RBN, please use without any warranty. 

Deploying AWS Api Gateway with Cloud Formation and AWS Lambda

Includes:
1. source and jar api-gateway-importer.jar
2. cf.json - Cloud Formtaion Template
3. MyApiGateway.json Swagger api gateway file for import


How to implement:

1. create S3 bucket and take a note of it. Use lowercase letters, avoid using periods. Example name:  <i>mybucket</i>
2. place your Api Gateway Lambda code (clients.zip) in  <i>mybucket</i> in directory clients
3. place your Swagger File MyApiGateway.json in  <i>mybucket</i>
4. place api-gateway-importer.jar in  <i>mybucket</i>
3. update cloud formation template so that it defines all your lambda functions. All defined lambda functions must be present in  <i>mybucket</i>
4. Create the stack 


 For example, the function UpdateClientFunction will need clients.zip to be present in mybucket in directory clients.
 

 <pre>
 "UpdateClientFunction": {
            "Type": "AWS::Lambda::Function",
            "Properties": {
                "Handler": "clients.update_client",
                "Role": { "Fn::GetAtt" : ["CFNRole", "Arn"] },
                "Code": {
                    "S3Bucket" : { "Ref": "S3BucketName" },
                    "S3Key" : "clients/clients.zip"
                },
                "Runtime": "python2.7"
            }
        },

</pre>
