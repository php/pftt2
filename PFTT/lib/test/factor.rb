# 
# Provides storage for results related to a particular factor. 
# 
# Provides inheritable properties hash for filtration
# 
# Provides inheritable requirements hash for ensuring compatibility.
# 
# Both properties and requirements hashes inherit up the standard 
# inheritance chain. This is done by tracing ancestor classes/modules
# and merging their appropriate hashes as we go. 
# 
# If the item we are asking about is a class, it checks all classes in ancestor order
# And if the item is an instance, it merges its own info on top of it's class' info.
# 

module Test
module Factor
  def results
    @_results ||= TestTelemetry::Array.new

  end

  def attach_result result
    self.results << result
  end

  # TODO: Refactor requirement and property logic (and their plurals) to share code.
  #   may depend on 1.9.2p290, which fixed a bug in multiple-def and __method__.
  def requirement arg
    @requirements ||= {}
    return @requirements.merge! arg if arg.is_a? Hash
    return requirements[arg]
  end

  def requirements deep=true
    @requirements ||= {}
    return @requirements unless deep
    if self.is_a? Class
      reqs={}
      self.ancestors.to_a.reverse_each do |ancestor|
        next unless ancestor.respond_to? :requirements
        reqs.merge! ancestor.requirements(false)
      end
      reqs
    else
      self.class.requirements.merge @requirements
    end
  end

  def property arg
    @properties ||= {}
    return @properties.merge! arg if arg.is_a? Hash
    return properties[arg]
  end
  
  def properties deep=true
    @properties ||= {}
    return @properties unless deep
    if self.is_a? Class
      props={}
      self.ancestors.to_a.reverse_each do |ancestor|
        next unless ancestor.respond_to? :properties
        props.merge! ancestor.properties(false)
      end
      props
    else
      self.class.properties.merge @properties
    end
  end

  # returns true if <self> meets the requirements of <other_factor>.
  # note that if both requirement and property for the same key are arrays,
  #   only an overlap is required; if you want more specific matches, use more keys.
  def meets_requirements_of? other_factor
    meets_requirements? other_factor.requirements
  end

  def meets_requirements? reqs
    catch(:compatibility) do
      properties.each_pair do |key,property|
        requirement = reqs[key]
        next if requirement.nil? # no requirement.
        next if property == requirement # perfect match.
        if requirement.is_a? Array
          next if requirement.include? property # included match
          next if property.is_a? Array and !(property & requirement).empty? # overlap match
        end
        throw( :compatibility, false )
      end
      throw( :compatibility, true )
    end
  end

  def self.included base
    base.class_exec(self) do |mod|
      extend mod
    end
  end
end

# 
# Include into a TypedArray to add functions that allow us to filter out items that do not
# match the requirement given. Relies on the contents implementing Test::Factors
# 
module FactorArray
  def filter(requirements_hsh)
    return self if requirements_hsh.nil?
    select{|factor| factor.meets_requirements? requirements_hsh}
  end
  def filter!(requirements_hsh)
    self.replace! self.filter requirements_hsh
  end
end

end # module Test
