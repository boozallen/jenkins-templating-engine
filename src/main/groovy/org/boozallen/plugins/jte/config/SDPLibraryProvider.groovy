package org.boozallen.plugins.jte.config

import org.boozallen.plugins.jte.console.TemplateLogger
import java.util.zip.ZipInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.io.BufferedReader
import java.nio.charset.StandardCharsets
import hudson.Extension 

@Extension class SDPLibraryProvider extends PluginLibraryProvider{

    HashMap libraries = [:]

    public static String getDescription(){
        "From a Library Providing Plugin"
    }

    SDPLibraryProvider(){
        def src = this.getClass().getProtectionDomain().getCodeSource()
        URL jar = src.getLocation()
        ZipFile zipFile = new ZipFile(new File(jar.toURI()))  
        ZipInputStream zipStream = new ZipInputStream(jar.openStream())
        ZipEntry zipEntry
        while( (zipEntry = zipStream.getNextEntry()) != null   ){
            String name = zipEntry.getName().toString()
            ArrayList parts = name.split("/")
            if(name.startsWith("libraries/") && name.endsWith(".groovy") && parts.size() >= 3){
                String libName = parts.getAt(1)
                String stepName = parts.last() - ".groovy" 
                if(!libraries[libName]){
                    libraries[libName] = [:]
                }
                libraries[libName][stepName] = getFileContents(zipFile, zipEntry) 
            } 
        }
    }

    String getFileContents(ZipFile z, ZipEntry e){
        InputStream stream = z.getInputStream(e)            
        StringBuilder stringBuilder = new StringBuilder()
        ArrayList lines = [] 
        try{
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))
            String line 
            while ((line = bufferedReader.readLine()) != null) {
                lines << line
            }
        }catch(any){}
        
        return lines.join("\n") 
    }

    public Boolean hasLibrary(String libName){
        return libName in libraries.keySet()
    }

    public List loadLibrary(String libName){
        TemplateLogger.print "Loading jar library ${libName}"
        libraries[libName].each{ stepName, stepContent -> 
            TemplateLogger.print "loading step -> ${stepName}"
            TemplateLogger.print stepContent
        }
        
        return new ArrayList()
    }



}