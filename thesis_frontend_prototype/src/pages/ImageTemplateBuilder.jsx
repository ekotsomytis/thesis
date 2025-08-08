import { useState, useEffect } from "react";
import { Button } from "../components/ui/button";
import { Card, CardContent, CardHeader, CardTitle } from "../components/ui/card";
import { Input } from "../components/ui/input";
import { Textarea } from "../components/ui/textarea";
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "../components/ui/select";
import { Checkbox } from "../components/ui/checkbox";
import { Label } from "../components/ui/label";
import { toast } from "react-hot-toast";
import api from "../services/api";

export default function ImageTemplateBuilder() {
  const [formData, setFormData] = useState({
    name: "",
    description: "",
    technology: "",
    version: "",
    preInstalledTools: [],
    environmentVariables: {},
    startupCommands: "",
    courseCode: "",
    isPublic: false
  });

  const [technologies, setTechnologies] = useState([]);
  const [technologyVersions, setTechnologyVersions] = useState({});
  const [availableTools, setAvailableTools] = useState([]);
  const [loading, setLoading] = useState(false);
  const [buildStatus, setBuildStatus] = useState("");

  useEffect(() => {
    loadTechnologies();
    loadAvailableTools();
  }, []);

  const loadTechnologies = async () => {
    try {
      const techs = await api.getWithToken('/images/technologies');
      const versions = await api.getWithToken('/images/technology-versions');
      setTechnologies(techs);
      setTechnologyVersions(versions);
    } catch (error) {
      console.error("Failed to load technologies:", error);
      toast.error("Failed to load technologies");
    }
  };

  const loadAvailableTools = () => {
    // Pre-defined list of common development tools
    setAvailableTools([
      { id: "git", name: "Git", category: "Version Control" },
      { id: "vim", name: "Vim", category: "Editor" },
      { id: "nano", name: "Nano", category: "Editor" },
      { id: "htop", name: "htop", category: "System Monitor" },
      { id: "tree", name: "Tree", category: "File Browser" },
      { id: "curl", name: "cURL", category: "Network" },
      { id: "wget", name: "wget", category: "Network" },
      { id: "jq", name: "jq", category: "JSON Processor" },
      { id: "docker", name: "Docker", category: "Containerization" },
      { id: "docker-compose", name: "Docker Compose", category: "Containerization" }
    ]);
  };

  const handleInputChange = (field, value) => {
    setFormData(prev => ({
      ...prev,
      [field]: value
    }));
  };

  const handleTechnologyChange = (technology) => {
    setFormData(prev => ({
      ...prev,
      technology,
      version: "" // Reset version when technology changes
    }));
  };

  const handleToolToggle = (toolId, checked) => {
    setFormData(prev => ({
      ...prev,
      preInstalledTools: checked 
        ? [...prev.preInstalledTools, toolId]
        : prev.preInstalledTools.filter(id => id !== toolId)
    }));
  };

  const handleEnvironmentVariableChange = (key, value) => {
    setFormData(prev => ({
      ...prev,
      environmentVariables: {
        ...prev.environmentVariables,
        [key]: value
      }
    }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    if (!formData.name || !formData.technology || !formData.version) {
      toast.error("Name, technology, and version are required");
      return;
    }

    setLoading(true);
    setBuildStatus("Creating template...");

    try {
      // Create the template
      const template = await api.postWithToken('/images', {
        ...formData,
        preInstalledTools: JSON.stringify(formData.preInstalledTools),
        environmentVariables: JSON.stringify(formData.environmentVariables)
      });

      setBuildStatus("Building Docker image...");
      toast.success("Template created successfully!");

      // Build the image
      const builtTemplate = await api.postWithToken(`/images/${template.id}/build`);
      
      setBuildStatus("Image built successfully!");
      toast.success("Docker image built successfully!");

      // Reset form
      setFormData({
        name: "",
        description: "",
        technology: "",
        version: "",
        preInstalledTools: [],
        environmentVariables: {},
        startupCommands: "",
        courseCode: "",
        isPublic: false
      });

    } catch (error) {
      console.error("Failed to create template:", error);
      toast.error("Failed to create template");
      setBuildStatus("Build failed");
    } finally {
      setLoading(false);
    }
  };

  const getVersionsForTechnology = (technology) => {
    return technologyVersions[technology] || [];
  };

  const getToolsByCategory = (category) => {
    return availableTools.filter(tool => tool.category === category);
  };

  const categories = [...new Set(availableTools.map(tool => tool.category))];

  return (
    <div className="container mx-auto p-6">
      <Card>
        <CardHeader>
          <CardTitle className="text-2xl font-bold">Create Custom Development Environment</CardTitle>
        </CardHeader>
        <CardContent>
          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Basic Information */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="name">Template Name *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) => handleInputChange("name", e.target.value)}
                  placeholder="e.g., Python 3.9 Development"
                  required
                />
              </div>
              
              <div>
                <Label htmlFor="description">Description</Label>
                <Input
                  id="description"
                  value={formData.description}
                  onChange={(e) => handleInputChange("description", e.target.value)}
                  placeholder="Brief description of the environment"
                />
              </div>
            </div>

            {/* Technology Selection */}
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              <div>
                <Label htmlFor="technology">Technology *</Label>
                <Select value={formData.technology} onValueChange={handleTechnologyChange}>
                  <SelectTrigger>
                    <SelectValue placeholder="Select technology" />
                  </SelectTrigger>
                  <SelectContent>
                    {technologies.map(tech => (
                      <SelectItem key={tech} value={tech}>
                        {tech.charAt(0).toUpperCase() + tech.slice(1)}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              
              <div>
                <Label htmlFor="version">Version *</Label>
                <Select 
                  value={formData.version} 
                  onValueChange={(value) => handleInputChange("version", value)}
                  disabled={!formData.technology}
                >
                  <SelectTrigger>
                    <SelectValue placeholder="Select version" />
                  </SelectTrigger>
                  <SelectContent>
                    {getVersionsForTechnology(formData.technology).map(version => (
                      <SelectItem key={version} value={version}>
                        {version}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Course Code */}
            <div>
              <Label htmlFor="courseCode">Course Code (Optional)</Label>
              <Input
                id="courseCode"
                value={formData.courseCode}
                onChange={(e) => handleInputChange("courseCode", e.target.value)}
                placeholder="e.g., CS101"
              />
            </div>

            {/* Pre-installed Tools */}
            <div>
              <Label>Pre-installed Tools</Label>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 mt-2">
                {categories.map(category => (
                  <div key={category} className="space-y-2">
                    <h4 className="font-medium text-sm text-gray-600">{category}</h4>
                    {getToolsByCategory(category).map(tool => (
                      <div key={tool.id} className="flex items-center space-x-2">
                        <Checkbox
                          id={tool.id}
                          checked={formData.preInstalledTools.includes(tool.id)}
                          onCheckedChange={(checked) => handleToolToggle(tool.id, checked)}
                        />
                        <Label htmlFor={tool.id} className="text-sm">{tool.name}</Label>
                      </div>
                    ))}
                  </div>
                ))}
              </div>
            </div>

            {/* Environment Variables */}
            <div>
              <Label>Environment Variables</Label>
              <div className="space-y-2">
                <div className="grid grid-cols-2 gap-2">
                  <Input
                    placeholder="Variable name"
                    value={formData.envVarName || ""}
                    onChange={(e) => setFormData(prev => ({ ...prev, envVarName: e.target.value }))}
                  />
                  <Input
                    placeholder="Variable value"
                    value={formData.envVarValue || ""}
                    onChange={(e) => setFormData(prev => ({ ...prev, envVarValue: e.target.value }))}
                  />
                </div>
                <Button
                  type="button"
                  variant="outline"
                  size="sm"
                  onClick={() => {
                    if (formData.envVarName && formData.envVarValue) {
                      handleEnvironmentVariableChange(formData.envVarName, formData.envVarValue);
                      setFormData(prev => ({ ...prev, envVarName: "", envVarValue: "" }));
                    }
                  }}
                >
                  Add Environment Variable
                </Button>
                {Object.keys(formData.environmentVariables).length > 0 && (
                  <div className="mt-2">
                    <Label>Current Environment Variables:</Label>
                    <div className="space-y-1">
                      {Object.entries(formData.environmentVariables).map(([key, value]) => (
                        <div key={key} className="flex items-center justify-between bg-gray-50 p-2 rounded">
                          <span className="text-sm">{key}={value}</span>
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            onClick={() => {
                              const newEnvVars = { ...formData.environmentVariables };
                              delete newEnvVars[key];
                              setFormData(prev => ({ ...prev, environmentVariables: newEnvVars }));
                            }}
                          >
                            Remove
                          </Button>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            </div>

            {/* Startup Commands */}
            <div>
              <Label htmlFor="startupCommands">Startup Commands</Label>
              <Textarea
                id="startupCommands"
                value={formData.startupCommands}
                onChange={(e) => handleInputChange("startupCommands", e.target.value)}
                placeholder="Commands to run when container starts (one per line)"
                rows={3}
              />
            </div>

            {/* Public Template */}
            <div className="flex items-center space-x-2">
              <Checkbox
                id="isPublic"
                checked={formData.isPublic}
                onCheckedChange={(checked) => handleInputChange("isPublic", checked)}
              />
              <Label htmlFor="isPublic">Make this template public (available to other professors)</Label>
            </div>

            {/* Build Status */}
            {buildStatus && (
              <div className="p-4 bg-blue-50 border border-blue-200 rounded">
                <p className="text-blue-800">{buildStatus}</p>
              </div>
            )}

            {/* Submit Button */}
            <Button type="submit" disabled={loading} className="w-full">
              {loading ? "Creating..." : "Create Template & Build Image"}
            </Button>
          </form>
        </CardContent>
      </Card>
    </div>
  );
} 