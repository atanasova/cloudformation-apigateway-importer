# cloudformation-apigateway-importer
Configuring and deploying AWS Api Gateway with Cloud Formation and AWS Lambda
Developing with AWS Lambda and Api Gateway 

The release of AWS Lambda and AWS Api Gateway has made serverless Microservice architecture possible and viable option in building cost effective web apps. Considering the price of 1 million lambda executions is only $0.20 a small web app can run nearly free in the Cloud. There is only one problem for now, AWS has not included AWS Api Gateway in Cloud Formation, which makes continous integration and deployment of Api Gateway a hard if not impossible task. AWS has released a <a href="https://github.com/awslabs/aws-apigateway-importer" target="_blank">java library</a> (a cli tool to import the gateway, which is basically a shell script wrapped around a java jar) and officially endorsed Swagger File for Gateway's acceptable format. 
But here at RBN, we don't just stop when AWS stops. So we took a sample cloud formation input and hooked their java library into our java Lambda and finished the task. 

Includes:
<ol>
<li>Custom Java Lambda function api-gateway-importer.jar (sources and jar)</li>
<li>cf.json - Cloud Formation Template</li>
<li>MyApiGateway.json Swagger Api Gateway json file for import</li>
</ol>

How to implement:
<ol>
<li>Create S3 bucket. Use lowercase letters, avoid using periods. Example name:  <i><b>mybucket</b></i></li>
<li>Place your Api Gateway Lambda code (clients.zip) in  <i><b>mybucket</b></i> in directory clients</li>
<li>Place your Swagger File MyApiGateway.json in  <i><b>mybucket</b></i></li>
<li>Place api-gateway-importer.jar in  <i><b>mybucket</b></i></li>
<li>Update cloud formation template so that it defines all your lambda functions. All defined lambda functions must be present in  <i><b>mybucket</b></i></li>
<li>Create the stack </li>
</ol>

<p>For example in cf.json (our cloud formation template), the function UpdateClientFunction will need clients.zip to be present in <i><b>mybucket</b></i> in directory clients.</p>
 

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


Next also in cf.json, update the UseGateway with all your api methods. The Java Lambda function (api-gateway-importer.jar) provided here with sources will loop through the Swagger Api Gateway file and update all the methods with the Cloud Formation generated Lambda Functions and Cloud Formation generated Role/Policy Arn. This will gurantee you that your Api Gateway will have the permissions to invoke your lambda functions. 

<pre>
"UseGateway": {
            "Type": "Custom::UseGateway",
            "Properties": {
                "ServiceToken": { "Fn::GetAtt": [ "SetupApiGatewayFunction", "Arn"] },
                "SwaggerFile": { "Ref": "SwaggerFile" },
                "Role": { "Fn::GetAtt" : ["CFNRole", "Arn"] },
                "StaticHostingUrl": { "Ref" : "StaticS3Bucket"},
                "S3Bucket":{ "Ref": "S3BucketName" },
                "RefApi":[{
                            "Path":"/clients",
                            "Method":"get",
                            "FunctionName":{ "Ref": "GetClientsFunction" },
                            "FunctionArn":{"Fn::GetAtt": [ "GetClientsFunction", "Arn"]}
                          },
                          {
                            "Path":"/clients",
                            "Method":"post",
                            "FunctionName":{ "Ref": "PostClientsFunction" },
                            "FunctionArn":{"Fn::GetAtt": [ "PostClientsFunction", "Arn"]}
                          },
                          {
                            "Path":"/clients/{id}",
                            "Method":"get",
                            "FunctionName":{ "Ref": "GetClientFunction" },
                            "FunctionArn":{"Fn::GetAtt": [ "GetClientFunction", "Arn"]}
                          },
                          {
                            "Path":"/clients/{id}",
                            "Method":"put",
                            "FunctionName":{ "Ref": "UpdateClientFunction" },
                            "FunctionArn":{"Fn::GetAtt": [ "UpdateClientFunction", "Arn"]}
                          },
                          {
                            "Path":"/clients/{id}",
                            "Method":"delete",
                            "FunctionName":{ "Ref": "DeleteClientFunction" },
                            "FunctionArn":{"Fn::GetAtt": [ "DeleteClientFunction", "Arn"]}
                          }
                        ]
            }
        }

</pre>

