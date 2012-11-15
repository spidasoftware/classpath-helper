require 'pismo'

# Usage
# ruby find_root_of_dependency.rb /Users/toverly/Code/git/calcdb/calcdb jtidy jtidy 4aug2000r7-dev


def findRequires(doc, package, artifact, revision)
 root = []
 path ="//h4/a[@name=\"#{package}-#{artifact}-#{revision}\"]"
 #puts "Searching #{path}"
 doc.xpath(path).each do |node|
    name = node.attribute("name").to_s
    if (name.include? "#{artifact}") && (name.include? "#{revision}")
      img = node.parent.xpath('/span/img')
      if(img.to_s=="")
        requiredBy =doc.at("#{path}/parent::*/following-sibling::table/tbody")
        if(requiredBy.children.length==0 || requiredBy.to_s=="")
          root <- "#{artifact}-#{revision}"
        else
          # If there are higher levels then find what they requir
          requiredBy.children.each do |row|
            subPackage = row.children[0].text
            if(subPackage=="org.grails.internal")
              root << "#{name}"
            else
              subroot = findRequires(doc, subPackage, row.children[1].text, row.children[2].text)
              if(subroot.length>0)
               root = root + subroot
              end
            end
          end
        end
      end
    end
  end
  return root
end

folder = ARGV[0]
package = ARGV[1]
artifact = ARGV[2]
revision = ARGV[3]

puts "Looking for #{package}.#{artifact}-#{revision} in #{folder}/target/dependency-report"

fileList = Dir.glob("#{folder}/target/dependency-report/**/*.html")
totalRoot = []
fileList.each do |f|
  puts "filtering #{f}"
  doc = Nokogiri::HTML(File.open(f).read)
  root = findRequires(doc, package, artifact, revision)
  totalRoot = totalRoot + root
end

puts "These root includes include that jar:"
totalRoot.uniq.each do |r|
  puts "#{r} >> #{package}.#{artifact}-#{revision}"
end


