package com.rbn.dashboard;

import javax.validation.constraints.NotNull;

public class RefApi {
	@NotNull
	public String Path;

	@NotNull
	public String Method;

	@NotNull
	public String FunctionArn;

	@NotNull
	public String FunctionName;

	public RefApi() {
	}

	public String getPath() {
		return Path;
	}

	public void setPath(String path) {
		this.Path = path;
	}

	public String getFunctionArn() {
		return FunctionArn;
	}

	public void setFunctionArn(String functionArn) {
		this.FunctionArn = functionArn;
	}

	public String getFunctionName() {
		return FunctionName;
	}

	public void setFunctionName(String functionName) {
		this.FunctionName = functionName;
	}

	public String getMethod() {
		return Method;
	}

	public void setMethod(String method) {
		this.Method = method;
	}

}
