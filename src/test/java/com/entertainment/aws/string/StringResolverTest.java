package com.entertainment.aws.string;
import org.junit.*;



public class StringResolverTest {

    String SSMWithVersion = "abc{ {resolve:ssm:/abc/abc:123}tbb";
    String templateSSMWithVersion = "{resolve:ssm:/abc/abc:123}";
    String SSMWithLabel = "abc{ {resolve:ssm:/abc/abc:test}tbb";
    String templateSSMWithLabel = "{resolve:ssm:/abc/abc:test}";
    String SSMWithOutVersion = "abc{ {resolve:ssm:/abc/abc}tbb";
    String templateSSMWithOutVersion = "{resolve:ssm:/abc/abc}";

    String secureSSMWithOutVersion = "abc{ {resolve:ssm-secure:/abc/abc}tbb";
    String templateSecureSSMWithOutVersion = "{resolve:ssm-secure:/abc/abc}";


    @Test
    public void testExtractTemplate() {
        StringResolver resolver = new StringResolver();
        String template = resolver.extractTemplate(SSMWithVersion, resolver.TYPE_SSM_STRING);
        assert (template.equals(templateSSMWithVersion));

        template = resolver.extractTemplate(SSMWithLabel, resolver.TYPE_SSM_STRING);
        assert (template.equals(templateSSMWithLabel));

        template = resolver.extractTemplate(SSMWithOutVersion, resolver.TYPE_SSM_STRING);
        assert (template.equals(templateSSMWithOutVersion));


        template = resolver.extractTemplate(secureSSMWithOutVersion, resolver.TYPE_SSM_SECURE_STRING);
        assert (template.equals(templateSecureSSMWithOutVersion));
    }

    @Test
    public void testGetTemplateType() {

        StringResolver resolver = new StringResolver();
        String type = resolver.getTemplateType(SSMWithOutVersion);
        assert (resolver.TYPE_SSM_STRING.equals(type));

        type = resolver.getTemplateType(secureSSMWithOutVersion);
        assert(resolver.TYPE_SSM_SECURE_STRING.equals(type));
    }

}
