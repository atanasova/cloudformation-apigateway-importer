package com.rbn.dashboard;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class HelloPojo {

	/**
	 * usually I name object properties with lower case but CloudFormation uses
	 * upper case and because I had to map the request to the POJO, I had to use
	 * upper case
	 */

	public static class RequestClass {
		@NotNull
		public String StackId;

		@NotNull
		public String ResponseURL;

		@NotNull
		public String RequestId;

		@NotNull
		public String RequestType;

		@NotNull
		public String LogicalResourceId;

		// @NotNullOnRequestType(RequestType = "Update")
		public String PhysicalResourceId;

		@NotNull
		@Valid
		public ResourceProperties ResourceProperties;

		public ResourceProperties getResourceProperties() {
			return ResourceProperties;
		}

		@Valid
		public RequestClass() {
		}

		public String getRequestType() {
			return RequestType;
		}

		public String getPhysicalResourceId() {
			return PhysicalResourceId;
		}

		public String getStackId() {
			return StackId;
		}

		public String getRequestId() {
			return RequestId;
		}

		public String getLogicalResourceId() {
			return LogicalResourceId;
		}

		public String getResponseUrl() {
			return ResponseURL;
		}

	}

	public static class ResponseClass {
		public String PhysicalResourceId;
		public String Status;
		public String Reason;
		public String StackId;
		public String LogicalResourceId;
		public String RequestId;
		public Data Data;

		public ResponseClass() {
		}

		public ResponseClass(String Status, String Reason, String StackId, String RequestId, String PhysicalResourceId,
		        String LogicalResourceId, Data Data) {
			this.StackId = StackId;
			this.RequestId = RequestId;
			this.Status = Status;
			this.Reason = Reason;
			this.PhysicalResourceId = PhysicalResourceId;
			this.LogicalResourceId = LogicalResourceId;
			this.Data = Data;

		}

		public static class Data {
			public String value;

			public Data() {

			}

			public Data(String value) {
				this.value = value;
			}

		}

	}

}