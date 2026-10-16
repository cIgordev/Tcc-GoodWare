package br.com.goodware.engine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import static org.junit.jupiter.api.Assertions.*;
class DependencyModuleTest {
 @ParameterizedTest @ValueSource(strings={"2.0","2.3","2.4","2.12.1","2.13.0","2.14.1"}) void affected(String v){assertTrue(DependencyModule.affected(v));}
 @ParameterizedTest @ValueSource(strings={"2.3.1","2.12.2","2.12.3","2.15.0","2.17.1","1.2.17","${version}","2.0-beta9"}) void notClaimedAffected(String v){assertFalse(DependencyModule.affected(v));}
 @Test void exactCoordinatesAndVersion(){String p="<project><dependencies><dependency><groupId>org.apache.logging.log4j</groupId><artifactId>log4j-core</artifactId><version>2.14.1</version></dependency></dependencies></project>";var m=new DependencyModule();assertEquals("DEPENDENCY_ADVISORY",m.scan("pom.xml","config",p).getFirst().category());assertTrue(m.scan("pom.xml","config",p.replace("log4j-core","log4j-api")).isEmpty());}
 @Test void rejectsXXEWithoutResolving(){String p="<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///etc/passwd'>]><project>&e;</project>";var f=new DependencyModule().scan("pom.xml","config",p);assertEquals("COVERAGE_NOTICE",f.getFirst().category());assertFalse(f.toString().contains("root:"));}
 @Test void malformedXmlReportsCoverage(){assertEquals("COVERAGE_NOTICE",new DependencyModule().scan("pom.xml","config","<bad>").getFirst().category());}
}
