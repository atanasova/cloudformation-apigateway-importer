# cloudformation-apigateway-importer
This software is property of RBN, please use without any warranty. 

Deploying AWS Api Gateway with Cloud Formation and AWS Lambda

Includes:
<ol>
<li>Custom Java Lambda function api-gateway-importer.jar (sources and jar)</li>
<li>cf.json - Cloud Formation Template</li>
<li>MyApiGateway.json Swagger api gateway file for import</li>
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

