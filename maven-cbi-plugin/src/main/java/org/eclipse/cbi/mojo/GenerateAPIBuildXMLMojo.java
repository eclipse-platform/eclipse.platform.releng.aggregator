package org.eclipse.tycho.pomgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.model.io.xpp3.MavenXpp3Writer;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.ReaderFactory;
import org.codehaus.plexus.util.xml.XmlStreamReader;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.eclipse.osgi.framework.adaptor.FilePath;
import org.eclipse.osgi.service.resolver.BundleDescription;
import org.eclipse.osgi.service.resolver.State;
import org.eclipse.tycho.ArtifactDescriptor;
import org.eclipse.tycho.ArtifactKey;
import org.eclipse.tycho.core.TychoProject;
import org.eclipse.tycho.core.osgitools.BundleReader;
import org.eclipse.tycho.core.osgitools.DefaultArtifactKey;
import org.eclipse.tycho.core.osgitools.DefaultReactorProject;
import org.eclipse.tycho.core.osgitools.DependencyComputer;
import org.eclipse.tycho.core.osgitools.EquinoxResolver;
import org.eclipse.tycho.core.osgitools.OsgiManifest;
import org.eclipse.tycho.core.osgitools.OsgiManifestParserException;
import org.eclipse.tycho.core.osgitools.targetplatform.DefaultTargetPlatform;
import org.eclipse.tycho.model.Feature;
import org.eclipse.tycho.model.FeatureRef;
import org.eclipse.tycho.model.PluginRef;
import org.eclipse.tycho.model.UpdateSite;
import org.osgi.framework.BundleException;

/**
 * @goal generate-api-build-xml
 * @phase generate-sources
 */
public class GenerateAPIBuildXMLMojo extends AbstractMojo {
	
	private static final String API_BUILD_XML_FILE = ".apibuild.xml";
	private static final String API_NATURE = "org.eclipse.pde.api.tools.apiAnalysisNature";
	
    /**
     * @parameter default-value="${project}"
     * @required
     * @readonly
     */
    protected MavenProject project;
    
    /**
     * @component role="org.eclipse.tycho.core.TychoProject"
     */
    private Map<String, TychoProject> projectTypes;
    
	public void execute() throws MojoExecutionException, MojoFailureException {
		File dotProject = new File(project.getBasedir(), ".project");
		if (!dotProject.exists()) {
			// no .project
			project.getProperties().setProperty("eclipserun.skip", "true");
			return;
		}
		if (dotProjectContainsApiNature(dotProject)) {
			generateBuildXML();
		} else {
			project.getProperties().setProperty("eclipserun.skip", "true");
		}
	}
	
	private boolean dotProjectContainsApiNature(File f){
		try{
			DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(f);
			doc.getDocumentElement().normalize();
			NodeList natures = doc.getElementsByTagName("nature");
			for (int i = 0; i < natures.getLength(); i++) {
				 
				   Node nature = natures.item(i);
				   String sNature = nature.getTextContent();
				   if( sNature != null){
					   if(API_NATURE.equals(sNature.trim())){
						   return true;
					   }
				   }
			}
		} catch (Exception e){
			e.printStackTrace();
			return false;
		}
		return false;
	}
	
	private void generateBuildXML(){
		try {
			System.out.println("Generating .apibuild.xml");
			File dotApiBuildXML = new File(project.getBasedir(), API_BUILD_XML_FILE);
			BufferedWriter bw = new BufferedWriter(new FileWriter(dotApiBuildXML));
			bw.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
			bw.write("<project name=\"apigen\" default=\"apigen\">\n");
			bw.write("  <target name=\"apigen\">\n");
			bw.write("  	<apitooling.apigeneration    \n");
			bw.write("      	projectname=\"" + calculateName() + "\"\n");
			bw.write("      	project=\"" + project.getBasedir() + "\"\n");
			bw.write("      	binary=\"" + project.getBuild().getDirectory() + "\"\n");
			bw.write("      	target=\"" + project.getBuild().getDirectory() + "/classes\"\n");
			bw.write("      	debug=\"true\"\n");
			bw.write("      \n");
			bw.write("      />\n");
			bw.write("  </target>\n");
			bw.write("</project>\n");
			bw.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String calculateName() {
		TychoProject projectType = projectTypes.get(project.getPackaging());
		ArtifactKey artifactKey = projectType
				.getArtifactKey(DefaultReactorProject.adapt(project));
		String symbolicName = artifactKey.getId();
		String version = artifactKey.getVersion();
		return symbolicName + "_" + version;
	}
}
