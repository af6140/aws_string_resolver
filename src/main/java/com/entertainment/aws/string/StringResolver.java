package com.entertainment.aws.string;
//Reference Pattern
//        For SSM Parameters, the reference-key segment is composed of the parameter name and version number. Use the following pattern:
//
//        '{{resolve:ssm:parameter-name:version}}'
//
//        Your reference must adhere to the following regular expression pattern for parameter-name and version:
//
//        '{{resolve:ssm:[a-zA-Z0-9_.-/]+:\\d+}}'

//Reference Pattern
//        For ssm-secure dynamic references, the reference-key segment is composed of the parameter name and version number. Use the following pattern:
//
//        '{{resolve:ssm-secure:parameter-name:version}}'
//
//        Your reference must adhere to the following regular expression pattern for parameter-name and version:
//
//        '{{resolve:ssm-secure:[a-zA-Z0-9_.-/]+:\\d+}}'


//Reference Pattern
//        For Secrets Manager secrets, the reference-key segment is composed of several segments, including the secret id, secret value key, version stage, and version id. Use the following pattern:
//
//        {{resolve:secretsmanager:secret-id:secret-string:json-key:version-stage:version-id}}
//


//{{resolve:secretsmanager:secret-id:secret-string:json-key:version-stage:version-id}}

import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagement;
import com.amazonaws.services.simplesystemsmanagement.AWSSimpleSystemsManagementClientBuilder;
import com.amazonaws.services.simplesystemsmanagement.model.*;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringResolver {

    public static String TYPE_SSM_STRING ="ssm";
    public static String TYPE_SSM_SECURE_STRING ="ssm-secure";
    public static String TYPE_SECRET_MANAGER_STRING ="secretmanager";

    public static String SSM_STRING_PATTERN="\\{resolve:ssm:[a-zA-Z0-9_.\\-/]+(:[a-zA-Z0-9_.\\-]+)?\\}";
    //public static String SSM_STRING_PATTERN="\\{\\{resolve:ssm:[a-zA-Z0-9_.-/]+:\\d+\\}\\}";
    public static String SSM_SECURE_STRING_PATTERN="\\{resolve:ssm-secure:[a-zA-Z0-9_.\\-/]+(:[a-zA-Z0-9_.\\-]+)?\\}";
    public static String SECRET_MANAGER_STRING_PATTERN="\\{resolve:secretmanager:[a-zA-Z0-9_.\\-/]+:[a-zA-Z0-9_.\\-/]+:[a-zA-Z0-9_.\\-/]+:[a-zA-Z0-9_.\\-/]+:\\d+\\}";

    private AWSSimpleSystemsManagement  ssm =null;

    private Pattern SSM_STRING_REGEX;
    private Pattern SSM_SECURE_STRING_REGEX;
    private Pattern SECRET_MANAGER_STRING_REGEX;


    public StringResolver(){

        this.SSM_STRING_REGEX = Pattern.compile(SSM_STRING_PATTERN);
        this.SSM_SECURE_STRING_REGEX = Pattern.compile(SSM_SECURE_STRING_PATTERN);
        this.SECRET_MANAGER_STRING_REGEX = Pattern.compile(SECRET_MANAGER_STRING_PATTERN);
    }

    public StringResolver build() {
        if(ssm==null) {
            ssm = AWSSimpleSystemsManagementClientBuilder.standard().withRegion(System.getenv("AWS_REGION")).build();
        }
        return this;
    }

    public StringResolver withAWSSimpleSystemsManagement(AWSSimpleSystemsManagement ssm){
        this.ssm=ssm;
        return this;
    }


    public String getTemplateType(String s) {
        if (s != null) {
            if (SSM_STRING_REGEX.matcher(s).find()) {
                return TYPE_SSM_STRING;
            }else if (SSM_SECURE_STRING_REGEX.matcher(s).find()) {
                return TYPE_SSM_SECURE_STRING;
            }else if (SECRET_MANAGER_STRING_REGEX.matcher(s).find()) {
                return TYPE_SECRET_MANAGER_STRING;
            }else {
                return null;
            }
        }
        return null;
    }

    private String extractTemplate(String input) {
        String type = this.getTemplateType(input);
        String template = null;
        if (TYPE_SSM_STRING.equalsIgnoreCase(type)) {
            Matcher m = this.SSM_STRING_REGEX.matcher(input);
            template = m.group();
        }else if (TYPE_SSM_SECURE_STRING.equalsIgnoreCase(type)) {
            Matcher m = this.SSM_SECURE_STRING_REGEX.matcher(input);
            template = m.group();
        } else if (TYPE_SECRET_MANAGER_STRING.equalsIgnoreCase(type)) {
            Matcher m = this.SECRET_MANAGER_STRING_REGEX.matcher(input);
            template = m.group();
        }

        return template;
    }

    protected String extractTemplate(String input, String type) {
        String template = null;
        if (TYPE_SSM_STRING.equalsIgnoreCase(type)) {
            Matcher m = this.SSM_STRING_REGEX.matcher(input);
            if (m.find()) {
                template = m.group(0);
            }else {
            }
        }else if (TYPE_SSM_SECURE_STRING.equalsIgnoreCase(type)) {
            Matcher m = this.SSM_SECURE_STRING_REGEX.matcher(input);
            if (m.find()) {
                template = m.group(0);
            }
        } else if (TYPE_SECRET_MANAGER_STRING.equalsIgnoreCase(type)) {
            Matcher m = this.SECRET_MANAGER_STRING_REGEX.matcher(input);
            if(m.find()) {
                template = m.group(0);
            }
        }

        return template;
    }

    public String resolveSsmString(String input) throws  ResolveException{
        String template = this.extractTemplate(input, TYPE_SSM_STRING).replaceAll("\\{|\\}", "");
        if(template==null) {
            throw new ResolveException("Cannot extract template from :"+ input);
        }
        String[] specs = template.split(":", 3);
        String path = specs[2];
        GetParameterRequest req= new GetParameterRequest();
        req.setName(path);
        GetParameterResult result = null;
        try {
            result = this.ssm.getParameter(req);
        } catch (ParameterNotFoundException e) {
            throw new ResolveException("Cannot find parameter :"+path);
        }
        Parameter p = result.getParameter();
        p.setType(ParameterType.String);


        String s_value =  p.getValue();
        return input.replaceAll(SSM_STRING_PATTERN, s_value);

    }

    public String resolveSsmSecureString(String input) throws ResolveException{
        String template = this.extractTemplate(input, TYPE_SSM_SECURE_STRING).replaceAll("\\{|\\}", "");
        String[] specs = template.split(":", 3);
        String path = specs[2];
        GetParameterRequest req= new GetParameterRequest();
        req.setName(path);
        req.setWithDecryption(true);
        GetParameterResult result =null;
        try {
            result = this.ssm.getParameter(req);
        } catch (ParameterNotFoundException e) {
            throw new ResolveException("Cannot find parameter :"+path);
        }
        Parameter p = result.getParameter();
        p.setType(ParameterType.SecureString);

        String s_value =  p.getValue();
        return input.replaceAll(SSM_SECURE_STRING_PATTERN, s_value);
    }

    public String resolveSecretManagerString(String input) {
        return input;
    }

    public String resolve(String input) throws ResolveException{
        String result = input;
        String type = this.getTemplateType(input);
        if (type ==null ) {
            return result;
        }

        if (TYPE_SSM_STRING.equalsIgnoreCase(type)) {
            result = this.resolveSsmString(input);
        }else if (TYPE_SSM_SECURE_STRING.equalsIgnoreCase(type)) {
            result = this.resolveSsmSecureString(input);
        } else if (TYPE_SECRET_MANAGER_STRING.equalsIgnoreCase(type)) {
            throw new ResolveException("Not supported yet.");
            //result = this.resolveSecretManagerString(input);
        }
        return result;
    }
}
