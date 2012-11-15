#!/usr/bin/env groovy
import com.rvkb.util.jar.*
import java.util.jar.JarFile
import java.io.*

///Totally combile the rest of the classpath scripts
def console = System.console()

def response
println "What would you like to do? "
println "  1) Find a given class in a directory of jars."
println "  2) Find a given class in a war file's lib folder."
println "  3) Compare two wars to see the difference in included jars in the lib folders."
println "  4) Run a general grails war analyzer to look for common issues."
println "  5) Look in the grails dependency report/lib folder for the root include for a given jar."
response = console.readLine("> Enter your choice: ")

switch(response) {
	case "1":
		def searchClass = console.readLine("> What class are you looking for? (ex: com.company.MyClass): ").toString().trim().replaceAll("(\\.)", {"/"})
		def directory = console.readLine("> What directory should we search? (relative or absolute): ")
		def file = new File(directory.toString().trim())
		if(!file.exists()){
			println "File (${directory}) does not exist."
			return
		}
		def containingJars = []
		for(jar in file.listFiles()){
			if(jar.name.endsWith(".jar")){
				def ju = new JarUtil(jarFile: new JarFile(jar))
				def pw = new PrintWriter(new File("lib/temp.txt"))
				ju.output = pw
				ju.list()
				new File("lib/temp.txt").eachLine{line ->
					if(line.contains(searchClass)) containingJars.add jar.name
				}
				new File("lib/temp.txt").delete()
			}
		}
		println "The following jars contain the class ${searchClass}:"
		 containingJars.unique().each{println "  ${it}"}
	break
	case "2":
		def searchClass = console.readLine("> What class are you looking for? (ex: com.company.MyClass): ").toString().trim().replaceAll("(\\.)", {"/"})
		def directory = console.readLine("> What war should we search? (relative or absolute): ")
		def file = new File(directory.toString().trim())
		if(!file.exists()){
			println "File (${directory}) does not exist."
			return
		}
		def containingJars = []
		def ju = new JarUtil(jarFile: new JarFile(file))
		def pw = new PrintWriter(new File("lib/temp.txt"))
		ju.output = pw
		ju.list()
		new File("lib/temp.txt").eachLine{line ->
			if(line.contains(searchClass)){
				def cleanLine = line.replaceAll("(WEB-INF/lib/)", {""}).replaceAll("(/${searchClass}.+)".toString(), {""})
				containingJars.add cleanLine
			} 
		}
		new File("lib/temp.txt").delete()
			
		println "The following jars contain the class ${searchClass}:"
		 containingJars.unique().each{println "  ${it}"}
	break
	case "3":
		//Compare two war files
		def path1 = console.readLine("> What is the first war file we use? (relative or absolute): ")
		def path2 = console.readLine("> What is the second war file we use? (relative or absolute): ")

		def file1 = new File(path1.toString().trim())
		def file2 = new File(path2.toString().trim())

		if(!file1.exists() || !file2.exists()){
			println "One or both file(s) does not exist."
		  	return
		}
		println("Comparing jars in $file1 and $file2")
		println("This can be slow.")

		def ju = new JarUtil(jarFile: new JarFile(file1))
		def pw = new PrintWriter(new File("lib/temp.txt"))
		ju.output = pw
		ju.list()
		def war0Jars = []
		new File("lib/temp.txt").eachLine{line ->
			if(line.endsWith(".jar")) war0Jars.add line.replace("WEB-INF/lib/","")
		}
		new File("lib/temp.txt").delete()		

		ju = new JarUtil(jarFile: new JarFile(file2))
		pw = new PrintWriter(new File("lib/temp.txt"))
		ju.output = pw
		ju.list()
		def war1Jars = []
		new File("lib/temp.txt").eachLine{line ->
			if(line.endsWith(".jar")) war1Jars.add line.replace("WEB-INF/lib/","")
		}
		new File("lib/temp.txt").delete()

		def oneNotTwo = war0Jars.findAll{!war1Jars.contains(it)}
		def twoNotOne = war1Jars.findAll{!war0Jars.contains(it)}
		println "  -----------------------------"
		println "  1 has ${war0Jars.size()} jars."
		println "  2 has ${war1Jars.size()} jars."
		println "  -----------------------------"
		println "  Jars that are in 1 but not 2:"
		for(jar in oneNotTwo){
		  println "    $jar"
		}
		println "  -----------------------------"
		println "  Jars that are in 2 but not 1:"
		for(jar in twoNotOne){
		  println "    $jar"
		}
	break
	case "4":
		def directory = console.readLine("> What war file should we analyze? (relative or absolute): ")
		def file = new File(directory.toString().trim())
		
		def path = file.absolutePath
		def name = file.name

		if(!file.exists()){
		  println "${file.name} does not exist."
		  return
		}

		println "War: $name"
		println "Getting jar list from war."

		def jars = []		
		def ju = new JarUtil(jarFile: new JarFile(file))
		def pw = new PrintWriter(new File("lib/temp.txt"))
		ju.output = pw
		ju.list()
		new File("lib/temp.txt").eachLine{line ->
			if(line.endsWith(".jar")) jars.add line.replace("WEB-INF/lib/","")
		}
		new File("lib/temp.txt").delete()

		
		println "-----------------------------------------"
		def groovyFiles = jars.findAll{it.contains('groovy-') || it.contains('groovy-all-')}
		println "Checking number of groovy jars: "
		println "  ${groovyFiles.size()} groovy files found, make sure they aren't duplicates."
		println "  "+groovyFiles.toString()

		def justNames = []
		for(j in jars){
		  j.replaceAll(/\D(\-((\.|\d|[a-zA-Z])|-SNAPSHOT)+jar)/) { match ->
		    justNames.add j.replace(match[1],"")
		  }
		}
		def unique = justNames.unique(false)

		def duplicateNames = []
		for(u in unique){
		  if(justNames.findAll{it==u}.size()>1) duplicateNames.add u
		}

		println "-----------------------------------------"
		println "Checking the duplicate jar names, there shouldn't be any/many of these."
		println " - If the files listed aren't duplicates, we regex'd it wrong, sorry."
		println "  Unique size: ${unique.size()}"
		println "  Best guess at some duplicates: "
		for(d in duplicateNames){
		  println "  $d"
		  for(o in jars){
		    if(o.startsWith(d+"-")) println "    - $o"
		  }
		}
		println "-----------------------------------------"
	break
	case "5":
		def directory = console.readLine("> What directory is the root of the grails app? (relative or absolute): ").toString().trim()
		def group = console.readLine("> What is the group? (ex: com.company): ").toString().trim()
		def artifact = console.readLine("> What is the artifact? (ex: commons): ").toString().trim()
		def version = console.readLine("> What is the version? (ex: 1.24) : ").toString().trim()
		//ruby find_root_of_dependency.rb /Users/toverly/Code/git/calcdb/calcdb jtidy jtidy 4aug2000r7-dev
		def commands = ["ruby", "lib/find_root_of_dependency.rb"]
		commands.add directory
		commands.add group
		commands.add artifact
		commands.add version
		def sout = new StringBuffer()
		def serr = new StringBuffer()
		def process = commands.execute()
		process.consumeProcessOutput(sout, serr)
		process.waitFor()
		println '' + serr
		println '' + sout // => test text
	break
	default:
		println "Invalid choice."
	break
}
