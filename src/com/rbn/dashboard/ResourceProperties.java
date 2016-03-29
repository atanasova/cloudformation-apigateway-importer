package com.rbn.dashboard;

import java.util.ArrayList;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class ResourceProperties {

	@NotNull
	public String SwaggerFile;

	@NotNull
	public String S3Bucket;

	@NotNull
	public String Role;

	@NotNull
	public String StaticHostingUrl;

	@NotNull
	@Valid
	public ArrayList<RefApi> RefApi;

	public ResourceProperties() {
	}

	public ArrayList<RefApi> getRefApi() {
		return RefApi;
	}

	public void setRefApi(ArrayList<RefApi> refApi) {
		this.RefApi = refApi;
	}

	public String getSwaggerFile() {
		return SwaggerFile;
	}

	public void setSwaggerFile(String swaggerFile) {
		this.SwaggerFile = swaggerFile;
	}

	public String getS3Bucket() {
		return S3Bucket;
	}

	public void setS3Bucket(String s3bucket) {
		this.S3Bucket = s3bucket;
	}

	public String getRole() {
		return Role;
	}

	public String getStaticHostingUrl() {
		return StaticHostingUrl;
	}

}
