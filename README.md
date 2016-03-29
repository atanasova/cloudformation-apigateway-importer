# cloudformation-apigateway-importer
<h5>Configuring and deploying AWS Api Gateway with CloudFormation and AWS Lambda</h5>

Includes:
<ol>
<li>Custom Java Lambda function api-gateway-importer.jar (sources and jar)</li>
<li>cf.json - CloudFormation Template</li>
<li>MyApiGateway.json Swagger Api Gateway json file for import</li>
</ol>

How to implement:
<ol>
<li>Create S3 bucket. Use lowercase letters, avoid using periods. Example name:  <i><b>mybucket</b></i></li>
<li>Place your Api Gateway Lambda code (clients.zip) in  <i><b>mybucket</b></i> in directory clients</li>
<li>Place your Swagger File MyApiGateway.json in  <i><b>mybucket</b></i></li>
<li>Place api-gateway-importer.jar in  <i><b>mybucket</b></i></li>
<li>Update CloudFormation template so that it defines all your lambda functions. All defined Lambda functions must be present in  <i><b>mybucket</b></i></li>
<li>Create the stack </li>
</ol>

<p>For example in cf.json (our CloudFormation template), the function UpdateClientFunction will need clients.zip to be present in <i><b>mybucket</b></i> in directory clients.</p> clients.zip is not a part of the repsitory. Use zip -r9 clients.zip clients.py to zip up a file for Lambda and upload to <i><b>mybucket</b></i>
 

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
        }
</pre>


Next also in cf.json, update the UseGateway with all your api methods. The Java Lambda function (api-gateway-importer.jar) provided here with sources will loop through the Swagger Api Gateway file and update all the methods with the CloudFormation generated Lambda Functions and CloudFormation generated Role/Policy Arn. This will gurantee you that your Api Gateway will have the permissions to invoke your lambda functions. 

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

When the stack creates, the created resource will have rest-api-id outputed in the resources tab in AWS Console. 

The CloudFormation template will ask for accout name, use lowercase anything. It will update CORS configuration for each Gateway method with a static bucket url, which will update Access-Control-Allow-Origin header to allow  a static S3 website in a bucket <your account>.static.dashboard.rbn.com to call your api gateway. You may alter the bucket name in the CloudFormation Template as you see fits. 
<pre>
"StaticS3Bucket":{
            "Type" : "AWS::S3::Bucket",
            "Properties" : {
                "BucketName" :{"Fn::Join": [".",[{ "Ref": "AccountName" }, "static.dashboard.rbn.com"]]},
                "WebsiteConfiguration" : {
                    "ErrorDocument" : "error.html",
                    "IndexDocument" : "index.html"
                }
            }
        }
</pre>
