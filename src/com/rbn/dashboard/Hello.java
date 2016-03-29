package com.rbn.dashboard;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.service.apigateway.importer.SwaggerApiFileImporter;
import com.amazonaws.service.apigateway.importer.config.ApiImporterDefaultModule;
import com.amazonaws.service.apigateway.importer.impl.ApiGatewaySwaggerFileImporter;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.rbn.dashboard.HelloPojo.RequestClass;
import com.rbn.dashboard.HelloPojo.ResponseClass;
import com.rbn.dashboard.HelloPojo.ResponseClass.Data;

/**
 * make sure you have a lambda execution role with iam:PassRole policy, logs, s3
 * make sure lambda has access to the s3 bucket 
 * Run and debug cloud formation
 * create handling for event type -> on create import api, on update update api, on delete, delete api
 */

/**
 * 
 * @author Tanya Atanasova <br>
 *         Created On: Feb 22, 2016 The Request Class RequestClass is defined in
 *         the HelloPojo.java The HelloPojo class is mapped to class Hello
 *         automatically by Lambda
 * 
 *
 * @since 1.0.0
 */
public class Hello implements RequestHandler<RequestClass, ResponseClass> {
	/**
	 * use function handler com.dashboard.rbn.Hello
	 */
	private String region = "us-east-1";

	private String apiId;
	private static final String updateSwaggerFile = "/tmp/temp.json";
	private static final String swaggerFile = "/tmp/local.json";
	private static final String apiGatewayPrefix = "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/";
	private static LambdaLogger logger;

	/**
	 * Expects an input: ApiRef
	 * [{"path":"/hello","method":"GET","functionName":"GetHelloFunction" }]
	 * Expects input SwaggerFile.json, expects a bucket for the swaggerFile
	 * 
	 **/

	public ResponseClass handleRequest(RequestClass request, Context context) {
		logger = context.getLogger();
		ResponseClass resp = new ResponseClass();

		/**
		 * validate using hybernate validator and javax.validate
		 */
		ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
		Validator validator = factory.getValidator();
		Set<ConstraintViolation<RequestClass>> ret = validator.validate(request);

		String message = "";

		if (!ret.isEmpty()) {
			for (ConstraintViolation<RequestClass> violation : ret) {
				message += violation.getPropertyPath() + " " + violation.getMessage() + "\n";
			}
			logger.log(message);

			resp = new ResponseClass("FAILED", "Validation Exception " + message, request.getStackId(),
			        request.getRequestId(), request.getPhysicalResourceId(), request.getLogicalResourceId(),
			        new Data());
			/**
			 * Create response for cloud formation
			 */
			createCfResponse(request.getResponseUrl(), resp);

			throw new IllegalArgumentException(message);
		}

		try {
			// get the api file and save to /tmp/local.json
			if (request.getRequestType().equals("Update")) {
				File apiFile = S3GetJson(request.getResourceProperties().getS3Bucket(),
				        request.getResourceProperties().getSwaggerFile());

				/**
				 * you don't need to do this unless your api has functions
				 * created by Cloud Formation This function will replace the the
				 * ApiGateway's swagger json file's lambda functions uris this
				 * will also setup your api gateway role or "credentials" iam
				 * for your api gateway to execute lambda
				 * 
				 * if you do not want to replace function uris then comment out
				 * ReplaceUri func and use
				 * updateApi(request.getPhysicalResourceId(), swaggerFile);
				 * swaggerFile (in-memory file at /tmp/local.json) should be
				 * written to by the s3 getJson method
				 */

				ReplaceUri(request, apiFile);
				// update the api with the newly updated swagger file
				updateApi(request.getPhysicalResourceId(), updateSwaggerFile);

				/**
				 * return response set PhysicalResourceId to the apiID, set
				 * Reason, set Status, etc, check out HelloPojo class for
				 * ResponseClass constructor
				 */

				resp = new ResponseClass("SUCCESS", "Update Complete " + request.getPhysicalResourceId(),
				        request.getStackId(), request.getRequestId(), request.getPhysicalResourceId(),
				        request.getLogicalResourceId(), new Data());

			} else if (request.getRequestType().equals("Create")) {
				File apiFile = S3GetJson(request.getResourceProperties().getS3Bucket(),
				        request.getResourceProperties().getSwaggerFile());

				ReplaceUri(request, apiFile);
				apiId = importApi(updateSwaggerFile);

				// setup response
				resp = new ResponseClass("SUCCESS", "Create Complete " + apiId, request.getStackId(),
				        request.getRequestId(), apiId, request.getLogicalResourceId(), new Data());

			} else if (request.getRequestType().equals("Delete")) {

				deleteApi(request.getPhysicalResourceId());

				// setup response
				resp = new ResponseClass("SUCCESS", "Delete Complete " + request.getPhysicalResourceId(),
				        request.getStackId(), request.getRequestId(), request.getPhysicalResourceId(),
				        request.getLogicalResourceId(), new Data());
			}

		} catch (Exception e) {
			logger.log(e.getMessage());

			// create response for the exception
			resp = new ResponseClass("FAILED", "Api Exception: " + e.getMessage(), request.getStackId(),
			        request.getRequestId(), request.getPhysicalResourceId(), request.getLogicalResourceId(),
			        new Data());

			// send response to cloud formation prior to throwing exception
			createCfResponse(request.getResponseUrl(), resp);

			throw new RuntimeException("Api Exception", e);
		}
		/**
		 * Hit up cloud formation with a put call to the response url and send
		 * our response object. Yes you have to do this in JAVA, no this is not
		 * python
		 */
		createCfResponse(request.getResponseUrl(), resp);

		return resp;

	}

	/**
	 * Imports api gateway on stack create
	 * 
	 * @param fileName
	 * @return
	 */
	private String importApi(String fileName) throws Exception {

		/**
		 * use default AWS credentials env provider chain, the default
		 * credential chain provider does not work in lambda
		 */

		AWSCredentialsProvider credentialsProvider = new AWSCredentialsProviderChain(
		        new EnvironmentVariableCredentialsProvider());
		// injects dependencies
		Injector injector = Guice.createInjector(new ApiImporterDefaultModule(credentialsProvider, region));

		// construct the SwaggerApiFileImporter class
		SwaggerApiFileImporter importer = injector.getInstance(ApiGatewaySwaggerFileImporter.class);

		// import api

		apiId = importer.importApi(fileName);

		return apiId;
	}

	/**
	 * Updates Api on Stack updates
	 * 
	 * @param apiId
	 * @param fileName
	 */
	private void updateApi(String apiId, String fileName) throws Exception {

		AWSCredentialsProvider credentialsProvider = new AWSCredentialsProviderChain(
		        new EnvironmentVariableCredentialsProvider());

		// injects dependencies
		Injector injector = Guice.createInjector(new ApiImporterDefaultModule(credentialsProvider, region));
		// injector.getInstance(key)
		// construct the SwaggerApiFileImporter class
		SwaggerApiFileImporter importer = injector.getInstance(ApiGatewaySwaggerFileImporter.class);

		// update api
		importer.updateApi(apiId, fileName);

	}

	/**
	 * Deletes the Api on stack Delete
	 * 
	 * @param apiId
	 */
	private void deleteApi(String apiId) throws Exception {
		AWSCredentialsProvider credentialsProvider = new AWSCredentialsProviderChain(
		        new EnvironmentVariableCredentialsProvider());

		// injects dependencies
		Injector injector = Guice.createInjector(new ApiImporterDefaultModule(credentialsProvider, region));

		// construct the SwaggerApiFileImporter class
		SwaggerApiFileImporter importer = injector.getInstance(ApiGatewaySwaggerFileImporter.class);

		// delete api
		importer.deleteApi(apiId);

	}

	/**
	 * deploys Api Gateway, we need stage param such as "develop, stage etc" NOT
	 * IN USE for now
	 * 
	 * @param apiId
	 * @param stage
	 */
	private void deployApi(String apiId, String stage) throws Exception {
		AWSCredentialsProvider credentialsProvider = new AWSCredentialsProviderChain(
		        new EnvironmentVariableCredentialsProvider());

		// injects dependencies
		Injector injector = Guice.createInjector(new ApiImporterDefaultModule(credentialsProvider, region));

		// construct the SwaggerApiFileImporter class
		SwaggerApiFileImporter importer = injector.getInstance(ApiGatewaySwaggerFileImporter.class);

		// deploy stage
		importer.deploy(apiId, stage);

	}

	/**
	 * This function is not necessary if you are just trying to update your api
	 * with a complete Swagger file. This function is used by Cloud Formation to
	 * update the api with the corresponding lambda functions and role arns from
	 * stacks created. It takes an list of type RefApi and updates the
	 * corresponding paths/methods.
	 * 
	 * @param request
	 * @param apiFile
	 * @return
	 * @throws IOException
	 * @throws JsonMappingException
	 * @throws JsonParseException
	 */
	public File ReplaceUri(RequestClass request, File apiFile)
	        throws JsonParseException, JsonMappingException, IOException {
		ObjectMapper mapper = new ObjectMapper();
		File outputFile = new File(updateSwaggerFile);
		ArrayList<RefApi> refApi = request.getResourceProperties().getRefApi();
		String credentials = request.getResourceProperties().getRole();
		String crossDomainRef = request.getResourceProperties().getStaticHostingUrl();
		/**
		 * Sample lambda uri to api gateway call: aws apigateway get-integration
		 * --rest-api-id amum2jtqje --resource-id qds75w --http-method POST
		 * "uri":
		 * "arn:aws:apigateway:us-east-1:lambda:path/2015-03-31/functions/arn:aws:lambda:us-east-1:123456789:function:getHello:functionGetHelloFunction/invocations",
		 * 
		 */

		String func;

		Map<String, Object> apiData = mapper.readValue(apiFile, Map.class);
		// Map<String, Object> pathData = new HashMap<String, Object>();
		for (RefApi api : refApi) {

			for (Map.Entry<String, Object> entry : apiData.entrySet()) {

				if (entry.getKey().equals("paths")) {
					Map<String, Object> paths = (Map<String, Object>) entry.getValue();

					for (Map.Entry<String, Object> path : paths.entrySet()) {
						Map<String, Object> methods = (Map<String, Object>) path.getValue();
						if (path.getKey().equals(api.getPath())) {
							for (Map.Entry<String, Object> method : methods.entrySet()) {
								// logger.log("method " + method.getValue());
								if (method.getKey().equals(api.getMethod())) {
									Map<String, Object> sections = (Map<String, Object>) method.getValue();
									for (Map.Entry<String, Object> section : sections.entrySet()) {
										func = null;
										if (section.getKey().equals("x-amazon-apigateway-integration")) {

											Map<String, String> integration = (Map<String, String>) section.getValue();

											func = apiGatewayPrefix + api.FunctionArn + "/invocations";

											integration.put("uri", func);
											integration.putIfAbsent("credentials", credentials);
											integration.put("credentials", credentials);

											section.setValue(integration);

											// go further, we need to update
											// CORS
											// params
											Map<String, Object> responses = (Map<String, Object>) section.getValue();
											for (Map.Entry<String, Object> response : responses.entrySet()) {
												if (response.getKey().equals("responses")) {
													Map<String, Object> def = (Map<String, Object>) response.getValue();
													for (Map.Entry<String, Object> res : def.entrySet()) {
														if (res.getKey().equals("default")) {
															Map<String, Object> corses = (Map<String, Object>) res
															        .getValue();
															for (Map.Entry<String, Object> cors : corses.entrySet()) {
																if (cors.getKey().equals("responseParameters")) {
																	Map<String, String> rules = (Map<String, String>) cors
																	        .getValue();
																	rules.put(
																	        "method.response.header.Access-Control-Allow-Origin",
																	        "'http://" + crossDomainRef + "'");
																	cors.setValue(rules);
																	// System.out.println(rules.toString());
																}
																// responseTemplate.setValue(rules);
																corses.put(cors.getKey(), cors.getValue());
															}
														}
														def.put(res.getKey(), res.getValue());
													}
												}
												responses.put(response.getKey(), response.getValue());
											} // end loop through responses
										}

										sections.put(section.getKey(), section.getValue());
									}
								}
								methods.put(method.getKey(), method.getValue());
							}
							paths.put(path.getKey(), path.getValue());
						}
					}
				}
				apiData.put(entry.getKey(), entry.getValue());
			}
		}

		mapper.writeValue(outputFile, apiData);
		return outputFile;

	}

	public File S3GetJson(String bucketName, String key) throws Exception {
		AmazonS3 s3 = new AmazonS3Client();

		// save it to a local temp file
		File f = new File(swaggerFile);

		// get object anf write to local file swaggerFile
		s3.getObject(new GetObjectRequest(bucketName, key), f);

		return f;
	}

	public void createCfResponse(String responseUrl, ResponseClass resp) {
		Gson gson = new Gson();
		String json = gson.toJson(resp);

		URL url;
		try {
			url = new URL(responseUrl);
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			connection.setDoOutput(true);
			connection.setRequestMethod("PUT");

			OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
			out.write(json);
			out.close();
			int responseCode = connection.getResponseCode();
			logger.log("Response Code: " + responseCode);

		} catch (MalformedURLException e) {
			logger.log(e.getMessage());
			e.printStackTrace();
		} catch (ProtocolException e) {
			logger.log(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			logger.log(e.getMessage());
			e.printStackTrace();
		}

	}

}
