package app;

import org.apache.catalina.Context;
import org.apache.catalina.startup.Tomcat;
import java.io.File;
import controller.*;
import util.HibernateUtil;
import org.apache.catalina.servlets.DefaultServlet;

public class Main {
    public static void main(String[] args) throws Exception {
        System.out.println("Initializing Hibernate...");
        HibernateUtil.getSessionFactory();
        System.out.println("Hibernate initialized");
        
        Tomcat tomcat = new Tomcat();
        tomcat.setPort(8088);
        
        String projectRoot = new File(".").getCanonicalPath();
        String docBase = new File(projectRoot, "src/main/webapp").getAbsolutePath();
        File docBaseFile = new File(docBase);
        
        System.out.println("Project Root: " + projectRoot);
        System.out.println("DocBase: " + docBase);
        System.out.println("DocBase exists: " + docBaseFile.exists());
        System.out.println("DocBase is directory: " + docBaseFile.isDirectory());
        
        Context context = tomcat.addContext("", docBase);
        context.addWelcomeFile("login.html");
        
        Tomcat.addServlet(context, "default", new DefaultServlet());
        context.addServletMappingDecoded("/*", "default");
        
        context.setResources(new org.apache.catalina.webresources.StandardRoot(context));
        context.getResources().setAllowLinking(true);
        
       
        tomcat.addServlet("", "LeaveServlet", "controller.LeaveServlet");
        context.addServletMappingDecoded("/leave/*", "LeaveServlet");
        
        
        tomcat.addServlet("", "LoginServlet", "controller.LoginServlet");
        context.addServletMappingDecoded("/login", "LoginServlet");
        
        tomcat.addServlet("", "SignupServlet", "controller.SignupServlet");
        context.addServletMappingDecoded("/signup", "SignupServlet");
        
        tomcat.getConnector();
        System.out.println("Starting Tomcat...");
        tomcat.start();
        System.out.println("Tomcat started on http://localhost:8088");
        tomcat.getServer().await();
    }
}
